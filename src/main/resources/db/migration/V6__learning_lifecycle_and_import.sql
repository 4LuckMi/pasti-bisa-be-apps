ALTER TABLE blueprint ADD COLUMN mode VARCHAR(16) NOT NULL DEFAULT 'TRYOUT'
    CHECK (mode IN ('TRYOUT', 'PRACTICE'));
ALTER TABLE blueprint ADD COLUMN demo BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE attempt ADD COLUMN request_hash CHAR(64);
ALTER TABLE attempt ADD COLUMN terminal_reason VARCHAR(24);
ALTER TABLE question_version ADD COLUMN source_title VARCHAR(180);
ALTER TABLE question_version ADD COLUMN source_page INTEGER CHECK (source_page > 0);
ALTER TABLE stored_file ADD COLUMN original_name VARCHAR(180);
ALTER TABLE stored_file ADD COLUMN purpose VARCHAR(24) NOT NULL DEFAULT 'QUESTION_ASSET'
    CHECK (purpose IN ('QUESTION_ASSET', 'IMPORT_SOURCE'));
ALTER TABLE import_batch ADD COLUMN subtest_id UUID REFERENCES subtest(id);
ALTER TABLE import_batch ADD COLUMN topic_id UUID;
ALTER TABLE import_batch ADD CONSTRAINT import_batch_topic_scope_fk
    FOREIGN KEY (topic_id, subtest_id) REFERENCES topic(id, subtest_id);
ALTER TABLE import_batch ADD COLUMN source_title VARCHAR(180);
ALTER TABLE import_batch ADD COLUMN source_year INTEGER CHECK (source_year BETWEEN 1900 AND 2200);
ALTER TABLE import_batch ADD COLUMN page_count INTEGER CHECK (page_count > 0);
ALTER TABLE import_batch ADD COLUMN error_code VARCHAR(64);
ALTER TABLE import_candidate ADD COLUMN draft_content JSONB;
ALTER TABLE job ADD COLUMN lease_token UUID;
CREATE INDEX import_batch_checksum_scope_idx ON stored_file(uploaded_by, sha256) WHERE purpose = 'IMPORT_SOURCE';
CREATE INDEX attempt_deadline_idx ON attempt(deadline_at) WHERE status = 'IN_PROGRESS';
CREATE INDEX attempt_item_seen_idx ON attempt_item(question_id, attempt_id);

-- Snapshot item/answer writes must be protected even if an API implementation is changed later.
CREATE FUNCTION guard_frozen_attempt_item() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    RAISE EXCEPTION 'Attempt item snapshots are immutable' USING ERRCODE = '23514';
END;
$$;
CREATE TRIGGER attempt_item_immutable_guard BEFORE UPDATE OR DELETE ON attempt_item
FOR EACH ROW EXECUTE FUNCTION guard_frozen_attempt_item();

CREATE FUNCTION guard_answer_attempt_state() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE state VARCHAR(24); deadline TIMESTAMPTZ;
BEGIN
    IF TG_OP = 'DELETE' THEN
        RAISE EXCEPTION 'Answer history cannot be deleted' USING ERRCODE = '23514';
    END IF;
    SELECT a.status, a.deadline_at INTO state, deadline FROM attempt a
    JOIN attempt_item item ON item.attempt_id = a.id WHERE item.id = NEW.attempt_item_id;
    IF state IS DISTINCT FROM 'IN_PROGRESS' OR deadline <= statement_timestamp() THEN
        RAISE EXCEPTION 'Attempt is no longer accepting answers' USING ERRCODE = '23514';
    END IF;
    IF TG_OP = 'UPDATE' AND NEW.revision <> OLD.revision + 1 THEN
        RAISE EXCEPTION 'Answer revision must advance by one' USING ERRCODE = '23514';
    END IF;
    RETURN NEW;
END;
$$;
CREATE TRIGGER answer_attempt_state_guard BEFORE INSERT OR UPDATE OR DELETE ON answer
FOR EACH ROW EXECUTE FUNCTION guard_answer_attempt_state();

CREATE FUNCTION guard_question_asset_source() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF TG_OP <> 'DELETE' AND EXISTS (SELECT 1 FROM stored_file WHERE id=NEW.file_id AND purpose='IMPORT_SOURCE') THEN
        RAISE EXCEPTION 'Import source PDFs cannot be exposed as student assets' USING ERRCODE='23514';
    END IF;
    IF TG_OP IN ('UPDATE','DELETE') AND EXISTS (
        SELECT 1 FROM question_version WHERE id=OLD.question_version_id AND status='PUBLISHED'
    ) THEN
        RAISE EXCEPTION 'Published question assets are immutable' USING ERRCODE='23514';
    END IF;
    IF TG_OP <> 'DELETE' AND EXISTS (
        SELECT 1 FROM question_version WHERE id=NEW.question_version_id AND status='PUBLISHED'
    ) THEN
        RAISE EXCEPTION 'Create assets before publishing a question version' USING ERRCODE='23514';
    END IF;
    IF TG_OP='DELETE' THEN RETURN OLD; END IF;
    RETURN NEW;
END;
$$;
CREATE TRIGGER question_asset_source_guard BEFORE INSERT OR UPDATE OR DELETE ON question_asset
FOR EACH ROW EXECUTE FUNCTION guard_question_asset_source();
