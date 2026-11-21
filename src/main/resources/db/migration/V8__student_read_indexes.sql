-- Read-side indexes only: do not rewrite historical attempt or score snapshots.
CREATE INDEX attempt_user_finalized_idx ON attempt(user_id, finalized_at DESC, id)
    WHERE status='COMPLETED';
CREATE INDEX question_latest_published_idx ON question_version(question_id, version DESC)
    WHERE status='PUBLISHED';
CREATE INDEX leaderboard_visible_period_idx ON leaderboard_cohort(period_start DESC, id)
    WHERE status='ACTIVE';
