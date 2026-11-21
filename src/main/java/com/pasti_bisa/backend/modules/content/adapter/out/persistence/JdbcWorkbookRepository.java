package com.pasti_bisa.backend.modules.content.adapter.out.persistence;

import java.util.*;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.*;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.json.JsonMapper;
import com.pasti_bisa.backend.modules.content.application.*;
import com.pasti_bisa.backend.modules.content.application.dto.WorkbookDtos.*;
import com.pasti_bisa.backend.modules.content.application.dto.ContentDtos.*;
import com.pasti_bisa.backend.modules.content.domain.QuestionValidator;
import com.pasti_bisa.backend.shared.web.*;

@Repository
public class JdbcWorkbookRepository implements WorkbookRepository {
    private final NamedParameterJdbcTemplate jdbc;private final JsonMapper json;private final ContentRepository content;
    public JdbcWorkbookRepository(NamedParameterJdbcTemplate jdbc,JsonMapper json,ContentRepository content){this.jdbc=jdbc;this.json=json;this.content=content;}
    private MapSqlParameterSource p(String key,Object value){return new MapSqlParameterSource(key,value);}
    private static void require(boolean condition,String code,String message){if(!condition)throw new BusinessException(409,code,message);}
    private void audit(UUID actor,String action,UUID id,String reason){
        jdbc.update("INSERT INTO audit_event(id,actor_id,action,entity_type,entity_id,reason,request_id) VALUES(gen_random_uuid(),:actor,:action,'CONTENT',:id,:reason,:request)",
            p("actor",actor).addValue("action",action).addValue("id",id).addValue("reason",reason).addValue("request",org.slf4j.MDC.get("requestId")));
    }
    private static final String SOURCE_SELECT="""
            SELECT qs.*,(SELECT count(*) FROM question_set_item i WHERE i.question_set_id=qs.id) AS question_count,
            (SELECT count(*) FROM question_set_item i JOIN published_question_pool p ON p.question_id=i.question_id WHERE i.question_set_id=qs.id) AS published_count
            FROM question_set qs
            """;
    private final RowMapper<QuestionSource> sourceMapper=(r,n)->new QuestionSource(r.getObject("id",UUID.class),r.getObject("exam_type_id",UUID.class),
            r.getString("code"),r.getInt("source_year"),r.getString("title"),r.getString("status"),r.getLong("question_count"),r.getLong("published_count"));
    @Override public PageResponse<QuestionSource> sources(SourceQuery q){
        var params=p("type",q.examTypeId()).addValue("year",q.sourceYear()).addValue("status",q.status()).addValue("search",q.search()).addValue("offset",q.offset()).addValue("limit",q.limit());
        String scope=" WHERE (:type::uuid IS NULL OR qs.exam_type_id=:type) AND (:year::int IS NULL OR qs.source_year=:year) AND (:status::varchar IS NULL OR qs.status=:status) AND (:search::varchar IS NULL OR position(lower(:search) in lower(qs.title||' '||qs.code))>0)";
        return new PageResponse<>(jdbc.query(SOURCE_SELECT+scope+" ORDER BY qs.source_year DESC,qs.code,qs.id LIMIT :limit OFFSET :offset",params,sourceMapper),
            jdbc.queryForObject("SELECT count(*) FROM question_set qs"+scope,params,Long.class),q.offset(),q.limit());
    }
    @Override public QuestionSource source(UUID id){return jdbc.queryForObject(SOURCE_SELECT+" WHERE qs.id=:id",p("id",id),sourceMapper);}
    private QuestionSource active(UUID id,boolean lock){
        if(lock)jdbc.queryForObject("SELECT id FROM question_set WHERE id=:id FOR UPDATE",p("id",id),UUID.class);
        var source=source(id);require(source.status().equals("ACTIVE"),"SOURCE_ARCHIVED","This source is archived");
        require(Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM exam_type e JOIN exam_category c ON c.id=e.category_id WHERE e.id=:id AND e.active AND c.active)",p("id",source.examTypeId()),Boolean.class)),"INVALID_CONTENT_SCOPE","Source exam type and category must be active");return source;
    }
    @Override public QuestionSource createSource(UUID actor,SourceRequest body){
        require(Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM exam_type e JOIN exam_category c ON c.id=e.category_id WHERE e.id=:id AND e.active AND c.active)",p("id",body.examTypeId()),Boolean.class)),"INVALID_CONTENT_SCOPE","Exam type/category must be active");
        UUID id=UUID.randomUUID();jdbc.update("INSERT INTO question_set(id,exam_type_id,code,source_year,title,created_by) VALUES(:id,:type,:code,:year,:title,:actor)",
            p("id",id).addValue("type",body.examTypeId()).addValue("code",body.code()).addValue("year",body.sourceYear()).addValue("title",body.title().trim()).addValue("actor",actor));
        audit(actor,"QUESTION_SOURCE_CREATED",id,body.reason());return source(id);
    }
    @Override public QuestionSource status(UUID actor,UUID id,SourceStatus body){
        jdbc.queryForObject("SELECT id FROM question_set WHERE id=:id FOR UPDATE",p("id",id),UUID.class);
        jdbc.update("UPDATE question_set SET status=:status WHERE id=:id",p("id",id).addValue("status",body.status()));
        audit(actor,"QUESTION_SOURCE_STATUS_CHANGED",id,body.reason());return source(id);
    }
    @Override public List<TemplateSheet> templateSheets(UUID id){
        var source=active(id,false);
        var sheets=jdbc.query("SELECT id,code FROM subtest WHERE exam_type_id=:id AND active ORDER BY code,id",p("id",source.examTypeId()),
                (r,n)->new TemplateSheet(r.getString("code").substring(0,Math.min(31,r.getString("code").length())),r.getObject("id",UUID.class)));
        require(!sheets.isEmpty()&&sheets.size()<=30,"INVALID_TEMPLATE_SCOPE","Use an exam type with 1 to 30 active subtests");
        require(sheets.stream().map(TemplateSheet::name).distinct().count()==sheets.size(),"AMBIGUOUS_SHEET_NAMES","Subtest codes must be distinct within Excel's 31-character sheet limit");return sheets;
    }
    @Override public QuestionSource link(UUID actor,UUID id,LinkSourceQuestion body){
        var source=active(id,true);
        UUID subtest=jdbc.queryForObject("SELECT subtest_id FROM question WHERE id=:id AND archived_at IS NULL FOR UPDATE",p("id",body.questionId()),UUID.class);
        content.validateScope(subtest,null);
        require(Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM subtest WHERE id=:id AND exam_type_id=:type)",p("id",subtest).addValue("type",source.examTypeId()),Boolean.class)),"INVALID_SOURCE_SCOPE","Question must belong to the source exam type");
        var params=p("source",id).addValue("question",body.questionId()).addValue("type",source.examTypeId()).addValue("subtest",subtest).addValue("code",body.sourceItemCode());
        var existing=jdbc.query("SELECT source_item_code FROM question_set_item WHERE question_set_id=:source AND question_id=:question",params,(r,n)->r.getString(1));
        if(!existing.isEmpty()){require(existing.getFirst().equals(body.sourceItemCode()),"SOURCE_CODE_IMMUTABLE","Existing source question codes cannot be renamed");return source(id);}
        jdbc.update("INSERT INTO question_set_item(question_set_id,question_id,exam_type_id,subtest_id,source_item_code) VALUES(:source,:question,:type,:subtest,:code)",params);
        audit(actor,"QUESTION_LINKED_TO_SOURCE",id,body.reason());return source(id);
    }
    @Override public PageResponse<SourceItem> sourceItems(UUID id,int offset,int limit){
        source(id);var params=p("id",id).addValue("offset",offset).addValue("limit",limit);
        var items=jdbc.query("""
            SELECT i.*,q.archived_at,v.id AS version_id,v.status,v.difficulty FROM question_set_item i JOIN question q ON q.id=i.question_id
            JOIN LATERAL(SELECT id,status,difficulty FROM question_version WHERE question_id=q.id ORDER BY version DESC LIMIT 1) v ON true
            WHERE i.question_set_id=:id ORDER BY i.subtest_id,i.source_item_code,i.question_id LIMIT :limit OFFSET :offset
            """,params,(r,n)->new SourceItem(r.getObject("question_id",UUID.class),r.getObject("subtest_id",UUID.class),r.getString("source_item_code"),r.getObject("version_id",UUID.class),r.getString("status"),r.getString("difficulty"),r.getObject("archived_at")!=null));
        return new PageResponse<>(items,jdbc.queryForObject("SELECT count(*) FROM question_set_item WHERE question_set_id=:id",params,Long.class),offset,limit);
    }
    @Override public Optional<UUID> duplicate(UUID actor,String hash,UUID source,String mappingHash){
        return jdbc.query("""
            SELECT b.id FROM import_batch b JOIN stored_file f ON f.id=b.file_id WHERE b.input_format='XLSX'
            AND b.uploader_id=:actor AND b.question_set_id=:source AND b.mapping_hash=:mapping AND f.sha256=:hash ORDER BY b.created_at,b.id LIMIT 1
            """,p("actor",actor).addValue("hash",hash).addValue("source",source).addValue("mapping",mappingHash),(r,n)->r.getObject(1,UUID.class)).stream().findFirst();
    }
    @Override public UUID createImport(UUID actor,StoredUpload file,WorkbookMetadata metadata,String mappingHash){
        var source=active(metadata.questionSetId(),true);var names=new HashSet<String>();var subtests=new HashSet<UUID>();
        for(var mapping:metadata.sheets()){
            require(names.add(mapping.sheetName().toLowerCase(Locale.ROOT))&&subtests.add(mapping.subtestId()),"DUPLICATE_SHEET_MAPPING","Each sheet and subtest must appear once");
            require(Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM subtest WHERE id=:id AND exam_type_id=:type AND active)",p("id",mapping.subtestId()).addValue("type",source.examTypeId()),Boolean.class)),"INVALID_CONTENT_SCOPE","Mapped subtest must belong to the source exam type");
        }
        UUID id=UUID.randomUUID();var params=p("id",id).addValue("file",file.id()).addValue("path",file.path()).addValue("hash",file.sha256())
            .addValue("size",file.size()).addValue("actor",actor).addValue("name",file.originalName()).addValue("source",source.id()).addValue("title",source.title())
            .addValue("year",source.sourceYear()).addValue("mapping",mappingHash);
        jdbc.update("""
            INSERT INTO stored_file(id,relative_path,sha256,size_bytes,mime_type,uploaded_by,original_name,purpose)
            VALUES(:file,:path,:hash,:size,'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',:actor,:name,'IMPORT_SOURCE')
            """,params);
        jdbc.update("""
            INSERT INTO import_batch(id,file_id,uploader_id,question_set_id,input_format,mapping_hash,status,source_title,source_year)
            VALUES(:id,:file,:actor,:source,'XLSX',:mapping,'QUEUED',:title,:year)
            """,params);
        for(var mapping:metadata.sheets())jdbc.update("INSERT INTO import_sheet(batch_id,subtest_id,sheet_name) VALUES(:batch,:subtest,:name)",p("batch",id).addValue("subtest",mapping.subtestId()).addValue("name",mapping.sheetName()));
        jdbc.update("INSERT INTO job(id,type,dedup_key,payload) VALUES(gen_random_uuid(),'IMPORT_XLSX',:key,jsonb_build_object('batchId',:id::text))",p("key","import:"+id).addValue("id",id.toString()));
        audit(actor,"WORKBOOK_UPLOADED",id,"WORKBOOK_IMPORT");return id;
    }
    @Override public WorkbookImport get(UUID id){
        var sheets=jdbc.query("""
            SELECT s.*,(SELECT count(*) FROM import_candidate c WHERE c.sheet_id=s.id AND c.validation_errors<>'[]'::jsonb) AS invalid_count
            FROM import_sheet s WHERE s.batch_id=:id ORDER BY s.sheet_index NULLS LAST,s.sheet_name
            """,p("id",id),(r,n)->new WorkbookSheet(r.getObject("id",UUID.class),r.getString("sheet_name"),r.getObject("subtest_id",UUID.class),r.getObject("sheet_index",Integer.class),r.getString("status"),r.getInt("candidate_count"),r.getInt("invalid_count")));
        return jdbc.queryForObject("SELECT b.*,j.status AS job_status FROM import_batch b LEFT JOIN job j ON j.dedup_key='import:'||b.id::text WHERE b.id=:id AND b.input_format='XLSX'",p("id",id),
            (r,n)->new WorkbookImport(id,r.getObject("question_set_id",UUID.class),r.getObject("file_id",UUID.class),r.getString("input_format"),r.getString("status"),r.getInt("candidate_count"),r.getInt("valid_count"),r.getInt("invalid_count"),r.getString("error_code"),r.getString("job_status"),r.getTimestamp("created_at").toInstant(),sheets));
    }
    @Override public WorkbookWork work(UUID id){
        var info=get(id);var source=source(info.questionSetId());String path=jdbc.queryForObject("SELECT relative_path FROM stored_file WHERE id=:id",p("id",info.fileId()),String.class);
        return new WorkbookWork(id,path,source.id(),source.examTypeId(),source.sourceYear(),source.title(),info.sheets().stream().map(s->new SheetMapping(s.sheetName(),s.subtestId())).toList());
    }
    @Override public PageResponse<WorkbookImport> imports(WorkbookQuery q){
        var params=p("source",q.questionSetId()).addValue("status",q.status()).addValue("offset",q.offset()).addValue("limit",q.limit());
        String filter=" FROM import_batch WHERE input_format='XLSX' AND (:source::uuid IS NULL OR question_set_id=:source) AND (:status::varchar IS NULL OR status=:status)";
        var values=jdbc.query("SELECT id"+filter+" ORDER BY created_at DESC,id DESC LIMIT :limit OFFSET :offset",params,(r,n)->r.getObject(1,UUID.class)).stream().map(this::get).toList();
        return new PageResponse<>(values,jdbc.queryForObject("SELECT count(*)"+filter,params,Long.class),q.offset(),q.limit());
    }
    private UUID expected(UUID source,UUID subtest,String code){
        return jdbc.query("""
            SELECT v.id FROM question_set_item i JOIN question_version v ON v.question_id=i.question_id
            WHERE i.question_set_id=:source AND i.subtest_id=:subtest AND i.source_item_code=:code ORDER BY v.version DESC LIMIT 1
            """,p("source",source).addValue("subtest",subtest).addValue("code",code),(r,n)->r.getObject(1,UUID.class)).stream().findFirst().orElse(null);
    }
    private record ReviewBaseline(UUID latest,UUID immutable){}
    private ReviewBaseline reviewBaseline(UUID source,UUID subtest,String code){
        // Draft content can change without changing version ID. Only immutable published versions
        // are safe baselines; a workbook staged against an existing draft must be uploaded again.
        return jdbc.query("""
            SELECT v.id,v.status FROM question_set_item i JOIN LATERAL(
                SELECT id,status FROM question_version WHERE question_id=i.question_id ORDER BY version DESC LIMIT 1
            ) v ON true WHERE i.question_set_id=:source AND i.subtest_id=:subtest AND i.source_item_code=:code
            """,p("source",source).addValue("subtest",subtest).addValue("code",code),(r,n)->{
                UUID id=r.getObject("id",UUID.class);return new ReviewBaseline(id,"PUBLISHED".equals(r.getString("status"))?id:null);
            }).stream().findFirst().orElse(new ReviewBaseline(null,null));
    }
    private boolean validTopic(UUID topic,UUID subtest){return topic==null||Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM topic WHERE id=:id AND subtest_id=:subtest AND active)",p("id",topic).addValue("subtest",subtest),Boolean.class));}
    @Override public void extracted(WorkbookWork work,List<ParsedSheet> sheets){
        var info=get(work.id());if(!List.of("QUEUED","FAILED").contains(info.status()))return;
        for(var sheet:sheets){
            var mapped=info.sheets().stream().filter(s->s.sheetName().equals(sheet.name())).findFirst().orElseThrow();
            var counts=sheet.rows().stream().collect(java.util.stream.Collectors.groupingBy(ParsedRow::code,java.util.stream.Collectors.counting()));
            for(var row:sheet.rows()){
                var errors=new ArrayList<>(row.errors());UUID topic=row.topicId();
                if(!validTopic(topic,mapped.subtestId())){errors.add("INVALID_TOPIC_SCOPE");topic=null;}
                if(counts.get(row.code())>1)errors.add("DUPLICATE_QUESTION_CODE");
                var baseline=reviewBaseline(work.sourceId(),mapped.subtestId(),row.code());
                if(baseline.latest()!=null&&baseline.immutable()==null)errors.add("QUESTION_HAS_DRAFT");
                jdbc.update("""
                    INSERT INTO import_candidate(batch_id,sheet_id,source_row,source_item_no,source_item_code,topic_id,difficulty,extracted_content,validation_errors,expected_version_id)
                    VALUES(:batch,:sheet,:row,:row,:code,:topic,:difficulty,:content::jsonb,:errors::jsonb,:expected)
                    """,p("batch",work.id()).addValue("sheet",mapped.id()).addValue("row",row.row()).addValue("code",row.code()).addValue("topic",topic).addValue("difficulty",row.difficulty())
                        .addValue("content",json.writeValueAsString(row.content())).addValue("errors",json.writeValueAsString(errors.stream().distinct().toList()))
                        .addValue("expected",baseline.immutable()));
            }
            jdbc.update("UPDATE import_sheet SET sheet_index=:index WHERE id=:id",p("id",mapped.id()).addValue("index",sheet.index()));
        }
        jdbc.update("UPDATE import_batch SET error_code=NULL,completed_at=now() WHERE id=:id",p("id",work.id()));refresh(work.id());
    }
    @Override public void failed(UUID id,String code){
        jdbc.update("UPDATE import_batch SET status='FAILED',error_code=:code,updated_at=now() WHERE id=:id",p("id",id).addValue("code",code));
        jdbc.update("UPDATE import_sheet SET status='FAILED' WHERE batch_id=:id",p("id",id));
    }
    private ExcelCandidate candidate(java.sql.ResultSet r,int n)throws java.sql.SQLException{return new ExcelCandidate(r.getObject("id",UUID.class),r.getObject("sheet_id",UUID.class),r.getString("sheet_name"),r.getObject("subtest_id",UUID.class),r.getInt("source_row"),r.getString("source_item_code"),r.getObject("topic_id",UUID.class),r.getString("difficulty"),
            json.readValue(r.getString("extracted_content"),QuestionContent.class),r.getString("draft_content")==null?null:json.readValue(r.getString("draft_content"),QuestionContent.class),
            json.readValue(r.getString("validation_errors"),json.getTypeFactory().constructCollectionType(List.class,String.class)),r.getObject("expected_version_id",UUID.class),r.getObject("resolved_question_id",UUID.class),r.getObject("resolved_version_id",UUID.class));}
    private final RowMapper<ExcelCandidate> candidateMapper=this::candidate;
    private static final String CANDIDATE="SELECT c.*,s.sheet_name,s.subtest_id FROM import_candidate c JOIN import_sheet s ON s.id=c.sheet_id ";
    @Override public PageResponse<ExcelCandidate> candidates(UUID id,UUID sheet,int offset,int limit){
        get(id);var params=p("batch",id).addValue("sheet",sheet).addValue("offset",offset).addValue("limit",limit);String filter=" WHERE c.batch_id=:batch AND (:sheet::uuid IS NULL OR c.sheet_id=:sheet)";
        return new PageResponse<>(jdbc.query(CANDIDATE+filter+" ORDER BY s.sheet_index,c.source_row,c.id LIMIT :limit OFFSET :offset",params,candidateMapper),
            jdbc.queryForObject("SELECT count(*) FROM import_candidate c"+filter,params,Long.class),offset,limit);
    }
    private ExcelCandidate lock(UUID batch,UUID id){return jdbc.queryForObject(CANDIDATE+" WHERE c.id=:id AND c.batch_id=:batch FOR UPDATE OF c",p("id",id).addValue("batch",batch),candidateMapper);}
    @Override public ExcelCandidate correct(UUID actor,UUID batch,UUID id,CandidateCorrection body){
        var source=active(get(batch).questionSetId(),true);var current=lock(batch,id);
        require(current.resolvedVersionId()==null,"CANDIDATE_ALREADY_SAVED","Create a new import or question revision");content.validateScope(current.subtestId(),body.topicId());
        var errors=new ArrayList<>(QuestionValidator.publicationErrors(body.content().toDomain()));
        if(Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM import_candidate WHERE sheet_id=:sheet AND id<>:id AND source_item_code=:code)",p("sheet",current.sheetId()).addValue("id",id).addValue("code",body.sourceItemCode()),Boolean.class)))errors.add("DUPLICATE_QUESTION_CODE");
        jdbc.update("""
            UPDATE import_candidate SET source_item_code=:code,topic_id=:topic,difficulty=:difficulty,draft_content=:content::jsonb,validation_errors=:errors::jsonb,expected_version_id=:expected WHERE id=:id
            """,p("id",id).addValue("code",body.sourceItemCode()).addValue("topic",body.topicId()).addValue("difficulty",body.difficulty()).addValue("content",json.writeValueAsString(body.content())).addValue("errors",json.writeValueAsString(errors))
                .addValue("expected",current.sourceItemCode().equals(body.sourceItemCode())?current.expectedVersionId():reviewBaseline(source.id(),current.subtestId(),body.sourceItemCode()).immutable()));
        refresh(batch);audit(actor,"WORKBOOK_CANDIDATE_CORRECTED",id,"STAFF_REVIEW");return lock(batch,id);
    }
    @Override public List<QuestionResponse> save(UUID actor,UUID batch,SaveWorkbookCandidates body){
        // Imports are relatively infrequent writes; serialize bulk revisions to avoid reversed question-lock order.
        jdbc.queryForObject("SELECT pg_advisory_xact_lock(149249)",new MapSqlParameterSource(),Object.class);
        var source=active(get(batch).questionSetId(),true);var result=new ArrayList<QuestionResponse>();
        require(new HashSet<>(body.candidateIds()).size()==body.candidateIds().size(),"DUPLICATE_CANDIDATE","Candidate IDs must be distinct");
        for(UUID id:body.candidateIds().stream().sorted().toList()){
            var c=lock(batch,id);
            if(c.resolvedVersionId()!=null){result.add(content.questionVersion(c.resolvedQuestionId(),c.resolvedVersionId()));continue;}
            require(c.validationErrors().isEmpty(),"INVALID_CANDIDATE","Correct all row errors before saving");content.validateScope(c.subtestId(),c.topicId());
            var draft=c.draftContent()==null?c.extractedContent():c.draftContent();
            require(QuestionValidator.publicationErrors(draft.toDomain()).isEmpty(),"INVALID_CANDIDATE","Question content is incomplete");
            UUID currentVersion=expected(source.id(),c.subtestId(),c.sourceItemCode());
            require(Objects.equals(currentVersion,c.expectedVersionId()),"CANDIDATE_STALE","Question changed after workbook extraction; upload or review a fresh revision");
            QuestionResponse saved;
            if(currentVersion==null){
                saved=content.createQuestion(actor,new CreateQuestionRequest(c.subtestId(),c.topicId(),source.title(),source.sourceYear(),draft),id,null);
                jdbc.update("INSERT INTO question_set_item(question_set_id,question_id,exam_type_id,subtest_id,source_item_code) VALUES(:source,:question,:type,:subtest,:code)",
                    p("source",source.id()).addValue("question",saved.id()).addValue("type",source.examTypeId()).addValue("subtest",c.subtestId()).addValue("code",c.sourceItemCode()));
            }else{
                UUID question=jdbc.queryForObject("SELECT question_id FROM question_version WHERE id=:id",p("id",currentVersion),UUID.class);
                jdbc.queryForObject("SELECT id FROM question WHERE id=:id AND archived_at IS NULL FOR UPDATE",p("id",question),UUID.class);
                var current=content.question(question);require(current.versionId().equals(c.expectedVersionId()),"CANDIDATE_STALE","Question changed after extraction");
                require(Objects.equals(current.topicId(),c.topicId()),"QUESTION_SCOPE_IMMUTABLE","Use a new question code when changing a master question's topic");
                String difficulty=jdbc.queryForObject("SELECT difficulty FROM question_version WHERE id=:id",p("id",current.versionId()),String.class);
                if(sameContent(current.content(),draft)&&Objects.equals(difficulty,c.difficulty()))saved=current;
                else{
                    require(current.status().equals("PUBLISHED"),"QUESTION_HAS_DRAFT","Review the existing draft before importing another revision");
                    saved=content.newVersion(actor,question,draft);
                }
            }
            if(saved.status().equals("DRAFT"))jdbc.update("UPDATE question_version SET difficulty=:difficulty,source_year=:year,source_title=:title WHERE id=:id AND status='DRAFT'",p("id",saved.versionId()).addValue("difficulty",c.difficulty()).addValue("year",source.sourceYear()).addValue("title",source.title()));
            jdbc.update("UPDATE import_candidate SET resolved_question_id=:question,resolved_version_id=:version WHERE id=:id",p("id",id).addValue("question",saved.id()).addValue("version",saved.versionId()));
            audit(actor,"WORKBOOK_CANDIDATE_SAVED",id,body.reason());result.add(content.questionVersion(saved.id(),saved.versionId()));
        }
        return result;
    }
    private void refresh(UUID batch){
        jdbc.update("""
            UPDATE import_sheet s SET candidate_count=x.total,status=CASE WHEN x.total>0 AND x.invalid=0 THEN 'READY' ELSE 'NEEDS_INPUT' END
            FROM (SELECT sheet_id,count(*) AS total,count(*) FILTER(WHERE validation_errors<>'[]'::jsonb) AS invalid FROM import_candidate WHERE batch_id=:batch GROUP BY sheet_id) x WHERE s.id=x.sheet_id
            """,p("batch",batch));
        jdbc.update("""
            UPDATE import_batch b SET candidate_count=x.total,valid_count=x.valid,invalid_count=x.total-x.valid,
            status=CASE WHEN x.total>0 AND x.total=x.valid THEN 'READY' ELSE 'NEEDS_INPUT' END,updated_at=now()
            FROM (SELECT count(*) AS total,count(*) FILTER(WHERE validation_errors='[]'::jsonb) AS valid FROM import_candidate WHERE batch_id=:batch) x WHERE b.id=:batch
            """,p("batch",batch));
    }
    private static boolean sameContent(QuestionContent a,QuestionContent b){
        if(!Objects.equals(a.stemText(),b.stemText())||!Objects.equals(a.explanation(),b.explanation())||a.shuffleAllowed()!=b.shuffleAllowed()||a.options().size()!=b.options().size())return false;
        for(int n=0;n<a.options().size();n++){
            var x=a.options().get(n);var y=b.options().get(n);
            if(!x.code().equals(y.code())||!x.text().equals(y.text())||x.correct()!=y.correct()||x.weight().compareTo(y.weight())!=0)return false;
        }return true;
    }
}
