ALTER TABLE app_user
    ADD COLUMN user_type VARCHAR(32) NOT NULL DEFAULT 'SISWA';

UPDATE app_user SET user_type = 'SISWA' WHERE user_type IS NULL;
