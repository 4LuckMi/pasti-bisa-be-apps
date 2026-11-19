ALTER TABLE user_profile
    ADD COLUMN school_name VARCHAR(180),
    ADD COLUMN school_grade VARCHAR(32),
    ADD COLUMN target_score INTEGER CHECK (target_score IS NULL OR target_score BETWEEN 0 AND 1000);
