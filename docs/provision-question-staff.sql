\set ON_ERROR_STOP on
-- Explicit operator action for one pre-existing account; no account/password or admin role is created.
INSERT INTO user_role(user_id,role_code)
SELECT id,'QUESTION_STAFF' FROM app_user
WHERE email_canonical=lower(:'staff_email') AND status='ACTIVE'
ON CONFLICT (user_id,role_code) DO NOTHING
RETURNING user_id,role_code;
