package com.pasti_bisa.backend.modules.exam.adapter.out.persistence;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import com.pasti_bisa.backend.modules.exam.application.ExamRepository;
import com.pasti_bisa.backend.modules.exam.application.dto.ExamDtos.*;
import com.pasti_bisa.backend.modules.exam.domain.ScoreCalculator.Calculation;
import com.pasti_bisa.backend.modules.exam.domain.ScoringModels.*;
import com.pasti_bisa.backend.shared.web.BusinessException;

@Repository
public class JdbcExamRepository implements ExamRepository {
    private final NamedParameterJdbcTemplate jdbc;
    private final JsonMapper json;
    public JdbcExamRepository(NamedParameterJdbcTemplate jdbc, JsonMapper json) { this.jdbc = jdbc; this.json = json; }
    private MapSqlParameterSource p(String name, Object value) { return new MapSqlParameterSource(name, value); }
    private static Instant instant(ResultSet rs, String column) throws SQLException {
        var timestamp = rs.getTimestamp(column); return timestamp == null ? null : timestamp.toInstant();
    }
    private final RowMapper<Attempt> attemptMapper = (rs, row) -> new Attempt(
            rs.getObject("id", UUID.class), rs.getObject("user_id", UUID.class), rs.getObject("blueprint_version_id", UUID.class),
            rs.getObject("blueprint_id", UUID.class), rs.getString("mode"), rs.getString("status"), rs.getString("request_hash"),
            rs.getString("terminal_reason"), instant(rs, "started_at"), instant(rs, "deadline_at"),instant(rs,"finalized_at"));
    private static final String ATTEMPT_SELECT = """
            SELECT a.*, bv.blueprint_id FROM attempt a JOIN blueprint_version bv ON bv.id = a.blueprint_version_id
            """;

    @Override public UUID lockUser(String email) {
        return jdbc.queryForObject("SELECT id FROM app_user WHERE email_canonical = :email AND status = 'ACTIVE' FOR UPDATE",
                p("email", email), UUID.class);
    }
    @Override public Optional<Attempt> findByKey(UUID userId, String key) {
        return jdbc.query(ATTEMPT_SELECT + " WHERE a.user_id = :userId AND a.idempotency_key = :key",
                p("userId", userId).addValue("key", key), attemptMapper).stream().findFirst();
    }
    @Override public Optional<Attempt> findActive(UUID userId) {
        return jdbc.query(ATTEMPT_SELECT + " WHERE a.user_id = :userId AND a.status IN ('IN_PROGRESS','FINALIZING') ORDER BY a.started_at DESC LIMIT 1",
                p("userId", userId), attemptMapper).stream().findFirst();
    }
    @Override public Attempt lockOwned(UUID id, String email) {
        return jdbc.queryForObject(ATTEMPT_SELECT + """
                JOIN app_user account ON account.id = a.user_id
                WHERE a.id = :id AND account.email_canonical = :email AND account.status = 'ACTIVE' FOR UPDATE OF a
                """, p("id", id).addValue("email", email), attemptMapper);
    }
    @Override public Attempt lock(UUID id) {
        return jdbc.queryForObject(ATTEMPT_SELECT + " WHERE a.id = :id FOR UPDATE OF a", p("id", id), attemptMapper);
    }
    @Override public Blueprint blueprint(UUID tryoutId) {
        return jdbc.queryForObject("""
                SELECT bv.id, b.id AS blueprint_id, b.exam_type_id, bv.duration_seconds, sp.id AS policy_id,
                       CASE WHEN sp.aggregation='SUM' AND sp.rounding='HALF_UP' THEN sp.method ELSE 'UNSUPPORTED' END AS method,
                       sp.scale, sp.rules, sp.code, sp.version
                FROM blueprint b JOIN blueprint_version bv ON bv.id = b.active_version_id
                JOIN scoring_policy sp ON sp.id = bv.scoring_policy_id
                JOIN exam_type e ON e.id=b.exam_type_id JOIN exam_category c ON c.id=e.category_id
                WHERE b.id = :id AND b.mode = 'TRYOUT' AND b.status IN ('ACTIVE','PUBLISHED')
                  AND e.active AND c.active
                  AND bv.status IN ('ACTIVE','PUBLISHED') AND sp.status = 'ACTIVE'
                """, p("id", tryoutId), (rs, row) -> new Blueprint(rs.getObject("id", UUID.class),
                    rs.getObject("blueprint_id", UUID.class), rs.getObject("exam_type_id", UUID.class), rs.getInt("duration_seconds"),
                    rs.getObject("policy_id", UUID.class), rs.getString("method"), rs.getBigDecimal("scale"),
                    json.readTree(rs.getString("rules")), rs.getString("code"), rs.getInt("version")));
    }

