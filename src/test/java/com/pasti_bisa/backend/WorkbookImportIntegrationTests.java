package com.pasti_bisa.backend;

import java.io.*;
import java.net.*;
import java.net.http.*;
import java.util.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import com.pasti_bisa.backend.modules.content.adapter.out.demo.DemoDataSeeder;
import com.pasti_bisa.backend.modules.content.adapter.out.excel.PoiWorkbookParser;
import com.pasti_bisa.backend.modules.content.application.*;
import com.pasti_bisa.backend.modules.exam.application.ExamService;
import com.pasti_bisa.backend.modules.operations.application.JobQueue;
import static org.assertj.core.api.Assertions.*;

@ActiveProfiles("demo")
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT)
class WorkbookImportIntegrationTests {
    @LocalServerPort int port;@Autowired JdbcTemplate jdbc;@Autowired JsonMapper json;
    @Autowired WorkbookImportService workbooks;@Autowired ContentImportService pdf;@Autowired ExamService exams;@Autowired JobQueue jobs;
    static final String REASON="Verify workbook source and immutable snapshots";
    private class Browser {
        final HttpClient client=HttpClient.newBuilder().cookieHandler(new CookieManager(null,CookiePolicy.ACCEPT_ALL)).build();
        String csrf,email;UUID id;
        HttpResponse<byte[]> request(String method,String path,byte[] body,String type,int expected)throws Exception{
            var r=HttpRequest.newBuilder(URI.create("http://localhost:"+port+"/api/v1"+path));
            if(csrf!=null)r.header("X-CSRF-TOKEN",csrf);if(type!=null)r.header("Content-Type",type);
            if(path.equals("/attempts"))r.header("Idempotency-Key",UUID.randomUUID().toString());
            var response=client.send(r.method(method,body==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofByteArray(body)).build(),HttpResponse.BodyHandlers.ofByteArray());
            assertThat(response.statusCode()).as(method+" "+path+" "+new String(response.body(),java.nio.charset.StandardCharsets.UTF_8)).isEqualTo(expected);return response;
        }
        JsonNode send(String method,String path,Object body,int expected)throws Exception{
            var r=request(method,path,body==null?null:json.writeValueAsBytes(body),body==null?null:"application/json",expected);
            return r.body().length==0?null:json.readTree(r.body());
        }
        Browser register(String role)throws Exception{
            csrf=send("GET","/csrf",null,200).get("token").asString();email="workbook-"+UUID.randomUUID()+"@example.com";
            var user=send("POST","/auth/register",Map.of("email",email,"displayName","Workbook Test","password","WorkbookVerify123!"),201);
            id=UUID.fromString(user.get("user").get("id").asString());
            if(role!=null){jdbc.update("INSERT INTO user_role(user_id,role_code) VALUES (?,?)",id,role);send("POST","/auth/login",Map.of("email",email,"password","WorkbookVerify123!"),200);}return this;
        }
        JsonNode upload(UUID source,List<Map<String,Object>> mappings,byte[] bytes,int expected)throws Exception{
            String boundary="verify"+UUID.randomUUID();var out=new ByteArrayOutputStream();
            out.write(("--"+boundary+"\r\nContent-Disposition: form-data; name=\"metadata\"\r\nContent-Type: application/json\r\n\r\n"+json.writeValueAsString(Map.of("questionSetId",source,"sheets",mappings))+"\r\n--"+boundary+"\r\nContent-Disposition: form-data; name=\"file\"; filename=\"questions.xlsx\"\r\nContent-Type: application/vnd.openxmlformats-officedocument.spreadsheetml.sheet\r\n\r\n").getBytes(java.nio.charset.StandardCharsets.UTF_8));
            out.write(bytes);out.write(("\r\n--"+boundary+"--\r\n").getBytes(java.nio.charset.StandardCharsets.US_ASCII));
            return json.readTree(request("POST","/staff/workbook-imports",out.toByteArray(),"multipart/form-data; boundary="+boundary,expected).body());
        }
    }
    private UUID source(Browser staff)throws Exception{return UUID.fromString(staff.send("POST","/staff/question-sets",Map.of("examTypeId",DemoDataSeeder.id("UTBK:type"),"code","WB_"+UUID.randomUUID().toString().toUpperCase(Locale.ROOT),"sourceYear",2025,"title","UTBK workbook 2025","reason",REASON),201).get("id").asString());}
    private List<Map<String,Object>> mapping(UUID subtest){return List.of(Map.of("sheetName","NUMERIK","subtestId",subtest));}
    private byte[] workbook(List<String> names,String firstText,boolean formula)throws Exception{
        try(var workbook=new XSSFWorkbook();var out=new ByteArrayOutputStream()){
            for(String name:names){var sheet=workbook.createSheet(name);var header=sheet.createRow(0);
                for(int column=0;column<PoiWorkbookParser.HEADERS.size();column++)header.createCell(column).setCellValue(PoiWorkbookParser.HEADERS.get(column));
                for(int index=0;index<6;index++){
                    var row=sheet.createRow(index+1);var values=new HashMap<String,String>();
                    values.put("question_code","Q"+(index+1));values.put("question_text",index==0?firstText:"Soal "+name+" "+index);
                    values.put("option_a","Benar");values.put("option_b","Salah");values.put("correct_answer","A");values.put("explanation","Pembahasan lengkap");values.put("difficulty",List.of("EASY","MEDIUM","HARD").get(index/2));values.put("shuffle_allowed","true");
                    for(int column=0;column<PoiWorkbookParser.HEADERS.size();column++)row.createCell(column).setCellValue(values.getOrDefault(PoiWorkbookParser.HEADERS.get(column),""));
                    if(formula&&index==0)row.getCell(2).setCellFormula("1+1");
                }
            }workbook.write(out);return out.toByteArray();
        }
    }
    private void drain()throws Exception{
        for(int n=0;n<100;n++){var claim=jobs.claim();if(claim.isEmpty())return;var job=claim.get();
            try{switch(job.type()){
                case "IMPORT_XLSX"->workbooks.process(job);case "IMPORT_PDF"->pdf.process(job);
                case "SCORE_ATTEMPT"->exams.score(UUID.fromString(job.payload().get("attemptId").asString()));default->throw new IllegalStateException(job.type());
            }jobs.succeeded(job);}catch(com.pasti_bisa.backend.shared.web.BusinessException e){jobs.failed(job,e);}
        }
    }
    private List<String> candidateIds(Browser staff,String batch)throws Exception{
        var page=staff.send("GET","/staff/workbook-imports/"+batch+"/candidates?limit=100",null,200);var ids=new ArrayList<String>();
        for(var item:page.get("items"))ids.add(item.get("id").asString());return ids;
    }
    @Test void workbookSheetsSavePublishAndGenerateDifferentFrozenSessions()throws Exception{
        var staff=new Browser().register("QUESTION_STAFF");var admin=new Browser().register("PLATFORM_ADMIN");var student=new Browser().register(null);
        UUID source=source(staff),subtest=DemoDataSeeder.id("UTBK:subtest"),second=UUID.randomUUID();UUID blueprint=null;
        jdbc.update("INSERT INTO subtest(id,exam_type_id,code,name) VALUES (?,?,?,'Workbook second subtest')",second,DemoDataSeeder.id("UTBK:type"),"WB_"+second.toString().substring(0,8));
        try{
            var bytes=workbook(List.of("NUMERIK","SECOND"),"Soal pertama",false);
            var mappings=List.<Map<String,Object>>of(Map.of("sheetName","NUMERIK","subtestId",subtest),Map.of("sheetName","SECOND","subtestId",second));
            String batch=staff.upload(source,mappings,bytes,202).get("id").asString();
            assertThat(staff.upload(source,mappings,bytes,202).get("id").asString()).isEqualTo(batch);
            drain();var info=staff.send("GET","/staff/workbook-imports/"+batch,null,200);
            assertThat(info.get("status").asString()).isEqualTo("READY");assertThat(info.get("candidateCount").asInt()).isEqualTo(12);assertThat(info.get("sheets").size()).isEqualTo(2);
            var download=staff.request("GET","/staff/imports/"+batch+"/source",null,null,200);
            assertThat(download.headers().firstValue("Content-Type").orElseThrow()).contains("spreadsheetml");
            var saved=staff.send("POST","/staff/workbook-imports/"+batch+"/save",Map.of("candidateIds",candidateIds(staff,batch),"reason",REASON),200);
            assertThat(saved.size()).isEqualTo(12);
            for(var question:saved)staff.send("POST","/staff/questions/"+question.get("id").asString()+"/publish",null,200);
            assertThat(staff.send("GET","/staff/question-sets/"+source,null,200).get("publishedCount").asInt()).isEqualTo(12);
            assertThat(staff.send("GET","/staff/question-sets/"+source+"/items?limit=5",null,200).get("total").asInt()).isEqualTo(12);
            assertThat(staff.send("GET","/staff/workbook-imports?questionSetId="+source,null,200).get("total").asInt()).isEqualTo(1);
            assertThat(staff.send("GET","/staff/question-sets/"+source+"/template-mapping",null,200).size()).isEqualTo(2);
            UUID reusedSource=source(staff);
            staff.send("POST","/staff/question-sets/"+reusedSource+"/items",Map.of("questionId",saved.get(0).get("id").asString(),"sourceItemCode","REUSED_Q1","reason",REASON),200);
            assertThat(staff.send("GET","/staff/question-sets/"+reusedSource,null,200).get("questionCount").asInt()).isEqualTo(1);
            staff.send("POST","/staff/question-sets/"+reusedSource+"/items",Map.of("questionId",DemoDataSeeder.id("CPNS:q:1"),"sourceItemCode","WRONG_TYPE","reason",REASON),409);
            staff.send("GET","/staff/imports/"+batch+"/candidates",null,409);
            var template=staff.request("GET","/staff/question-sets/"+source+"/template",null,null,200);
            try(var book=new XSSFWorkbook(new ByteArrayInputStream(template.body()))){assertThat(book.getSheet("NUMERIK")).isNotNull();}
            admin.send("GET","/admin/question-sets/"+source,null,200);admin.send("GET","/staff/workbook-imports/"+batch,null,403);
            student.send("GET","/staff/workbook-imports/"+batch,null,403);
            blueprint=UUID.fromString(admin.send("POST","/admin/blueprints",Map.of("examTypeId",DemoDataSeeder.id("UTBK:type"),"code","WB_"+UUID.randomUUID().toString().toUpperCase(Locale.ROOT),"title","Workbook tryout","reason",REASON),201).get("id").asString());
            var section=Map.of("subtestId",subtest,"questionCount",2,"questionSetIds",List.of(source),"difficultyQuotas",List.of(Map.of("difficulty","EASY","count",1),Map.of("difficulty","MEDIUM","count",1)),"repeatPolicy","NEVER_SEEN");
            var version=admin.send("POST","/admin/blueprints/"+blueprint+"/versions",Map.of("durationSeconds",600,"scoringPolicyId",DemoDataSeeder.id("policy"),"sections",List.of(section),"reason",REASON),201);
            assertThat(version.get("ready").asBoolean()).isTrue();String versionId=version.get("id").asString();
            var publish=json.createObjectNode().putNull("expectedActiveVersionId").put("reason",REASON);
            admin.send("POST","/admin/blueprints/"+blueprint+"/versions/"+versionId+"/publish",publish,200);
            var first=student.send("POST","/attempts",Map.of("tryoutId",blueprint),200);String firstId=first.get("attemptId").asString();
            var frozen=jdbc.queryForList("SELECT question_version_id FROM attempt_item WHERE attempt_id=? ORDER BY position",UUID.class,UUID.fromString(firstId));
            var difficulty=jdbc.queryForList("SELECT v.difficulty FROM attempt_item i JOIN question_version v ON v.id=i.question_version_id WHERE i.attempt_id=? ORDER BY v.difficulty",String.class,UUID.fromString(firstId));
            assertThat(difficulty).containsExactly("EASY","MEDIUM");
            assertThat(jdbc.queryForObject("SELECT count(*) FROM attempt_item i JOIN question_set_item s ON s.question_id=i.question_id WHERE i.attempt_id=? AND s.question_set_id=?",Long.class,UUID.fromString(firstId),source)).isEqualTo(2);
            student.send("POST","/users/current/attempts/"+firstId+"/finalize",Map.of(),200);drain();
            var next=student.send("POST","/attempts",Map.of("tryoutId",blueprint),200);String nextId=next.get("attemptId").asString();
            var secondVersions=jdbc.queryForList("SELECT question_version_id FROM attempt_item WHERE attempt_id=?",UUID.class,UUID.fromString(nextId));
            assertThat(secondVersions).doesNotContainAnyElementsOf(frozen);
            assertThat(student.send("POST","/attempts",Map.of("tryoutId",blueprint),200).get("attemptId").asString()).isEqualTo(nextId);
            student.send("POST","/users/current/attempts/"+nextId+"/finalize",Map.of(),200);drain();
            var corrected=workbook(List.of("NUMERIK","SECOND"),"Soal pertama diperbaiki",false);
            String correction=staff.upload(source,mappings,corrected,202).get("id").asString();drain();
            var revisions=staff.send("POST","/staff/workbook-imports/"+correction+"/save",Map.of("candidateIds",candidateIds(staff,correction),"reason",REASON),200);
            assertThat(jdbc.queryForObject("SELECT count(*) FROM question_set_item WHERE question_set_id=?",Long.class,source)).isEqualTo(12);
            assertThat(revisions).anySatisfy(q->assertThat(q.get("version").asInt()).isEqualTo(2));
            assertThat(jdbc.queryForList("SELECT question_version_id FROM attempt_item WHERE attempt_id=? ORDER BY position",UUID.class,UUID.fromString(firstId))).isEqualTo(frozen);
            assertThatThrownBy(()->jdbc.update("DELETE FROM blueprint_section_source WHERE section_id IN (SELECT id FROM blueprint_section WHERE blueprint_version_id=?)",UUID.fromString(versionId))).isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
            staff.send("PATCH","/staff/question-sets/"+source+"/status",Map.of("status","ARCHIVED","reason",REASON),200);
            assertThat(student.send("GET","/tryouts/"+blueprint,null,200).get("ready").asBoolean()).isFalse();
        }finally{
            if(blueprint!=null)jdbc.update("UPDATE blueprint SET status='ARCHIVED' WHERE id=?",blueprint);
            jdbc.update("UPDATE subtest SET active=false WHERE id=?",second);
        }
    }
    @Test void invalidRowsRemainStagedAndCanBeCorrectedBeforeSave()throws Exception{
        var staff=new Browser().register("QUESTION_STAFF");UUID source=source(staff);
        String batch=staff.upload(source,mapping(DemoDataSeeder.id("UTBK:subtest")),workbook(List.of("NUMERIK"),"Soal",true),202).get("id").asString();drain();
        var page=staff.send("GET","/staff/workbook-imports/"+batch+"/candidates",null,200);var candidate=page.get("items").get(0);String id=candidate.get("id").asString();
        assertThat(candidate.get("validationErrors").toString()).contains("FORMULA_NOT_ALLOWED");
        staff.send("POST","/staff/workbook-imports/"+batch+"/save",Map.of("candidateIds",List.of(id),"reason",REASON),409);
        var correction=json.createObjectNode().put("sourceItemCode","Q1").put("difficulty","EASY").putNull("topicId");
        var content=correction.putObject("content");content.put("stemText","Pertanyaan yang diperbaiki").put("explanation","Penjelasan benar").put("shuffleAllowed",false);
        var options=content.putArray("options");options.addObject().put("code","A").put("text","Benar").put("correct",true).put("weight",1);options.addObject().put("code","B").put("text","Salah").put("correct",false).put("weight",0);
        assertThat(staff.send("PATCH","/staff/workbook-imports/"+batch+"/candidates/"+id,correction,200).get("validationErrors").isEmpty()).isTrue();
        staff.send("POST","/staff/workbook-imports/"+batch+"/save",Map.of("candidateIds",List.of(id),"reason",REASON),200);
    }
    @Test void staleRevisionsRollbackWholeSaveAndCrossExamMappingsAreRejected()throws Exception{
        var staff=new Browser().register("QUESTION_STAFF");UUID source=source(staff);var mappings=mapping(DemoDataSeeder.id("UTBK:subtest"));
        byte[] bytes=workbook(List.of("NUMERIK"),"Original",false);
        staff.upload(source,mapping(DemoDataSeeder.id("CPNS:subtest")),bytes,409);
        String initial=staff.upload(source,mappings,bytes,202).get("id").asString();drain();
        var saved=staff.send("POST","/staff/workbook-imports/"+initial+"/save",Map.of("candidateIds",candidateIds(staff,initial),"reason",REASON),200);
        String blocked=staff.upload(source,mappings,workbook(List.of("NUMERIK"),"Staged while master is draft",false),202).get("id").asString();drain();
        var blockedRow=staff.send("GET","/staff/workbook-imports/"+blocked+"/candidates",null,200).get("items").get(0);
        assertThat(blockedRow.get("validationErrors").toString()).contains("QUESTION_HAS_DRAFT");
        for(var q:saved)staff.send("POST","/staff/questions/"+q.get("id").asString()+"/publish",null,200);
        var correction=json.createObjectNode().put("sourceItemCode",blockedRow.get("sourceItemCode").asString()).put("difficulty",blockedRow.get("difficulty").asString()).putNull("topicId");
        correction.set("content",blockedRow.get("extractedContent"));String blockedId=blockedRow.get("id").asString();
        staff.send("PATCH","/staff/workbook-imports/"+blocked+"/candidates/"+blockedId,correction,200);
        assertThat(staff.send("POST","/staff/workbook-imports/"+blocked+"/save",Map.of("candidateIds",List.of(blockedId),"reason",REASON),409).get("code").asString()).isEqualTo("CANDIDATE_STALE");
        String first=staff.upload(source,mappings,workbook(List.of("NUMERIK"),"Correction A",false),202).get("id").asString();
        String second=staff.upload(source,mappings,workbook(List.of("NUMERIK"),"Correction B",false),202).get("id").asString();drain();
        staff.send("POST","/staff/workbook-imports/"+first+"/save",Map.of("candidateIds",candidateIds(staff,first),"reason",REASON),200);
        var failure=staff.send("POST","/staff/workbook-imports/"+second+"/save",Map.of("candidateIds",candidateIds(staff,second),"reason",REASON),409);
        assertThat(failure.get("code").asString()).isEqualTo("CANDIDATE_STALE");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM import_candidate WHERE batch_id=? AND resolved_version_id IS NOT NULL",Long.class,UUID.fromString(second))).isZero();
    }
    @Test void malformedWorkbookFailsWithSafeCodeAndNoAutomaticRetryLoop()throws Exception{
        var staff=new Browser().register("QUESTION_STAFF");UUID source=source(staff);
        String batch=staff.upload(source,mapping(DemoDataSeeder.id("UTBK:subtest")),new byte[]{'P','K',3,4,1,2,3},202).get("id").asString();drain();
        var result=staff.send("GET","/staff/workbook-imports/"+batch,null,200);
        assertThat(result.get("status").asString()).isEqualTo("FAILED");assertThat(result.get("errorCode").asString()).isEqualTo("INVALID_WORKBOOK");assertThat(result.get("jobStatus").asString()).isEqualTo("FAILED");
        assertThat(jdbc.queryForObject("SELECT retry_count FROM job WHERE dedup_key=?",Integer.class,"import:"+batch)).isEqualTo(1);
    }
}
