CREATE TABLE app_user (
    id UUID PRIMARY KEY,
    email VARCHAR(320) NOT NULL,
    email_canonical VARCHAR(320) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    display_name VARCHAR(120) NOT NULL,
    status VARCHAR(24) NOT NULL DEFAULT 'ACTIVE',
    email_verified_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT app_user_status_check CHECK (status IN ('ACTIVE', 'SUSPENDED', 'PENDING_VERIFICATION'))
);

CREATE TABLE user_role (
    user_id UUID NOT NULL REFERENCES app_user(id),
    role_code VARCHAR(32) NOT NULL,
    PRIMARY KEY (user_id, role_code),
    CONSTRAINT user_role_code_check CHECK (role_code IN ('CUSTOMER', 'QUESTION_STAFF', 'PLATFORM_ADMIN'))
);

CREATE TABLE exam_category (
    id UUID PRIMARY KEY,
    code VARCHAR(40) NOT NULL UNIQUE,
    name VARCHAR(120) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE exam_type (
    id UUID PRIMARY KEY,
    category_id UUID NOT NULL REFERENCES exam_category(id),
    code VARCHAR(40) NOT NULL,
    name VARCHAR(120) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    UNIQUE (category_id, code)
);

CREATE TABLE subtest (
    id UUID PRIMARY KEY,
    exam_type_id UUID NOT NULL REFERENCES exam_type(id),
    code VARCHAR(40) NOT NULL,
    name VARCHAR(120) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    UNIQUE (exam_type_id, code)
);

CREATE TABLE question (
    id UUID PRIMARY KEY,
    subtest_id UUID NOT NULL REFERENCES subtest(id),
    code VARCHAR(80) NOT NULL UNIQUE,
    archived_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE question_version (
    id UUID PRIMARY KEY,
    question_id UUID NOT NULL REFERENCES question(id),
    version INTEGER NOT NULL CHECK (version > 0),
    stem JSONB NOT NULL,
    explanation TEXT,
    source_year INTEGER,
    difficulty VARCHAR(24),
    status VARCHAR(24) NOT NULL,
    shuffle_allowed BOOLEAN NOT NULL DEFAULT TRUE,
    created_by UUID REFERENCES app_user(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (question_id, version)
);

CREATE TABLE question_option (
    id UUID PRIMARY KEY,
    question_version_id UUID NOT NULL REFERENCES question_version(id),
    option_code VARCHAR(32) NOT NULL,
    content JSONB NOT NULL,
    original_position INTEGER NOT NULL,
    correct_key BOOLEAN NOT NULL DEFAULT FALSE,
    score_weight NUMERIC(10, 4) NOT NULL DEFAULT 0,
    UNIQUE (question_version_id, option_code),
    UNIQUE (question_version_id, original_position)
);

CREATE INDEX question_version_status_idx ON question_version(status);
CREATE INDEX question_subtest_active_idx ON question(subtest_id) WHERE archived_at IS NULL;

CREATE TABLE blueprint (
    id UUID PRIMARY KEY,
    exam_type_id UUID NOT NULL REFERENCES exam_type(id),
    code VARCHAR(80) NOT NULL,
    title VARCHAR(180) NOT NULL,
    status VARCHAR(24) NOT NULL DEFAULT 'DRAFT',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (exam_type_id, code)
);

CREATE TABLE blueprint_version (
    id UUID PRIMARY KEY,
    blueprint_id UUID NOT NULL REFERENCES blueprint(id),
    version INTEGER NOT NULL CHECK (version > 0),
    duration_seconds INTEGER NOT NULL CHECK (duration_seconds > 0),
    scoring_policy JSONB NOT NULL,
    status VARCHAR(24) NOT NULL DEFAULT 'DRAFT',
    effective_at TIMESTAMPTZ,
    UNIQUE (blueprint_id, version)
);

CREATE TABLE blueprint_section (
    id UUID PRIMARY KEY,
    blueprint_version_id UUID NOT NULL REFERENCES blueprint_version(id),
    subtest_id UUID NOT NULL REFERENCES subtest(id),
    position INTEGER NOT NULL,
    item_count INTEGER NOT NULL CHECK (item_count > 0),
    filters JSONB NOT NULL DEFAULT '{}'::jsonb,
    UNIQUE (blueprint_version_id, position)
);

CREATE TABLE attempt (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES app_user(id),
    blueprint_version_id UUID NOT NULL REFERENCES blueprint_version(id),
    mode VARCHAR(24) NOT NULL,
    status VARCHAR(24) NOT NULL,
    idempotency_key VARCHAR(128) NOT NULL,
    started_at TIMESTAMPTZ NOT NULL,
    deadline_at TIMESTAMPTZ NOT NULL,
    finalized_at TIMESTAMPTZ,
    entitlement_snapshot JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (user_id, idempotency_key),
    CHECK (deadline_at > started_at)
);

CREATE UNIQUE INDEX attempt_one_active_per_user_idx ON attempt(user_id)
    WHERE status IN ('IN_PROGRESS', 'FINALIZING');
CREATE INDEX attempt_user_started_idx ON attempt(user_id, started_at DESC);

CREATE TABLE attempt_item (
    id UUID PRIMARY KEY,
    attempt_id UUID NOT NULL REFERENCES attempt(id),
    question_id UUID NOT NULL REFERENCES question(id),
    question_version_id UUID NOT NULL REFERENCES question_version(id),
    section_id UUID NOT NULL REFERENCES blueprint_section(id),
    position INTEGER NOT NULL,
    option_order JSONB NOT NULL,
    UNIQUE (attempt_id, position),
    UNIQUE (attempt_id, question_id)
);

CREATE TABLE answer (
    attempt_item_id UUID PRIMARY KEY REFERENCES attempt_item(id),
    selected_option_id UUID REFERENCES question_option(id),
    revision INTEGER NOT NULL DEFAULT 0 CHECK (revision >= 0),
    flagged BOOLEAN NOT NULL DEFAULT FALSE,
    received_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE quota_day (
    user_id UUID NOT NULL REFERENCES app_user(id),
    day_wib DATE NOT NULL,
    used_count SMALLINT NOT NULL DEFAULT 0 CHECK (used_count BETWEEN 0 AND 2),
    PRIMARY KEY (user_id, day_wib)
);

CREATE TABLE quota_usage (
    attempt_id UUID PRIMARY KEY REFERENCES attempt(id),
    user_id UUID NOT NULL,
    day_wib DATE NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    FOREIGN KEY (user_id, day_wib) REFERENCES quota_day(user_id, day_wib)
);

CREATE TABLE score_run (
    id UUID PRIMARY KEY,
    attempt_id UUID NOT NULL REFERENCES attempt(id),
    policy_version VARCHAR(80) NOT NULL,
    correction_version INTEGER NOT NULL DEFAULT 1,
    input_hash VARCHAR(128) NOT NULL,
    total_score NUMERIC(12, 4) NOT NULL,
    details JSONB NOT NULL,
    computed_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (attempt_id, policy_version, correction_version, input_hash)
);

CREATE TABLE plan (
    id UUID PRIMARY KEY,
    code VARCHAR(80) NOT NULL UNIQUE,
    name VARCHAR(120) NOT NULL,
    months INTEGER NOT NULL CHECK (months > 0),
    amount_minor BIGINT NOT NULL CHECK (amount_minor >= 0),
    currency CHAR(3) NOT NULL,
    benefits JSONB NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE purchase_order (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES app_user(id),
    plan_id UUID NOT NULL REFERENCES plan(id),
    provider_order_id VARCHAR(160) UNIQUE,
    amount_minor BIGINT NOT NULL CHECK (amount_minor >= 0),
    currency CHAR(3) NOT NULL,
    status VARCHAR(24) NOT NULL DEFAULT 'PENDING',
    idempotency_key VARCHAR(128) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (user_id, idempotency_key)
);

CREATE TABLE payment_event (
    id UUID PRIMARY KEY,
    provider VARCHAR(48) NOT NULL,
    provider_event_id VARCHAR(180) NOT NULL,
    order_id UUID REFERENCES purchase_order(id),
    amount_minor BIGINT,
    verified BOOLEAN NOT NULL DEFAULT FALSE,
    received_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (provider, provider_event_id)
);

CREATE TABLE entitlement_grant (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES app_user(id),
    order_id UUID NOT NULL UNIQUE REFERENCES purchase_order(id),
    starts_at TIMESTAMPTZ NOT NULL,
    ends_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ,
    revoke_reason TEXT,
    CHECK (ends_at > starts_at)
);

CREATE TABLE stored_file (
    id UUID PRIMARY KEY,
    relative_path VARCHAR(500) NOT NULL UNIQUE,
    sha256 CHAR(64) NOT NULL,
    size_bytes BIGINT NOT NULL CHECK (size_bytes >= 0),
    mime_type VARCHAR(160) NOT NULL,
    uploaded_by UUID NOT NULL REFERENCES app_user(id),
    visibility VARCHAR(24) NOT NULL DEFAULT 'PRIVATE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE import_batch (
    id UUID PRIMARY KEY,
    file_id UUID NOT NULL REFERENCES stored_file(id),
    uploader_id UUID NOT NULL REFERENCES app_user(id),
    status VARCHAR(24) NOT NULL DEFAULT 'QUEUED',
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    completed_at TIMESTAMPTZ
);

CREATE TABLE job (
    id UUID PRIMARY KEY,
    type VARCHAR(80) NOT NULL,
    dedup_key VARCHAR(180) NOT NULL UNIQUE,
    payload JSONB NOT NULL DEFAULT '{}'::jsonb,
    status VARCHAR(24) NOT NULL DEFAULT 'QUEUED',
    run_after TIMESTAMPTZ NOT NULL DEFAULT now(),
    lease_owner VARCHAR(120),
    lease_until TIMESTAMPTZ,
    retry_count INTEGER NOT NULL DEFAULT 0,
    max_attempts INTEGER NOT NULL DEFAULT 5,
    last_error TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX job_claim_idx ON job(status, run_after, lease_until);

CREATE TABLE audit_event (
    id UUID PRIMARY KEY,
    actor_id UUID REFERENCES app_user(id),
    action VARCHAR(100) NOT NULL,
    entity_type VARCHAR(80) NOT NULL,
    entity_id UUID,
    reason TEXT,
    request_id VARCHAR(100),
    occurred_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX audit_entity_time_idx ON audit_event(entity_type, entity_id, occurred_at DESC);
