-- Complete entities from the PastiSiap PRD while keeping V1 immutable.

CREATE TABLE target (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    kind VARCHAR(32) NOT NULL,
    institution VARCHAR(180) NOT NULL,
    program VARCHAR(180),
    source_status VARCHAR(32) NOT NULL DEFAULT 'USER_SELECTED',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT target_kind_check CHECK (kind IN ('CAMPUS', 'CIVIL_SERVICE', 'ACADEMY', 'SCHOOL', 'OTHER'))
);
CREATE INDEX target_kind_institution_idx ON target(kind, institution);

CREATE TABLE user_profile (
    user_id UUID PRIMARY KEY REFERENCES app_user(id) ON DELETE CASCADE,
    display_name VARCHAR(120) NOT NULL,
    target_id UUID REFERENCES target(id) ON DELETE SET NULL,
    leaderboard_opt_in BOOLEAN NOT NULL DEFAULT FALSE,
    consent JSONB NOT NULL DEFAULT '{}'::jsonb,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

INSERT INTO user_profile (user_id, display_name)
SELECT id, display_name FROM app_user
ON CONFLICT (user_id) DO NOTHING;

CREATE FUNCTION create_user_profile() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    INSERT INTO user_profile (user_id, display_name)
    VALUES (NEW.id, NEW.display_name)
    ON CONFLICT (user_id) DO NOTHING;
    RETURN NEW;
END;
$$;
CREATE TRIGGER app_user_create_profile
AFTER INSERT ON app_user
FOR EACH ROW EXECUTE FUNCTION create_user_profile();

CREATE TABLE topic (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    subtest_id UUID NOT NULL REFERENCES subtest(id),
    name VARCHAR(160) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    UNIQUE (subtest_id, name),
    UNIQUE (id, subtest_id)
);

ALTER TABLE question ADD COLUMN topic_id UUID;
ALTER TABLE question ADD CONSTRAINT question_topic_subtest_fk
    FOREIGN KEY (topic_id, subtest_id) REFERENCES topic(id, subtest_id);
CREATE INDEX question_topic_active_idx ON question(topic_id) WHERE archived_at IS NULL;

CREATE TABLE import_candidate (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    batch_id UUID NOT NULL REFERENCES import_batch(id) ON DELETE CASCADE,
    source_page INTEGER NOT NULL CHECK (source_page > 0),
    source_item_no INTEGER NOT NULL CHECK (source_item_no > 0),
    extracted_content JSONB NOT NULL,
    validation_errors JSONB NOT NULL DEFAULT '[]'::jsonb,
    resolved_question_id UUID REFERENCES question(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (batch_id, source_page, source_item_no)
);
CREATE INDEX import_candidate_batch_idx ON import_candidate(batch_id, source_page, source_item_no);

ALTER TABLE question ADD COLUMN candidate_id UUID REFERENCES import_candidate(id) ON DELETE SET NULL;
CREATE UNIQUE INDEX question_candidate_unique_idx ON question(candidate_id) WHERE candidate_id IS NOT NULL;

CREATE TABLE question_asset (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    question_version_id UUID NOT NULL REFERENCES question_version(id) ON DELETE RESTRICT,
    file_id UUID NOT NULL REFERENCES stored_file(id) ON DELETE RESTRICT,
    purpose VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (question_version_id, file_id, purpose)
);

CREATE TABLE scoring_policy (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(100) NOT NULL,
    version INTEGER NOT NULL CHECK (version > 0),
    method VARCHAR(48) NOT NULL,
    rules JSONB NOT NULL,
    aggregation VARCHAR(48) NOT NULL,
    scale NUMERIC(12, 4) NOT NULL,
    rounding VARCHAR(32) NOT NULL,
    code_hash VARCHAR(128) NOT NULL,
    status VARCHAR(24) NOT NULL DEFAULT 'DRAFT',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (code, version)
);

ALTER TABLE blueprint_version ADD CONSTRAINT blueprint_version_id_blueprint_unique UNIQUE (id, blueprint_id);
ALTER TABLE blueprint ADD COLUMN active_version_id UUID;
ALTER TABLE blueprint ADD CONSTRAINT blueprint_active_version_fk
    FOREIGN KEY (active_version_id, id)
    REFERENCES blueprint_version(id, blueprint_id) ON DELETE RESTRICT;

ALTER TABLE blueprint_version ADD COLUMN scoring_policy_id UUID;
INSERT INTO scoring_policy (code, version, method, rules, aggregation, scale, rounding, code_hash, status)
SELECT 'blueprint-version-' || id::text,
       1,
       COALESCE(scoring_policy->>'method', 'LEGACY'),
       scoring_policy,
       COALESCE(scoring_policy->>'aggregation', 'SUM'),
       COALESCE(NULLIF(scoring_policy->>'scale', '')::NUMERIC, 100),
       COALESCE(scoring_policy->>'rounding', 'HALF_UP'),
       md5(scoring_policy::text),
       'ACTIVE'
FROM blueprint_version;
UPDATE blueprint_version AS blueprint
SET scoring_policy_id = policy.id
FROM scoring_policy AS policy
WHERE policy.code = 'blueprint-version-' || blueprint.id::text;
ALTER TABLE blueprint_version ALTER COLUMN scoring_policy_id SET NOT NULL;
ALTER TABLE blueprint_version ADD CONSTRAINT blueprint_version_scoring_policy_fk
    FOREIGN KEY (scoring_policy_id) REFERENCES scoring_policy(id) ON DELETE RESTRICT;

ALTER TABLE score_run ADD COLUMN scoring_policy_id UUID;
UPDATE score_run AS score
SET scoring_policy_id = blueprint.scoring_policy_id
FROM attempt
JOIN blueprint_version AS blueprint ON blueprint.id = attempt.blueprint_version_id
WHERE attempt.id = score.attempt_id;
ALTER TABLE score_run ALTER COLUMN scoring_policy_id SET NOT NULL;
ALTER TABLE score_run ADD CONSTRAINT score_run_scoring_policy_fk
    FOREIGN KEY (scoring_policy_id) REFERENCES scoring_policy(id) ON DELETE RESTRICT;
CREATE INDEX score_run_attempt_time_idx ON score_run(attempt_id, computed_at DESC);

CREATE TABLE exam_form (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    blueprint_version_id UUID NOT NULL REFERENCES blueprint_version(id) ON DELETE RESTRICT,
    label VARCHAR(120) NOT NULL,
    status VARCHAR(24) NOT NULL DEFAULT 'DRAFT',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (blueprint_version_id, label),
    UNIQUE (id, blueprint_version_id)
);

ALTER TABLE attempt ADD COLUMN form_id UUID;
ALTER TABLE attempt ADD CONSTRAINT attempt_form_blueprint_fk
    FOREIGN KEY (form_id, blueprint_version_id) REFERENCES exam_form(id, blueprint_version_id) ON DELETE RESTRICT;

CREATE TABLE exam_form_item (
    form_id UUID NOT NULL REFERENCES exam_form(id) ON DELETE RESTRICT,
    question_version_id UUID NOT NULL REFERENCES question_version(id) ON DELETE RESTRICT,
    section_id UUID NOT NULL REFERENCES blueprint_section(id) ON DELETE RESTRICT,
    position INTEGER NOT NULL CHECK (position > 0),
    PRIMARY KEY (form_id, question_version_id),
    UNIQUE (form_id, position)
);

CREATE TABLE score_section (
    score_run_id UUID NOT NULL REFERENCES score_run(id) ON DELETE RESTRICT,
    subtest_id UUID NOT NULL REFERENCES subtest(id) ON DELETE RESTRICT,
    correct_count INTEGER NOT NULL CHECK (correct_count >= 0),
    wrong_count INTEGER NOT NULL CHECK (wrong_count >= 0),
    empty_count INTEGER NOT NULL CHECK (empty_count >= 0),
    points NUMERIC(12, 4) NOT NULL,
    max_points NUMERIC(12, 4) NOT NULL CHECK (max_points >= 0),
    accuracy NUMERIC(7, 4) NOT NULL CHECK (accuracy BETWEEN 0 AND 100),
    PRIMARY KEY (score_run_id, subtest_id)
);

ALTER TABLE purchase_order ADD COLUMN plan_code_snapshot VARCHAR(80);
ALTER TABLE purchase_order ADD COLUMN months_snapshot INTEGER;
ALTER TABLE purchase_order ADD COLUMN benefits_snapshot JSONB;
ALTER TABLE purchase_order ADD COLUMN provider VARCHAR(48);
ALTER TABLE purchase_order ADD COLUMN expires_at TIMESTAMPTZ;
ALTER TABLE purchase_order ADD COLUMN updated_at TIMESTAMPTZ NOT NULL DEFAULT now();
UPDATE purchase_order AS orders
SET plan_code_snapshot = plans.code,
    months_snapshot = plans.months,
    benefits_snapshot = plans.benefits
FROM plan AS plans
WHERE plans.id = orders.plan_id;
ALTER TABLE purchase_order ALTER COLUMN plan_code_snapshot SET NOT NULL;
ALTER TABLE purchase_order ALTER COLUMN months_snapshot SET NOT NULL;
ALTER TABLE purchase_order ALTER COLUMN benefits_snapshot SET NOT NULL;
ALTER TABLE purchase_order ADD CONSTRAINT purchase_order_months_snapshot_check CHECK (months_snapshot > 0);
CREATE INDEX purchase_order_user_created_idx ON purchase_order(user_id, created_at DESC);
CREATE INDEX purchase_order_status_created_idx ON purchase_order(status, created_at DESC);

ALTER TABLE payment_event ADD COLUMN signature_status VARCHAR(24) NOT NULL DEFAULT 'UNVERIFIED';
ALTER TABLE payment_event ADD COLUMN payload_redacted JSONB NOT NULL DEFAULT '{}'::jsonb;
CREATE INDEX payment_event_order_received_idx ON payment_event(order_id, received_at DESC);

CREATE TABLE subscription (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES app_user(id) ON DELETE RESTRICT,
    starts_at TIMESTAMPTZ NOT NULL,
    ends_at TIMESTAMPTZ NOT NULL,
    status VARCHAR(24) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CHECK (ends_at > starts_at),
    UNIQUE (user_id, ends_at)
);
CREATE INDEX subscription_user_ends_idx ON subscription(user_id, ends_at DESC);

ALTER TABLE entitlement_grant ADD COLUMN subscription_id UUID REFERENCES subscription(id) ON DELETE RESTRICT;
CREATE INDEX entitlement_user_interval_idx ON entitlement_grant(user_id, starts_at, ends_at);

CREATE TABLE leaderboard_cohort (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    form_id UUID NOT NULL REFERENCES exam_form(id) ON DELETE RESTRICT,
    scoring_policy_id UUID NOT NULL REFERENCES scoring_policy(id) ON DELETE RESTRICT,
    period_start TIMESTAMPTZ NOT NULL,
    period_end TIMESTAMPTZ NOT NULL,
    attempt_policy VARCHAR(32) NOT NULL,
    status VARCHAR(24) NOT NULL DEFAULT 'DRAFT',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CHECK (period_end > period_start),
    UNIQUE (form_id, scoring_policy_id, period_start, period_end, attempt_policy)
);

CREATE TABLE leaderboard_entry (
    cohort_id UUID NOT NULL REFERENCES leaderboard_cohort(id) ON DELETE RESTRICT,
    user_id UUID NOT NULL REFERENCES app_user(id) ON DELETE RESTRICT,
    score_run_id UUID NOT NULL REFERENCES score_run(id) ON DELETE RESTRICT,
    rank INTEGER NOT NULL CHECK (rank > 0),
    display_name_snapshot VARCHAR(120) NOT NULL,
    tie_break_data JSONB NOT NULL DEFAULT '{}'::jsonb,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (cohort_id, user_id)
);
CREATE INDEX leaderboard_entry_rank_idx ON leaderboard_entry(cohort_id, rank);

CREATE TABLE password_reset_token (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    token_hash CHAR(64) NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL,
    consumed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX password_reset_user_created_idx ON password_reset_token(user_id, created_at DESC);

CREATE TABLE email_verification_token (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    token_hash CHAR(64) NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL,
    consumed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX email_verification_user_created_idx ON email_verification_token(user_id, created_at DESC);

CREATE INDEX stored_file_sha256_idx ON stored_file(sha256);
CREATE INDEX import_batch_status_created_idx ON import_batch(status, created_at);
CREATE INDEX question_option_version_idx ON question_option(question_version_id);
CREATE INDEX attempt_item_question_idx ON attempt_item(question_id, attempt_id);
CREATE INDEX quota_usage_user_day_idx ON quota_usage(user_id, day_wib);
CREATE INDEX audit_actor_time_idx ON audit_event(actor_id, occurred_at DESC);
ALTER TABLE audit_event ADD COLUMN before_redacted JSONB;
ALTER TABLE audit_event ADD COLUMN after_redacted JSONB;

ALTER TABLE import_batch ADD COLUMN candidate_count INTEGER NOT NULL DEFAULT 0 CHECK (candidate_count >= 0);
ALTER TABLE import_batch ADD COLUMN valid_count INTEGER NOT NULL DEFAULT 0 CHECK (valid_count >= 0);
ALTER TABLE import_batch ADD COLUMN invalid_count INTEGER NOT NULL DEFAULT 0 CHECK (invalid_count >= 0);
ALTER TABLE import_batch ADD COLUMN updated_at TIMESTAMPTZ NOT NULL DEFAULT now();

-- Enforce cross-row relationships that cannot be expressed by simple CHECK constraints.
ALTER TABLE question_version ADD CONSTRAINT question_version_question_id_id_unique UNIQUE (question_id, id);
ALTER TABLE attempt_item ADD CONSTRAINT attempt_item_version_belongs_to_question_fk
    FOREIGN KEY (question_id, question_version_id)
    REFERENCES question_version(question_id, id) ON DELETE RESTRICT;
ALTER TABLE attempt ADD CONSTRAINT attempt_id_user_unique UNIQUE (id, user_id);
ALTER TABLE quota_usage ADD CONSTRAINT quota_usage_attempt_owner_fk
    FOREIGN KEY (attempt_id, user_id) REFERENCES attempt(id, user_id) ON DELETE RESTRICT;

CREATE FUNCTION validate_blueprint_section_subtest() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE blueprint_exam_type UUID;
DECLARE section_exam_type UUID;
BEGIN
    SELECT exam_type_id INTO blueprint_exam_type
    FROM blueprint_version version
    JOIN blueprint ON blueprint.id = version.blueprint_id
    WHERE version.id = NEW.blueprint_version_id;
    SELECT exam_type_id INTO section_exam_type FROM subtest WHERE id = NEW.subtest_id;
    IF blueprint_exam_type IS NULL OR section_exam_type IS NULL OR blueprint_exam_type <> section_exam_type THEN
        RAISE EXCEPTION 'Blueprint section subtest must belong to the blueprint exam type'
            USING ERRCODE = '23514';
    END IF;
    RETURN NEW;
END;
$$;
CREATE TRIGGER blueprint_section_subtest_guard
BEFORE INSERT OR UPDATE OF blueprint_version_id, subtest_id ON blueprint_section
FOR EACH ROW EXECUTE FUNCTION validate_blueprint_section_subtest();

CREATE FUNCTION validate_attempt_item_snapshot() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE question_family UUID;
DECLARE item_subtest UUID;
DECLARE section_subtest UUID;
DECLARE section_version UUID;
DECLARE attempt_version UUID;
DECLARE question_status VARCHAR(24);
BEGIN
    SELECT question.id, question.subtest_id, version.status INTO question_family, item_subtest, question_status
    FROM question_version version
    JOIN question ON question.id = version.question_id
    WHERE version.id = NEW.question_version_id;
    IF question_family IS NULL OR question_family <> NEW.question_id THEN
        RAISE EXCEPTION 'Attempt item question version must belong to its question'
            USING ERRCODE = '23514';
    END IF;
    IF question_status <> 'PUBLISHED' THEN
        RAISE EXCEPTION 'Attempts can only include published question versions'
            USING ERRCODE = '23514';
    END IF;
    SELECT blueprint_version_id, subtest_id INTO section_version, section_subtest
    FROM blueprint_section WHERE id = NEW.section_id;
    SELECT blueprint_version_id INTO attempt_version FROM attempt WHERE id = NEW.attempt_id;
    IF section_version IS NULL OR attempt_version IS NULL OR section_version <> attempt_version THEN
        RAISE EXCEPTION 'Attempt item section must belong to the attempt blueprint version'
            USING ERRCODE = '23514';
    END IF;
    IF item_subtest IS NULL OR section_subtest IS NULL OR item_subtest <> section_subtest THEN
        RAISE EXCEPTION 'Attempt item question must belong to the selected section subtest'
            USING ERRCODE = '23514';
    END IF;
    RETURN NEW;
END;
$$;
CREATE TRIGGER attempt_item_snapshot_guard
BEFORE INSERT OR UPDATE OF attempt_id, question_id, question_version_id, section_id ON attempt_item
FOR EACH ROW EXECUTE FUNCTION validate_attempt_item_snapshot();

CREATE FUNCTION validate_answer_option_version() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE item_version UUID;
DECLARE option_version UUID;
BEGIN
    IF NEW.selected_option_id IS NULL THEN
        RETURN NEW;
    END IF;
    SELECT question_version_id INTO item_version
    FROM attempt_item WHERE id = NEW.attempt_item_id;
    SELECT question_version_id INTO option_version
    FROM question_option WHERE id = NEW.selected_option_id;
    IF item_version IS NULL OR option_version IS NULL OR item_version <> option_version THEN
        RAISE EXCEPTION 'Selected answer option must belong to the attempt item question version'
            USING ERRCODE = '23514';
    END IF;
    RETURN NEW;
END;
$$;
CREATE TRIGGER answer_option_version_guard
BEFORE INSERT OR UPDATE OF attempt_item_id, selected_option_id ON answer
FOR EACH ROW EXECUTE FUNCTION validate_answer_option_version();

CREATE FUNCTION validate_exam_form_item() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE form_version UUID;
DECLARE section_version UUID;
DECLARE section_subtest UUID;
DECLARE question_subtest UUID;
BEGIN
    SELECT blueprint_version_id INTO form_version FROM exam_form WHERE id = NEW.form_id;
    SELECT blueprint_version_id, subtest_id INTO section_version, section_subtest
    FROM blueprint_section WHERE id = NEW.section_id;
    SELECT question.subtest_id INTO question_subtest
    FROM question_version version
    JOIN question ON question.id = version.question_id
    WHERE version.id = NEW.question_version_id;
    IF form_version IS NULL OR section_version IS NULL OR form_version <> section_version THEN
        RAISE EXCEPTION 'Exam form section must belong to the form blueprint version'
            USING ERRCODE = '23514';
    END IF;
    IF question_subtest IS NULL OR section_subtest <> question_subtest THEN
        RAISE EXCEPTION 'Exam form question must belong to the selected section subtest'
            USING ERRCODE = '23514';
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM question_version
        WHERE id = NEW.question_version_id AND status = 'PUBLISHED'
    ) THEN
        RAISE EXCEPTION 'Exam form can only reference published question versions'
            USING ERRCODE = '23514';
    END IF;
    RETURN NEW;
END;
$$;
CREATE TRIGGER exam_form_item_guard
BEFORE INSERT OR UPDATE OF form_id, question_version_id, section_id ON exam_form_item
FOR EACH ROW EXECUTE FUNCTION validate_exam_form_item();

CREATE FUNCTION prevent_published_question_version_changes() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF OLD.status = 'PUBLISHED' OR EXISTS (
        SELECT 1 FROM attempt_item WHERE question_version_id = OLD.id
    ) OR EXISTS (
        SELECT 1 FROM exam_form_item WHERE question_version_id = OLD.id
    ) THEN
        RAISE EXCEPTION 'A published or referenced question version is immutable; create a new version'
            USING ERRCODE = '23514';
    END IF;
    IF TG_OP = 'DELETE' THEN
        RETURN OLD;
    END IF;
    RETURN NEW;
END;
$$;
CREATE TRIGGER question_version_immutable_guard
BEFORE UPDATE OR DELETE ON question_version
FOR EACH ROW EXECUTE FUNCTION prevent_published_question_version_changes();

CREATE FUNCTION prevent_published_blueprint_version_changes() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF OLD.status IN ('ACTIVE', 'PUBLISHED') OR EXISTS (
        SELECT 1 FROM attempt WHERE blueprint_version_id = OLD.id
    ) OR EXISTS (
        SELECT 1 FROM exam_form WHERE blueprint_version_id = OLD.id
    ) THEN
        RAISE EXCEPTION 'An active or referenced blueprint version is immutable; create a new version'
            USING ERRCODE = '23514';
    END IF;
    IF TG_OP = 'DELETE' THEN
        RETURN OLD;
    END IF;
    RETURN NEW;
END;
$$;
CREATE TRIGGER blueprint_version_immutable_guard
BEFORE UPDATE OR DELETE ON blueprint_version
FOR EACH ROW EXECUTE FUNCTION prevent_published_blueprint_version_changes();

CREATE FUNCTION prevent_referenced_scoring_policy_changes() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF EXISTS (SELECT 1 FROM blueprint_version WHERE scoring_policy_id = OLD.id)
       OR EXISTS (SELECT 1 FROM score_run WHERE scoring_policy_id = OLD.id) THEN
        RAISE EXCEPTION 'A scoring policy used by a blueprint or result is immutable; create a new policy version'
            USING ERRCODE = '23514';
    END IF;
    IF TG_OP = 'DELETE' THEN
        RETURN OLD;
    END IF;
    RETURN NEW;
END;
$$;
CREATE TRIGGER scoring_policy_immutable_guard
BEFORE UPDATE OR DELETE ON scoring_policy
FOR EACH ROW EXECUTE FUNCTION prevent_referenced_scoring_policy_changes();

CREATE FUNCTION prevent_published_question_option_changes() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE option_version UUID;
BEGIN
    IF TG_OP = 'DELETE' THEN
        option_version := OLD.question_version_id;
    ELSE
        option_version := NEW.question_version_id;
    END IF;
    IF EXISTS (
        SELECT 1 FROM question_version
        WHERE id = option_version AND status = 'PUBLISHED'
    ) OR EXISTS (
        SELECT 1 FROM attempt_item WHERE question_version_id = option_version
    ) OR EXISTS (
        SELECT 1 FROM exam_form_item WHERE question_version_id = option_version
    ) THEN
        RAISE EXCEPTION 'Options for a published or referenced question version are immutable'
            USING ERRCODE = '23514';
    END IF;
    IF TG_OP = 'UPDATE' AND OLD.question_version_id <> NEW.question_version_id AND (
        EXISTS (SELECT 1 FROM question_version WHERE id = OLD.question_version_id AND status = 'PUBLISHED')
        OR EXISTS (SELECT 1 FROM attempt_item WHERE question_version_id = OLD.question_version_id)
        OR EXISTS (SELECT 1 FROM exam_form_item WHERE question_version_id = OLD.question_version_id)
    ) THEN
        RAISE EXCEPTION 'An option cannot be moved out of a published or referenced question version'
            USING ERRCODE = '23514';
    END IF;
    IF TG_OP = 'DELETE' THEN
        RETURN OLD;
    END IF;
    RETURN NEW;
END;
$$;
CREATE TRIGGER question_option_immutable_guard
BEFORE INSERT OR UPDATE OR DELETE ON question_option
FOR EACH ROW EXECUTE FUNCTION prevent_published_question_option_changes();

CREATE FUNCTION prevent_frozen_blueprint_section_changes() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE section_version UUID;
BEGIN
    IF TG_OP = 'DELETE' OR TG_OP = 'UPDATE' THEN
        section_version := OLD.blueprint_version_id;
        IF EXISTS (
            SELECT 1 FROM blueprint_version
            WHERE id = section_version AND status IN ('ACTIVE', 'PUBLISHED')
        ) OR EXISTS (
            SELECT 1 FROM attempt WHERE blueprint_version_id = section_version
        ) OR EXISTS (
            SELECT 1 FROM exam_form WHERE blueprint_version_id = section_version
        ) THEN
            RAISE EXCEPTION 'Sections for an active or referenced blueprint version are immutable'
                USING ERRCODE = '23514';
        END IF;
    END IF;
    IF TG_OP = 'INSERT' OR TG_OP = 'UPDATE' THEN
        section_version := NEW.blueprint_version_id;
        IF EXISTS (
            SELECT 1 FROM blueprint_version
            WHERE id = section_version AND status IN ('ACTIVE', 'PUBLISHED')
        ) OR EXISTS (
            SELECT 1 FROM attempt WHERE blueprint_version_id = section_version
        ) OR EXISTS (
            SELECT 1 FROM exam_form WHERE blueprint_version_id = section_version
        ) THEN
            RAISE EXCEPTION 'Sections for an active or referenced blueprint version are immutable'
                USING ERRCODE = '23514';
        END IF;
    END IF;
    IF TG_OP = 'DELETE' THEN
        RETURN OLD;
    END IF;
    RETURN NEW;
END;
$$;
CREATE TRIGGER blueprint_section_immutable_guard
BEFORE INSERT OR UPDATE OR DELETE ON blueprint_section
FOR EACH ROW EXECUTE FUNCTION prevent_frozen_blueprint_section_changes();

CREATE FUNCTION prevent_frozen_exam_form_item_changes() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE form_id_value UUID;
DECLARE form_status VARCHAR(24);
BEGIN
    IF TG_OP = 'DELETE' OR TG_OP = 'UPDATE' THEN
        form_id_value := OLD.form_id;
        SELECT status INTO form_status FROM exam_form WHERE id = form_id_value;
        IF form_status IS DISTINCT FROM 'DRAFT' THEN
            RAISE EXCEPTION 'Items for an active or frozen exam form are immutable'
                USING ERRCODE = '23514';
        END IF;
    END IF;
    IF TG_OP = 'INSERT' OR TG_OP = 'UPDATE' THEN
        form_id_value := NEW.form_id;
        SELECT status INTO form_status FROM exam_form WHERE id = form_id_value;
        IF form_status IS DISTINCT FROM 'DRAFT' THEN
            RAISE EXCEPTION 'Items for an active or frozen exam form are immutable'
                USING ERRCODE = '23514';
        END IF;
    END IF;
    IF TG_OP = 'DELETE' THEN
        RETURN OLD;
    END IF;
    RETURN NEW;
END;
$$;
CREATE TRIGGER exam_form_item_immutable_guard
BEFORE INSERT OR UPDATE OR DELETE ON exam_form_item
FOR EACH ROW EXECUTE FUNCTION prevent_frozen_exam_form_item_changes();

CREATE FUNCTION prevent_frozen_exam_form_changes() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF (TG_OP = 'UPDATE' OR TG_OP = 'DELETE') AND (
        OLD.status <> 'DRAFT'
        OR EXISTS (SELECT 1 FROM attempt WHERE form_id = OLD.id)
    ) THEN
        RAISE EXCEPTION 'An active or referenced exam form is immutable'
            USING ERRCODE = '23514';
    END IF;
    IF TG_OP = 'DELETE' THEN
        RETURN OLD;
    END IF;
    RETURN NEW;
END;
$$;
CREATE TRIGGER exam_form_immutable_guard
BEFORE UPDATE OR DELETE ON exam_form
FOR EACH ROW EXECUTE FUNCTION prevent_frozen_exam_form_changes();

CREATE FUNCTION validate_attempt_blueprint_and_form() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE blueprint_state VARCHAR(24);
DECLARE form_state VARCHAR(24);
BEGIN
    SELECT status INTO blueprint_state
    FROM blueprint_version WHERE id = NEW.blueprint_version_id;
    IF blueprint_state IS NULL OR blueprint_state NOT IN ('ACTIVE', 'PUBLISHED') THEN
        RAISE EXCEPTION 'Attempts can only use an active blueprint version'
            USING ERRCODE = '23514';
    END IF;
    IF NEW.form_id IS NOT NULL THEN
        SELECT status INTO form_state FROM exam_form
        WHERE id = NEW.form_id AND blueprint_version_id = NEW.blueprint_version_id;
        IF form_state IS NULL OR form_state NOT IN ('ACTIVE', 'PUBLISHED') THEN
            RAISE EXCEPTION 'Attempts can only use an active exam form for their blueprint version'
                USING ERRCODE = '23514';
        END IF;
    END IF;
    RETURN NEW;
END;
$$;
CREATE TRIGGER attempt_blueprint_form_guard
BEFORE INSERT OR UPDATE OF blueprint_version_id, form_id ON attempt
FOR EACH ROW EXECUTE FUNCTION validate_attempt_blueprint_and_form();

CREATE FUNCTION validate_entitlement_subscription() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF NEW.subscription_id IS NOT NULL AND NOT EXISTS (
        SELECT 1 FROM subscription
        WHERE id = NEW.subscription_id
          AND user_id = NEW.user_id
          AND starts_at <= NEW.starts_at
          AND ends_at >= NEW.ends_at
    ) THEN
        RAISE EXCEPTION 'Entitlement grant must fit within the matching user subscription interval'
            USING ERRCODE = '23514';
    END IF;
    RETURN NEW;
END;
$$;
CREATE TRIGGER entitlement_subscription_guard
BEFORE INSERT OR UPDATE OF user_id, subscription_id, starts_at, ends_at ON entitlement_grant
FOR EACH ROW EXECUTE FUNCTION validate_entitlement_subscription();

CREATE FUNCTION validate_leaderboard_entry() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE cohort_policy UUID;
DECLARE cohort_form UUID;
DECLARE result_policy UUID;
DECLARE result_user UUID;
DECLARE result_form UUID;
DECLARE user_opted_in BOOLEAN;
BEGIN
    SELECT scoring_policy_id, form_id INTO cohort_policy, cohort_form
    FROM leaderboard_cohort WHERE id = NEW.cohort_id;
    SELECT score.scoring_policy_id, attempt.user_id, attempt.form_id
    INTO result_policy, result_user, result_form
    FROM score_run AS score
    JOIN attempt ON attempt.id = score.attempt_id
    WHERE score.id = NEW.score_run_id;
    SELECT leaderboard_opt_in INTO user_opted_in FROM user_profile WHERE user_id = NEW.user_id;
    IF cohort_policy IS NULL OR result_policy <> cohort_policy
       OR result_user <> NEW.user_id OR result_form <> cohort_form
       OR user_opted_in IS DISTINCT FROM TRUE THEN
        RAISE EXCEPTION 'Leaderboard entry requires a matching result and an opted-in user'
            USING ERRCODE = '23514';
    END IF;
    RETURN NEW;
END;
$$;
CREATE TRIGGER leaderboard_entry_integrity_guard
BEFORE INSERT OR UPDATE OF cohort_id, user_id, score_run_id ON leaderboard_entry
FOR EACH ROW EXECUTE FUNCTION validate_leaderboard_entry();
