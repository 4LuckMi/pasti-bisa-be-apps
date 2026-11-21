package com.pasti_bisa.backend;

import java.net.*;
import java.net.http.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
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
class StudentExperienceIntegrationTests {
    @LocalServerPort int port;
    @Autowired JdbcTemplate jdbc;
    @Autowired JsonMapper json;
    private class Browser {
        final HttpClient client=HttpClient.newBuilder().cookieHandler(new CookieManager(null,CookiePolicy.ACCEPT_ALL)).build();
        String token,email; UUID id;
        JsonNode send(String method,String path,String body,int expected)throws Exception{
            var request=HttpRequest.newBuilder(URI.create("http://localhost:"+port+"/api/v1"+path));
            if(token!=null)request.header("X-CSRF-TOKEN",token);
            if(body!=null)request.header("Content-Type","application/json");
            if(path.equals("/attempts"))request.header("Idempotency-Key",UUID.randomUUID().toString());
            var response=client.send(request.method(method,body==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(body)).build(),HttpResponse.BodyHandlers.ofString());
            assertThat(response.statusCode()).as(method+" "+path+" "+response.body()).isEqualTo(expected);
            return response.body().isBlank()?null:json.readTree(response.body());
        }
        Browser register()throws Exception{
            token=send("GET","/csrf",null,200).get("token").asString(); email="experience-"+UUID.randomUUID()+"@example.com";
            var account=send("POST","/auth/register",json.writeValueAsString(Map.of("email",email,"displayName","Test Student","password","ExperienceTest123!")),201);
            id=UUID.fromString(account.get("user").get("id").asString());return this;
        }
    }
    private UUID completed(Browser b,String category,Instant at,UUID form,int points){
        UUID attempt=UUID.randomUUID(),score=UUID.randomUUID();
        jdbc.update("""
                INSERT INTO attempt(id,user_id,blueprint_version_id,mode,status,idempotency_key,started_at,deadline_at,finalized_at,terminal_reason,form_id)
                VALUES (?,?,?,'TRYOUT','COMPLETED',?,?,?,?,'SUBMITTED',?)
                """,attempt,b.id,DemoDataSeeder.id(category+":blueprint-version"),attempt.toString(),
                OffsetDateTime.ofInstant(at.minusSeconds(60),ZoneOffset.UTC),OffsetDateTime.ofInstant(at.plusSeconds(300),ZoneOffset.UTC),OffsetDateTime.ofInstant(at,ZoneOffset.UTC),form);
        jdbc.update("""
                INSERT INTO score_run(id,attempt_id,policy_version,input_hash,total_score,details,scoring_policy_id)
                VALUES (?,?,'DEMO_BINARY:1',?,?, '{}',?)
                """,score,attempt,"1".repeat(64),points,DemoDataSeeder.id("policy"));return score;
    }
    private UUID paid(Browser b,boolean full){
        UUID plan=UUID.randomUUID(),order=UUID.randomUUID();
        jdbc.update("INSERT INTO plan(id,code,family_code,name,months,amount_minor,currency,benefits) VALUES (?,?,?,'Test plan',1,39000,'IDR',?::jsonb)",
                plan,"TEST-"+plan,"TEST-"+plan,"{\"fullLeaderboard\":"+full+"}");
        jdbc.update("""
                INSERT INTO purchase_order(id,user_id,plan_id,amount_minor,currency,status,idempotency_key,plan_code_snapshot,months_snapshot,benefits_snapshot)
                VALUES (?,?,?,39000,'IDR','PAID',?,?,1,?::jsonb)
                """,order,b.id,plan,order.toString(),"TEST-"+plan,"{\"fullLeaderboard\":"+full+"}");
        jdbc.update("INSERT INTO entitlement_grant(id,user_id,order_id,starts_at,ends_at) VALUES (?,?,?,now()-interval '1 day',now()+interval '1 month')",UUID.randomUUID(),b.id,order);
        return plan;
    }
    @Test void openApiKeepsStudentAndAdminDashboardContractsSeparate()throws Exception{
        var response=HttpClient.newHttpClient().send(HttpRequest.newBuilder(URI.create("http://localhost:"+port+"/api-docs")).GET().build(),HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(200);
        var spec=json.readTree(response.body());
        assertThat(spec.get("components").get("schemas").get("StudentDashboard").get("properties").has("scoreGroups")).isTrue();
        assertThat(spec.get("components").get("schemas").get("Dashboard").get("properties").has("activeAccounts")).isTrue();
        new Browser().send("GET","/users/current/dashboard",null,401);
    }
    @Test void historyAndDashboardUseAllRowsAndCategoryAndWibDates()throws Exception{
        var b=new Browser().register();Instant now=Instant.now();
        for(int n=0;n<103;n++)completed(b,"UTBK",now.minusSeconds(n*100L),null,80);
        completed(b,"CPNS",now,null,20);
        var stranger=new Browser().register();completed(stranger,"UTBK",now,null,5);
        var page=b.send("GET","/users/current/learning-history?category=UTBK&limit=6&offset=102",null,200);
        assertThat(page.get("total").asLong()).isEqualTo(103);assertThat(page.get("items").size()).isEqualTo(1);
        assertThat(page.get("summary").get("completed").asLong()).isEqualTo(103);
        var dashboard=b.send("GET","/users/current/dashboard?category=CPNS&mode=TRYOUT",null,200);
        assertThat(dashboard.get("totalTryouts").asLong()).isEqualTo(1);
        assertThat(dashboard.get("scoreGroups").size()).isEqualTo(1);
        assertThat(dashboard.get("scoreGroups").get(0).get("average").asInt()).isEqualTo(20);
        assertThat(dashboard.get("elapsedSeconds").asLong()).isEqualTo(60);
        assertThat(dashboard.get("currentStreakDays").asInt()).isEqualTo(1);
        var mixed=b.send("GET","/users/current/dashboard",null,200);assertThat(mixed.get("scoreGroups").size()).isEqualTo(2);
        String today=LocalDate.now(ZoneId.of("Asia/Jakarta")).toString();
        assertThat(b.send("GET","/users/current/learning-history?category=CPNS&from="+today+"&to="+today,null,200).get("total").asLong()).isEqualTo(1);
        b.send("GET","/users/current/learning-history?period=WEEK&from="+today,null,400);
        b.send("GET","/users/current/learning-history?limit=101",null,400);
        b.send("GET","/users/current/learning-history?from=2026-12-01&to=2026-01-01",null,400);
        assertThat(b.send("GET","/users/current/attempts?limit=1",null,200).isArray()).isTrue();
    }
    @Test void profilePatchPreservesOmittedFieldsAndPrivacyRequiresExplicitConsent()throws Exception{
        var b=new Browser().register();
        var profile=b.send("PATCH","/users/current/profile","{\"targetCategory\":\"CPNS\",\"targetInstitution\":\"Kementerian Contoh\"}",200);
        assertThat(profile.get("schoolName").isNull()).isTrue();
        assertThat(profile.get("targetKind").asString()).isEqualTo("CIVIL_SERVICE");
        profile=b.send("PATCH","/users/current/profile","{\"displayName\":\"Nama Baru\"}",200);
        assertThat(profile.get("targetInstitution").asString()).isEqualTo("Kementerian Contoh");
        var bad=b.send("PATCH","/users/current/profile","{\"targetCategory\":\"UTBK\",\"targetInstitution\":\"Kampus\"}",400);
        assertThat(bad.get("fieldErrors").has("schoolName")).isTrue();
        b.send("PATCH","/users/current/profile","{\"displayName\":null}",400);
        assertThat(b.send("PATCH","/users/current/profile","{\"targetCategory\":null}",200).get("targetKind").isNull()).isTrue();
        assertThat(b.send("GET","/users/current/privacy",null,200).get("leaderboardOptIn").asBoolean()).isFalse();
        var privacy=b.send("PATCH","/users/current/privacy","{\"leaderboardOptIn\":true}",200);
        assertThat(privacy.get("leaderboardOptIn").asBoolean()).isTrue();assertThat(privacy.get("updatedAt").isNull()).isFalse();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM audit_event WHERE actor_id=? AND action='STUDENT_PRIVACY_UPDATED'",Long.class,b.id)).isEqualTo(1);
        b.send("PATCH","/users/current/privacy","{}",400);
    }
    @Test void catalogPlansAndMembershipUsePersistedValues()throws Exception{
        var b=new Browser().register();
        var cards=b.send("GET","/catalog/tryouts?category=UTBK&ready=true&limit=1",null,200);
        assertThat(cards.get("total").asInt()).isGreaterThanOrEqualTo(1);assertThat(cards.get("items").get(0).get("ready").asBoolean()).isTrue();
        var topics=b.send("GET","/catalog/practice-topics?category=CPNS&ready=true",null,200);
        assertThat(topics.get("items").get(0).get("category").asString()).isEqualTo("CPNS");
        assertThat(b.send("GET","/catalog/tryouts?search=no-such-"+UUID.randomUUID(),null,200).get("total").asInt()).isZero();
        assertThat(b.send("GET","/users/current/membership",null,200).get("tier").asString()).isEqualTo("FREE");
        UUID plan=paid(b,true);
        var membership=b.send("GET","/users/current/membership",null,200);
        assertThat(membership.get("tier").asString()).isEqualTo("PREMIUM");
        assertThat(membership.get("benefits").get("fullLeaderboard").asBoolean()).isTrue();
        assertThat(membership.get("plan").get("amountMinor").asLong()).isEqualTo(39000);
        assertThat(b.send("GET","/plans/"+plan,null,200).get("id").asString()).isEqualTo(plan.toString());
        b.send("GET","/plans?limit=1",null,200);
        jdbc.update("UPDATE plan SET active=false WHERE id=?",plan);
        b.send("GET","/plans/"+plan,null,404);
        assertThat(b.send("GET","/users/current/membership",null,200).get("tier").asString()).isEqualTo("PREMIUM");
        jdbc.update("UPDATE entitlement_grant SET revoked_at=now() WHERE user_id=?",b.id);
        assertThat(b.send("GET","/users/current/membership",null,200).get("tier").asString()).isEqualTo("FREE");
    }
    @Test void scoringFailureIsVisibleAndDoesNotAskFrontendToPollForever()throws Exception{
        var b=new Browser().register();
        var start=b.send("POST","/attempts","{\"tryoutId\":\""+DemoDataSeeder.id("UTBK:blueprint")+"\"}",200);
        String id=start.get("attemptId").asString();
        b.send("POST","/users/current/attempts/"+id+"/finalize","{}",200);
        var waiting=b.send("GET","/users/current/attempts/"+id+"/result",null,200);
        assertThat(waiting.get("processing").get("poll").asBoolean()).isTrue();
        jdbc.update("UPDATE job SET status='FAILED',retry_count=max_attempts,last_error='Internal secret must not leak' WHERE dedup_key=?","score:"+id);
        var failed=b.send("GET","/users/current/attempts/"+id+"/result",null,200);
        assertThat(failed.get("processing").get("state").asString()).isEqualTo("FAILED");
        assertThat(failed.get("processing").get("poll").asBoolean()).isFalse();
        assertThat(failed.toString()).doesNotContain("Internal secret");
        assertThat(b.send("GET","/users/current/learning-history",null,200).get("summary").get("scoringFailed").asInt()).isEqualTo(1);
        var stranger=new Browser().register();stranger.send("GET","/users/current/attempts/"+id+"/result",null,404);
    }
    @Test void leaderboardIsCohortScopedAndPreviewCannotBeBypassed()throws Exception{
        var viewer=new Browser().register();UUID form=UUID.randomUUID(),cohort=UUID.randomUUID();
        jdbc.update("INSERT INTO exam_form(id,blueprint_version_id,label,status) VALUES (?,?,?,'ACTIVE')",form,DemoDataSeeder.id("UTBK:blueprint-version"),"Test "+form);
        jdbc.update("INSERT INTO leaderboard_cohort(id,form_id,scoring_policy_id,period_start,period_end,attempt_policy,status) VALUES (?,?,?,now()-interval '1 hour',now()+interval '1 day','BEST','ACTIVE')",cohort,form,DemoDataSeeder.id("policy"));
        for(int rank=1;rank<=5;rank++){
            var participant=rank==5?viewer:new Browser().register();
            participant.send("PATCH","/users/current/privacy","{\"leaderboardOptIn\":true}",200);
            UUID score=completed(participant,"UTBK",Instant.now(),form,100-rank);
            jdbc.update("INSERT INTO leaderboard_entry(cohort_id,user_id,score_run_id,rank,display_name_snapshot) VALUES (?,?,?,?,'Siswa')",cohort,participant.id,score,rank);
        }
        String route="/leaderboards/cohorts/"+cohort+"/entries";
        var preview=viewer.send("GET",route,null,200);
        assertThat(preview.get("items").size()).isEqualTo(3);assertThat(preview.get("ownRank").get("rank").asInt()).isEqualTo(5);
        assertThat(preview.toString()).doesNotContain("@example.com");
        assertThat(viewer.send("GET",route+"?offset=3",null,200).get("items").isEmpty()).isTrue();
        viewer.send("GET",route+"?targetKind=CAMPUS",null,403);
        assertThat(viewer.send("GET","/leaderboard?category=UTBK",null,200).size()).isEqualTo(3);
        UUID plan=paid(viewer,true);
        assertThat(viewer.send("GET",route+"?offset=3",null,200).get("items").size()).isEqualTo(2);
        assertThat(viewer.send("GET",route+"?targetKind=CAMPUS",null,200).get("items").isEmpty()).isTrue();
        viewer.send("PATCH","/users/current/privacy","{\"leaderboardOptIn\":false}",200);
        var hidden=viewer.send("GET",route,null,200);assertThat(hidden.get("ownRank").isNull()).isTrue();assertThat(hidden.get("totalParticipants").asInt()).isEqualTo(4);
        viewer.send("GET","/leaderboards/cohorts?category=UTBK",null,200);
        jdbc.update("UPDATE leaderboard_cohort SET period_start=now()+interval '2 days',period_end=now()+interval '3 days' WHERE id=?",cohort);
        viewer.send("GET",route,null,404);
        jdbc.update("UPDATE leaderboard_cohort SET status='ARCHIVED' WHERE id=?",cohort);
        jdbc.update("UPDATE plan SET active=false WHERE id=?",plan);
    }
}
