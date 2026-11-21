-- Additive internal operations. Existing published content and attempts are not rewritten.
ALTER TABLE app_user ADD COLUMN auth_revision BIGINT NOT NULL DEFAULT 0 CHECK (auth_revision >= 0);
ALTER TABLE question_version ADD COLUMN editor_revision BIGINT NOT NULL DEFAULT 0 CHECK (editor_revision >= 0);
ALTER TABLE plan ADD COLUMN family_code VARCHAR(80);
UPDATE plan SET family_code=code;
ALTER TABLE plan ALTER COLUMN family_code SET NOT NULL;
ALTER TABLE plan ADD COLUMN revision INTEGER NOT NULL DEFAULT 1 CHECK (revision > 0);
ALTER TABLE plan ADD CONSTRAINT plan_family_revision_unique UNIQUE(family_code,revision);

CREATE TABLE refund_request (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL REFERENCES purchase_order(id),
    requested_by UUID NOT NULL REFERENCES app_user(id),
    amount_minor BIGINT NOT NULL CHECK(amount_minor > 0),
    currency CHAR(3) NOT NULL,
    status VARCHAR(24) NOT NULL DEFAULT 'PENDING_PROVIDER'
        CHECK(status IN ('PENDING_PROVIDER','PROCESSING','SUCCEEDED','FAILED','CANCELLED')),
    reason VARCHAR(500) NOT NULL,
    idempotency_key VARCHAR(128) NOT NULL,
    request_id VARCHAR(100),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    completed_at TIMESTAMPTZ,
    UNIQUE(order_id,idempotency_key)
);
CREATE INDEX refund_order_status_idx ON refund_request(order_id,status);
CREATE INDEX audit_time_idx ON audit_event(occurred_at DESC,id DESC);

CREATE FUNCTION protect_plan_price_version() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF TG_OP='DELETE' OR ROW(NEW.code,NEW.name,NEW.months,NEW.amount_minor,NEW.currency,NEW.benefits,NEW.family_code,NEW.revision)
       IS DISTINCT FROM ROW(OLD.code,OLD.name,OLD.months,OLD.amount_minor,OLD.currency,OLD.benefits,OLD.family_code,OLD.revision) THEN
        RAISE EXCEPTION 'Plan prices and benefits are versioned; create a new plan revision' USING ERRCODE='23514';
    END IF;
    RETURN NEW;
END;
$$;
CREATE TRIGGER plan_price_version_guard BEFORE UPDATE OR DELETE ON plan
FOR EACH ROW EXECUTE FUNCTION protect_plan_price_version();

-- Direct changes by operators also invalidate existing login sessions.
CREATE FUNCTION invalidate_account_sessions() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF ROW(NEW.status,NEW.password_hash,NEW.email_canonical) IS DISTINCT FROM ROW(OLD.status,OLD.password_hash,OLD.email_canonical) THEN
        NEW.auth_revision := OLD.auth_revision + 1;
    END IF;
    RETURN NEW;
END;
$$;
CREATE TRIGGER account_session_revision BEFORE UPDATE ON app_user
FOR EACH ROW EXECUTE FUNCTION invalidate_account_sessions();
CREATE FUNCTION invalidate_role_sessions() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF TG_OP='DELETE' THEN
        UPDATE app_user SET auth_revision=auth_revision+1 WHERE id=OLD.user_id;
        RETURN OLD;
    END IF;
    UPDATE app_user SET auth_revision=auth_revision+1 WHERE id=NEW.user_id;
    IF TG_OP='UPDATE' AND OLD.user_id<>NEW.user_id THEN
        UPDATE app_user SET auth_revision=auth_revision+1 WHERE id=OLD.user_id;
    END IF;
    RETURN NEW;
END;
$$;
CREATE TRIGGER role_session_revision AFTER INSERT OR UPDATE OR DELETE ON user_role
FOR EACH ROW EXECUTE FUNCTION invalidate_role_sessions();
