package com.pasti_bisa.backend.modules.exam.adapter.out.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.*;
import java.util.*;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.*;
import org.springframework.stereotype.Repository;
import com.pasti_bisa.backend.modules.exam.application.StudentExperienceRepository;
import com.pasti_bisa.backend.modules.exam.application.ScoringProgress;
import com.pasti_bisa.backend.modules.exam.application.dto.StudentExperienceDtos.*;
import com.pasti_bisa.backend.modules.exam.application.dto.StudentLearningDtos.QuotaResponse;
import com.pasti_bisa.backend.shared.web.*;

/** Read models only. Exam writes and frozen scoring snapshots remain in ExamRepository. */
@Repository
public class JdbcStudentExperienceRepository implements StudentExperienceRepository {
    private final NamedParameterJdbcTemplate jdbc;
    private static final ZoneId WIB=ZoneId.of("Asia/Jakarta");
    public JdbcStudentExperienceRepository(NamedParameterJdbcTemplate jdbc){this.jdbc=jdbc;}
    private static Instant time(ResultSet r,String key)throws SQLException{
        var t=r.getObject(key,OffsetDateTime.class);return t==null?null:t.toInstant();
    }
    private static final String HISTORY="""
            FROM attempt a JOIN app_user u ON u.id=a.user_id
            JOIN blueprint_version v ON v.id=a.blueprint_version_id
            JOIN blueprint b ON b.id=v.blueprint_id JOIN exam_type e ON e.id=b.exam_type_id
            JOIN exam_category c ON c.id=e.category_id
            LEFT JOIN LATERAL (SELECT (s.filters->>'topicId')::uuid AS topic_id FROM blueprint_section s
              WHERE s.blueprint_version_id=v.id AND a.mode='PRACTICE' ORDER BY s.position LIMIT 1) scope ON true
            LEFT JOIN topic t ON t.id=scope.topic_id
            LEFT JOIN LATERAL (SELECT total_score,policy_version,computed_at FROM score_run
              WHERE attempt_id=a.id ORDER BY computed_at DESC,id DESC LIMIT 1) score ON true
            LEFT JOIN job j ON j.dedup_key='score:'||a.id::text AND j.type='SCORE_ATTEMPT'
            WHERE u.email_canonical=:email
              AND (:category::varchar IS NULL OR c.code=:category)
              AND (:mode::varchar IS NULL OR a.mode=:mode)
              AND (:status::varchar IS NULL OR a.status=:status)
              AND (:search::varchar IS NULL OR position(lower(:search) IN lower(b.title||' '||coalesce(t.name,'')))>0)
              AND (:tryoutId::uuid IS NULL OR (a.mode='TRYOUT' AND b.id=:tryoutId))
              AND (:topicId::uuid IS NULL OR (a.mode='PRACTICE' AND t.id=:topicId))
              AND (:from::timestamptz IS NULL OR a.started_at>=:from)
              AND (:to::timestamptz IS NULL OR a.started_at<:to)
            """;
    private MapSqlParameterSource historyParams(String email,LearningQuery q,Instant from,Instant to){
        return new MapSqlParameterSource("email",email).addValue("category",q.category()).addValue("mode",q.mode())
            .addValue("status",q.status()).addValue("search",q.search()).addValue("tryoutId",q.tryoutId())
            .addValue("topicId",q.topicId()).addValue("from",offset(from)).addValue("to",offset(to))
            .addValue("offset",q.offset()).addValue("limit",q.limit());
    }
    private static OffsetDateTime offset(Instant t){return t==null?null:t.atOffset(ZoneOffset.UTC);}
    private static final RowMapper<HistoryEntry> ENTRY=(r,n)->new HistoryEntry(r.getObject("id",UUID.class),
            r.getObject("blueprint_id",UUID.class),r.getString("title"),r.getString("category"),r.getString("mode"),
            r.getString("status"),r.getString("terminal_reason"),r.getObject("topic_id",UUID.class),r.getString("topic_name"),
            time(r,"started_at"),time(r,"finalized_at"),r.getBigDecimal("total_score"),r.getInt("question_count"),
            r.getInt("duration_seconds"),ScoringProgress.from(r.getString("status"),r.getString("job_status")));
    private static final String ENTRY_COLUMNS="""
            SELECT a.id,b.id AS blueprint_id,b.title,c.code AS category,a.mode,a.status,a.terminal_reason,
              scope.topic_id,t.name AS topic_name,a.started_at,a.finalized_at,score.total_score,v.duration_seconds,
              (SELECT count(*) FROM attempt_item i WHERE i.attempt_id=a.id) AS question_count,j.status AS job_status
            """;
    @Override public HistoryPage history(String email,LearningQuery q,Instant from,Instant to){
        var p=historyParams(email,q,from,to);
        var summary=jdbc.queryForObject("""
                SELECT count(*) AS total,count(*) FILTER(WHERE a.status='COMPLETED') AS completed,
                count(*) FILTER(WHERE a.status='IN_PROGRESS') AS in_progress,
                count(*) FILTER(WHERE a.status='FINALIZING') AS scoring,
                count(*) FILTER(WHERE a.status='FINALIZING' AND (j.status IS NULL OR j.status IN ('FAILED','SUCCEEDED'))) AS failed
                """+HISTORY,p,(r,n)->new HistorySummary(r.getLong("total"),r.getLong("completed"),r.getLong("in_progress"),r.getLong("scoring"),r.getLong("failed")));
        var items=jdbc.query(ENTRY_COLUMNS+HISTORY+" ORDER BY a.started_at DESC,a.id DESC LIMIT :limit OFFSET :offset",p,ENTRY);
        return new HistoryPage(items,summary.total(),q.offset(),q.limit(),summary);
    }
    private record Totals(long tryouts,long practices,long completed,long elapsed){}
    private record ScoreScope(UUID id,String name,String policy,long count,java.math.BigDecimal average){}
    @Override public Dashboard dashboard(String email,LearningQuery q,Instant now){
        var p=historyParams(email,q,null,null).addValue("now",offset(now));
        var totals=jdbc.queryForObject("""
                SELECT count(*) FILTER(WHERE a.mode='TRYOUT') AS tryouts,count(*) FILTER(WHERE a.mode='PRACTICE') AS practices,
                count(*) FILTER(WHERE a.status='COMPLETED' AND a.terminal_reason IN ('SUBMITTED','TIMED_OUT')) AS completed,
                coalesce(sum(greatest(0,extract(epoch FROM least(coalesce(a.finalized_at,:now),a.deadline_at,:now)-a.started_at))),0)::bigint AS elapsed
                """+HISTORY,p,(r,n)->new Totals(r.getLong("tryouts"),r.getLong("practices"),r.getLong("completed"),r.getLong("elapsed")));
        String eligible=" AND a.status='COMPLETED' AND a.terminal_reason IN ('SUBMITTED','TIMED_OUT') AND score.total_score IS NOT NULL";
        var scopes=jdbc.query("SELECT e.id,e.name,score.policy_version,count(*) AS count,round(avg(score.total_score),2) AS average "+HISTORY+eligible+
                " GROUP BY e.id,e.name,score.policy_version ORDER BY e.name,score.policy_version",p,(r,n)->new ScoreScope(r.getObject("id",UUID.class),r.getString("name"),r.getString("policy_version"),r.getLong("count"),r.getBigDecimal("average")));
        var groups=scopes.stream().map(s->{
            var points=jdbc.query("SELECT a.id,a.finalized_at,score.total_score "+HISTORY+eligible+
                    " AND e.id=:exam AND score.policy_version=:policy ORDER BY a.finalized_at DESC,a.id DESC LIMIT 12",
                    new MapSqlParameterSource(p.getValues()).addValue("exam",s.id()).addValue("policy",s.policy()),
                    (r,n)->new ScorePoint(r.getObject("id",UUID.class),time(r,"finalized_at"),r.getBigDecimal("total_score")));
            return new ScoreGroup(s.id(),s.name(),s.policy(),s.count(),s.average(),points.reversed());
        }).toList();
        var days=jdbc.query("SELECT DISTINCT (a.finalized_at AT TIME ZONE 'Asia/Jakarta')::date AS day "+HISTORY+eligible+
                " ORDER BY day DESC",p,(r,n)->r.getObject("day",LocalDate.class));
        LocalDate day=now.atZone(WIB).toLocalDate(),expected=day;int streak=0;
        if(!days.isEmpty()&&days.getFirst().equals(day.minusDays(1)))expected=day.minusDays(1);
        for(var completed:days){if(!completed.equals(expected))break;streak++;expected=expected.minusDays(1);}
        int used=jdbc.query("SELECT used_count FROM quota_day WHERE user_id=(SELECT id FROM app_user WHERE email_canonical=:email) AND day_wib=:day",
                new MapSqlParameterSource("email",email).addValue("day",day),(r,n)->r.getInt(1)).stream().findFirst().orElse(0);
        return new Dashboard(q.category(),q.mode(),totals.tryouts(),totals.practices(),totals.completed(),totals.elapsed(),streak,groups,
                jdbc.query(ENTRY_COLUMNS+HISTORY+" ORDER BY a.started_at DESC,a.id DESC LIMIT 5",p,ENTRY),
                new QuotaResponse(day,used,2,Math.max(0,2-used)),membership(email,now));
    }
    @Override public Membership membership(String email,Instant now){
        var p=new MapSqlParameterSource("email",email).addValue("now",offset(now));
        var grants=jdbc.query("""
                SELECT g.starts_at,g.ends_at,o.plan_id,o.plan_code_snapshot,p.name AS plan_name,o.months_snapshot,
                o.amount_minor,o.currency,coalesce((o.benefits_snapshot->>'fullLeaderboard')::boolean,false) AS full_leaderboard
                FROM entitlement_grant g JOIN purchase_order o ON o.id=g.order_id
                JOIN plan p ON p.id=o.plan_id JOIN app_user u ON u.id=g.user_id LEFT JOIN subscription s ON s.id=g.subscription_id
                WHERE u.email_canonical=:email AND o.user_id=g.user_id AND o.status='PAID' AND g.revoked_at IS NULL
                AND g.starts_at<=:now AND g.ends_at>:now AND (g.subscription_id IS NULL OR s.status='ACTIVE')
                ORDER BY full_leaderboard DESC,g.ends_at DESC,g.id
                """,p,(r,n)->new Membership("PREMIUM",time(r,"starts_at"),time(r,"ends_at"),
                    new MembershipPlan(r.getObject("plan_id",UUID.class),r.getString("plan_code_snapshot"),r.getString("plan_name"),
                        r.getInt("months_snapshot"),r.getLong("amount_minor"),r.getString("currency")),
                    new Benefits(r.getBoolean("full_leaderboard")),3,2));
        return grants.stream().findFirst().orElseGet(()->new Membership("FREE",null,null,null,new Benefits(false),3,2));
    }
    private static final RowMapper<Plan> PLAN=(r,n)->new Plan(r.getObject("id",UUID.class),r.getString("code"),r.getString("family_code"),
            r.getInt("revision"),r.getString("name"),r.getInt("months"),r.getLong("amount_minor"),r.getString("currency"),new Benefits(r.getBoolean("full_leaderboard")));
    private static final String PLAN_COLUMNS="SELECT p.*,coalesce((p.benefits->>'fullLeaderboard')::boolean,false) AS full_leaderboard FROM plan p ";
    @Override public PageResponse<Plan> plans(Paging q){
        var p=new MapSqlParameterSource("limit",q.limit()).addValue("offset",q.offset());
        return new PageResponse<>(jdbc.query(PLAN_COLUMNS+"WHERE active ORDER BY family_code,revision DESC,id LIMIT :limit OFFSET :offset",p,PLAN),
            jdbc.queryForObject("SELECT count(*) FROM plan WHERE active",p,Long.class),q.offset(),q.limit());
    }
    @Override public Plan plan(UUID id){return jdbc.queryForObject(PLAN_COLUMNS+"WHERE p.id=:id AND p.active",new MapSqlParameterSource("id",id),PLAN);}

