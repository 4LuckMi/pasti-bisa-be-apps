package com.pasti_bisa.backend.modules.exam.adapter.out.persistence;

import java.time.*;
import java.util.*;
import org.springframework.jdbc.core.namedparam.*;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.json.JsonMapper;
import com.pasti_bisa.backend.modules.exam.application.QuestionPoolRepository;
import com.pasti_bisa.backend.modules.exam.application.dto.ExamDtos.*;
import com.pasti_bisa.backend.shared.web.BusinessException;

@Repository
public class JdbcQuestionPoolRepository implements QuestionPoolRepository {
    private final NamedParameterJdbcTemplate jdbc;private final JsonMapper json;
    public JdbcQuestionPoolRepository(NamedParameterJdbcTemplate jdbc,JsonMapper json){this.jdbc=jdbc;this.json=json;}
    @Override public List<Section> sections(UUID version){return jdbc.query("SELECT * FROM blueprint_section WHERE blueprint_version_id=:id ORDER BY position",new MapSqlParameterSource("id",version),(r,n)->{
        var filters=json.readTree(r.getString("filters"));var quotas=new ArrayList<DifficultyQuota>();
        for(String key:filters.propertyNames())if(!List.of("topicId","sourceYear","difficultyQuotas","repeatPolicy").contains(key))
            throw new BusinessException(409,"UNSUPPORTED_BLUEPRINT_FILTER","Blueprint contains an unsupported filter");
        if(filters.has("difficultyQuotas"))for(var item:filters.get("difficultyQuotas"))quotas.add(new DifficultyQuota(item.get("difficulty").asString(),item.get("count").asInt()));
        String policy=filters.hasNonNull("repeatPolicy")?filters.get("repeatPolicy").asString():null;
        if(policy!=null&&!List.of("UNSEEN_TODAY","NEVER_SEEN","RECYCLE_OLDEST").contains(policy))throw new BusinessException(409,"UNSUPPORTED_BLUEPRINT_FILTER","Unsupported repeat policy");
        if(!quotas.isEmpty()&&(quotas.size()>3||quotas.stream().anyMatch(q->q.count()<1||!List.of("EASY","MEDIUM","HARD").contains(q.difficulty()))
                ||quotas.stream().map(DifficultyQuota::difficulty).distinct().count()!=quotas.size()||quotas.stream().mapToInt(DifficultyQuota::count).sum()!=r.getInt("item_count")))
            throw new BusinessException(409,"UNSUPPORTED_BLUEPRINT_FILTER","Invalid difficulty allocation");
        return new Section(r.getObject("id",UUID.class),r.getObject("subtest_id",UUID.class),r.getInt("position"),r.getInt("item_count"),
            filters.hasNonNull("topicId")?UUID.fromString(filters.get("topicId").asString()):null,filters.hasNonNull("sourceYear")?filters.get("sourceYear").asInt():null,quotas,policy);
    });}
    private static final String SCOPE="""
        FROM published_question_pool p WHERE p.subtest_id=:subtest AND (:topic::uuid IS NULL OR p.topic_id=:topic)
        AND (:year::int IS NULL OR p.source_year=:year) AND (:difficulty::varchar IS NULL OR p.difficulty=:difficulty)
        AND (:method<>'OPTION_WEIGHT' OR p.weighted_ready)
        AND (NOT EXISTS(SELECT 1 FROM blueprint_section_source ss WHERE ss.section_id=:section)
          OR EXISTS(SELECT 1 FROM blueprint_section_source ss JOIN question_set qs ON qs.id=ss.question_set_id AND qs.status='ACTIVE'
            JOIN question_set_item i ON i.question_set_id=qs.id WHERE ss.section_id=:section AND i.question_id=p.question_id))
        """;
    private MapSqlParameterSource params(Section s,String difficulty){
        String method=jdbc.queryForObject("SELECT p.method FROM blueprint_section s JOIN blueprint_version v ON v.id=s.blueprint_version_id JOIN scoring_policy p ON p.id=v.scoring_policy_id WHERE s.id=:id",new MapSqlParameterSource("id",s.id()),String.class);
        return new MapSqlParameterSource("subtest",s.subtestId()).addValue("section",s.id()).addValue("topic",s.topicId()).addValue("year",s.sourceYear()).addValue("difficulty",difficulty).addValue("method",method);
    }
    @Override public long available(UUID section){
        return jdbc.queryForObject("SELECT question_section_available(:id)",new MapSqlParameterSource("id",section),Long.class);
    }
    private List<PoolItem> draw(UUID user,Section s,String difficulty,int count,boolean excludeToday,LocalDate day){
        String policy=s.repeatPolicy();if(policy!=null)excludeToday=policy.equals("UNSEEN_TODAY");
        var p=params(s,difficulty).addValue("user",user).addValue("count",count).addValue("exclude",excludeToday).addValue("never","NEVER_SEEN".equals(policy))
            .addValue("from",day.atStartOfDay(ZoneId.of("Asia/Jakarta")).toOffsetDateTime()).addValue("to",day.plusDays(1).atStartOfDay(ZoneId.of("Asia/Jakarta")).toOffsetDateTime());
        return jdbc.query("""
            SELECT eligible.question_id,eligible.version_id,eligible.shuffle_allowed FROM (SELECT p.*
            """+SCOPE+"""
            ) eligible LEFT JOIN LATERAL(SELECT max(a.started_at) AS last_seen FROM attempt_item seen JOIN attempt a ON a.id=seen.attempt_id
              WHERE seen.question_id=eligible.question_id AND a.user_id=:user) exposure ON true
            WHERE (NOT :never OR exposure.last_seen IS NULL) AND (NOT :exclude OR NOT EXISTS(
              SELECT 1 FROM attempt_item seen JOIN attempt a ON a.id=seen.attempt_id WHERE seen.question_id=eligible.question_id
              AND a.user_id=:user AND a.started_at>=:from AND a.started_at<:to))
            ORDER BY exposure.last_seen ASC NULLS FIRST,random() LIMIT :count
            """,p,(r,n)->new PoolItem(r.getObject("question_id",UUID.class),r.getObject("version_id",UUID.class),r.getBoolean("shuffle_allowed")));
    }
    @Override public List<PoolItem> select(UUID user,Section s,boolean excludeToday,LocalDate day){
        if(s.difficultyQuotas().isEmpty())return draw(user,s,null,s.count(),excludeToday,day);
        var result=new ArrayList<PoolItem>();for(var quota:s.difficultyQuotas()){
            var rows=draw(user,s,quota.difficulty(),quota.count(),excludeToday,day);
            if(rows.size()!=quota.count())throw new BusinessException(409,"POOL_NOT_READY","Insufficient eligible questions for a difficulty allocation");result.addAll(rows);
        }return result;
    }
}
