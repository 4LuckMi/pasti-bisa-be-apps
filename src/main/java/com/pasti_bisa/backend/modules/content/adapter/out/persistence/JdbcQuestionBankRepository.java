package com.pasti_bisa.backend.modules.content.adapter.out.persistence;

import com.pasti_bisa.backend.shared.web.MutationReason;

import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.namedparam.*;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.json.JsonMapper;
import com.pasti_bisa.backend.modules.content.application.*;
import com.pasti_bisa.backend.modules.content.application.dto.QuestionBankDtos.*;
import com.pasti_bisa.backend.modules.content.application.dto.ContentDtos.*;
import com.pasti_bisa.backend.shared.web.PageResponse;
import com.pasti_bisa.backend.shared.web.BusinessException;

@Repository
public class JdbcQuestionBankRepository implements QuestionBankRepository {
    private final NamedParameterJdbcTemplate jdbc;
    private final ContentRepository content;
    private final JsonMapper json;
    public JdbcQuestionBankRepository(NamedParameterJdbcTemplate jdbc,ContentRepository content,JsonMapper json) { this.jdbc=jdbc;this.content=content;this.json=json; }
    private MapSqlParameterSource p(String key,Object value) { return new MapSqlParameterSource(key,value); }
    private static final String BANK_FROM="""
        FROM question q JOIN subtest s ON s.id=q.subtest_id JOIN exam_type e ON e.id=s.exam_type_id JOIN exam_category c ON c.id=e.category_id
        LEFT JOIN topic t ON t.id=q.topic_id LEFT JOIN import_candidate candidate ON candidate.id=q.candidate_id
        JOIN LATERAL(SELECT * FROM question_version WHERE question_id=q.id ORDER BY version DESC LIMIT 1) v ON true
        WHERE (:category::uuid IS NULL OR c.id=:category) AND (:type::uuid IS NULL OR e.id=:type)
          AND (:subtest::uuid IS NULL OR s.id=:subtest) AND (:topic::uuid IS NULL OR t.id=:topic)
          AND (:year::int IS NULL OR v.source_year=:year) AND (:batch::uuid IS NULL OR candidate.batch_id=:batch)
          AND ((:status::varchar IS NULL AND q.archived_at IS NULL) OR (:status='ARCHIVED' AND q.archived_at IS NOT NULL) OR (v.status=:status AND q.archived_at IS NULL))
          AND (:search::varchar IS NULL OR q.code ILIKE '%'||:search||'%' OR v.stem->>'text' ILIKE '%'||:search||'%' OR v.source_title ILIKE '%'||:search||'%')
        """;
    @Override public PageResponse<QuestionSummary> search(BankQuery q) {
        var params=p("category",q.categoryId()).addValue("type",q.examTypeId()).addValue("subtest",q.subtestId()).addValue("topic",q.topicId()).addValue("year",q.sourceYear()).addValue("batch",q.batchId()).addValue("status",q.status()).addValue("search",q.search()).addValue("offset",q.offset()).addValue("limit",q.limit());
        var items=jdbc.query("""
            SELECT q.id,q.code,v.id AS version_id,v.version,CASE WHEN q.archived_at IS NOT NULL THEN 'ARCHIVED' ELSE v.status END AS state,
              v.editor_revision,c.code AS category,e.name AS type,s.name AS subtest,t.name AS topic,v.source_year,v.source_title,left(v.stem->>'text',160) AS preview,candidate.batch_id
            """+BANK_FROM+" ORDER BY q.created_at DESC,q.id DESC LIMIT :limit OFFSET :offset",params,(rs,row)->new QuestionSummary(rs.getObject("id",UUID.class),rs.getString("code"),rs.getObject("version_id",UUID.class),rs.getInt("version"),rs.getString("state"),rs.getLong("editor_revision"),rs.getString("category"),rs.getString("type"),rs.getString("subtest"),rs.getString("topic"),rs.getObject("source_year",Integer.class),rs.getString("source_title"),rs.getString("preview"),rs.getObject("batch_id",UUID.class)));
        return new PageResponse<>(items,jdbc.queryForObject("SELECT count(*) "+BANK_FROM,params,Long.class),q.offset(),q.limit());
    }
    @Override public List<VersionSummary> versions(UUID id) {
        return jdbc.query("SELECT id,version,status,editor_revision,created_by,created_at FROM question_version WHERE question_id=:id ORDER BY version DESC",p("id",id),
            (rs,row)->new VersionSummary(rs.getObject("id",UUID.class),rs.getInt("version"),rs.getString("status"),rs.getLong("editor_revision"),rs.getObject("created_by",UUID.class),rs.getTimestamp("created_at").toInstant()));
    }
    @Override public DraftView draft(UUID id) {
        var question=content.question(id);long revision=jdbc.queryForObject("SELECT editor_revision FROM question_version WHERE id=:id",p("id",question.versionId()),Long.class);
        return new DraftView(question,revision);
    }
    @Override public DraftView update(UUID actor,UUID id,DraftRequest body) {
        jdbc.queryForObject("SELECT id FROM question WHERE id=:id AND archived_at IS NULL FOR UPDATE",p("id",id),UUID.class);
        var current=draft(id);
        if(!current.question().status().equals("DRAFT")) throw new BusinessException(409,"VERSION_NOT_DRAFT","Create a new version before editing published content");
        if(!current.question().versionId().equals(body.expectedVersionId()) || current.revision()!=body.expectedRevision()) throw new BusinessException(409,"REVISION_CONFLICT","Draft changed; reload before saving");
        var params=p("id",body.expectedVersionId()).addValue("stem",json.writeValueAsString(json.createObjectNode().put("text",body.content().stemText())))
            .addValue("explanation",body.content().explanation()).addValue("shuffle",body.content().shuffleAllowed());
        jdbc.update("UPDATE question_version SET stem=:stem::jsonb,explanation=:explanation,shuffle_allowed=:shuffle,editor_revision=editor_revision+1 WHERE id=:id",params);
        jdbc.update("DELETE FROM question_option WHERE question_version_id=:id",params);
        int position=1;for(var option:body.content().options()) {
            jdbc.update("""
                INSERT INTO question_option(id,question_version_id,option_code,content,original_position,correct_key,score_weight)
                VALUES(gen_random_uuid(),:version,:code,:content::jsonb,:position,:correct,:weight)
                """,p("version",body.expectedVersionId()).addValue("code",option.code()).addValue("content",json.writeValueAsString(json.createObjectNode().put("text",option.text())))
                    .addValue("position",position++).addValue("correct",option.correct()).addValue("weight",option.weight()));
        }
        return draft(id);
    }
    private static final String IMPORT_FROM="""
        FROM import_batch b JOIN stored_file f ON f.id=b.file_id WHERE (:subtest::uuid IS NULL OR b.subtest_id=:subtest)
          AND (:uploader::uuid IS NULL OR b.uploader_id=:uploader) AND (:status::varchar IS NULL OR b.status=:status)
          AND (:search::varchar IS NULL OR f.original_name ILIKE '%'||:search||'%' OR b.source_title ILIKE '%'||:search||'%' OR b.id::text=:search)
        """;
    @Override public PageResponse<ImportSummary> imports(ImportQuery q) {
        var params=p("subtest",q.subtestId()).addValue("uploader",q.uploaderId()).addValue("status",q.status()).addValue("search",q.search()).addValue("offset",q.offset()).addValue("limit",q.limit());
        var items=jdbc.query("SELECT b.id,b.uploader_id,f.original_name "+IMPORT_FROM+" ORDER BY b.created_at DESC,b.id DESC LIMIT :limit OFFSET :offset",params,
            (rs,row)->new ImportSummary(content.importById(rs.getObject("id",UUID.class)),rs.getString("original_name"),rs.getObject("uploader_id",UUID.class)));
        return new PageResponse<>(items,jdbc.queryForObject("SELECT count(*) "+IMPORT_FROM,params,Long.class),q.offset(),q.limit());
    }
    @Override public ImportResponse retry(UUID actor,UUID id) {
        var job=jdbc.queryForObject("SELECT id,status FROM job WHERE type='IMPORT_PDF' AND dedup_key=:key FOR UPDATE",p("key","import:"+id),
            (rs,row)->new JobState(rs.getObject("id",UUID.class),rs.getString("status")));
        if(!job.status().equals("FAILED")) throw new BusinessException(409,"JOB_NOT_RETRYABLE","Wait for automatic retries; only exhausted failed jobs can be manually retried");
        jdbc.update("UPDATE job SET status='QUEUED',retry_count=0,last_error=NULL,lease_until=NULL,lease_token=NULL,lease_owner=NULL,run_after=now() WHERE id=:id",p("id",job.id()));
        jdbc.update("UPDATE import_batch SET status='QUEUED',error_code=NULL,updated_at=now() WHERE id=:id",p("id",id));return content.importById(id);
    }
    private record JobState(UUID id,String status) {}
    @Override public StaffDashboard dashboard() {
        return jdbc.queryForObject("""
            SELECT (SELECT count(*) FROM question q WHERE q.archived_at IS NULL AND EXISTS(SELECT 1 FROM question_version v WHERE v.question_id=q.id AND v.status='PUBLISHED')) AS published,
              (SELECT count(*) FROM question q JOIN LATERAL(SELECT status FROM question_version WHERE question_id=q.id ORDER BY version DESC LIMIT 1) v ON true WHERE q.archived_at IS NULL AND v.status='DRAFT') AS draft,
              (SELECT count(*) FROM question WHERE archived_at IS NOT NULL) AS archived,(SELECT count(*) FROM import_batch) AS imports,
              (SELECT count(*) FROM import_batch WHERE status='NEEDS_INPUT') AS needs_input,(SELECT count(*) FROM import_batch WHERE status='FAILED') AS failed
            """,new MapSqlParameterSource(),(rs,row)->new StaffDashboard(rs.getLong("published"),rs.getLong("draft"),rs.getLong("archived"),rs.getLong("imports"),rs.getLong("needs_input"),rs.getLong("failed")));
    }
    @Override public PageResponse<ContentAudit> audit(int offset,int limit) {
        var items=jdbc.query("SELECT id,actor_id,action,entity_id,reason,request_id,occurred_at FROM audit_event WHERE entity_type='CONTENT' ORDER BY occurred_at DESC,id DESC LIMIT :limit OFFSET :offset",p("limit",limit).addValue("offset",offset),
            (rs,row)->new ContentAudit(rs.getObject("id",UUID.class),rs.getObject("actor_id",UUID.class),rs.getString("action"),rs.getObject("entity_id",UUID.class),rs.getString("reason"),rs.getString("request_id"),rs.getTimestamp("occurred_at").toInstant()));
        return new PageResponse<>(items,jdbc.queryForObject("SELECT count(*) FROM audit_event WHERE entity_type='CONTENT'",new MapSqlParameterSource(),Long.class),offset,limit);
    }
    private static final String REQUIREMENTS_FROM="""
        FROM blueprint b JOIN blueprint_version v ON v.blueprint_id=b.id JOIN blueprint_section s ON s.blueprint_version_id=v.id
        JOIN subtest t ON t.id=s.subtest_id WHERE b.mode='TRYOUT' AND b.status<>'ARCHIVED'
        AND v.version=(SELECT max(version) FROM blueprint_version WHERE blueprint_id=b.id)
        """;
    @Override public PageResponse<PoolRequirement> requirements(int offset,int limit) {
        var items=jdbc.query("""
            SELECT b.id,b.title,v.id AS version_id,v.version,s.position,s.subtest_id,t.name,s.item_count,
              (SELECT count(*) FROM question q JOIN LATERAL(SELECT source_year FROM question_version WHERE question_id=q.id AND status='PUBLISHED' ORDER BY version DESC LIMIT 1) qv ON true
               WHERE q.subtest_id=s.subtest_id AND q.archived_at IS NULL AND (s.filters->>'topicId' IS NULL OR q.topic_id=(s.filters->>'topicId')::uuid)
                 AND (s.filters->>'sourceYear' IS NULL OR qv.source_year=(s.filters->>'sourceYear')::int)) AS available
            """+REQUIREMENTS_FROM+" ORDER BY b.id,s.position LIMIT :limit OFFSET :offset",p("limit",limit).addValue("offset",offset),
            (rs,row)->new PoolRequirement(rs.getObject("id",UUID.class),rs.getString("title"),rs.getObject("version_id",UUID.class),rs.getInt("version"),rs.getInt("position"),rs.getObject("subtest_id",UUID.class),rs.getString("name"),rs.getInt("item_count"),rs.getLong("available")));
        return new PageResponse<>(items,jdbc.queryForObject("SELECT count(*) "+REQUIREMENTS_FROM,new MapSqlParameterSource(),Long.class),offset,limit);
    }
    @Override public void audit(UUID actor,String action,UUID id,String reason,String requestId) {
        jdbc.update("INSERT INTO audit_event(id,actor_id,action,entity_type,entity_id,reason,request_id) VALUES(gen_random_uuid(),:actor,:action,'CONTENT',:id,:reason,:rid)",p("actor",actor).addValue("action",action).addValue("id",id).addValue("reason",reason).addValue("rid",requestId));
    }
}
