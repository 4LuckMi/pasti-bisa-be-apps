package com.pasti_bisa.backend;

import java.net.*;
import java.net.http.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import com.pasti_bisa.backend.modules.content.adapter.out.demo.DemoDataSeeder;
import static org.assertj.core.api.Assertions.*;

@ActiveProfiles("demo")
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AdministrationIntegrationTests {
    @LocalServerPort int port;
    @Autowired JsonMapper json;
    @Autowired JdbcTemplate jdbc;
    Browser admin;
    private class Browser {
        final HttpClient client=HttpClient.newBuilder().cookieHandler(new CookieManager(null,CookiePolicy.ACCEPT_ALL)).build();
        String csrf,email;UUID id;
        JsonNode send(String method,String path,String body,int expected) throws Exception { return send(method,path,body,expected,null); }
        JsonNode send(String method,String path,String body,int expected,String key) throws Exception {
            var request=HttpRequest.newBuilder(URI.create("http://localhost:"+port+path));
            if(csrf!=null) request.header("X-CSRF-TOKEN",csrf);
            if(key!=null) request.header("Idempotency-Key",key);
            if(body!=null) request.header("Content-Type","application/json");
            var response=client.send(request.method(method,body==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(body)).build(),HttpResponse.BodyHandlers.ofString());
            assertThat(response.statusCode()).as(method+" "+path+" "+response.body()).isEqualTo(expected);
            assertThat(response.headers().firstValue("X-Request-ID")).isPresent();
            return response.body().isBlank()?null:json.readTree(response.body());
        }
        Browser register() throws Exception {
            csrf=send("GET","/api/v1/csrf",null,200).get("token").asString();email="internal-verify-"+UUID.randomUUID()+"@example.com";
            var response=send("POST","/api/v1/auth/register",json.writeValueAsString(Map.of("email",email,"displayName","Internal API Test","password","InternalVerify123!")),201);
            id=UUID.fromString(response.get("user").get("id").asString());return this;
        }
        void login() throws Exception {
            csrf=send("GET","/api/v1/csrf",null,200).get("token").asString();
            send("POST","/api/v1/auth/login",json.writeValueAsString(Map.of("email",email,"password","InternalVerify123!")),200);
        }
    }
    @BeforeAll void bootstrapTestAdmin() throws Exception {
        admin=new Browser().register();jdbc.update("INSERT INTO user_role(user_id,role_code) VALUES(?,'PLATFORM_ADMIN')",admin.id);admin.login();
    }
    private UUID demo(String key) { return DemoDataSeeder.id(key); }
    private String body(Object value) { return json.writeValueAsString(value); }
    private String reason() { return "{\"reason\":\"Integration verification\"}"; }
    private Map<String,Object> content(String stem) {
        return Map.of("stemText",stem,"options",List.of(Map.of("code","A","text","2","correct",true,"weight",1),Map.of("code","B","text","3","correct",false,"weight",0)),"explanation","One plus one equals two.","shuffleAllowed",false);
    }
    @Test void roleBoundariesLastAdminAndRevokedSessions() throws Exception {
        Browser student=new Browser().register();Browser staff=new Browser().register();
        admin.send("PUT","/api/v1/admin/users/"+staff.id+"/roles",body(Map.of("roles",List.of("QUESTION_STAFF"),"reason","Assign test staff")),200);
        staff.send("GET","/api/v1/users/current",null,401);staff.login();
        for(String path:List.of("/api/v1/admin/users","/api/v1/admin/orders","/api/v1/admin/roles","/api/v1/admin/audit","/api/v1/admin/dashboard")) {
            student.send("GET",path,null,403);staff.send("GET",path,null,403);
        }
        student.send("GET","/api/v1/staff/question-bank",null,403);admin.send("GET","/api/v1/staff/question-bank",null,403);
        staff.send("GET","/api/v1/staff/question-bank",null,200);
        assertThat(admin.send("PATCH","/api/v1/admin/users/"+admin.id+"/status","{\"status\":\"SUSPENDED\",\"reason\":\"Last admin guard\"}",409).get("code").asString()).isEqualTo("LAST_ACTIVE_ADMIN");
        admin.send("PUT","/api/v1/admin/users/"+admin.id+"/roles","{\"roles\":[\"CUSTOMER\"],\"reason\":\"Last admin guard\"}",409);
        admin.send("PATCH","/api/v1/admin/users/"+student.id+"/status","{\"status\":\"SUSPENDED\",\"reason\":\"Test suspension\"}",200);
        student.send("GET","/api/v1/users/current",null,401);
        admin.send("PATCH","/api/v1/admin/users/"+student.id+"/status","{\"status\":\"ACTIVE\",\"reason\":\"Test restoration\"}",200);
        student.send("GET","/api/v1/users/current",null,401);student.login();student.send("GET","/api/v1/users/current",null,200);
        var detail=admin.send("GET","/api/v1/admin/users/"+student.id+"?reason=Support-check",null,200);
        assertThat(detail.toString()).doesNotContain("password_hash","passwordHash","auth_revision","provider_subject");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM audit_event WHERE entity_id=? AND action='USER_SUPPORT_VIEWED' AND request_id IS NOT NULL",Long.class,student.id)).isEqualTo(1);
        admin.send("GET","/api/v1/admin/users?offset=-1",null,400);admin.send("GET","/api/v1/admin/users?limit=101",null,400);
        admin.send("GET","/api/v1/admin/users/not-a-uuid?reason=Support",null,400);
    }
    @Test void internalAccountIsSsoOnlyAndAudited() throws Exception {
        String email="new-staff-"+UUID.randomUUID()+"@example.com";
        var account=admin.send("POST","/api/v1/admin/users",body(Map.of("email",email,"displayName","New test staff","roles",List.of("QUESTION_STAFF"),"reason","Provision test internal account")),201);
        assertThat(account.get("userType").asString()).isEqualTo("INTERNAL");
        assertThat(account.get("roles").toString()).isEqualTo("[\"QUESTION_STAFF\"]");
        assertThat(jdbc.queryForObject("SELECT password_hash FROM app_user WHERE email_canonical=?",String.class,email)).isNull();
        admin.send("POST","/api/v1/admin/users",body(Map.of("email",email,"displayName","Duplicate","roles",List.of("QUESTION_STAFF"),"reason","Duplicate verification")),409);
        admin.send("POST","/api/v1/admin/users",body(Map.of("email","bad@example.com","displayName","Invalid","roles",List.of("CUSTOMER"),"reason","Invalid role test")),400);
        assertThat(admin.send("GET","/api/v1/admin/roles",null,200)).hasSize(3);
    }
    @Test void concurrentStatusChangesCannotSuspendBothAdmins() throws Exception {
        Browser second=new Browser().register();
        admin.send("PUT","/api/v1/admin/users/"+second.id+"/roles","{\"roles\":[\"PLATFORM_ADMIN\"],\"reason\":\"Concurrent guard fixture\"}",200);second.login();
        java.util.function.Function<Browser,Integer> suspend=browser->{
            try {
                var request=HttpRequest.newBuilder(URI.create("http://localhost:"+port+"/api/v1/admin/users/"+browser.id+"/status"))
                    .header("X-CSRF-TOKEN",browser.csrf).header("Content-Type","application/json")
                    .method("PATCH",HttpRequest.BodyPublishers.ofString("{\"status\":\"SUSPENDED\",\"reason\":\"Concurrent last admin check\"}")).build();
                return browser.client.send(request,HttpResponse.BodyHandlers.ofString()).statusCode();
            } catch(Exception e) { throw new RuntimeException(e); }
        };
        var first=java.util.concurrent.CompletableFuture.supplyAsync(()->suspend.apply(admin));
        var other=java.util.concurrent.CompletableFuture.supplyAsync(()->suspend.apply(second));
        int firstStatus=first.get(),secondStatus=other.get();assertThat(List.of(firstStatus,secondStatus)).containsExactlyInAnyOrder(200,409);
        if(firstStatus==200) {
            second.send("PATCH","/api/v1/admin/users/"+admin.id+"/status","{\"status\":\"ACTIVE\",\"reason\":\"Restore test administrator\"}",200);admin.login();
        }
        admin.send("PUT","/api/v1/admin/users/"+second.id+"/roles","{\"roles\":[\"CUSTOMER\"],\"reason\":\"Remove temporary test admin role\"}",200);
    }
    @Test void staffSearchDraftRevisionAndImmutablePublishedVersion() throws Exception {
        Browser staff=new Browser().register();jdbc.update("INSERT INTO user_role(user_id,role_code) VALUES(?,'QUESTION_STAFF')",staff.id);staff.login();
        var taxonomy=staff.send("GET","/api/v1/staff/taxonomy",null,200);var scope=taxonomy.get(0);
        String subtest=scope.get("subtestId").asString(),topic=scope.get("topicId").asString();
        var created=staff.send("POST","/api/v1/staff/questions",body(Map.of("subtestId",subtest,"topicId",topic,"sourceTitle","Integration source unique","sourceYear",2026,"content",content("What is one plus one?"))),201);
        String id=created.get("id").asString(),version=created.get("versionId").asString();
        String root="/api/v1/staff/questions/"+id;
        var draft=staff.send("GET",root+"/draft",null,200);assertThat(draft.get("revision").asLong()).isZero();
        staff.send("PUT",root+"/draft",body(Map.of("expectedVersionId",version,"content",content("Missing revision"),"reason","Validation test")),400);
        staff.send("PUT",root+"/draft",body(Map.of("expectedVersionId",version,"expectedRevision",0,"content",Map.of("stemText","Null option","options",Arrays.asList(null,Map.of("code","B","text","3","correct",false,"weight",0)),"shuffleAllowed",false),"reason","Validation test")),400);
        String update=body(Map.of("expectedVersionId",version,"expectedRevision",0,"content",content("Edited one plus one?"),"reason","Fix draft wording"));
        assertThat(staff.send("PUT",root+"/draft",update,200).get("revision").asLong()).isEqualTo(1);
        staff.send("PUT",root+"/draft",update,409);
        staff.send("POST",root+"/publish",null,200);
        staff.send("PUT",root+"/draft",body(Map.of("expectedVersionId",version,"expectedRevision",1,"content",content("Must not overwrite"),"reason","Immutable verification")),409);
        staff.send("POST",root+"/versions",body(content("Next draft version")),200);
        var old=staff.send("GET",root+"/versions/"+version,null,200);
        assertThat(old.get("content").get("stemText").asString()).isEqualTo("Edited one plus one?");
        assertThat(staff.send("GET",root+"/versions",null,200)).hasSize(2);
        var results=staff.send("GET","/api/v1/staff/question-bank?subtestId="+subtest+"&topicId="+topic+"&sourceYear=2026&status=DRAFT&search=Integration%20source%20unique&limit=10",null,200);
        assertThat(results.get("total").asLong()).isEqualTo(1);assertThat(results.get("items").get(0).get("id").asString()).isEqualTo(id);
        staff.send("POST",root+"/archive",null,200);
        assertThat(staff.send("GET","/api/v1/staff/question-bank?status=ARCHIVED&search="+created.get("code").asString(),null,200).get("total").asLong()).isEqualTo(1);
        staff.send("GET","/api/v1/staff/dashboard",null,200);staff.send("GET","/api/v1/staff/import-history",null,200);staff.send("GET","/api/v1/staff/audit",null,200);staff.send("GET","/api/v1/staff/pool-requirements",null,200);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM audit_event WHERE entity_id=? AND action='QUESTION_DRAFT_UPDATED' AND request_id IS NOT NULL",Long.class,UUID.fromString(id))).isEqualTo(1);
    }
    @Test void taxonomyPriceAndBlueprintVersioningProtectOldSnapshots() throws Exception {
        var taxonomy=admin.send("GET","/api/v1/admin/taxonomy",null,200);
        // Demo IDs are stable, avoiding dependence on list order.
        UUID typeId=jdbc.queryForObject("SELECT exam_type_id FROM blueprint WHERE id=?",UUID.class,demo("UTBK:blueprint"));
        UUID subtest=jdbc.queryForObject("SELECT subtest_id FROM blueprint_section WHERE blueprint_version_id=(SELECT active_version_id FROM blueprint WHERE id=?) LIMIT 1",UUID.class,demo("UTBK:blueprint"));
        String code="VERIFY_"+UUID.randomUUID().toString().replace("-","").toUpperCase();
        var policy=admin.send("POST","/api/v1/admin/scoring-policies",body(Map.of("code",code,"method","BINARY","scale",100,"rules",Map.of("correctPoints",1,"wrongPoints",0,"emptyPoints",0),"reason","Integration scoring policy")),201);
        var master=admin.send("POST","/api/v1/admin/blueprints",body(Map.of("examTypeId",typeId,"code",code,"title","Integration tryout","reason","Create test blueprint")),201);
        String id=master.get("id").asString();
        var insufficient=admin.send("POST","/api/v1/admin/blueprints/"+id+"/versions",body(Map.of("durationSeconds",600,"scoringPolicyId",policy.get("id").asString(),"sections",List.of(Map.of("subtestId",subtest,"questionCount",500)),"reason","Insufficient stock test")),201);
        assertThat(insufficient.get("ready").asBoolean()).isFalse();
        admin.send("POST","/api/v1/admin/blueprints/"+id+"/versions/"+insufficient.get("id").asString()+"/publish",reason(),409);
        var version=admin.send("POST","/api/v1/admin/blueprints/"+id+"/versions",body(Map.of("durationSeconds",600,"scoringPolicyId",policy.get("id").asString(),"sections",List.of(Map.of("subtestId",subtest,"questionCount",2)),"reason","Ready stock test")),201);
        String versionId=version.get("id").asString();assertThat(version.get("ready").asBoolean()).isTrue();
        admin.send("POST","/api/v1/admin/blueprints/"+id+"/versions/"+versionId+"/publish",reason(),200);
        Browser student=new Browser().register();var attempt=student.send("POST","/api/v1/attempts",body(Map.of("tryoutId",id)),200,UUID.randomUUID().toString());
        var next=admin.send("POST","/api/v1/admin/blueprints/"+id+"/versions",body(Map.of("durationSeconds",1200,"scoringPolicyId",policy.get("id").asString(),"sections",List.of(Map.of("subtestId",subtest,"questionCount",3)),"reason","New composition")),201);
        admin.send("POST","/api/v1/admin/blueprints/"+id+"/versions/"+next.get("id").asString()+"/publish",reason(),409);
        admin.send("POST","/api/v1/admin/blueprints/"+id+"/versions/"+next.get("id").asString()+"/publish",body(Map.of("expectedActiveVersionId",versionId,"reason","Publish version two")),200);
        var session=student.send("GET","/api/v1/users/current/attempts/"+attempt.get("attemptId").asString(),null,200);
        assertThat(session.get("questions")).hasSize(2);
        assertThat(jdbc.queryForObject("SELECT blueprint_version_id FROM attempt WHERE id=?",UUID.class,UUID.fromString(attempt.get("attemptId").asString()))).isEqualTo(UUID.fromString(versionId));
        var planBody=Map.of("code",code,"name","Integration premium","months",1,"amountMinor",39000,"currency","IDR","benefits",Map.of("fullLeaderboard",true),"reason","Price revision test");
        var plan=admin.send("POST","/api/v1/admin/plans",body(planBody),201);
        var changed=new HashMap<String,Object>(planBody);changed.put("amountMinor",49000);
        var newer=admin.send("POST","/api/v1/admin/plans/"+plan.get("id").asString()+"/versions",body(changed),201);
        assertThat(newer.get("revision").asInt()).isEqualTo(2);
        var oldPlan=admin.send("GET","/api/v1/admin/plans/"+plan.get("id").asString(),null,200);
        assertThat(oldPlan.get("amountMinor").asLong()).isEqualTo(39000);assertThat(oldPlan.get("active").asBoolean()).isFalse();
        admin.send("PATCH","/api/v1/admin/plans/"+plan.get("id").asString()+"/availability","{\"active\":true,\"reason\":\"Old revision must not sell\"}",409);
        String topicId=demo("UTBK:topic").toString();
        // Hierarchy cannot be moved after creation, even when it has no FK consumers yet.
        UUID otherSubtest=jdbc.queryForObject("SELECT subtest_id FROM topic WHERE id=?",UUID.class,demo("CPNS:topic"));
        admin.send("PATCH","/api/v1/admin/topics/"+topicId,body(Map.of("subtestId",otherSubtest,"name","Wrong scope","active",true,"reason","Cross-scope test")),409);
        admin.send("PATCH","/api/v1/admin/blueprints/"+id+"/status","{\"status\":\"ARCHIVED\",\"reason\":\"Hide test catalog from independent demo fixtures\"}",200);
    }
    @Test void financeReadAndRefundRequestsNeverFakeGatewaySuccess() throws Exception {
        Browser buyer=new Browser().register();UUID planId=UUID.randomUUID(),order=UUID.randomUUID();
        jdbc.update("INSERT INTO plan(id,code,family_code,name,months,amount_minor,currency,benefits) VALUES(?,?,?,'Finance test',1,50000,'IDR','{}')",planId,"FIN-"+planId,"FIN-"+planId);
        jdbc.update("INSERT INTO purchase_order(id,user_id,plan_id,amount_minor,currency,status,idempotency_key,plan_code_snapshot,months_snapshot,benefits_snapshot) VALUES(?,?,?,50000,'IDR','PAID',?,'FINANCE_TEST',1,'{}')",order,buyer.id,planId,UUID.randomUUID().toString());
        admin.send("GET","/api/v1/admin/orders?userId="+buyer.id,null,200);
        admin.send("POST","/api/v1/admin/orders/"+order+"/reconcile",reason(),503);
        String key=UUID.randomUUID().toString();String request="{\"amountMinor\":30000,\"reason\":\"Test partial refund\"}";
        var refund=admin.send("POST","/api/v1/admin/orders/"+order+"/refunds",request,202,key);
        assertThat(refund.get("status").asString()).isEqualTo("PENDING_PROVIDER");
        assertThat(admin.send("POST","/api/v1/admin/orders/"+order+"/refunds",request,202,key).get("id").asString()).isEqualTo(refund.get("id").asString());
        admin.send("POST","/api/v1/admin/orders/"+order+"/refunds","{\"amountMinor\":25000,\"reason\":\"Excess\"}",409,UUID.randomUUID().toString());
        admin.send("POST","/api/v1/admin/orders/"+order+"/refunds","{\"amountMinor\":20000,\"reason\":\"Changed request\"}",409,key);
        assertThat(jdbc.queryForObject("SELECT status FROM purchase_order WHERE id=?",String.class,order)).isEqualTo("PAID");
        assertThat(admin.send("GET","/api/v1/admin/orders/"+order,null,200).get("refunds")).hasSize(1);
        var dashboard=admin.send("GET","/api/v1/admin/dashboard",null,200);assertThat(dashboard.get("paymentProviderConfigured").asBoolean()).isFalse();
        for(var revenue:dashboard.get("revenue")) if(revenue.get("currency").asString().equals("IDR")) assertThat(revenue.get("refundedMinor").asLong()).isZero();
        admin.send("GET","/api/v1/admin/subscriptions",null,200);admin.send("GET","/api/v1/admin/jobs",null,200);admin.send("GET","/api/v1/admin/audit?limit=1",null,200);
    }
    @Test void failedImportRetryKeepsDedupAndRejectsActiveLease() throws Exception {
        Browser staff=new Browser().register();jdbc.update("INSERT INTO user_role(user_id,role_code) VALUES(?,'QUESTION_STAFF')",staff.id);staff.login();
        UUID file=UUID.randomUUID(),batch=UUID.randomUUID(),job=UUID.randomUUID();
        jdbc.update("INSERT INTO stored_file(id,relative_path,sha256,size_bytes,mime_type,uploaded_by,purpose) VALUES(?,?,?,1,'application/pdf',?,'IMPORT_SOURCE')",file,"test/"+file+".pdf","a".repeat(64),staff.id);
        jdbc.update("INSERT INTO import_batch(id,file_id,uploader_id,status,subtest_id,source_title) VALUES(?,?,?,'FAILED',(SELECT id FROM subtest LIMIT 1),'Retry test')",batch,file,staff.id);
        jdbc.update("INSERT INTO job(id,type,dedup_key,payload,status,retry_count) VALUES(?,'IMPORT_PDF',?,?::jsonb,'FAILED',5)",job,"import:"+batch,"{\"batchId\":\""+batch+"\"}");
        assertThat(staff.send("POST","/api/v1/staff/imports/"+batch+"/retry",reason(),200).get("jobStatus").asString()).isEqualTo("QUEUED");
        staff.send("POST","/api/v1/staff/imports/"+batch+"/retry",reason(),409);
        admin.send("POST","/api/v1/admin/jobs/"+job+"/retry",reason(),409);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM job WHERE dedup_key=?",Long.class,"import:"+batch)).isEqualTo(1);
        // Prevent another test's job drainer from trying a deliberately absent test PDF.
        jdbc.update("UPDATE job SET status='SUCCEEDED' WHERE id=?",job);
    }
}
