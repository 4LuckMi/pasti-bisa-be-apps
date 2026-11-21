-- Add sources and workbook staging without changing historical attempt snapshots.
ALTER TABLE subtest ADD CONSTRAINT subtest_id_exam_type_unique UNIQUE(id,exam_type_id);
ALTER TABLE question ADD CONSTRAINT question_id_subtest_unique UNIQUE(id,subtest_id);
ALTER TABLE question_version ADD CONSTRAINT question_version_id_question_unique UNIQUE(id,question_id);

CREATE TABLE question_set (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    exam_type_id UUID NOT NULL REFERENCES exam_type(id),
    code VARCHAR(80) NOT NULL UNIQUE,
    source_year INTEGER NOT NULL CHECK(source_year BETWEEN 1900 AND 2200),
    title VARCHAR(180) NOT NULL,
    status VARCHAR(24) NOT NULL DEFAULT 'ACTIVE' CHECK(status IN ('ACTIVE','ARCHIVED')),
    created_by UUID REFERENCES app_user(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE(id,exam_type_id),
    UNIQUE(id,source_year)
);
CREATE INDEX question_set_type_year_idx ON question_set(exam_type_id,source_year);
CREATE TABLE question_set_item (
    question_set_id UUID NOT NULL,
    question_id UUID NOT NULL,
    exam_type_id UUID NOT NULL,
    subtest_id UUID NOT NULL,
    source_item_code VARCHAR(80) NOT NULL,
    PRIMARY KEY(question_set_id,question_id),
    UNIQUE(question_set_id,subtest_id,source_item_code),
    FOREIGN KEY(question_set_id,exam_type_id) REFERENCES question_set(id,exam_type_id),
    FOREIGN KEY(subtest_id,exam_type_id) REFERENCES subtest(id,exam_type_id),
    FOREIGN KEY(question_id,subtest_id) REFERENCES question(id,subtest_id)
);
CREATE INDEX question_set_item_question_idx ON question_set_item(question_id,question_set_id);

-- Existing source years remain provenance. Backfill a collection per exam type/year.
INSERT INTO question_set(exam_type_id,code,source_year,title)
SELECT DISTINCT s.exam_type_id,'LEGACY-'||s.exam_type_id::text||'-'||v.source_year,v.source_year,
       'Legacy question sources '||v.source_year
FROM question q JOIN subtest s ON s.id=q.subtest_id
JOIN LATERAL(SELECT source_year FROM question_version WHERE question_id=q.id ORDER BY version DESC LIMIT 1) v ON true
WHERE v.source_year BETWEEN 1900 AND 2200;
INSERT INTO question_set_item(question_set_id,question_id,exam_type_id,subtest_id,source_item_code)
SELECT qs.id,q.id,s.exam_type_id,q.subtest_id,q.code
FROM question q JOIN subtest s ON s.id=q.subtest_id
JOIN LATERAL(SELECT source_year FROM question_version WHERE question_id=q.id ORDER BY version DESC LIMIT 1) v ON true
JOIN question_set qs ON qs.code='LEGACY-'||s.exam_type_id::text||'-'||v.source_year;

ALTER TABLE import_batch ADD COLUMN question_set_id UUID REFERENCES question_set(id);
ALTER TABLE import_batch ADD COLUMN input_format VARCHAR(8) NOT NULL DEFAULT 'PDF' CHECK(input_format IN ('PDF','XLSX'));
ALTER TABLE import_batch ADD COLUMN mapping_hash CHAR(64);
ALTER TABLE import_batch ADD CONSTRAINT workbook_source_year_fk FOREIGN KEY(question_set_id,source_year) REFERENCES question_set(id,source_year);
ALTER TABLE import_batch ADD CONSTRAINT workbook_batch_scope CHECK(input_format<>'XLSX' OR
    (question_set_id IS NOT NULL AND subtest_id IS NULL AND topic_id IS NULL AND mapping_hash IS NOT NULL));
CREATE TABLE import_sheet (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    batch_id UUID NOT NULL REFERENCES import_batch(id),
    subtest_id UUID NOT NULL REFERENCES subtest(id),
    sheet_name VARCHAR(31) NOT NULL,
    sheet_index INTEGER CHECK(sheet_index>=0),
    status VARCHAR(24) NOT NULL DEFAULT 'QUEUED' CHECK(status IN ('QUEUED','READY','NEEDS_INPUT','FAILED')),
    candidate_count INTEGER NOT NULL DEFAULT 0,
    UNIQUE(batch_id,sheet_name),
    UNIQUE(batch_id,subtest_id),
    UNIQUE(id,batch_id)
);
ALTER TABLE import_candidate ALTER COLUMN source_page DROP NOT NULL;
ALTER TABLE import_candidate ADD COLUMN sheet_id UUID;
ALTER TABLE import_candidate ADD COLUMN source_row INTEGER CHECK(source_row>=2);
ALTER TABLE import_candidate ADD COLUMN source_item_code VARCHAR(80);
ALTER TABLE import_candidate ADD COLUMN topic_id UUID REFERENCES topic(id);
ALTER TABLE import_candidate ADD COLUMN difficulty VARCHAR(16) CHECK(difficulty IN ('EASY','MEDIUM','HARD'));
ALTER TABLE import_candidate ADD COLUMN expected_version_id UUID REFERENCES question_version(id);
ALTER TABLE import_candidate ADD COLUMN resolved_version_id UUID;
ALTER TABLE import_candidate ADD CONSTRAINT candidate_sheet_batch_fk FOREIGN KEY(sheet_id,batch_id) REFERENCES import_sheet(id,batch_id);
ALTER TABLE import_candidate ADD CONSTRAINT candidate_resolved_version_fk FOREIGN KEY(resolved_version_id,resolved_question_id)
    REFERENCES question_version(id,question_id);
ALTER TABLE import_candidate ADD CONSTRAINT candidate_resolved_question_required CHECK(resolved_version_id IS NULL OR resolved_question_id IS NOT NULL);
ALTER TABLE import_candidate ADD CONSTRAINT candidate_location CHECK(
    (sheet_id IS NULL AND source_page IS NOT NULL AND source_row IS NULL) OR
    (sheet_id IS NOT NULL AND source_page IS NULL AND source_row IS NOT NULL AND source_item_code IS NOT NULL));
CREATE UNIQUE INDEX candidate_sheet_row_unique ON import_candidate(sheet_id,source_row) WHERE sheet_id IS NOT NULL;
CREATE INDEX workbook_duplicate_idx ON import_batch(uploader_id,question_set_id,mapping_hash) WHERE input_format='XLSX';

CREATE TABLE blueprint_section_source (
    section_id UUID NOT NULL REFERENCES blueprint_section(id),
    question_set_id UUID NOT NULL REFERENCES question_set(id),
    PRIMARY KEY(section_id,question_set_id)
);
CREATE FUNCTION guard_section_source() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE sid UUID; version_id UUID; expected_type UUID; actual_type UUID; state VARCHAR(24);
BEGIN
    sid=CASE WHEN TG_OP='DELETE' THEN OLD.section_id ELSE NEW.section_id END;
    SELECT s.blueprint_version_id,b.exam_type_id,v.status INTO version_id,expected_type,state
    FROM blueprint_section s JOIN blueprint_version v ON v.id=s.blueprint_version_id JOIN blueprint b ON b.id=v.blueprint_id
    WHERE s.id=sid FOR UPDATE OF v;
    IF state<>'DRAFT' OR EXISTS(SELECT 1 FROM attempt WHERE blueprint_version_id=version_id)
       OR EXISTS(SELECT 1 FROM exam_form WHERE blueprint_version_id=version_id) THEN
        RAISE EXCEPTION 'Published blueprint sources are immutable' USING ERRCODE='23514';
    END IF;
    IF TG_OP='UPDATE' THEN RAISE EXCEPTION 'Replace draft source membership with delete and insert' USING ERRCODE='23514'; END IF;
    IF TG_OP<>'DELETE' THEN
        SELECT exam_type_id INTO actual_type FROM question_set WHERE id=NEW.question_set_id AND status='ACTIVE';
        IF actual_type IS DISTINCT FROM expected_type THEN
            RAISE EXCEPTION 'Source must be active and match blueprint exam type' USING ERRCODE='23514';
        END IF;
    END IF;
    IF TG_OP='DELETE' THEN RETURN OLD; END IF; RETURN NEW;
END; $$;
CREATE TRIGGER section_source_guard BEFORE INSERT OR UPDATE OR DELETE ON blueprint_section_source
FOR EACH ROW EXECUTE FUNCTION guard_section_source();
CREATE FUNCTION guard_source_identity() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF NEW.exam_type_id<>OLD.exam_type_id OR NEW.source_year<>OLD.source_year OR NEW.code<>OLD.code THEN
        RAISE EXCEPTION 'Source identity is immutable; create a new source' USING ERRCODE='23514';
    END IF; RETURN NEW;
END; $$;
CREATE TRIGGER source_identity_guard BEFORE UPDATE ON question_set FOR EACH ROW EXECUTE FUNCTION guard_source_identity();
CREATE FUNCTION validate_import_sheet_scope() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF NOT EXISTS(SELECT 1 FROM import_batch b JOIN question_set qs ON qs.id=b.question_set_id
        JOIN subtest s ON s.exam_type_id=qs.exam_type_id WHERE b.id=NEW.batch_id AND b.input_format='XLSX' AND s.id=NEW.subtest_id) THEN
        RAISE EXCEPTION 'Sheet subtest must match workbook source' USING ERRCODE='23514';
    END IF; RETURN NEW;
END; $$;
CREATE TRIGGER import_sheet_scope_guard BEFORE INSERT OR UPDATE OF batch_id,subtest_id ON import_sheet
FOR EACH ROW EXECUTE FUNCTION validate_import_sheet_scope();
CREATE FUNCTION validate_workbook_candidate_scope() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE sheet_subtest UUID;
BEGIN
    IF NEW.sheet_id IS NULL THEN RETURN NEW; END IF;
    SELECT subtest_id INTO sheet_subtest FROM import_sheet WHERE id=NEW.sheet_id;
    IF NEW.topic_id IS NOT NULL AND NOT EXISTS(SELECT 1 FROM topic WHERE id=NEW.topic_id AND subtest_id=sheet_subtest) THEN
        RAISE EXCEPTION 'Candidate topic must match sheet subtest' USING ERRCODE='23514';
    END IF;
    IF NEW.resolved_question_id IS NOT NULL AND NOT EXISTS(SELECT 1 FROM question WHERE id=NEW.resolved_question_id AND subtest_id=sheet_subtest) THEN
        RAISE EXCEPTION 'Candidate master must match sheet subtest' USING ERRCODE='23514';
    END IF;
    IF NEW.expected_version_id IS NOT NULL AND NOT EXISTS(SELECT 1 FROM question_version v JOIN question q ON q.id=v.question_id WHERE v.id=NEW.expected_version_id AND q.subtest_id=sheet_subtest) THEN
        RAISE EXCEPTION 'Expected version must match sheet subtest' USING ERRCODE='23514';
    END IF; RETURN NEW;
END; $$;
CREATE TRIGGER workbook_candidate_scope_guard BEFORE INSERT OR UPDATE ON import_candidate FOR EACH ROW EXECUTE FUNCTION validate_workbook_candidate_scope();

-- One reusable read model: latest published, active taxonomy, complete MCQ options.
CREATE VIEW published_question_pool AS
SELECT q.id AS question_id,q.subtest_id,q.topic_id,s.exam_type_id,v.id AS version_id,v.source_year,v.difficulty,v.shuffle_allowed,
       EXISTS(SELECT 1 FROM question_option o WHERE o.question_version_id=v.id AND o.score_weight>0) AS weighted_ready
FROM question q JOIN subtest s ON s.id=q.subtest_id AND s.active
JOIN exam_type e ON e.id=s.exam_type_id AND e.active JOIN exam_category c ON c.id=e.category_id AND c.active
LEFT JOIN topic t ON t.id=q.topic_id
JOIN LATERAL(SELECT * FROM question_version WHERE question_id=q.id AND status='PUBLISHED' ORDER BY version DESC LIMIT 1) v ON true
WHERE q.archived_at IS NULL AND (q.topic_id IS NULL OR t.active)
AND (SELECT count(*) FROM question_option o WHERE o.question_version_id=v.id) BETWEEN 2 AND 5
AND (SELECT count(*) FROM question_option o WHERE o.question_version_id=v.id AND o.correct_key)=1
AND NOT EXISTS(SELECT 1 FROM question_option o WHERE o.question_version_id=v.id AND (o.score_weight IS NULL OR o.score_weight<0));

CREATE FUNCTION question_section_available(section_uuid UUID) RETURNS BIGINT LANGUAGE sql STABLE AS $$
WITH definition AS (
    SELECT s.*,p.method FROM blueprint_section s JOIN blueprint_version v ON v.id=s.blueprint_version_id
    JOIN scoring_policy p ON p.id=v.scoring_policy_id AND p.status='ACTIVE' AND p.method IN ('BINARY','OPTION_WEIGHT')
    WHERE s.id=section_uuid
), eligible AS (
    SELECT pool.* FROM definition d JOIN published_question_pool pool ON pool.subtest_id=d.subtest_id
    WHERE (d.filters->>'topicId' IS NULL OR pool.topic_id=(d.filters->>'topicId')::uuid)
    AND (d.filters->>'sourceYear' IS NULL OR pool.source_year=(d.filters->>'sourceYear')::int)
    AND (d.method<>'OPTION_WEIGHT' OR pool.weighted_ready)
    AND (NOT EXISTS(SELECT 1 FROM blueprint_section_source ss WHERE ss.section_id=d.id)
      OR EXISTS(SELECT 1 FROM blueprint_section_source ss JOIN question_set qs ON qs.id=ss.question_set_id AND qs.status='ACTIVE'
        JOIN question_set_item i ON i.question_set_id=qs.id WHERE ss.section_id=d.id AND i.question_id=pool.question_id))
), quotas AS (
    SELECT q->>'difficulty' AS difficulty,(q->>'count')::int AS needed FROM definition d,
    jsonb_array_elements(coalesce(d.filters->'difficultyQuotas','[]'::jsonb)) q
)
SELECT CASE WHEN EXISTS(SELECT 1 FROM quotas) THEN
    (SELECT coalesce(sum(least(needed,(SELECT count(*) FROM eligible e WHERE e.difficulty=q.difficulty))),0) FROM quotas q)
    ELSE (SELECT count(*) FROM eligible) END;
$$;