    @Override public Blueprint practiceBlueprint(UUID topicId, int count) {
        var topic = jdbc.queryForObject("""
                SELECT t.subtest_id, s.exam_type_id, t.name FROM topic t JOIN subtest s ON s.id = t.subtest_id
                JOIN exam_type e ON e.id=s.exam_type_id JOIN exam_category c ON c.id=e.category_id
                WHERE t.id = :id AND t.active AND s.active AND e.active AND c.active
                """, p("id", topicId), (rs, row) -> new PracticeTopic(rs.getObject("subtest_id", UUID.class),
                    rs.getObject("exam_type_id", UUID.class), rs.getString("name")));
        // Serialize blueprint creation across different users requesting the same topic.
        jdbc.queryForObject("SELECT id FROM topic WHERE id = :id FOR UPDATE", p("id", topicId), UUID.class);
        String code = "practice:" + topicId + ":" + count;
        var params = p("code", code).addValue("typeId", topic.examTypeId());
        var existing = jdbc.query("SELECT active_version_id FROM blueprint WHERE code = :code AND exam_type_id = :typeId",
                params, (rs, row) -> rs.getObject(1, UUID.class));
        UUID policyId;
        var policies = jdbc.query("SELECT id FROM scoring_policy WHERE code = 'PRACTICE_BINARY' AND version = 1",
                p("unused", 0), (rs, row) -> rs.getObject(1, UUID.class));
        if (policies.isEmpty()) {
            jdbc.update("""
                    INSERT INTO scoring_policy(id, code, version, method, rules, aggregation, scale, rounding, code_hash, status)
                    VALUES (gen_random_uuid(),'PRACTICE_BINARY',1,'BINARY','{"correctPoints":1,"wrongPoints":0,"emptyPoints":0}',
                            'SUM',100,'HALF_UP','practice-binary-v1','ACTIVE') ON CONFLICT (code,version) DO NOTHING
                    """, p("unused", 0));
        }
        policyId = jdbc.queryForObject("SELECT id FROM scoring_policy WHERE code = 'PRACTICE_BINARY' AND version = 1", p("unused", 0), UUID.class);
        UUID blueprintId, versionId;
        if (existing.isEmpty()) {
            blueprintId = UUID.randomUUID(); versionId = UUID.randomUUID();
            params.addValue("id", blueprintId).addValue("versionId", versionId).addValue("policyId", policyId)
                    .addValue("title", "Latihan " + topic.name()).addValue("duration", count * 120)
                    .addValue("subtestId", topic.subtestId()).addValue("count", count).addValue("topicId", topicId.toString());
            jdbc.update("INSERT INTO blueprint(id,exam_type_id,code,title,mode) VALUES (:id,:typeId,:code,:title,'PRACTICE')", params);
            jdbc.update("""
                    INSERT INTO blueprint_version(id,blueprint_id,version,duration_seconds,scoring_policy,scoring_policy_id)
                    VALUES (:versionId,:id,1,:duration,'{}',:policyId)
                    """, params);
            jdbc.update("""
                    INSERT INTO blueprint_section(id,blueprint_version_id,subtest_id,position,item_count,filters)
                    VALUES (gen_random_uuid(),:versionId,:subtestId,1,:count,jsonb_build_object('topicId',:topicId::text))
                    """, params);
            jdbc.update("UPDATE blueprint_version SET status='ACTIVE' WHERE id=:versionId", params);
            jdbc.update("UPDATE blueprint SET status='ACTIVE',active_version_id=:versionId WHERE id=:id", params);
        } else {
            versionId = existing.getFirst();
            blueprintId = jdbc.queryForObject("SELECT blueprint_id FROM blueprint_version WHERE id=:id", p("id", versionId), UUID.class);
        }
        return new Blueprint(versionId, blueprintId, topic.examTypeId(), count * 120, policyId, "BINARY", BigDecimal.valueOf(100),
                json.readTree("{\"correctPoints\":1,\"wrongPoints\":0,\"emptyPoints\":0}"), "PRACTICE_BINARY", 1);
    }
    private record PracticeTopic(UUID subtestId, UUID examTypeId, String name) {}

