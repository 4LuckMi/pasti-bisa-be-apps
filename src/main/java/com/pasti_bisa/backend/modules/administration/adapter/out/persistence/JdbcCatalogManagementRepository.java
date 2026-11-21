package com.pasti_bisa.backend.modules.administration.adapter.out.persistence;

import com.pasti_bisa.backend.shared.web.PageResponse;

import java.math.BigDecimal;
import java.util.*;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.*;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.json.JsonMapper;
import com.pasti_bisa.backend.modules.administration.application.CatalogManagementRepository;
import com.pasti_bisa.backend.modules.administration.application.dto.AdministrationDtos.*;
import com.pasti_bisa.backend.modules.administration.application.dto.CatalogManagementDtos.*;
import com.pasti_bisa.backend.modules.exam.domain.ScoreCalculator;
import com.pasti_bisa.backend.modules.exam.domain.ScoringModels.ScorePolicy;
import com.pasti_bisa.backend.shared.web.BusinessException;

@Repository
public class JdbcCatalogManagementRepository implements CatalogManagementRepository {
    private final NamedParameterJdbcTemplate jdbc;
    private final JsonMapper json;
    public JdbcCatalogManagementRepository(NamedParameterJdbcTemplate jdbc,JsonMapper json) { this.jdbc=jdbc;this.json=json; }
    private MapSqlParameterSource p(String key,Object value) { return new MapSqlParameterSource(key,value); }
    private void require(boolean condition,String code,String message) { if(!condition) throw new BusinessException(409,code,message); }
    private void serialize() { jdbc.getJdbcTemplate().execute("SELECT pg_advisory_xact_lock(73482108)"); }
    private <T> PageResponse<T> page(String select,String from,ListQuery q,RowMapper<T> mapper,String order) {
        var params=p("search",q.search()).addValue("status",q.status()).addValue("limit",q.limit()).addValue("offset",q.offset());
        return new PageResponse<>(jdbc.query(select+from+" ORDER BY "+order+" LIMIT :limit OFFSET :offset",params,mapper),jdbc.queryForObject("SELECT count(*) "+from,params,Long.class),q.offset(),q.limit());
    }
    private List<TaxonomyItem> taxonomy(String table,String parent,boolean code) {
        // Table/column names are constants supplied only by this class, never request input.
        return jdbc.query("SELECT id,"+(parent==null?"NULL::uuid":parent)+" AS parent_id,"+(code?"code":"NULL::varchar")+" AS code,name,active FROM "+table+" ORDER BY name,id",
            new MapSqlParameterSource(),(rs,row)->new TaxonomyItem(rs.getObject("id",UUID.class),rs.getObject("parent_id",UUID.class),rs.getString("code"),rs.getString("name"),rs.getBoolean("active")));
    }
    @Override public TaxonomyTree taxonomy() { return new TaxonomyTree(taxonomy("exam_category",null,true),taxonomy("exam_type","category_id",true),taxonomy("subtest","exam_type_id",true),taxonomy("topic","subtest_id",false)); }
    private TaxonomyItem saveTaxonomy(String table,String parentColumn,UUID parent,UUID id,String code,String name,boolean active) {
        serialize();boolean create=id==null;if(create) id=UUID.randomUUID();
        var params=p("id",id).addValue("parent",parent).addValue("code",code).addValue("name",name.trim()).addValue("active",active);
        if(!create) {
            if(parentColumn!=null) {
                UUID existing=jdbc.queryForObject("SELECT "+parentColumn+" FROM "+table+" WHERE id=:id FOR UPDATE",params,UUID.class);
                require(Objects.equals(existing,parent),"TAXONOMY_PARENT_IMMUTABLE","Create a new taxonomy item instead of moving existing content across scopes");
            } else jdbc.queryForObject("SELECT id FROM "+table+" WHERE id=:id FOR UPDATE",params,UUID.class);
            if(code!=null) {
                String existing=jdbc.queryForObject("SELECT code FROM "+table+" WHERE id=:id",params,String.class);
                require(existing.equals(code),"TAXONOMY_CODE_IMMUTABLE","Taxonomy codes are stable identifiers");
            }
            jdbc.update("UPDATE "+table+" SET name=:name,active=:active WHERE id=:id",params);
        } else jdbc.update("INSERT INTO "+table+"(id,name,active"+(parentColumn==null?"":","+parentColumn)+(code==null?"":",code")+") VALUES(:id,:name,:active"+(parentColumn==null?"":",:parent")+(code==null?"":",:code")+")",params);
        return new TaxonomyItem(id,parent,code,name.trim(),active);
    }
    @Override public TaxonomyItem category(UUID id,CategoryRequest b) { return saveTaxonomy("exam_category",null,null,id,b.code(),b.name(),b.active()); }
    @Override public TaxonomyItem examType(UUID id,ExamTypeRequest b) { return saveTaxonomy("exam_type","category_id",b.categoryId(),id,b.code(),b.name(),b.active()); }
    @Override public TaxonomyItem subtest(UUID id,SubtestRequest b) { return saveTaxonomy("subtest","exam_type_id",b.examTypeId(),id,b.code(),b.name(),b.active()); }
    @Override public TaxonomyItem topic(UUID id,TopicRequest b) { return saveTaxonomy("topic","subtest_id",b.subtestId(),id,null,b.name(),b.active()); }
    private final RowMapper<PlanResponse> planMapper=(rs,row)-> {
        var benefits=decode(rs.getString("benefits"));
        return new PlanResponse(rs.getObject("id",UUID.class),rs.getString("code"),rs.getString("family_code"),rs.getInt("revision"),rs.getString("name"),rs.getInt("months"),rs.getLong("amount_minor"),rs.getString("currency"),new PlanBenefits(benefits.has("fullLeaderboard")&&benefits.get("fullLeaderboard").asBoolean()),rs.getBoolean("active"));
    };
    @Override public PageResponse<PlanResponse> plans(ListQuery q) { return page("SELECT p.* ","FROM plan p WHERE (:search::varchar IS NULL OR p.name ILIKE '%'||:search||'%' OR p.family_code=:search)",q,planMapper,"p.family_code,p.revision DESC"); }
    @Override public PlanResponse plan(UUID id) { return jdbc.queryForObject("SELECT * FROM plan WHERE id=:id",p("id",id),planMapper); }
    @Override public PlanResponse createPlan(UUID previous,PlanRequest body) {
        serialize();String family=body.code();int revision=1;
        if(previous!=null) {
            var current=plan(previous);require(current.familyCode().equals(family),"PLAN_FAMILY_MISMATCH","New price revision must keep its family code");
            int latest=jdbc.queryForObject("SELECT max(revision) FROM plan WHERE family_code=:code",p("code",family),Integer.class);
            require(current.revision()==latest,"STALE_PLAN_VERSION","Create the next version from the latest plan revision");revision=latest+1;
            jdbc.update("UPDATE plan SET active=false WHERE family_code=:code",p("code",family));
        } else require(jdbc.queryForObject("SELECT count(*) FROM plan WHERE family_code=:code",p("code",family),Long.class)==0,"PLAN_ALREADY_EXISTS","Create a new version of the existing plan");
        UUID id=UUID.randomUUID();
        jdbc.update("""
            INSERT INTO plan(id,code,family_code,revision,name,months,amount_minor,currency,benefits,active)
            VALUES(:id,:code,:family,:revision,:name,:months,:amount,:currency,:benefits::jsonb,true)
            """,p("id",id).addValue("code",revision==1?family:family+"_V"+revision).addValue("family",family).addValue("revision",revision)
                .addValue("name",body.name().trim()).addValue("months",body.months()).addValue("amount",body.amountMinor()).addValue("currency",body.currency()).addValue("benefits",json.writeValueAsString(body.benefits())));
        return plan(id);
    }
    @Override public PlanResponse planActive(UUID id,boolean active) {
        serialize();var plan=plan(id);
        if(active) {
            require(plan.revision()==jdbc.queryForObject("SELECT max(revision) FROM plan WHERE family_code=:family",p("family",plan.familyCode()),Integer.class),"STALE_PLAN_VERSION","Only the latest price revision can be offered");
            jdbc.update("UPDATE plan SET active=false WHERE family_code=:family",p("family",plan.familyCode()));
        }
        jdbc.update("UPDATE plan SET active=:active WHERE id=:id",p("id",id).addValue("active",active));return plan(id);
    }
    private tools.jackson.databind.JsonNode decode(String encoded) { return json.readTree(encoded); }
    private ScoreRules rules(String encoded) {
        var r=json.readTree(encoded);return new ScoreRules(r.hasNonNull("correctPoints")?new BigDecimal(r.get("correctPoints").asString()):null,
            r.hasNonNull("wrongPoints")?new BigDecimal(r.get("wrongPoints").asString()):null,r.hasNonNull("emptyPoints")?new BigDecimal(r.get("emptyPoints").asString()):null);
    }
    private final RowMapper<PolicyResponse> policyMapper=(rs,row)->new PolicyResponse(rs.getObject("id",UUID.class),rs.getString("code"),rs.getInt("version"),rs.getString("method"),rs.getBigDecimal("scale"),rules(rs.getString("rules")),rs.getString("status"));
    @Override public PageResponse<PolicyResponse> policies(ListQuery q) { return page("SELECT p.* ","FROM scoring_policy p WHERE (:search::varchar IS NULL OR p.code ILIKE '%'||:search||'%') AND (:status::varchar IS NULL OR p.status=:status)",q,policyMapper,"p.code,p.version DESC"); }
    @Override public PolicyResponse createPolicy(PolicyRequest body) {
        serialize();var rules=json.createObjectNode();
        if(body.rules().correctPoints()!=null) rules.put("correctPoints",body.rules().correctPoints());
        if(body.rules().wrongPoints()!=null) rules.put("wrongPoints",body.rules().wrongPoints());
        if(body.rules().emptyPoints()!=null) rules.put("emptyPoints",body.rules().emptyPoints());
        ScoreCalculator.validatePolicy(new ScorePolicy(null,body.method(),body.code(),1,body.scale(),rules));
        int version=jdbc.queryForObject("SELECT COALESCE(max(version),0)+1 FROM scoring_policy WHERE code=:code",p("code",body.code()),Integer.class);
        UUID id=UUID.randomUUID();String encoded=json.writeValueAsString(rules);
        jdbc.update("""
            INSERT INTO scoring_policy(id,code,version,method,rules,aggregation,scale,rounding,code_hash,status)
            VALUES(:id,:code,:version,:method,:rules::jsonb,'SUM',:scale,'HALF_UP',md5(:method||:rules||:scale::text),'ACTIVE')
            """,p("id",id).addValue("code",body.code()).addValue("version",version).addValue("method",body.method()).addValue("rules",encoded).addValue("scale",body.scale()));
        return jdbc.queryForObject("SELECT * FROM scoring_policy WHERE id=:id",p("id",id),policyMapper);
    }
    private final RowMapper<BlueprintResponse> blueprintMapper=(rs,row)->new BlueprintResponse(rs.getObject("id",UUID.class),rs.getObject("exam_type_id",UUID.class),rs.getString("code"),rs.getString("title"),rs.getString("status"),rs.getObject("active_version_id",UUID.class));
    @Override public PageResponse<BlueprintResponse> blueprints(ListQuery q) { return page("SELECT b.* ","FROM blueprint b WHERE b.mode='TRYOUT' AND (:search::varchar IS NULL OR b.title ILIKE '%'||:search||'%' OR b.code ILIKE '%'||:search||'%') AND (:status::varchar IS NULL OR b.status=:status)",q,blueprintMapper,"b.created_at DESC,b.id DESC"); }
    private BlueprintResponse master(UUID id,boolean lock) { return jdbc.queryForObject("SELECT * FROM blueprint WHERE id=:id AND mode='TRYOUT'"+(lock?" FOR UPDATE":""),p("id",id),blueprintMapper); }
    @Override public BlueprintDetail blueprint(UUID id) {
        var master=master(id,false);
        return new BlueprintDetail(master,jdbc.query("SELECT id FROM blueprint_version WHERE blueprint_id=:id ORDER BY version DESC",p("id",id),(rs,row)->rs.getObject(1,UUID.class)).stream().map(v->version(id,v)).toList());
    }
    private void activeExamType(UUID id) {
        require(Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM exam_type e JOIN exam_category c ON c.id=e.category_id WHERE e.id=:id AND e.active AND c.active)",p("id",id),Boolean.class)),"INVALID_CONTENT_SCOPE","Exam type/category must be active");
    }
    @Override public BlueprintResponse createBlueprint(BlueprintRequest body) {
        activeExamType(body.examTypeId());UUID id=UUID.randomUUID();
        jdbc.update("INSERT INTO blueprint(id,exam_type_id,code,title,mode,status) VALUES(:id,:type,:code,:title,'TRYOUT','DRAFT')",p("id",id).addValue("type",body.examTypeId()).addValue("code",body.code()).addValue("title",body.title().trim()));return master(id,false);
    }
    @Override public BlueprintVersion createVersion(UUID id,BlueprintVersionRequest body) {
        var master=master(id,true);activeExamType(master.examTypeId());
        var policy=jdbc.queryForObject("SELECT * FROM scoring_policy WHERE id=:id AND status='ACTIVE'",p("id",body.scoringPolicyId()),policyMapper);
        var ruleJson=jdbc.queryForObject("SELECT rules::text FROM scoring_policy WHERE id=:id",p("id",policy.id()),String.class);
        ScoreCalculator.validatePolicy(new ScorePolicy(policy.id(),policy.method(),policy.code(),policy.version(),policy.scale(),json.readTree(ruleJson)));
        var used=new HashSet<UUID>();
        for(var section:body.sections()) {
            require(section.questionSetIds().isEmpty()||section.sourceYear()==null,"AMBIGUOUS_SOURCE_FILTER","Use questionSetIds or legacy sourceYear, not both");
            require(new HashSet<>(section.questionSetIds()).size()==section.questionSetIds().size(),"DUPLICATE_SOURCE","Source IDs must be distinct");
            for(UUID source:section.questionSetIds())require(Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM question_set WHERE id=:id AND exam_type_id=:type AND status='ACTIVE')",p("id",source).addValue("type",master.examTypeId()),Boolean.class)),"INVALID_SOURCE_SCOPE","Source must be active and match blueprint exam type");
            if(!section.difficultyQuotas().isEmpty()){
                require(section.difficultyQuotas().stream().map(DifficultyAllocation::difficulty).distinct().count()==section.difficultyQuotas().size(),"DUPLICATE_DIFFICULTY","Use each difficulty once");
                require(section.difficultyQuotas().stream().mapToInt(DifficultyAllocation::count).sum()==section.questionCount(),"INVALID_DIFFICULTY_ALLOCATION","Difficulty counts must sum to section questionCount");
            }
            require(used.add(section.subtestId()),"OVERLAPPING_SECTIONS","Use one section per subtest to prevent overlapping pools");
            require(Boolean.TRUE.equals(jdbc.queryForObject("""
                SELECT EXISTS(SELECT 1 FROM subtest s WHERE s.id=:subtest AND s.exam_type_id=:type AND s.active
                  AND (:topic::uuid IS NULL OR EXISTS(SELECT 1 FROM topic t WHERE t.id=:topic AND t.subtest_id=s.id AND t.active)))
                """,p("subtest",section.subtestId()).addValue("type",master.examTypeId()).addValue("topic",section.topicId()),Boolean.class)),"INVALID_CONTENT_SCOPE","Section must belong to this exam type and topic scope");
        }
        UUID versionId=UUID.randomUUID();int number=jdbc.queryForObject("SELECT COALESCE(max(version),0)+1 FROM blueprint_version WHERE blueprint_id=:id",p("id",id),Integer.class);
        jdbc.update("INSERT INTO blueprint_version(id,blueprint_id,version,duration_seconds,scoring_policy,scoring_policy_id) VALUES(:id,:master,:number,:duration,'{}',:policy)",p("id",versionId).addValue("master",id).addValue("number",number).addValue("duration",body.durationSeconds()).addValue("policy",policy.id()));
        int position=1;for(var section:body.sections()) {
            var filter=json.createObjectNode();if(section.topicId()!=null) filter.put("topicId",section.topicId().toString());if(section.sourceYear()!=null) filter.put("sourceYear",section.sourceYear());
            if(!section.difficultyQuotas().isEmpty())filter.set("difficultyQuotas",json.valueToTree(section.difficultyQuotas()));
            if(section.repeatPolicy()!=null)filter.put("repeatPolicy",section.repeatPolicy());
            UUID sectionId=UUID.randomUUID();
            jdbc.update("INSERT INTO blueprint_section(id,blueprint_version_id,subtest_id,position,item_count,filters) VALUES(:id,:version,:subtest,:position,:count,:filter::jsonb)",p("id",sectionId).addValue("version",versionId).addValue("subtest",section.subtestId()).addValue("position",position++).addValue("count",section.questionCount()).addValue("filter",json.writeValueAsString(filter)));
            for(UUID source:section.questionSetIds())jdbc.update("INSERT INTO blueprint_section_source(section_id,question_set_id) VALUES(:section,:source)",p("section",sectionId).addValue("source",source));
        }
        return version(id,versionId);
    }
    @Override public BlueprintVersion version(UUID id,UUID versionId) {
        master(id,false);
        var p=p("id",versionId).addValue("master",id);
        var sections=jdbc.query("""
            SELECT s.id,s.position,s.subtest_id,t.name,s.item_count,s.filters,(s.filters->>'topicId')::uuid AS topic_id,(s.filters->>'sourceYear')::int AS source_year,
              question_section_available(s.id) AS available
            FROM blueprint_section s JOIN subtest t ON t.id=s.subtest_id JOIN exam_type e ON e.id=t.exam_type_id JOIN exam_category c ON c.id=e.category_id
            JOIN blueprint_version bv ON bv.id=s.blueprint_version_id JOIN scoring_policy sp ON sp.id=bv.scoring_policy_id
            WHERE s.blueprint_version_id=:id ORDER BY s.position
            """,p,(rs,row)->{
                var filter=json.readTree(rs.getString("filters"));var allocations=new ArrayList<DifficultyAllocation>();
                if(filter.has("difficultyQuotas"))for(var allocation:filter.get("difficultyQuotas"))allocations.add(new DifficultyAllocation(allocation.get("difficulty").asString(),allocation.get("count").asInt()));
                var sources=jdbc.query("SELECT question_set_id FROM blueprint_section_source WHERE section_id=:id ORDER BY question_set_id",p("id",rs.getObject("id",UUID.class)),(r,n)->r.getObject(1,UUID.class));
                return new BlueprintSection(rs.getObject("id",UUID.class),rs.getInt("position"),rs.getObject("subtest_id",UUID.class),rs.getString("name"),rs.getInt("item_count"),rs.getObject("topic_id",UUID.class),rs.getObject("source_year",Integer.class),rs.getLong("available"),sources,allocations,filter.hasNonNull("repeatPolicy")?filter.get("repeatPolicy").asString():null);
            });
        return jdbc.queryForObject("SELECT * FROM blueprint_version WHERE id=:id AND blueprint_id=:master",p,(rs,row)->new BlueprintVersion(versionId,rs.getInt("version"),rs.getString("status"),rs.getInt("duration_seconds"),rs.getObject("scoring_policy_id",UUID.class),sections,!sections.isEmpty()&&sections.stream().allMatch(s->s.available()>=s.questionCount())));
    }
    @Override public BlueprintDetail publish(UUID id,UUID versionId,PublishRequest body) {
        var master=master(id,true);activeExamType(master.examTypeId());
        if(Objects.equals(master.activeVersionId(),versionId)) return blueprint(id);
        require(Objects.equals(master.activeVersionId(),body.expectedActiveVersionId()),"STALE_BLUEPRINT_VERSION","Active version changed; reload before publishing");
        var version=version(id,versionId);require(version.status().equals("DRAFT"),"VERSION_NOT_DRAFT","Only a new draft version can be published");
        require(version.ready(),"POOL_NOT_READY","At least one section has insufficient published questions");
        jdbc.update("UPDATE blueprint_version SET status='ACTIVE',effective_at=now() WHERE id=:id",p("id",versionId));
        jdbc.update("UPDATE blueprint SET status='ACTIVE',active_version_id=:version WHERE id=:id",p("id",id).addValue("version",versionId));return blueprint(id);
    }
    @Override public BlueprintDetail status(UUID id,String status) {
        var master=master(id,true);
        if(status.equals("ACTIVE")) { activeExamType(master.examTypeId());require(master.activeVersionId()!=null,"NO_PUBLISHED_VERSION","Publish a blueprint version before activation");require(version(id,master.activeVersionId()).ready(),"POOL_NOT_READY","Published question stock is insufficient"); }
        jdbc.update("UPDATE blueprint SET status=:status WHERE id=:id",p("id",id).addValue("status",status));return blueprint(id);
    }
}
