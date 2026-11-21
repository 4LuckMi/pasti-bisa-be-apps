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
                       (SELECT COUNT(*) FROM attempt a JOIN blueprint_version previous ON previous.id=a.blueprint_version_id
                        WHERE previous.blueprint_id=blueprint.id AND a.mode='TRYOUT'
                        AND a.user_id = (SELECT id FROM app_user WHERE email_canonical = :email)) AS attempt_count,
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
                      AND attempt.mode='TRYOUT' AND EXISTS (
                        SELECT 1 FROM blueprint_version previous WHERE previous.id=attempt.blueprint_version_id AND previous.blueprint_id=blueprint.id)
                    ORDER BY score.computed_at DESC LIMIT 1
                ) latest ON TRUE
                WHERE blueprint.mode = 'TRYOUT' AND blueprint.status IN ('ACTIVE', 'PUBLISHED')
                  AND exam_type.active AND category.active
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
                       category.code AS category, COUNT(pool.question_id) AS question_count
                FROM topic
                JOIN subtest ON subtest.id = topic.subtest_id
                JOIN exam_type ON exam_type.id = subtest.exam_type_id
                JOIN exam_category category ON category.id = exam_type.category_id
                LEFT JOIN published_question_pool pool ON pool.topic_id=topic.id
                WHERE topic.active AND subtest.active AND exam_type.active AND category.active
                  AND (CAST(:category AS varchar) IS NULL OR upper(category.code) = upper(:category))
                GROUP BY topic.id, subtest.code, subtest.name, category.code
                ORDER BY category.code, subtest.name, topic.name
                """, categoryParams(query), (rs, row) -> new PracticeTopicResponse(
                    rs.getObject("id", UUID.class), rs.getString("topic"), rs.getString("subtest_code"),
                    rs.getString("subtest"), rs.getString("category"), rs.getLong("question_count")));
    }

    @Override public PracticeTopicDetail findTopic(UUID id) {
        return jdbc.queryForObject("""
                SELECT t.id,t.name AS topic,s.id AS subtest_id,s.code AS subtest_code,s.name AS subtest,
                       e.id AS exam_type_id,e.name AS exam_type,c.code AS category,
                       (SELECT COUNT(*) FROM published_question_pool p WHERE p.topic_id=t.id) AS question_count
                FROM topic t JOIN subtest s ON s.id=t.subtest_id JOIN exam_type e ON e.id=s.exam_type_id
                JOIN exam_category c ON c.id=e.category_id WHERE t.id=:id AND t.active AND s.active AND e.active AND c.active
                """,new MapSqlParameterSource("id",id),(rs,row)->{
                    long count=rs.getLong("question_count");int maximum=(int)Math.min(50,count);
                    return new PracticeTopicDetail(id,rs.getString("topic"),rs.getObject("subtest_id",UUID.class),rs.getString("subtest_code"),
                        rs.getString("subtest"),rs.getObject("exam_type_id",UUID.class),rs.getString("exam_type"),rs.getString("category"),
                        count,Math.min(10,maximum),maximum,120,count>0);
                });
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
    public List<AttemptHistoryResponse> findHistory(String email, HistoryQuery query) {
        return jdbc.query("""
                SELECT attempt.id, blueprint.id AS tryout_id, blueprint.title AS tryout_title,
                       attempt.mode, attempt.status, attempt.started_at, attempt.finalized_at,
                       latest.total_score, latest.details::text AS details,
                       scope.topic_id,topic.name AS topic_name,category.code AS category,attempt.terminal_reason,
                       (SELECT count(*) FROM attempt_item item WHERE item.attempt_id=attempt.id) AS question_count,
                       version.duration_seconds
                FROM attempt
                JOIN app_user account ON account.id = attempt.user_id
                JOIN blueprint_version version ON version.id = attempt.blueprint_version_id
                JOIN blueprint ON blueprint.id = version.blueprint_id
                JOIN exam_type ON exam_type.id=blueprint.exam_type_id
                JOIN exam_category category ON category.id=exam_type.category_id
                LEFT JOIN LATERAL (
                    SELECT (section.filters->>'topicId')::uuid AS topic_id FROM blueprint_section section
                    WHERE section.blueprint_version_id=version.id AND attempt.mode='PRACTICE' ORDER BY section.position LIMIT 1
                ) scope ON TRUE
                LEFT JOIN topic ON topic.id=scope.topic_id
                LEFT JOIN LATERAL (
                    SELECT total_score, details FROM score_run WHERE attempt_id = attempt.id
                    ORDER BY computed_at DESC LIMIT 1
                ) latest ON TRUE
                WHERE account.email_canonical = :email
                  AND (:mode::varchar IS NULL OR attempt.mode=:mode::varchar)
                  AND (:tryoutId::uuid IS NULL OR (attempt.mode='TRYOUT' AND blueprint.id=:tryoutId::uuid))
                  AND (:topicId::uuid IS NULL OR (attempt.mode='PRACTICE' AND scope.topic_id=:topicId::uuid))
                ORDER BY attempt.started_at DESC,attempt.id DESC LIMIT :limit OFFSET :offset
                """, new MapSqlParameterSource("email", email).addValue("mode",query.mode(),java.sql.Types.VARCHAR)
                    .addValue("tryoutId",query.tryoutId()).addValue("topicId",query.topicId())
                    .addValue("offset",query.offset()).addValue("limit",query.limit()), (rs, row) -> new AttemptHistoryResponse(
                    rs.getObject("id", UUID.class), rs.getObject("tryout_id", UUID.class), rs.getString("tryout_title"),
                    rs.getString("mode"), rs.getString("status"), rs.getObject("started_at", OffsetDateTime.class),
                    rs.getObject("finalized_at", OffsetDateTime.class), rs.getBigDecimal("total_score"), rs.getString("details"),
                    rs.getObject("topic_id",UUID.class),rs.getString("topic_name"),rs.getString("category"),rs.getString("terminal_reason"),
                    rs.getInt("question_count"),rs.getInt("duration_seconds")));
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
    public List<LeaderboardResponse> findLeaderboard(String email,CategoryQuery query) {
        return jdbc.query("""
                WITH selected AS (
                  SELECT h.id FROM leaderboard_cohort h JOIN exam_form f ON f.id=h.form_id
                  JOIN blueprint_version v ON v.id=f.blueprint_version_id JOIN blueprint b ON b.id=v.blueprint_id
                  JOIN exam_type e ON e.id=b.exam_type_id JOIN exam_category c ON c.id=e.category_id
                  WHERE h.status='ACTIVE' AND h.period_start<=now() AND h.period_end>now()
                  AND (:category::varchar IS NULL OR upper(c.code)=upper(:category))
                  ORDER BY h.period_start DESC,h.id LIMIT 1
                )
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
                JOIN attempt ranked ON ranked.id=score.attempt_id AND ranked.status='COMPLETED' AND ranked.terminal_reason IN ('SUBMITTED','TIMED_OUT')
                JOIN user_profile profile ON profile.user_id = entry.user_id AND profile.leaderboard_opt_in
                JOIN app_user u ON u.id=entry.user_id AND u.status='ACTIVE'
                WHERE cohort.id=(SELECT id FROM selected)
                  AND (CAST(:category AS varchar) IS NULL OR upper(category.code) = upper(:category))
                ORDER BY entry.rank,entry.user_id LIMIT CASE WHEN EXISTS(
                  SELECT 1 FROM entitlement_grant g JOIN purchase_order o ON o.id=g.order_id
                  LEFT JOIN subscription s ON s.id=g.subscription_id
                  WHERE g.user_id=(SELECT id FROM app_user WHERE email_canonical=:email)
                  AND o.user_id=g.user_id AND o.status='PAID' AND g.revoked_at IS NULL AND g.starts_at<=now() AND g.ends_at>now()
                  AND (g.subscription_id IS NULL OR s.status='ACTIVE') AND o.benefits_snapshot->>'fullLeaderboard'='true'
                ) THEN 100 ELSE 3 END
                """, categoryParams(query).addValue("email",email), (rs, row) -> new LeaderboardResponse(
                    rs.getInt("rank"), rs.getString("display_name"), rs.getBigDecimal("total_score"),
                    rs.getString("category"), rs.getObject("period_start", OffsetDateTime.class),
                    rs.getObject("period_end", OffsetDateTime.class)));
    }

    private MapSqlParameterSource categoryParams(CategoryQuery query) {
        return new MapSqlParameterSource().addValue("category", query.category(), java.sql.Types.VARCHAR);
    }
}
