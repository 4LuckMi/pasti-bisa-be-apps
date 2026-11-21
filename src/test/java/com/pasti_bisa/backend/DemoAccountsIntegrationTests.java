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
import com.pasti_bisa.backend.modules.identity.adapter.out.demo.DemoInternalAccountSeeder;
import static org.assertj.core.api.Assertions.*;

@ActiveProfiles({"demo","demo-accounts"})
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties={
    "DEMO_ADMIN_PASSWORD=AdminDemoTest-strong123!", "DEMO_STAFF_PASSWORD=StaffDemoTest-strong456!",
    "pastisiap.demo.accounts.exit-after-seed=false"})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DemoAccountsIntegrationTests {
    private static final String ADMIN_PASSWORD="AdminDemoTest-strong123!",STAFF_PASSWORD="StaffDemoTest-strong456!";
    @LocalServerPort int port;
    @Autowired DemoInternalAccountSeeder seeder;
    @Autowired JdbcTemplate jdbc;
    @Autowired JsonMapper json;
    private final UUID collisionId=UUID.randomUUID();
    private UUID adminId(){return DemoInternalAccountSeeder.id("PLATFORM_ADMIN");}
    private UUID staffId(){return DemoInternalAccountSeeder.id("QUESTION_STAFF");}
    private class Browser {
        final HttpClient client=HttpClient.newBuilder().cookieHandler(new CookieManager(null,CookiePolicy.ACCEPT_ALL)).build();
        String csrf;
        JsonNode send(String method,String path,String body,int status)throws Exception{
            var request=HttpRequest.newBuilder(URI.create("http://localhost:"+port+"/api/v1"+path));
            if(csrf!=null)request.header("X-CSRF-TOKEN",csrf);
            if(body!=null)request.header("Content-Type","application/json");
            var response=client.send(request.method(method,body==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(body)).build(),HttpResponse.BodyHandlers.ofString());
            assertThat(response.statusCode()).as(method+" "+path).isEqualTo(status);
            return response.body().isBlank()?null:json.readTree(response.body());
        }
        void login(String email,String password,String role)throws Exception{
            csrf=send("GET","/csrf",null,200).get("token").asString();
            var user=send("POST","/auth/login",json.writeValueAsString(Map.of("email",email,"password",password)),200).get("user");
            assertThat(user.get("email").asString()).isEqualTo(email);
            assertThat(user.get("userType").asString()).isEqualTo("INTERNAL");
            assertThat(user.get("roles").size()).isEqualTo(1);
            assertThat(user.get("roles").get(0).asString()).isEqualTo(role);
        }
    }
    @Test @Order(1) void actualPasswordLoginsAndRoleBoundaries()throws Exception{
        var admin=new Browser();admin.login(DemoInternalAccountSeeder.ADMIN_EMAIL,ADMIN_PASSWORD,"PLATFORM_ADMIN");
        var staff=new Browser();staff.login(DemoInternalAccountSeeder.STAFF_EMAIL,STAFF_PASSWORD,"QUESTION_STAFF");
        for(String path:List.of("/admin/roles","/admin/users","/admin/dashboard"))admin.send("GET",path,null,200);
        for(String path:List.of("/staff/dashboard","/staff/taxonomy","/staff/question-bank","/staff/question-sets","/staff/workbook-imports","/staff/import-history","/staff/pool-requirements","/staff/audit"))staff.send("GET",path,null,200);
        admin.send("GET","/staff/dashboard",null,403);staff.send("GET","/admin/users",null,403);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM audit_event WHERE action='DEMO_INTERNAL_ACCOUNT_CREATED' AND actor_id IN (?,?)",Integer.class,adminId(),staffId())).isEqualTo(2);
    }
    @Test @Order(2) void repeatedSeedPreservesPasswordAndRevision(){
        var before=jdbc.queryForList("SELECT id,password_hash,auth_revision FROM app_user WHERE id IN (?,?) ORDER BY id",adminId(),staffId());
        assertThat(seeder.seed(ADMIN_PASSWORD,STAFF_PASSWORD)).allMatch(account->!account.created());
        assertThat(jdbc.queryForList("SELECT id,password_hash,auth_revision FROM app_user WHERE id IN (?,?) ORDER BY id",adminId(),staffId())).isEqualTo(before);
        assertThatThrownBy(()->seeder.seed(ADMIN_PASSWORD,"ChangedStaffSecret-123!")).isInstanceOf(IllegalStateException.class);
        assertThat(jdbc.queryForList("SELECT id,password_hash,auth_revision FROM app_user WHERE id IN (?,?) ORDER BY id",adminId(),staffId())).isEqualTo(before);
        assertThatThrownBy(()->seeder.seed("short",STAFF_PASSWORD)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(()->seeder.seed(ADMIN_PASSWORD,ADMIN_PASSWORD)).isInstanceOf(IllegalArgumentException.class);
    }
    @Test @Order(3) void changedPermissionsAreNeverRestored(){
        jdbc.update("UPDATE app_user SET status='SUSPENDED' WHERE id=?",staffId());
        try{
            assertThatThrownBy(()->seeder.seed(ADMIN_PASSWORD,STAFF_PASSWORD)).isInstanceOf(IllegalStateException.class);
            assertThat(jdbc.queryForObject("SELECT status FROM app_user WHERE id=?",String.class,staffId())).isEqualTo("SUSPENDED");
        }finally{jdbc.update("UPDATE app_user SET status='ACTIVE' WHERE id=?",staffId());}
        jdbc.update("INSERT INTO user_role(user_id,role_code) VALUES(?,'CUSTOMER')",staffId());
        try{assertThatThrownBy(()->seeder.seed(ADMIN_PASSWORD,STAFF_PASSWORD)).isInstanceOf(IllegalStateException.class);}
        finally{jdbc.update("DELETE FROM user_role WHERE user_id=? AND role_code='CUSTOMER'",staffId());}
    }
    @Test @Order(4) void reservedEmailCollisionCannotPromoteCustomerAndRollsBackBothAccounts(){
        cleanup();
        jdbc.update("INSERT INTO app_user(id,email,email_canonical,password_hash,display_name,user_type) VALUES(?,?,?,?,?,'SISWA')",collisionId,DemoInternalAccountSeeder.STAFF_EMAIL,DemoInternalAccountSeeder.STAFF_EMAIL,"test-only-hash","Customer collision test");
        jdbc.update("INSERT INTO user_role(user_id,role_code) VALUES(?,'CUSTOMER')",collisionId);
        assertThatThrownBy(()->seeder.seed(ADMIN_PASSWORD,STAFF_PASSWORD)).isInstanceOf(IllegalStateException.class);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM app_user WHERE id=?",Integer.class,adminId())).isZero();
        assertThat(jdbc.queryForList("SELECT role_code FROM user_role WHERE user_id=?",String.class,collisionId)).containsExactly("CUSTOMER");
    }
    @AfterAll void cleanup(){
        for(UUID id:List.of(adminId(),staffId(),collisionId)){
            jdbc.update("DELETE FROM audit_event WHERE actor_id=? OR entity_id=?",id,id);
            jdbc.update("DELETE FROM user_role WHERE user_id=?",id);
            jdbc.update("DELETE FROM user_profile WHERE user_id=?",id);
            jdbc.update("DELETE FROM app_user WHERE id=?",id);
        }
    }
}