    private static final String COHORT_FROM="""
            FROM leaderboard_cohort h JOIN exam_form f ON f.id=h.form_id JOIN blueprint_version v ON v.id=f.blueprint_version_id
            JOIN blueprint b ON b.id=v.blueprint_id JOIN exam_type e ON e.id=b.exam_type_id JOIN exam_category c ON c.id=e.category_id
            WHERE h.status='ACTIVE' AND h.period_start<=:now
            """;
    private static final String COHORT_COLUMNS="SELECT h.*,b.title,c.code AS category,e.id AS exam_type_id ";
    private static Cohort cohort(ResultSet r,Instant now)throws SQLException{return new Cohort(r.getObject("id",UUID.class),r.getString("title"),
            r.getString("category"),r.getObject("exam_type_id",UUID.class),r.getObject("form_id",UUID.class),r.getObject("scoring_policy_id",UUID.class),
            time(r,"period_start"),time(r,"period_end"),r.getString("attempt_policy"),!time(r,"period_end").isAfter(now));}
    @Override public PageResponse<Cohort> cohorts(CohortQuery q,Instant now){
        var p=new MapSqlParameterSource("now",offset(now)).addValue("category",q.category()).addValue("examType",q.examTypeId())
            .addValue("ended",Boolean.TRUE.equals(q.includeEnded())).addValue("limit",q.limit()).addValue("offset",q.offset());
        String scope=COHORT_FROM+" AND (:ended OR h.period_end>:now) AND (:category::varchar IS NULL OR c.code=:category) AND (:examType::uuid IS NULL OR e.id=:examType)";
        return new PageResponse<>(jdbc.query(COHORT_COLUMNS+scope+" ORDER BY h.period_start DESC,h.id LIMIT :limit OFFSET :offset",p,(r,n)->cohort(r,now)),
            jdbc.queryForObject("SELECT count(*) "+scope,p,Long.class),q.offset(),q.limit());
    }
    private static final String RANK_FROM="""
            FROM leaderboard_entry le JOIN score_run sr ON sr.id=le.score_run_id
            JOIN attempt ranked ON ranked.id=sr.attempt_id AND ranked.status='COMPLETED' AND ranked.terminal_reason IN ('SUBMITTED','TIMED_OUT')
            JOIN user_profile up ON up.user_id=le.user_id AND up.leaderboard_opt_in
            JOIN app_user u ON u.id=le.user_id AND u.status='ACTIVE'
            LEFT JOIN target t ON t.id=up.target_id WHERE le.cohort_id=:id
            """;
    private static final String RANK_COLUMNS="SELECT le.rank,le.display_name_snapshot,sr.total_score,t.kind,t.institution,t.program,u.email_canonical=:email AS current_user ";
    private static final RowMapper<RankingEntry> RANK=(r,n)->new RankingEntry(r.getInt("rank"),r.getString("display_name_snapshot"),r.getBigDecimal("total_score"),
            r.getString("kind"),r.getString("institution"),r.getString("program"),r.getBoolean("current_user"));
    @Override public Rankings rankings(String email,UUID id,RankingQuery q,Instant now){
        var p=new MapSqlParameterSource("id",id).addValue("email",email).addValue("now",offset(now));
        var cohort=jdbc.queryForObject(COHORT_COLUMNS+COHORT_FROM+" AND h.id=:id",p,(r,n)->cohort(r,now));
        boolean full=membership(email,now).benefits().fullLeaderboard();
        if(!full&&q.filtered())throw new BusinessException(403,"PREMIUM_REQUIRED","Target filters require full leaderboard access");
        boolean opted=Boolean.TRUE.equals(jdbc.queryForObject("SELECT leaderboard_opt_in FROM user_profile WHERE user_id=(SELECT id FROM app_user WHERE email_canonical=:email)",p,Boolean.class));
        String filters=" AND (:kind::varchar IS NULL OR t.kind=:kind) AND (:institution::varchar IS NULL OR t.institution=:institution) AND (:program::varchar IS NULL OR t.program=:program)";
        p.addValue("kind",q.targetKind()).addValue("institution",q.targetInstitution()).addValue("program",q.targetProgram());
        long total=jdbc.queryForObject("SELECT count(*) "+RANK_FROM+filters,p,Long.class);
        int limit=full?q.limit():Math.min(q.limit(),Math.max(0,3-q.offset()));
        p.addValue("limit",limit).addValue("offset",q.offset());
        var rows=jdbc.query(RANK_COLUMNS+RANK_FROM+filters+" ORDER BY le.rank,le.user_id LIMIT :limit OFFSET :offset",p,RANK);
        var own=jdbc.query(RANK_COLUMNS+RANK_FROM+" AND u.email_canonical=:email",p,RANK).stream().findFirst().orElse(null);
        return new Rankings(cohort,full?"FULL":"PREVIEW",opted,total,full?total:Math.min(3,total),q.offset(),limit,rows,own);
    }

