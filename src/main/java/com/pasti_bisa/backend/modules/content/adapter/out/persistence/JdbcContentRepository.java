package com.pasti_bisa.backend.modules.content.adapter.out.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.json.JsonMapper;
import com.pasti_bisa.backend.modules.content.application.ContentRepository;
import com.pasti_bisa.backend.modules.content.application.dto.ContentDtos.*;
import com.pasti_bisa.backend.modules.content.domain.QuestionValidator;
import com.pasti_bisa.backend.modules.content.domain.ContentModels.ExtractedPage;
import com.pasti_bisa.backend.modules.content.domain.ContentModels.ParsedCandidate;
import com.pasti_bisa.backend.shared.web.BusinessException;

@Repository
public class JdbcContentRepository implements ContentRepository {
    private final NamedParameterJdbcTemplate jdbc;
    private final JsonMapper json;
    public JdbcContentRepository(NamedParameterJdbcTemplate jdbc,JsonMapper json) { this.jdbc=jdbc;this.json=json; }
    private MapSqlParameterSource p(String key,Object value) { return new MapSqlParameterSource(key,value); }
    @Override public List<TaxonomyResponse> taxonomy() {
        return jdbc.query("""
                SELECT c.id AS category_id,c.code AS category_code,c.name AS category_name,
                       e.id AS type_id,e.code AS type_code,e.name AS type_name,
                       s.id AS subtest_id,s.name AS subtest_name,t.id AS topic_id,t.name AS topic_name
                FROM exam_category c JOIN exam_type e ON e.category_id=c.id AND e.active
                JOIN subtest s ON s.exam_type_id=e.id AND s.active
                LEFT JOIN topic t ON t.subtest_id=s.id AND t.active WHERE c.active
                ORDER BY c.code,e.code,s.name,t.name
                """,new MapSqlParameterSource(),(rs,row)->new TaxonomyResponse(rs.getObject("category_id",UUID.class),
                    rs.getString("category_code"),rs.getString("category_name"),rs.getObject("type_id",UUID.class),
                    rs.getString("type_code"),rs.getString("type_name"),rs.getObject("subtest_id",UUID.class),
                    rs.getString("subtest_name"),rs.getObject("topic_id",UUID.class),rs.getString("topic_name")));
    }
    @Override public UUID actor(String email) {
        return jdbc.queryForObject("SELECT id FROM app_user WHERE email_canonical=:email AND status='ACTIVE' FOR UPDATE",p("email",email),UUID.class);
    }
    @Override public void validateScope(UUID subtestId,UUID topicId) {
        if(!Boolean.TRUE.equals(jdbc.queryForObject("""
                SELECT EXISTS (SELECT 1 FROM subtest s JOIN exam_type e ON e.id=s.exam_type_id
                  JOIN exam_category c ON c.id=e.category_id WHERE s.id=:subtest AND s.active AND e.active AND c.active
                  AND (:topic::uuid IS NULL OR EXISTS (SELECT 1 FROM topic t WHERE t.id=:topic::uuid AND t.subtest_id=s.id AND t.active)))
                """,p("subtest",subtestId).addValue("topic",topicId),Boolean.class)))
            throw new BusinessException(400,"INVALID_CONTENT_SCOPE","Subtest/topic does not belong to an active category");
    }
    @Override public Optional<UUID> duplicate(UUID actor,String hash,ImportMetadata metadata) {
        return jdbc.query("""
                SELECT b.id FROM import_batch b JOIN stored_file f ON f.id=b.file_id
                WHERE f.uploaded_by=:actor AND f.sha256=:hash AND f.purpose='IMPORT_SOURCE'
                  AND b.subtest_id=:subtest AND b.topic_id IS NOT DISTINCT FROM :topic::uuid ORDER BY b.created_at LIMIT 1
                """,p("actor",actor).addValue("hash",hash).addValue("subtest",metadata.subtestId()).addValue("topic",metadata.topicId()),
                (rs,row)->rs.getObject(1,UUID.class)).stream().findFirst();
    }
    @Override public UUID createImport(UUID actor,StoredUpload file,ImportMetadata metadata) {
        UUID id=UUID.randomUUID();
        var params=p("id",id).addValue("fileId",file.id()).addValue("actor",actor).addValue("path",file.path())
                .addValue("size",file.size()).addValue("hash",file.sha256()).addValue("name",file.originalName())
                .addValue("subtest",metadata.subtestId()).addValue("topic",metadata.topicId()).addValue("title",metadata.sourceTitle().trim())
                .addValue("year",metadata.sourceYear(),java.sql.Types.INTEGER);
        jdbc.update("""
                INSERT INTO stored_file(id,relative_path,sha256,size_bytes,mime_type,uploaded_by,original_name,purpose)
                VALUES (:fileId,:path,:hash,:size,'application/pdf',:actor,:name,'IMPORT_SOURCE')
                """,params);
        jdbc.update("""
                INSERT INTO import_batch(id,file_id,uploader_id,status,subtest_id,topic_id,source_title,source_year)
                VALUES (:id,:fileId,:actor,'QUEUED',:subtest,:topic,:title,:year)
                """,params);
        jdbc.update("""
                INSERT INTO job(id,type,dedup_key,payload) VALUES (gen_random_uuid(),'IMPORT_PDF',:key,jsonb_build_object('batchId',:batchId::text))
                """,p("key","import:"+id).addValue("batchId",id.toString()));
        audit(actor,"IMPORT_UPLOADED",id);
        return id;
    }
    private static final String IMPORT_SELECT="""
            SELECT b.*,j.status AS job_status,j.last_error AS job_error FROM import_batch b
            LEFT JOIN job j ON j.dedup_key='import:'||b.id::text
            """;
    private final RowMapper<ImportResponse> importMapper=(rs,row)->new ImportResponse(rs.getObject("id",UUID.class),rs.getString("status"),
            rs.getObject("file_id",UUID.class),rs.getObject("subtest_id",UUID.class),rs.getObject("topic_id",UUID.class),rs.getString("source_title"),
            rs.getObject("source_year",Integer.class),rs.getObject("page_count",Integer.class),rs.getInt("candidate_count"),rs.getInt("valid_count"),
            rs.getInt("invalid_count"),rs.getString("error_code"),rs.getString("job_status"),rs.getString("job_error"),rs.getTimestamp("created_at").toInstant());
    @Override public ImportResponse importById(UUID id) { return jdbc.queryForObject(IMPORT_SELECT+" WHERE b.id=:id",p("id",id),importMapper); }
    @Override public List<ImportResponse> imports(int offset,int limit) {
        return jdbc.query(IMPORT_SELECT+" ORDER BY b.created_at DESC LIMIT :limit OFFSET :offset",p("limit",limit).addValue("offset",offset),importMapper);
    }
    @Override public ImportWork work(UUID id) {
        return jdbc.queryForObject("""
                SELECT b.id,f.relative_path,b.subtest_id,b.topic_id FROM import_batch b JOIN stored_file f ON f.id=b.file_id WHERE b.id=:id
                """,p("id",id),(rs,row)->new ImportWork(id,rs.getString("relative_path"),rs.getObject("subtest_id",UUID.class),rs.getObject("topic_id",UUID.class)));
    }
    @Override public void extracted(UUID id,List<ExtractedPage> pages,List<ParsedCandidate> candidates) {
        for(var candidate:candidates) jdbc.update("""
                INSERT INTO import_candidate(batch_id,source_page,source_item_no,extracted_content,validation_errors)
                VALUES (:id,:page,:number,:content::jsonb,:errors::jsonb)
                ON CONFLICT (batch_id,source_page,source_item_no) DO NOTHING
                """,p("id",id).addValue("page",candidate.page()).addValue("number",candidate.number())
                    .addValue("content",json.writeValueAsString(candidate.content())).addValue("errors",json.writeValueAsString(candidate.errors())));
        jdbc.update("UPDATE import_batch SET page_count=:count,error_code=NULL,completed_at=now(),updated_at=now() WHERE id=:id",
                p("id",id).addValue("count",pages.size()));
        refreshCounts(id);
    }
    @Override public void extractionFailed(UUID id) {
        jdbc.update("UPDATE import_batch SET status='FAILED',error_code='EXTRACTION_FAILED',updated_at=now() WHERE id=:id",p("id",id));
    }
    @Override public List<CandidateResponse> candidates(UUID id,int offset,int limit) {
        importById(id);
        return jdbc.query("SELECT * FROM import_candidate WHERE batch_id=:id ORDER BY source_page,source_item_no LIMIT :limit OFFSET :offset",
                p("id",id).addValue("limit",limit).addValue("offset",offset),this::candidate);
    }
    private CandidateResponse candidate(ResultSet rs,int row) throws SQLException {
        String draft=rs.getString("draft_content");
        return new CandidateResponse(rs.getObject("id",UUID.class),rs.getInt("source_page"),rs.getInt("source_item_no"),
                json.readValue(rs.getString("extracted_content"),QuestionContent.class),draft==null?null:json.readValue(draft,QuestionContent.class),
                json.readValue(rs.getString("validation_errors"),json.getTypeFactory().constructCollectionType(List.class,String.class)),
                rs.getObject("resolved_question_id",UUID.class));
    }
    @Override public void correctCandidate(UUID batchId,UUID candidateId,QuestionContent content,List<String> errors) {
        CandidateResponse candidate=lockCandidate(batchId,candidateId);
        if(candidate.questionId()!=null) throw new BusinessException(409,"CANDIDATE_ALREADY_SAVED","Create a new question version instead");
        jdbc.update("UPDATE import_candidate SET draft_content=:content::jsonb,validation_errors=:errors::jsonb WHERE id=:id",
                p("id",candidateId).addValue("content",json.writeValueAsString(content)).addValue("errors",json.writeValueAsString(errors)));
        refreshCounts(batchId);
    }
    private CandidateResponse lockCandidate(UUID batchId,UUID id) {
        return jdbc.queryForObject("SELECT * FROM import_candidate WHERE batch_id=:batch AND id=:id FOR UPDATE",p("batch",batchId).addValue("id",id),this::candidate);
    }
    @Override public QuestionResponse saveCandidate(UUID actor,UUID batchId,UUID candidateId) {
        CandidateResponse candidate=lockCandidate(batchId,candidateId);
        if(candidate.questionId()!=null) return question(candidate.questionId());
        var batch=importById(batchId);
        var content=candidate.draftContent()==null?candidate.extractedContent():candidate.draftContent();
        var question=createQuestion(actor,new CreateQuestionRequest(batch.subtestId(),batch.topicId(),batch.sourceTitle(),batch.sourceYear(),content),candidateId,candidate.sourcePage());
        jdbc.update("UPDATE import_candidate SET resolved_question_id=:question WHERE id=:id",p("question",question.id()).addValue("id",candidateId));
        return question;
    }
    @Override public QuestionResponse createQuestion(UUID actor,CreateQuestionRequest request,UUID candidateId,Integer sourcePage) {
        validateScope(request.subtestId(),request.topicId());
        UUID id=UUID.randomUUID();
        var params=p("id",id).addValue("subtest",request.subtestId()).addValue("topic",request.topicId())
                .addValue("candidate",candidateId).addValue("code","Q-"+id);
        jdbc.update("INSERT INTO question(id,subtest_id,topic_id,candidate_id,code) VALUES (:id,:subtest,:topic,:candidate,:code)",params);
        insertVersion(actor,id,1,request.content(),request.sourceYear(),request.sourceTitle(),sourcePage);
        audit(actor,"QUESTION_DRAFT_CREATED",id);
        return question(id);
    }
    private void insertVersion(UUID actor,UUID questionId,int number,QuestionContent content,Integer year,String title,Integer page) {
        UUID version=UUID.randomUUID();
        var stem=json.createObjectNode().put("text",content.stemText());
        var params=p("id",version).addValue("questionId",questionId).addValue("number",number).addValue("actor",actor)
                .addValue("stem",json.writeValueAsString(stem)).addValue("explanation",content.explanation()).addValue("shuffle",content.shuffleAllowed())
                .addValue("year",year,java.sql.Types.INTEGER).addValue("title",title).addValue("page",page,java.sql.Types.INTEGER);
        jdbc.update("""
                INSERT INTO question_version(id,question_id,version,stem,explanation,source_year,status,shuffle_allowed,created_by,source_title,source_page)
                VALUES (:id,:questionId,:number,:stem::jsonb,:explanation,:year,'DRAFT',:shuffle,:actor,:title,:page)
                """,params);
        int position=1;
        if(content.options()!=null) for(var option:content.options()) jdbc.update("""
                INSERT INTO question_option(id,question_version_id,option_code,content,original_position,correct_key,score_weight)
                VALUES (gen_random_uuid(),:version,:code,:content::jsonb,:position,:correct,:weight)
                """,p("version",version).addValue("code",option.code()).addValue("content",json.writeValueAsString(json.createObjectNode().put("text",option.text())))
                    .addValue("position",position++).addValue("correct",option.correct()).addValue("weight",option.weight()==null?java.math.BigDecimal.ZERO:option.weight()));
    }
    @Override public QuestionResponse question(UUID id) {
        var question=jdbc.queryForObject("""
                SELECT q.id,q.code,q.subtest_id,q.topic_id,v.id AS version_id,v.version,v.status,v.source_year,v.source_title,v.source_page,
                       v.stem->>'text' AS stem,v.explanation,v.shuffle_allowed FROM question q
                JOIN LATERAL (SELECT * FROM question_version WHERE question_id=q.id ORDER BY version DESC LIMIT 1) v ON TRUE WHERE q.id=:id
                """,p("id",id),(rs,row)->new QuestionResponse(id,rs.getObject("version_id",UUID.class),rs.getString("code"),rs.getInt("version"),rs.getString("status"),
                    rs.getObject("subtest_id",UUID.class),rs.getObject("topic_id",UUID.class),rs.getObject("source_year",Integer.class),rs.getString("source_title"),
                    rs.getObject("source_page",Integer.class),new QuestionContent(rs.getString("stem"),List.of(),rs.getString("explanation"),rs.getBoolean("shuffle_allowed"))));
        var options=jdbc.query("SELECT * FROM question_option WHERE question_version_id=:id ORDER BY original_position",p("id",question.versionId()),
                (rs,row)->new OptionDraft(rs.getString("option_code"),json.readTree(rs.getString("content")).get("text").asString(),rs.getBoolean("correct_key"),rs.getBigDecimal("score_weight")));
        return new QuestionResponse(id,question.versionId(),question.code(),question.version(),question.status(),question.subtestId(),question.topicId(),question.sourceYear(),
                question.sourceTitle(),question.sourcePage(),new QuestionContent(question.content().stemText(),options,question.content().explanation(),question.content().shuffleAllowed()));
    }
    @Override public List<QuestionResponse> questions(UUID subtest,String search,int offset,int limit) {
        return jdbc.query("""
                SELECT q.id FROM question q WHERE q.archived_at IS NULL AND (:subtest::uuid IS NULL OR q.subtest_id=:subtest::uuid)
                AND (:search::varchar IS NULL OR q.code ILIKE '%'||:search||'%' OR EXISTS (
                    SELECT 1 FROM question_version v WHERE v.question_id=q.id AND v.stem->>'text' ILIKE '%'||:search||'%'))
                ORDER BY q.created_at DESC LIMIT :limit OFFSET :offset
                """,p("subtest",subtest).addValue("search",search,java.sql.Types.VARCHAR).addValue("limit",limit).addValue("offset",offset),
                (rs,row)->rs.getObject(1,UUID.class)).stream().map(this::question).toList();
    }
    @Override public QuestionResponse newVersion(UUID actor,UUID id,QuestionContent content) {
        jdbc.queryForObject("SELECT id FROM question WHERE id=:id AND archived_at IS NULL FOR UPDATE",p("id",id),UUID.class);
        var current=question(id);
        insertVersion(actor,id,current.version()+1,content,current.sourceYear(),current.sourceTitle(),current.sourcePage());
        audit(actor,"QUESTION_VERSION_CREATED",id);
        return question(id);
    }
    @Override public QuestionResponse publish(UUID actor,UUID id) {
        jdbc.queryForObject("SELECT id FROM question WHERE id=:id AND archived_at IS NULL FOR UPDATE",p("id",id),UUID.class);
        var current=question(id);
        if(current.status().equals("PUBLISHED")) return current;
        var errors=new ArrayList<>(QuestionValidator.publicationErrors(current.content().toDomain()));
        if(current.sourceTitle()==null||current.sourceTitle().isBlank()) errors.add("SOURCE_MISSING");
        if(!errors.isEmpty()) throw new BusinessException(409,"QUESTION_INCOMPLETE","Cannot publish: "+String.join(", ",errors));
        jdbc.update("UPDATE question_version SET status='PUBLISHED' WHERE id=:id AND status='DRAFT'",p("id",current.versionId()));
        audit(actor,"QUESTION_PUBLISHED",id);
        return question(id);
    }
    @Override public void archive(UUID actor,UUID id) {
        jdbc.queryForObject("SELECT id FROM question WHERE id=:id FOR UPDATE",p("id",id),UUID.class);
        jdbc.update("UPDATE question SET archived_at=COALESCE(archived_at,now()) WHERE id=:id",p("id",id));
        audit(actor,"QUESTION_ARCHIVED",id);
    }
    @Override public String sourcePath(UUID id) { return work(id).path(); }
    private void refreshCounts(UUID id) {
        jdbc.update("""
                UPDATE import_batch SET candidate_count=counts.total,valid_count=counts.valid,invalid_count=counts.total-counts.valid,
                  status=CASE WHEN counts.total>0 AND counts.valid=counts.total THEN 'READY' ELSE 'NEEDS_INPUT' END,updated_at=now()
                FROM (SELECT count(*) AS total,count(*) FILTER (WHERE validation_errors='[]'::jsonb) AS valid
                      FROM import_candidate WHERE batch_id=:id) counts WHERE import_batch.id=:id
                """,p("id",id));
    }
    private void audit(UUID actor,String action,UUID id) {
        jdbc.update("INSERT INTO audit_event(id,actor_id,action,entity_type,entity_id) VALUES (gen_random_uuid(),:actor,:action,'CONTENT',:id)",
                p("actor",actor).addValue("action",action).addValue("id",id));
    }
}
