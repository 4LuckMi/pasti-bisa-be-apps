ALTER TABLE app_user ALTER COLUMN password_hash DROP NOT NULL;

CREATE TABLE oauth_account (
    user_id UUID NOT NULL REFERENCES app_user(id),
    provider VARCHAR(40) NOT NULL,
    provider_subject VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (provider, provider_subject),
    UNIQUE (user_id, provider)
);