    private MapSqlParameterSource catalogParams(CatalogQuery q){return new MapSqlParameterSource("category",q.category()).addValue("search",q.search())
            .addValue("examType",q.examTypeId()).addValue("subtest",q.subtestId()).addValue("ready",q.ready())
            .addValue("offset",q.offset()).addValue("limit",q.limit());}
    // Readiness uses the same latest published version and taxonomy filters as exam selection.
    private static final String TRYOUT_CTE="""
            WITH cards AS (SELECT b.id,b.code,b.title,b.created_at,c.code AS category,e.id AS exam_type_id,e.name AS exam_type,
            v.id AS version_id,v.duration_seconds,
            (SELECT coalesce(sum(s.item_count),0) FROM blueprint_section s WHERE s.blueprint_version_id=v.id)::int AS question_count,
            EXISTS(SELECT 1 FROM blueprint_section s WHERE s.blueprint_version_id=v.id)
            AND NOT EXISTS(SELECT 1 FROM blueprint_section s WHERE s.blueprint_version_id=v.id
                AND question_section_available(s.id)<s.item_count) AS ready,
            (SELECT count(*) FROM attempt a JOIN blueprint_version av ON av.id=a.blueprint_version_id
              WHERE av.blueprint_id=b.id AND a.user_id=(SELECT id FROM app_user WHERE email_canonical=:email)) AS attempt_count,
            latest.total_score,latest.computed_at
            FROM blueprint b JOIN blueprint_version v ON v.id=b.active_version_id AND v.status IN ('ACTIVE','PUBLISHED')
            JOIN scoring_policy sp ON sp.id=v.scoring_policy_id AND sp.status='ACTIVE'
            JOIN exam_type e ON e.id=b.exam_type_id AND e.active JOIN exam_category c ON c.id=e.category_id AND c.active
            LEFT JOIN LATERAL(SELECT sr.total_score,sr.computed_at FROM score_run sr JOIN attempt a ON a.id=sr.attempt_id
              JOIN blueprint_version av ON av.id=a.blueprint_version_id WHERE av.blueprint_id=b.id AND a.mode='TRYOUT'
              AND a.terminal_reason IN ('SUBMITTED','TIMED_OUT') AND a.user_id=(SELECT id FROM app_user WHERE email_canonical=:email)
              ORDER BY sr.computed_at DESC,sr.id DESC LIMIT 1) latest ON true
            WHERE b.mode='TRYOUT' AND b.status IN ('ACTIVE','PUBLISHED')
            AND (:category::varchar IS NULL OR c.code=:category) AND (:examType::uuid IS NULL OR e.id=:examType)
            AND (:subtest::uuid IS NULL OR EXISTS(SELECT 1 FROM blueprint_section s WHERE s.blueprint_version_id=v.id AND s.subtest_id=:subtest))
            AND (:search::varchar IS NULL OR position(lower(:search) IN lower(b.title||' '||b.code))>0))
            """;
    @Override public PageResponse<TryoutCard> tryouts(String email,CatalogQuery q){
        var p=catalogParams(q).addValue("email",email);String filter=" FROM cards WHERE (:ready::boolean IS NULL OR ready=:ready)";
        return new PageResponse<>(jdbc.query(TRYOUT_CTE+"SELECT *"+filter+("TITLE".equals(q.sort())?" ORDER BY title,id":" ORDER BY created_at DESC,id")+" LIMIT :limit OFFSET :offset",p,
            (r,n)->new TryoutCard(r.getObject("id",UUID.class),r.getString("code"),r.getString("title"),r.getString("category"),
                r.getObject("exam_type_id",UUID.class),r.getString("exam_type"),r.getObject("version_id",UUID.class),r.getInt("duration_seconds"),
                r.getInt("question_count"),r.getBoolean("ready"),r.getLong("attempt_count"),r.getBigDecimal("total_score"),time(r,"computed_at"))),
            jdbc.queryForObject(TRYOUT_CTE+"SELECT count(*)"+filter,p,Long.class),q.offset(),q.limit());
    }
    private static final String TOPIC_CTE="""
            WITH cards AS (SELECT t.id,t.name AS topic,s.id AS subtest_id,s.code AS subtest_code,s.name AS subtest,
            e.id AS exam_type_id,e.name AS exam_type,c.code AS category,
            (SELECT count(*) FROM published_question_pool p WHERE p.topic_id=t.id) AS question_count
            FROM topic t JOIN subtest s ON s.id=t.subtest_id AND s.active JOIN exam_type e ON e.id=s.exam_type_id AND e.active
            JOIN exam_category c ON c.id=e.category_id AND c.active WHERE t.active
            AND (:category::varchar IS NULL OR c.code=:category) AND (:examType::uuid IS NULL OR e.id=:examType)
            AND (:subtest::uuid IS NULL OR s.id=:subtest)
            AND (:search::varchar IS NULL OR position(lower(:search) IN lower(t.name||' '||s.name))>0))
            """;
    @Override public PageResponse<TopicCard> topics(CatalogQuery q){
        if("NEWEST".equals(q.sort()))throw new BusinessException(400,"INVALID_FILTER","Practice topics support TITLE ordering");
        var p=catalogParams(q);String filter=" FROM cards WHERE (:ready::boolean IS NULL OR (question_count>0)=:ready)";
        return new PageResponse<>(jdbc.query(TOPIC_CTE+"SELECT *"+filter+" ORDER BY topic,id LIMIT :limit OFFSET :offset",p,
            (r,n)->new TopicCard(r.getObject("id",UUID.class),r.getString("topic"),r.getObject("subtest_id",UUID.class),r.getString("subtest_code"),
                r.getString("subtest"),r.getObject("exam_type_id",UUID.class),r.getString("exam_type"),r.getString("category"),r.getLong("question_count"),r.getLong("question_count")>0)),
            jdbc.queryForObject(TOPIC_CTE+"SELECT count(*)"+filter,p,Long.class),q.offset(),q.limit());
    }
}
