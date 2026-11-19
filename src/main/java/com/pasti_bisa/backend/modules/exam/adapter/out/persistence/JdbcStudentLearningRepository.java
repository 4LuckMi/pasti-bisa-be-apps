package com.pasti_bisa.backend.modules.exam.adapter.out.persistence;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import com.pasti_bisa.backend.modules.exam.application.StudentLearningRepository;
import com.pasti_bisa.backend.modules.exam.application.dto.StudentLearningDtos.*;

@Repository
public class JdbcStudentLearningRepository implements StudentLearningRepository {
    private final NamedParameterJdbcTemplate jdbc;
    public JdbcStudentLearningRepository(NamedParameterJdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override
    public List<TryoutResponse> findTryouts(String email, CategoryQuery query) {
        return jdbc.query("""
                SELECT blueprint.id, blueprint.code, blueprint.title, category.code AS category,
                       exam_type.name AS exam_type, version.id AS blueprint_version_id, version.duration_seconds,
                       (SELECT COALESCE(SUM(section.item_count), 0) FROM blueprint_section section
                        WHERE section.blueprint_version_id = version.id) AS question_count,
                       (SELECT COUNT(*) FROM attempt WHERE blueprint_version_id = version.id
                        AND user_id = (SELECT id FROM app_user WHERE email_canonical = :email)) AS attempt_count,
                       latest.total_score AS latest_score, latest.computed_at AS latest_completed_at
                FROM blueprint
                JOIN exam_type ON exam_type.id = blueprint.exam_type_id
                JOIN exam_category category ON category.id = exam_type.category_id
                JOIN blueprint_version version ON version.id = blueprint.active_version_id
                     AND version.status IN ('ACTIVE', 'PUBLISHED')
                LEFT JOIN LATERAL (
                    SELECT score.total_score, score.computed_at FROM score_run score
                    JOIN attempt ON attempt.id = score.attempt_id
                    WHERE attempt.user_id = (SELECT id FROM app_user WHERE email_canonical = :email)
                      AND attempt.blueprint_version_id = version.id
                    ORDER BY score.computed_at DESC LIMIT 1
                ) latest ON TRUE
                WHERE blueprint.mode = 'TRYOUT' AND blueprint.status IN ('ACTIVE', 'PUBLISHED')
                  AND (CAST(:category AS varchar) IS NULL OR upper(category.code) = upper(:category))
                ORDER BY blueprint.created_at DESC
                """, categoryParams(query).addValue("email", email), (rs, row) -> new TryoutResponse(
                    rs.getObject("id", UUID.class), rs.getString("code"), rs.getString("title"),
                    rs.getString("category"), rs.getString("exam_type"), rs.getObject("blueprint_version_id", UUID.class),
                    rs.getInt("duration_seconds"), rs.getLong("question_count"), rs.getLong("attempt_count"),
                    rs.getBigDecimal("latest_score"), rs.getObject("latest_completed_at", OffsetDateTime.class)));
    }

    @Override
    public List<PracticeTopicResponse> findTopics(CategoryQuery query) {
        return jdbc.query("""
                SELECT topic.id, topic.name AS topic, subtest.code AS subtest_code, subtest.name AS subtest,
                       category.code AS category, COUNT(DISTINCT question.id) FILTER (WHERE version.id IS NOT NULL) AS question_count
                FROM topic
                JOIN subtest ON subtest.id = topic.subtest_id
                JOIN exam_type ON exam_type.id = subtest.exam_type_id
                JOIN exam_category category ON category.id = exam_type.category_id
                LEFT JOIN question ON question.topic_id = topic.id AND question.archived_at IS NULL
                LEFT JOIN question_version version ON version.question_id = question.id AND version.status = 'PUBLISHED'
                WHERE topic.active AND subtest.active
                  AND (CAST(:category AS varchar) IS NULL OR upper(category.code) = upper(:category))
                GROUP BY topic.id, subtest.code, subtest.name, category.code
                ORDER BY category.code, subtest.name, topic.name
                """, categoryParams(query), (rs, row) -> new PracticeTopicResponse(
                    rs.getObject("id", UUID.class), rs.getString("topic"), rs.getString("subtest_code"),
                    rs.getString("subtest"), rs.getString("category"), rs.getLong("question_count")));
    }

    @Override
    public int findUsedQuota(String email, LocalDate day) {
        return jdbc.query("""
                SELECT quota.used_count FROM quota_day quota
                JOIN app_user account ON account.id = quota.user_id
                WHERE account.email_canonical = :email AND quota.day_wib = :day
                """, new MapSqlParameterSource("email", email).addValue("day", day),
                (rs, row) -> rs.getInt("used_count")).stream().findFirst().orElse(0);
    }

    @Override
    public List<AttemptHistoryResponse> findHistory(String email) {
        return jdbc.query("""
                SELECT attempt.id, blueprint.id AS tryout_id, blueprint.title AS tryout_title,
                       attempt.mode, attempt.status, attempt.started_at, attempt.finalized_at,
                       latest.total_score, latest.details::text AS details
                FROM attempt
                JOIN app_user account ON account.id = attempt.user_id
                JOIN blueprint_version version ON version.id = attempt.blueprint_version_id
                JOIN blueprint ON blueprint.id = version.blueprint_id
                LEFT JOIN LATERAL (
                    SELECT total_score, details FROM score_run WHERE attempt_id = attempt.id
                    ORDER BY computed_at DESC LIMIT 1
                ) latest ON TRUE
                WHERE account.email_canonical = :email ORDER BY attempt.started_at DESC LIMIT 100
                """, new MapSqlParameterSource("email", email), (rs, row) -> new AttemptHistoryResponse(
                    rs.getObject("id", UUID.class), rs.getObject("tryout_id", UUID.class), rs.getString("tryout_title"),
                    rs.getString("mode"), rs.getString("status"), rs.getObject("started_at", OffsetDateTime.class),
                    rs.getObject("finalized_at", OffsetDateTime.class), rs.getBigDecimal("total_score"), rs.getString("details")));
    }

    @Override
    public List<SubscriptionResponse> findSubscriptions(String email) {
        return jdbc.query("""
                SELECT subscription.id, subscription.starts_at, subscription.ends_at, subscription.status,
                       EXISTS (SELECT 1 FROM entitlement_grant grant_record WHERE grant_record.subscription_id = subscription.id
                               AND grant_record.revoked_at IS NULL AND grant_record.starts_at <= now()
                               AND grant_record.ends_at > now()) AS active
                FROM subscription JOIN app_user account ON account.id = subscription.user_id
                WHERE account.email_canonical = :email ORDER BY subscription.ends_at DESC
                """, new MapSqlParameterSource("email", email), (rs, row) -> new SubscriptionResponse(
                    rs.getObject("id", UUID.class), rs.getObject("starts_at", OffsetDateTime.class),
                    rs.getObject("ends_at", OffsetDateTime.class), rs.getString("status"), rs.getBoolean("active")));
    }

    @Override
    public List<LeaderboardResponse> findLeaderboard(CategoryQuery query) {
        return jdbc.query("""
                SELECT entry.rank, entry.display_name_snapshot AS display_name, score.total_score,
                       category.code AS category, cohort.period_start, cohort.period_end
                FROM leaderboard_entry entry
                JOIN leaderboard_cohort cohort ON cohort.id = entry.cohort_id
                JOIN exam_form form ON form.id = cohort.form_id
                JOIN blueprint_version version ON version.id = form.blueprint_version_id
                JOIN blueprint ON blueprint.id = version.blueprint_id
                JOIN exam_type ON exam_type.id = blueprint.exam_type_id
                JOIN exam_category category ON category.id = exam_type.category_id
                JOIN score_run score ON score.id = entry.score_run_id
                JOIN user_profile profile ON profile.user_id = entry.user_id AND profile.leaderboard_opt_in
                WHERE cohort.status = 'ACTIVE' AND cohort.period_end > now()
                  AND (CAST(:category AS varchar) IS NULL OR upper(category.code) = upper(:category))
                ORDER BY entry.rank LIMIT 100
                """, categoryParams(query), (rs, row) -> new LeaderboardResponse(
                    rs.getInt("rank"), rs.getString("display_name"), rs.getBigDecimal("total_score"),
                    rs.getString("category"), rs.getObject("period_start", OffsetDateTime.class),
                    rs.getObject("period_end", OffsetDateTime.class)));
    }

    private MapSqlParameterSource categoryParams(CategoryQuery query) {
        return new MapSqlParameterSource().addValue("category", query.category(), java.sql.Types.VARCHAR);
    }
}
