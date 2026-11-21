\set ON_ERROR_STOP on
-- One explicit operator-selected existing account. Never creates an account or shared password.
BEGIN;
SELECT pg_advisory_xact_lock(73482107);
INSERT INTO user_role(user_id,role_code)
SELECT id,'PLATFORM_ADMIN' FROM app_user
WHERE email_canonical=lower(:'admin_email') AND status='ACTIVE'
ON CONFLICT(user_id,role_code) DO NOTHING
RETURNING user_id,role_code;
COMMIT;
