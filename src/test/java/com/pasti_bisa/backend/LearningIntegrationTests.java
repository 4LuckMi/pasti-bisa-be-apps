package com.pasti_bisa.backend;

import java.io.ByteArrayOutputStream;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import com.pasti_bisa.backend.modules.content.adapter.out.demo.DemoDataSeeder;
import com.pasti_bisa.backend.modules.content.application.ContentImportService;
import com.pasti_bisa.backend.modules.exam.application.ExamService;
import com.pasti_bisa.backend.modules.operations.application.JobQueue;
import static org.assertj.core.api.Assertions.*;

@ActiveProfiles("demo")
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT)
class LearningIntegrationTests {
    @LocalServerPort int port;
    @Autowired JsonMapper json;
    @Autowired JdbcTemplate jdbc;
    @Autowired ExamService exams;
    @Autowired JobQueue jobs;
    @Autowired ContentImportService imports;
    @Autowired com.pasti_bisa.backend.modules.content.application.PdfExtractor extractor;
    @Value("${pastisiap.storage.root}") String storage;

    private class Browser {
        final HttpClient client=HttpClient.newBuilder().cookieHandler(new CookieManager(null,CookiePolicy.ACCEPT_ALL)).build();
        String csrfHeader,csrfToken,email;UUID userId;
        JsonNode send(String method,String path,String body,int status,String key) throws Exception {
            var request=HttpRequest.newBuilder(URI.create("http://localhost:"+port+path));
            if(csrfToken!=null) request.header(csrfHeader,csrfToken);
            if(key!=null) request.header("Idempotency-Key",key);
            if(body!=null) request.header("Content-Type","application/json");
            request.method(method,body==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(body));
            var response=client.send(request.build(),HttpResponse.BodyHandlers.ofString());
            assertThat(response.statusCode()).as(method+" "+path+" "+response.body()).isEqualTo(status);
            assertThat(response.headers().firstValue("X-Request-ID")).isPresent();
            return response.body().isBlank()?null:json.readTree(response.body());
        }
        Browser register(boolean staff) throws Exception {
            var csrf=send("GET","/api/v1/csrf",null,200,null);
            csrfHeader=csrf.get("headerName").asString();csrfToken=csrf.get("token").asString();
            email="verify-"+UUID.randomUUID()+"@example.com";
            var account=send("POST","/api/v1/auth/register","{\"email\":\""+email+"\",\"displayName\":\"Integration Test\",\"password\":\"LocalVerify123!\"}",201,null);
            userId=UUID.fromString(account.get("user").get("id").asString());
            if(staff) {
                jdbc.update("INSERT INTO user_role(user_id,role_code) VALUES (?,'QUESTION_STAFF')",userId);
                send("POST","/api/v1/auth/login","{\"email\":\""+email+"\",\"password\":\"LocalVerify123!\"}",200,null);
            }
            return this;
        }
    }
    private UUID tryout() { return DemoDataSeeder.id("UTBK:blueprint"); }
    private String startBody() { return "{\"tryoutId\":\""+tryout()+"\"}"; }
    private String start(Browser browser,String key) throws Exception {
        return browser.send("POST","/api/v1/attempts",startBody(),200,key).get("attemptId").asString();
    }
    private void drainJobs() throws Exception {
        for(int i=0;i<30;i++) {
            var job=jobs.claim();if(job.isEmpty()) return;
            switch(job.get().type()) {
                case "SCORE_ATTEMPT" -> exams.score(UUID.fromString(job.get().payload().get("attemptId").asString()));
                case "IMPORT_PDF" -> imports.process(job.get());
                default -> throw new IllegalStateException("Unexpected test job type");
            }
            jobs.succeeded(job.get());
        }
    }
    private void finish(Browser browser,String id) throws Exception {
        browser.send("POST","/api/v1/users/current/attempts/"+id+"/finalize","{}",200,null);drainJobs();
    }
    @Test void scannedPdfUsesRealTesseractOcr() throws Exception {
        var image=new java.awt.image.BufferedImage(1400,700,java.awt.image.BufferedImage.TYPE_INT_RGB);
        var graphics=image.createGraphics();graphics.setColor(java.awt.Color.WHITE);graphics.fillRect(0,0,1400,700);
        graphics.setColor(java.awt.Color.BLACK);graphics.setFont(new java.awt.Font("DejaVu Sans",java.awt.Font.PLAIN,36));
        int y=65;
        for(String line:List.of("1. Berapa hasil dua tambah tiga?","A. Empat","B. Lima","Kunci: B","Pembahasan: Dua ditambah tiga sama dengan lima.")) {
            graphics.drawString(line,45,y);y+=70;
        }
        graphics.dispose();
        Path file=Files.createTempFile("verify-scan-",".pdf");
        try {
            try(var document=new org.apache.pdfbox.pdmodel.PDDocument()) {
                var page=new org.apache.pdfbox.pdmodel.PDPage(new org.apache.pdfbox.pdmodel.common.PDRectangle(840,420));
                document.addPage(page);
                try(var stream=new org.apache.pdfbox.pdmodel.PDPageContentStream(document,page)) {
                    stream.drawImage(org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory.createFromImage(document,image),0,0,840,420);
                }
                document.save(file.toFile());
            }
            var pages=extractor.extract(file);
            assertThat(pages).hasSize(1);assertThat(pages.getFirst().ocr()).isTrue();
            assertThat(pages.getFirst().text()).containsIgnoringCase("Kunci").containsIgnoringCase("Lima");
            assertThat(com.pasti_bisa.backend.modules.content.domain.TemplateQuestionParser.parse(pages)).hasSize(1);
        } finally { Files.deleteIfExists(file); }
    }
    @Test void expiredJobLeaseCannotOverwriteNewWorkerResult() throws Exception {
        drainJobs();UUID id=UUID.randomUUID();
        jdbc.update("INSERT INTO job(id,type,payload,dedup_key) VALUES (?,'VERIFY_LEASE','{}',?)",id,"verify:"+id);
        var old=jobs.claim().orElseThrow();assertThat(old.id()).isEqualTo(id);
        jdbc.update("UPDATE job SET lease_until=now()-interval '1 second' WHERE id=?",id);
        var current=jobs.claim().orElseThrow();assertThat(current.id()).isEqualTo(id);assertThat(current.token()).isNotEqualTo(old.token());
        jobs.failed(old,new IllegalStateException("stale"));
        assertThat(jdbc.queryForObject("SELECT status FROM job WHERE id=?",String.class,id)).isEqualTo("PROCESSING");
        jobs.succeeded(current);
        assertThat(jdbc.queryForObject("SELECT status FROM job WHERE id=?",String.class,id)).isEqualTo("SUCCEEDED");
    }
    @Test void demoCatalogHasFourCategoriesAndTwelveQuestionsPerTopic() throws Exception {
        var browser=new Browser().register(false);
        assertThat(browser.send("GET","/api/v1/tryouts",null,200,null).size()).isEqualTo(4);
        assertThat(browser.send("GET","/api/v1/practice/topics",null,200,null).get(0).get("question_count").asLong()).isGreaterThanOrEqualTo(12);
        assertThat(browser.send("GET","/api/v1/tryouts/"+tryout(),null,200,null).get("ready").asBoolean()).isTrue();
    }
    @Test void concurrentStartFreezesOneSnapshotAndConsumesOneQuota() throws Exception {
        var browser=new Browser().register(false);String key=UUID.randomUUID().toString();
        var one=CompletableFuture.supplyAsync(()->{try{return start(browser,key);}catch(Exception e){throw new RuntimeException(e);}});
        var two=CompletableFuture.supplyAsync(()->{try{return start(browser,key);}catch(Exception e){throw new RuntimeException(e);}});
        String id=one.get();assertThat(two.get()).isEqualTo(id);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM quota_usage WHERE user_id=?",Integer.class,browser.userId)).isEqualTo(1);
        browser.send("POST","/api/v1/practice/attempts","{\"topicId\":\""+DemoDataSeeder.id("UTBK:topic")+"\",\"questionCount\":1}",409,key);
        var first=browser.send("GET","/api/v1/users/current/attempts/"+id+"/questions/1",null,200,null);
        assertThat(browser.send("GET","/api/v1/users/current/attempts/"+id+"/questions/1",null,200,null)).isEqualTo(first);
        assertThat(first.get("explanation").isNull()).isTrue();
        for(var option:first.get("options")) assertThat(option.get("correct").isNull()).isTrue();
        var stranger=new Browser().register(false);
        stranger.send("GET","/api/v1/users/current/attempts/"+id+"/questions/1",null,404,null);
        UUID correct=jdbc.queryForObject("""
                SELECT o.id FROM question_option o JOIN attempt_item i ON i.question_version_id=o.question_version_id
                WHERE i.attempt_id=?::uuid AND i.position=1 AND o.correct_key
                """,UUID.class,id);
        String answer="{\"selectedOptionId\":\""+correct+"\",\"expectedRevision\":0,\"flagged\":true}";
        browser.send("PUT","/api/v1/users/current/attempts/"+id+"/answers/1",answer,200,null);
        browser.send("PUT","/api/v1/users/current/attempts/"+id+"/answers/1",answer,409,null);
        browser.send("PUT","/api/v1/users/current/attempts/"+id+"/answers/1",answer.replace("\"expectedRevision\":0","\"expectedRevision\":1"),200,null);
        finish(browser,id);
        finish(browser,id);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM score_run WHERE attempt_id=?::uuid",Integer.class,id)).isEqualTo(1);
        var result=browser.send("GET","/api/v1/users/current/attempts/"+id+"/result",null,200,null);
        assertThat(result.get("totalScore").asDouble()).isBetween(33.33,33.34);
        assertThat(result.get("details").get("empty").asInt()).isEqualTo(2);
        assertThat(browser.send("GET","/api/v1/users/current/attempts/"+id+"/review",null,200,null).size()).isEqualTo(3);
        browser.send("PUT","/api/v1/users/current/attempts/"+id+"/answers/1",answer,409,null);
        UUID version=jdbc.queryForObject("SELECT question_version_id FROM attempt_item WHERE attempt_id=?::uuid AND position=1",UUID.class,id);
        assertThatThrownBy(()->jdbc.update("UPDATE question_version SET explanation='changed' WHERE id=?",version)).isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }
    @Test void quotaBlocksThirdTryoutButPracticeDoesNotConsumeQuota() throws Exception {
        var browser=new Browser().register(false);
        String first=start(browser,UUID.randomUUID().toString());finish(browser,first);
        String second=start(browser,UUID.randomUUID().toString());finish(browser,second);
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM attempt_item a JOIN attempt_item b ON a.question_id=b.question_id
                WHERE a.attempt_id=?::uuid AND b.attempt_id=?::uuid
                """,Integer.class,first,second)).isZero();
        assertThat(browser.send("POST","/api/v1/attempts",startBody(),409,UUID.randomUUID().toString()).get("code").asString()).isEqualTo("DAILY_QUOTA_EXHAUSTED");
        var practice=browser.send("POST","/api/v1/practice/attempts","{\"topicId\":\""+DemoDataSeeder.id("UTBK:topic")+"\",\"questionCount\":2}",200,UUID.randomUUID().toString());
        finish(browser,practice.get("attemptId").asString());
        assertThat(browser.send("GET","/api/v1/users/current/tryout-quota",null,200,null).get("used").asInt()).isEqualTo(2);
        assertThat(browser.send("GET","/api/v1/tryouts",null,200,null).size()).isEqualTo(4);
    }
    @Test void insufficientPoolRollsBackAndServerDeadlineFinalizes() throws Exception {
        var browser=new Browser().register(false);
        browser.send("POST","/api/v1/practice/attempts","{\"topicId\":\""+DemoDataSeeder.id("UTBK:topic")+"\",\"questionCount\":50}",409,UUID.randomUUID().toString());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM attempt WHERE user_id=?",Integer.class,browser.userId)).isZero();
        String id=start(browser,UUID.randomUUID().toString());
        jdbc.update("UPDATE attempt SET started_at=now()-interval '2 hours',deadline_at=now()-interval '1 hour' WHERE id=?::uuid",id);
        browser.send("PUT","/api/v1/users/current/attempts/"+id+"/answers/1","{\"expectedRevision\":0}",409,null);
        exams.expire(UUID.fromString(id));drainJobs();
        assertThat(browser.send("GET","/api/v1/users/current/attempts/"+id+"/result",null,200,null).get("terminalReason").asString()).isEqualTo("TIMED_OUT");
    }
    @Test void uploadIsPrivateDeduplicatedAndRequiresExplicitPublish() throws Exception {
        var customer=new Browser().register(false);
        customer.send("GET","/api/v1/staff/imports",null,403,null);
        var staff=new Browser().register(true);
        byte[] pdf=Files.readAllBytes(Path.of(storage,"demo","sample-questions.pdf"));
        String batch=upload(staff,pdf);
        assertThat(upload(staff,pdf)).isEqualTo(batch);
        drainJobs();
        var candidates=staff.send("GET","/api/v1/staff/imports/"+batch+"/candidates",null,200,null);
        assertThat(candidates.size()).isEqualTo(2);
        UUID candidate=UUID.fromString(candidates.get(0).get("id").asString());
        var draft=staff.send("POST","/api/v1/staff/imports/"+batch+"/candidates/"+candidate+"/save",null,200,null);
        assertThat(draft.get("status").asString()).isEqualTo("DRAFT");
        String question=draft.get("id").asString();
        var published=staff.send("POST","/api/v1/staff/questions/"+question+"/publish",null,200,null);
        assertThat(published.get("status").asString()).isEqualTo("PUBLISHED");
        staff.send("POST","/api/v1/staff/questions/"+question+"/publish",null,200,null);
        staff.send("GET","/api/v1/staff/imports/"+batch+"/candidates",null,200,null);
        customer.send("GET","/api/v1/staff/imports/"+batch+"/source",null,403,null);
        var src=staff.client.send(HttpRequest.newBuilder(URI.create("http://localhost:"+port+"/api/v1/staff/imports/"+batch+"/source")).GET().build(),HttpResponse.BodyHandlers.ofByteArray());
        assertThat(src.statusCode()).isEqualTo(200);assertThat(src.body()).isEqualTo(pdf);
        var revised=staff.send("POST","/api/v1/staff/questions/"+question+"/versions",json.writeValueAsString(published.get("content")),200,null);
        assertThat(revised.get("version").asInt()).isEqualTo(2);
        assertThat(revised.get("status").asString()).isEqualTo("DRAFT");
        assertThat(jdbc.queryForObject("SELECT status FROM question_version WHERE id=?::uuid",String.class,published.get("versionId").asString())).isEqualTo("PUBLISHED");
    }
    private String upload(Browser staff,byte[] pdf) throws Exception {
        String boundary="verify-"+UUID.randomUUID();
        String metadata="{\"subtestId\":\""+DemoDataSeeder.id("SEKOLAH:subtest")+"\",\"topicId\":\""+DemoDataSeeder.id("SEKOLAH:topic")+"\",\"sourceTitle\":\"PDF integration example\",\"sourceYear\":2026}";
        var bytes=new ByteArrayOutputStream();
        bytes.write(("--"+boundary+"\r\nContent-Disposition: form-data; name=\"metadata\"\r\nContent-Type: application/json\r\n\r\n"+metadata+"\r\n--"+boundary+"\r\nContent-Disposition: form-data; name=\"file\"; filename=\"sample.pdf\"\r\nContent-Type: application/pdf\r\n\r\n").getBytes(StandardCharsets.UTF_8));
        bytes.write(pdf);bytes.write(("\r\n--"+boundary+"--\r\n").getBytes(StandardCharsets.UTF_8));
        var request=HttpRequest.newBuilder(URI.create("http://localhost:"+port+"/api/v1/staff/imports"))
                .header(staff.csrfHeader,staff.csrfToken).header("Content-Type","multipart/form-data; boundary="+boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(bytes.toByteArray())).build();
        var response=staff.client.send(request,HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).as(response.body()).isEqualTo(202);
        return json.readTree(response.body()).get("id").asString();
    }
}