    @Override public List<Section> sections(UUID versionId) {
        return jdbc.query("SELECT id,subtest_id,position,item_count,filters FROM blueprint_section WHERE blueprint_version_id=:id ORDER BY position",
                p("id", versionId), (rs, row) -> {
                    JsonNode filter = json.readTree(rs.getString("filters"));
                    for (String key : filter.propertyNames()) if (!List.of("topicId", "sourceYear").contains(key))
                        throw new BusinessException(409,"UNSUPPORTED_BLUEPRINT_FILTER","Blueprint contains an unsupported filter");
                    return new Section(rs.getObject("id", UUID.class),rs.getObject("subtest_id", UUID.class),rs.getInt("position"),rs.getInt("item_count"),
                            filter.hasNonNull("topicId") ? UUID.fromString(filter.get("topicId").asString()) : null,
                            filter.hasNonNull("sourceYear") ? filter.get("sourceYear").asInt() : null);
                });
    }

    @Override public List<PoolItem> pool(UUID userId, Section section, boolean excludeToday, LocalDate day) {
        return jdbc.query("""
                SELECT q.id AS question_id, version.id AS version_id, version.shuffle_allowed
                FROM question q
                JOIN LATERAL (SELECT id, shuffle_allowed, source_year FROM question_version WHERE question_id=q.id
                              AND status='PUBLISHED' ORDER BY version DESC LIMIT 1) version ON TRUE
                LEFT JOIN LATERAL (SELECT max(a.started_at) AS last_seen FROM attempt_item seen JOIN attempt a ON a.id=seen.attempt_id
                                   WHERE seen.question_id=q.id AND a.user_id=:userId) exposure ON TRUE
                WHERE q.subtest_id=:subtestId AND q.archived_at IS NULL
                  AND EXISTS(SELECT 1 FROM subtest s WHERE s.id=q.subtest_id AND s.active)
                  AND (q.topic_id IS NULL OR EXISTS(SELECT 1 FROM topic t WHERE t.id=q.topic_id AND t.active))
                  AND (:topicId::uuid IS NULL OR q.topic_id=:topicId::uuid)
                  AND (:sourceYear::integer IS NULL OR version.source_year=:sourceYear::integer)
                  AND (NOT :excludeToday OR NOT EXISTS (
                      SELECT 1 FROM attempt_item seen JOIN attempt a ON a.id=seen.attempt_id
                      WHERE seen.question_id=q.id AND a.user_id=:userId
                        AND (a.started_at AT TIME ZONE 'Asia/Jakarta')::date=:day))
                ORDER BY exposure.last_seen ASC NULLS FIRST, random() LIMIT :count
                """, p("userId",userId).addValue("subtestId",section.subtestId()).addValue("topicId",section.topicId())
                    .addValue("sourceYear",section.sourceYear(),java.sql.Types.INTEGER).addValue("excludeToday",excludeToday)
                    .addValue("day",day).addValue("count",section.count()),
                (rs,row)->new PoolItem(rs.getObject("question_id",UUID.class),rs.getObject("version_id",UUID.class),rs.getBoolean("shuffle_allowed")));
    }
    @Override public List<Option> options(UUID versionId) {
        return jdbc.query("SELECT id,option_code,correct_key,score_weight FROM question_option WHERE question_version_id=:id ORDER BY original_position",
                p("id",versionId),(rs,row)->new Option(rs.getObject("id",UUID.class),rs.getString("option_code"),
                    rs.getBoolean("correct_key"),rs.getBigDecimal("score_weight")));
    }
    @Override public int usedQuota(UUID userId, LocalDate day) {
        return jdbc.query("SELECT used_count FROM quota_day WHERE user_id=:id AND day_wib=:day",p("id",userId).addValue("day",day),
                (rs,row)->rs.getInt(1)).stream().findFirst().orElse(0);
    }
    @Override public void insertAttempt(UUID id,UUID userId,Blueprint b,String mode,String key,String hash,Instant now,Instant deadline) {
        jdbc.update("""
                INSERT INTO attempt(id,user_id,blueprint_version_id,mode,status,idempotency_key,request_hash,started_at,deadline_at)
                VALUES (:id,:userId,:versionId,:mode,'IN_PROGRESS',:key,:hash,:started,:deadline)
                """,p("id",id).addValue("userId",userId).addValue("versionId",b.id()).addValue("mode",mode).addValue("key",key)
                    .addValue("hash",hash).addValue("started",java.sql.Timestamp.from(now)).addValue("deadline",java.sql.Timestamp.from(deadline)));
    }
    @Override public void insertItem(UUID attemptId,UUID sectionId,PoolItem item,int position,List<String> order) {
        jdbc.update("""
                INSERT INTO attempt_item(id,attempt_id,question_id,question_version_id,section_id,position,option_order)
                VALUES (gen_random_uuid(),:attemptId,:questionId,:versionId,:sectionId,:position,:optionOrder::jsonb)
                """,p("attemptId",attemptId).addValue("questionId",item.questionId()).addValue("versionId",item.versionId())
                    .addValue("sectionId",sectionId).addValue("position",position).addValue("optionOrder",json.writeValueAsString(order)));
    }
    @Override public void consumeQuota(UUID userId,UUID attemptId,LocalDate day) {
        var params=p("userId",userId).addValue("attemptId",attemptId).addValue("day",day);
        jdbc.update("INSERT INTO quota_day(user_id,day_wib,used_count) VALUES (:userId,:day,1) ON CONFLICT (user_id,day_wib) DO UPDATE SET used_count=quota_day.used_count+1",params);
        jdbc.update("INSERT INTO quota_usage(attempt_id,user_id,day_wib) VALUES (:attemptId,:userId,:day)",params);
    }
    @Override public List<Navigation> navigation(UUID id) {
        return jdbc.query("""
                SELECT item.position, answer.selected_option_id IS NOT NULL AS answered, COALESCE(answer.flagged,false) AS flagged,
                       COALESCE(answer.revision,0) AS revision FROM attempt_item item LEFT JOIN answer ON answer.attempt_item_id=item.id
                WHERE item.attempt_id=:id ORDER BY item.position
                """,p("id",id),(rs,row)->new Navigation(rs.getInt("position"),rs.getBoolean("answered"),rs.getBoolean("flagged"),rs.getInt("revision")));
    }
    @Override public CourseContext context(UUID versionId,boolean practice) {
        var context=jdbc.queryForObject("""
                SELECT b.title,c.code AS category,e.name AS exam_type,t.id AS topic_id,t.name AS topic_name
                FROM blueprint_version v JOIN blueprint b ON b.id=v.blueprint_id
                JOIN exam_type e ON e.id=b.exam_type_id JOIN exam_category c ON c.id=e.category_id
                LEFT JOIN LATERAL (
                    SELECT (filters->>'topicId')::uuid AS topic_id FROM blueprint_section
                    WHERE blueprint_version_id=v.id AND :practice ORDER BY position LIMIT 1
                ) scope ON TRUE LEFT JOIN topic t ON t.id=scope.topic_id WHERE v.id=:id
                """,p("id",versionId).addValue("practice",practice),(rs,row)->new CourseContext(rs.getString("title"),rs.getString("category"),
                    rs.getString("exam_type"),rs.getObject("topic_id",UUID.class),rs.getString("topic_name"),List.of()));
        var sections=jdbc.query("""
                SELECT section.position,section.subtest_id,s.name,section.item_count FROM blueprint_section section
                JOIN subtest s ON s.id=section.subtest_id WHERE section.blueprint_version_id=:id ORDER BY section.position
                """,p("id",versionId),(rs,row)->new SessionSection(rs.getInt("position"),rs.getObject("subtest_id",UUID.class),rs.getString("name"),rs.getInt("item_count")));
        return new CourseContext(context.title(),context.category(),context.examType(),context.topicId(),context.topicName(),sections);
    }
    @Override public AnswerResponse saveAnswer(UUID attemptId,int position,AnswerRequest answer,Instant now) {
        var params=p("id",attemptId).addValue("position",position).addValue("optionId",answer.selectedOptionId())
                .addValue("flagged",answer.flagged()).addValue("revision",answer.expectedRevision());
        UUID itemId=jdbc.queryForObject("SELECT id FROM attempt_item WHERE attempt_id=:id AND position=:position",params,UUID.class);
        params.addValue("itemId",itemId);
        if(answer.selectedOptionId()!=null && !Boolean.TRUE.equals(jdbc.queryForObject("""
                SELECT EXISTS (SELECT 1 FROM question_option o JOIN attempt_item i ON i.question_version_id=o.question_version_id
                               WHERE i.id=:itemId AND o.id=:optionId)
                """,params,Boolean.class))) throw new BusinessException(400,"INVALID_OPTION","Option does not belong to this question");
        int changed=jdbc.update("""
                INSERT INTO answer(attempt_item_id,selected_option_id,flagged,revision)
                SELECT :itemId,:optionId,:flagged,1 WHERE :revision=0
                ON CONFLICT (attempt_item_id) DO UPDATE SET selected_option_id=:optionId,flagged=:flagged,
                    revision=answer.revision+1,received_at=now() WHERE answer.revision=:revision
                """,params);
        // For revisions > 0 the INSERT SELECT has no row, so use an explicit conditional update.
        if(answer.expectedRevision()>0) changed=jdbc.update("""
                UPDATE answer SET selected_option_id=:optionId,flagged=:flagged,revision=revision+1,received_at=now()
                WHERE attempt_item_id=:itemId AND revision=:revision
                """,params);
        if(changed!=1) throw new BusinessException(409,"REVISION_CONFLICT","Fetch the latest question before saving again");
        return new AnswerResponse(position,answer.selectedOptionId(),answer.flagged(),answer.expectedRevision()+1,now);
    }
    @Override public void finalizeAttempt(UUID id,String reason,Instant now) {
        jdbc.update("UPDATE attempt SET status='FINALIZING',terminal_reason=:reason,finalized_at=:now WHERE id=:id AND status='IN_PROGRESS'",
                p("id",id).addValue("reason",reason).addValue("now",java.sql.Timestamp.from(now)));
        jdbc.update("""
                INSERT INTO job(id,type,dedup_key,payload) VALUES (gen_random_uuid(),'SCORE_ATTEMPT',:key,jsonb_build_object('attemptId',:id::text))
                ON CONFLICT (dedup_key) DO NOTHING
                """,p("id",id.toString()).addValue("key","score:"+id));
    }
    @Override public List<UUID> expired(Instant now) {
        return jdbc.query("SELECT id FROM attempt WHERE status='IN_PROGRESS' AND deadline_at<=:now ORDER BY deadline_at LIMIT 100",
                p("now",java.sql.Timestamp.from(now)),(rs,row)->rs.getObject(1,UUID.class));
    }
    @Override public ScorePolicy policy(UUID attemptId) {
        return jdbc.queryForObject("""
                SELECT p.* FROM scoring_policy p JOIN blueprint_version b ON b.scoring_policy_id=p.id
                JOIN attempt a ON a.blueprint_version_id=b.id WHERE a.id=:id
                """,p("id",attemptId),(rs,row)->new ScorePolicy(rs.getObject("id",UUID.class),rs.getString("method"),rs.getString("code"),
                    rs.getInt("version"),rs.getBigDecimal("scale"),json.readTree(rs.getString("rules"))));
    }
    @Override public List<ScoringItem> scoringItems(UUID id) {
        return jdbc.query("""
                SELECT q.subtest_id, a.selected_option_id, COALESCE(o.correct_key,false) AS correct,
                       COALESCE(o.score_weight,0) AS selected_weight,
                       (SELECT max(score_weight) FROM question_option WHERE question_version_id=i.question_version_id) AS max_weight
                FROM attempt_item i JOIN question q ON q.id=i.question_id
                LEFT JOIN answer a ON a.attempt_item_id=i.id LEFT JOIN question_option o ON o.id=a.selected_option_id
                WHERE i.attempt_id=:id ORDER BY i.position
                """,p("id",id),(rs,row)->new ScoringItem(rs.getObject("subtest_id",UUID.class),rs.getObject("selected_option_id",UUID.class),
                    rs.getBoolean("correct"),rs.getBigDecimal("selected_weight"),rs.getBigDecimal("max_weight")));
    }
    @Override public void saveScore(Attempt attempt,ScorePolicy policy,Calculation score) {
        var details=json.createObjectNode().put("label","Indeks latihan PastiSiap; bukan skor resmi")
                .put("correct",score.correct()).put("wrong",score.wrong()).put("empty",score.empty());
        UUID scoreId=UUID.randomUUID();
        var params=p("id",scoreId).addValue("attemptId",attempt.id()).addValue("policyId",policy.id()).addValue("version",policy.code()+":"+policy.version())
                .addValue("inputHash",com.pasti_bisa.backend.modules.exam.application.ExamService.hash(json.writeValueAsString(scoringItems(attempt.id()))))
                .addValue("total",score.total()).addValue("details",json.writeValueAsString(details));
        jdbc.update("""
                INSERT INTO score_run(id,attempt_id,policy_version,input_hash,total_score,details,scoring_policy_id)
                VALUES (:id,:attemptId,:version,:inputHash,:total,:details::jsonb,:policyId)
                """,params);
        for(var section:score.sections()) jdbc.update("""
                INSERT INTO score_section(score_run_id,subtest_id,correct_count,wrong_count,empty_count,points,max_points,accuracy)
                VALUES (:id,:subtest,:correct,:wrong,:empty,:points,:max,:accuracy)
                """,p("id",scoreId).addValue("subtest",section.subtestId()).addValue("correct",section.correct())
                    .addValue("wrong",section.wrong()).addValue("empty",section.empty()).addValue("points",section.points())
                    .addValue("max",section.maxPoints()).addValue("accuracy",section.accuracy()));
        jdbc.update("UPDATE attempt SET status='COMPLETED' WHERE id=:attemptId",params);
    }
    @Override public ResultResponse result(UUID id) {
        var rows=jdbc.query("""
                SELECT a.status,a.terminal_reason,a.finalized_at,s.id AS score_id,s.policy_version,s.total_score,s.details,p.method
                FROM attempt a LEFT JOIN score_run s ON s.attempt_id=a.id
                LEFT JOIN scoring_policy p ON p.id=s.scoring_policy_id WHERE a.id=:id ORDER BY s.computed_at DESC LIMIT 1
                """,p("id",id),(rs,row)->new ResultRow(rs.getString("status"),rs.getString("terminal_reason"),rs.getObject("score_id",UUID.class),
                    rs.getString("policy_version"),rs.getBigDecimal("total_score"),rs.getString("details"),rs.getString("method"),instant(rs,"finalized_at")));
        var result=rows.getFirst();
        List<SectionResult> sections=result.scoreId()==null?List.of():jdbc.query("SELECT * FROM score_section WHERE score_run_id=:id ORDER BY subtest_id",
                p("id",result.scoreId()),(rs,row)->new SectionResult(rs.getObject("subtest_id",UUID.class),rs.getInt("correct_count"),rs.getInt("wrong_count"),
                    rs.getInt("empty_count"),rs.getBigDecimal("points"),rs.getBigDecimal("max_points"),rs.getBigDecimal("accuracy")));
        return new ResultResponse(id,result.status(),result.reason(),result.method(),result.version(),result.total(),
                result.details()==null?null:json.readTree(result.details()),sections,result.finalizedAt());
    }
    private record ResultRow(String status,String reason,UUID scoreId,String version,BigDecimal total,String details,String method,Instant finalizedAt) {}
    @Override public TryoutDetail detail(UUID id) {
        Blueprint blueprint=blueprint(id);
        var fields=jdbc.queryForObject("""
                SELECT b.title,b.demo,c.code FROM blueprint b JOIN exam_type e ON e.id=b.exam_type_id
                JOIN exam_category c ON c.id=e.category_id WHERE b.id=:id
                """,p("id",id),(rs,row)->new DetailFields(rs.getString("title"),rs.getBoolean("demo"),rs.getString("code")));
        List<SectionResponse> sections=sections(blueprint.id()).stream().map(section->{
            long available=jdbc.queryForObject("""
                    SELECT COUNT(*) FROM question q JOIN LATERAL (
                      SELECT source_year FROM question_version WHERE question_id=q.id AND status='PUBLISHED' ORDER BY version DESC LIMIT 1
                    ) v ON TRUE
                    WHERE q.subtest_id=:subtestId AND q.archived_at IS NULL AND (:topicId::uuid IS NULL OR q.topic_id=:topicId::uuid)
                      AND EXISTS(SELECT 1 FROM subtest s WHERE s.id=q.subtest_id AND s.active)
                      AND (q.topic_id IS NULL OR EXISTS(SELECT 1 FROM topic t WHERE t.id=q.topic_id AND t.active))
                      AND (:year::integer IS NULL OR v.source_year=:year::integer)
                    """,p("subtestId",section.subtestId()).addValue("topicId",section.topicId()).addValue("year",section.sourceYear(),java.sql.Types.INTEGER),Long.class);
            String name=jdbc.queryForObject("SELECT name FROM subtest WHERE id=:id",p("id",section.subtestId()),String.class);
            return new SectionResponse(section.id(),section.subtestId(),name,section.position(),section.count(),available);
        }).toList();
        return new TryoutDetail(id,fields.title(),fields.category(),blueprint.durationSeconds(),blueprint.method(),blueprint.scale(),fields.demo(),
                !sections.isEmpty()&&sections.stream().allMatch(s->s.available()>=s.questionCount()),sections);
    }
    private record DetailFields(String title,boolean demo,String category) {}
}
