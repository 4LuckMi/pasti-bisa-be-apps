package com.pasti_bisa.backend.modules.administration.adapter.out.persistence;

import com.pasti_bisa.backend.shared.web.MutationReason;

import com.pasti_bisa.backend.shared.web.PageResponse;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.*;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.json.JsonMapper;
import com.pasti_bisa.backend.modules.administration.application.AdministrationRepository;
import com.pasti_bisa.backend.modules.administration.application.dto.AdministrationDtos.*;
import com.pasti_bisa.backend.shared.web.BusinessException;

@Repository
public class JdbcAdministrationRepository implements AdministrationRepository {
    private final NamedParameterJdbcTemplate jdbc;
    private final JsonMapper json;
    public JdbcAdministrationRepository(NamedParameterJdbcTemplate jdbc,JsonMapper json) { this.jdbc=jdbc;this.json=json; }
    private MapSqlParameterSource p(String key,Object value) { return new MapSqlParameterSource(key,value); }
    private static Instant time(ResultSet rs,String key) throws SQLException { var value=rs.getTimestamp(key);return value==null?null:value.toInstant(); }
    private MapSqlParameterSource parameters(ListQuery q) {
        return p("search",q.search()).addValue("status",q.status()).addValue("userId",q.userId())
            .addValue("from",q.from()==null?null:java.sql.Timestamp.from(q.from())).addValue("to",q.to()==null?null:java.sql.Timestamp.from(q.to()))
            .addValue("offset",q.offset()).addValue("limit",q.limit());
    }
    private <T> PageResponse<T> page(String select,String from,MapSqlParameterSource p,ListQuery q,RowMapper<T> mapper,String order) {
        long total=jdbc.queryForObject("SELECT count(*) "+from,p,Long.class);
        var items=jdbc.query(select+from+" ORDER BY "+order+" LIMIT :limit OFFSET :offset",p,mapper);
        return new PageResponse<>(items,total,q.offset(),q.limit());
    }
    @Override public UUID requireAdmin(String email) {
        return jdbc.query("""
            SELECT u.id FROM app_user u JOIN user_role r ON r.user_id=u.id
            WHERE u.email_canonical=:email AND u.status='ACTIVE' AND r.role_code='PLATFORM_ADMIN'
            """,p("email",email),(rs,row)->rs.getObject(1,UUID.class)).stream().findFirst()
            .orElseThrow(()->new BusinessException(403,"ACCESS_DENIED","Active platform admin required"));
    }
    @Override public void lockAccessChanges() { jdbc.getJdbcTemplate().execute("SELECT pg_advisory_xact_lock(73482107)"); }
    private List<String> userRoles(UUID id) { return jdbc.queryForList("SELECT role_code FROM user_role WHERE user_id=:id ORDER BY role_code",p("id",id),String.class); }
    private AdminUserResponse account(ResultSet rs,int row) throws SQLException {
        UUID id=rs.getObject("id",UUID.class);
        return new AdminUserResponse(id,rs.getString("email"),rs.getString("display_name"),rs.getString("user_type"),rs.getString("status"),userRoles(id),time(rs,"created_at"),time(rs,"updated_at"));
    }
    @Override public PageResponse<AdminUserResponse> users(ListQuery q) {
        return page("SELECT u.id,u.email,u.display_name,u.user_type,u.status,u.created_at,u.updated_at ","""
            FROM app_user u WHERE (:search::varchar IS NULL OR u.email ILIKE '%'||:search||'%' OR u.display_name ILIKE '%'||:search||'%' OR u.id::text=:search)
              AND (:status::varchar IS NULL OR u.status=:status) AND (:from::timestamptz IS NULL OR u.created_at>=:from)
              AND (:to::timestamptz IS NULL OR u.created_at<:to)
            """,parameters(q),q,this::account,"u.created_at DESC,u.id DESC");
    }
    @Override public UserDetail user(UUID id) {
        var account=jdbc.queryForObject("SELECT id,email,display_name,user_type,status,created_at,updated_at FROM app_user WHERE id=:id",p("id",id),this::account);
        var profile=jdbc.queryForObject("""
            SELECT p.school_name,p.school_grade,p.target_score,t.kind,t.institution,t.program FROM user_profile p
            LEFT JOIN target t ON t.id=p.target_id WHERE p.user_id=:id
            """,p("id",id),(rs,row)->new SupportProfile(rs.getString("school_name"),rs.getString("school_grade"),rs.getString("kind"),rs.getString("institution"),rs.getString("program"),rs.getObject("target_score",Integer.class)));
        return new UserDetail(account,profile,jdbc.queryForObject("SELECT count(*) FROM attempt WHERE user_id=:id",p("id",id),Long.class),
            jdbc.queryForObject("SELECT count(*) FROM purchase_order WHERE user_id=:id",p("id",id),Long.class));
    }
    @Override public PageResponse<UserAttempt> attempts(UUID user,ListQuery q) {
        return page("SELECT a.id,b.title,a.mode,a.status,a.terminal_reason,a.started_at,a.finalized_at,s.total_score ","""
            FROM attempt a JOIN blueprint_version v ON v.id=a.blueprint_version_id JOIN blueprint b ON b.id=v.blueprint_id
            LEFT JOIN LATERAL(SELECT total_score FROM score_run WHERE attempt_id=a.id ORDER BY computed_at DESC LIMIT 1) s ON true
            WHERE a.user_id=:target AND (:status::varchar IS NULL OR a.status=:status)
            AND (:from::timestamptz IS NULL OR a.started_at>=:from) AND (:to::timestamptz IS NULL OR a.started_at<:to)
            """,parameters(q).addValue("target",user),q,(rs,row)->new UserAttempt(rs.getObject("id",UUID.class),rs.getString("title"),rs.getString("mode"),rs.getString("status"),rs.getString("terminal_reason"),time(rs,"started_at"),time(rs,"finalized_at"),rs.getBigDecimal("total_score")),"a.started_at DESC,a.id DESC");
    }
    @Override public AdminUserResponse createUser(UUID actor,InternalUserRequest body) {
        UUID id=UUID.randomUUID();String email=body.email().trim().toLowerCase(java.util.Locale.ROOT);
        jdbc.update("INSERT INTO app_user(id,email,email_canonical,display_name,password_hash,user_type) VALUES(:id,:email,:email,:name,NULL,'INTERNAL')",
            p("id",id).addValue("email",email).addValue("name",body.displayName().trim()));
        for(String role:new java.util.TreeSet<>(body.roles())) jdbc.update("INSERT INTO user_role(user_id,role_code) VALUES(:id,:role)",p("id",id).addValue("role",role));
        return user(id).account();
    }
    private void protectLastAdmin(UUID id,boolean removing) {
        if(!removing) return;
        boolean last=Boolean.TRUE.equals(jdbc.queryForObject("""
            SELECT EXISTS(SELECT 1 FROM app_user u JOIN user_role r ON r.user_id=u.id
              WHERE u.id=:id AND u.status='ACTIVE' AND r.role_code='PLATFORM_ADMIN')
            AND NOT EXISTS(SELECT 1 FROM app_user u JOIN user_role r ON r.user_id=u.id
              WHERE u.id<>:id AND u.status='ACTIVE' AND r.role_code='PLATFORM_ADMIN')
            """,p("id",id),Boolean.class));
        if(last) throw new BusinessException(409,"LAST_ACTIVE_ADMIN","At least one active platform admin must remain");
    }
    @Override public AdminUserResponse roles(UUID actor,UUID id,RolesRequest body) {
        jdbc.queryForObject("SELECT id FROM app_user WHERE id=:id FOR UPDATE",p("id",id),UUID.class);
        var roles=new java.util.TreeSet<>(body.roles());protectLastAdmin(id,!roles.contains("PLATFORM_ADMIN"));
        // Unchanged roles do not revoke a session. Changes are revisioned by the database trigger.
        for(String old:userRoles(id)) if(!roles.contains(old)) jdbc.update("DELETE FROM user_role WHERE user_id=:id AND role_code=:role",p("id",id).addValue("role",old));
        for(String role:roles) jdbc.update("INSERT INTO user_role(user_id,role_code) VALUES(:id,:role) ON CONFLICT DO NOTHING",p("id",id).addValue("role",role));
        return user(id).account();
    }
    @Override public AdminUserResponse status(UUID actor,UUID id,StatusRequest body) {
        jdbc.queryForObject("SELECT id FROM app_user WHERE id=:id FOR UPDATE",p("id",id),UUID.class);protectLastAdmin(id,!body.status().equals("ACTIVE"));
        jdbc.update("UPDATE app_user SET status=:status,updated_at=now() WHERE id=:id",p("id",id).addValue("status",body.status()));return user(id).account();
    }
    private static final String ORDER_SELECT="""
        SELECT o.id,o.user_id,u.email,o.plan_id,o.plan_code_snapshot,o.months_snapshot,o.amount_minor,o.currency,o.status,o.provider,o.provider_order_id,o.created_at,o.expires_at
        """;
    private final RowMapper<OrderResponse> orderMapper=(rs,row)->new OrderResponse(rs.getObject("id",UUID.class),rs.getObject("user_id",UUID.class),rs.getString("email"),rs.getObject("plan_id",UUID.class),rs.getString("plan_code_snapshot"),rs.getInt("months_snapshot"),rs.getLong("amount_minor"),rs.getString("currency"),rs.getString("status"),rs.getString("provider"),rs.getString("provider_order_id"),time(rs,"created_at"),time(rs,"expires_at"));
    @Override public PageResponse<OrderResponse> orders(ListQuery q) {
        return page(ORDER_SELECT,"""
            FROM purchase_order o JOIN app_user u ON u.id=o.user_id WHERE (:status::varchar IS NULL OR o.status=:status)
            AND (:userId::uuid IS NULL OR o.user_id=:userId) AND (:search::varchar IS NULL OR o.id::text=:search OR u.email ILIKE '%'||:search||'%' OR o.provider_order_id ILIKE '%'||:search||'%')
            AND (:from::timestamptz IS NULL OR o.created_at>=:from) AND (:to::timestamptz IS NULL OR o.created_at<:to)
            """,parameters(q),q,orderMapper,"o.created_at DESC,o.id DESC");
    }
    private final RowMapper<RefundResponse> refundMapper=(rs,row)->new RefundResponse(rs.getObject("id",UUID.class),rs.getObject("order_id",UUID.class),rs.getLong("amount_minor"),rs.getString("currency"),rs.getString("status"),time(rs,"created_at"),time(rs,"completed_at"));
    @Override public OrderDetail order(UUID id) {
        var order=jdbc.queryForObject(ORDER_SELECT+" FROM purchase_order o JOIN app_user u ON u.id=o.user_id WHERE o.id=:id",p("id",id),orderMapper);
        var events=jdbc.query("SELECT id,provider,provider_event_id,amount_minor,verified,signature_status,received_at FROM payment_event WHERE order_id=:id ORDER BY received_at,id",p("id",id),
            (rs,row)->new PaymentEventResponse(rs.getObject("id",UUID.class),rs.getString("provider"),rs.getString("provider_event_id"),rs.getObject("amount_minor",Long.class),rs.getBoolean("verified"),rs.getString("signature_status"),time(rs,"received_at")));
        return new OrderDetail(order,events,jdbc.query("SELECT * FROM refund_request WHERE order_id=:id ORDER BY created_at,id",p("id",id),refundMapper));
    }
    @Override public RefundResponse refund(UUID actor,UUID orderId,RefundRequest body,String key,String requestId) {
        jdbc.queryForObject("SELECT id FROM purchase_order WHERE id=:id FOR UPDATE",p("id",orderId),UUID.class);
        var params=p("order",orderId).addValue("key",key);
        var replay=jdbc.query("SELECT * FROM refund_request WHERE order_id=:order AND idempotency_key=:key",params,refundMapper);
        if(!replay.isEmpty()) {
            if(replay.getFirst().amountMinor()!=body.amountMinor()) throw new BusinessException(409,"IDEMPOTENCY_CONFLICT","Key was used with another refund amount");
            return replay.getFirst();
        }
        var order=order(orderId).order();if(!order.status().equals("PAID")) throw new BusinessException(409,"ORDER_NOT_PAID","Only paid orders can have a refund request");
        long reserved=jdbc.queryForObject("SELECT COALESCE(sum(amount_minor),0) FROM refund_request WHERE order_id=:order AND status IN ('PENDING_PROVIDER','PROCESSING','SUCCEEDED')",params,Long.class);
        if(body.amountMinor()>order.amountMinor()-reserved) throw new BusinessException(409,"REFUND_AMOUNT_EXCEEDED","Requested refund exceeds the remaining paid amount");
        UUID id=UUID.randomUUID();params.addValue("id",id).addValue("actor",actor).addValue("amount",body.amountMinor()).addValue("currency",order.currency()).addValue("reason",body.reason()).addValue("requestId",requestId);
        jdbc.update("INSERT INTO refund_request(id,order_id,requested_by,amount_minor,currency,reason,idempotency_key,request_id) VALUES(:id,:order,:actor,:amount,:currency,:reason,:key,:requestId)",params);
        return jdbc.queryForObject("SELECT * FROM refund_request WHERE id=:id",p("id",id),refundMapper);
    }
    private SubscriptionResponse subscription(ResultSet rs,int row) throws SQLException {
        UUID id=rs.getObject("id",UUID.class);
        var grants=jdbc.query("SELECT id,order_id,starts_at,ends_at,revoked_at FROM entitlement_grant WHERE subscription_id=:id ORDER BY starts_at,id",p("id",id),
            (g,i)->new GrantResponse(g.getObject("id",UUID.class),g.getObject("order_id",UUID.class),time(g,"starts_at"),time(g,"ends_at"),time(g,"revoked_at")));
        return new SubscriptionResponse(id,rs.getObject("user_id",UUID.class),time(rs,"starts_at"),time(rs,"ends_at"),rs.getString("status"),grants);
    }
    @Override public PageResponse<SubscriptionResponse> subscriptions(ListQuery q) {
        return page("SELECT s.* ","""
            FROM subscription s WHERE (:userId::uuid IS NULL OR s.user_id=:userId) AND (:status::varchar IS NULL OR s.status=:status)
            AND (:from::timestamptz IS NULL OR s.created_at>=:from) AND (:to::timestamptz IS NULL OR s.created_at<:to)
            """,parameters(q),q,this::subscription,"s.created_at DESC,s.id DESC");
    }
    @Override public SubscriptionResponse subscription(UUID id) { return jdbc.queryForObject("SELECT * FROM subscription WHERE id=:id",p("id",id),this::subscription); }
    @Override public SubscriptionResponse revoke(UUID id) {
        jdbc.queryForObject("SELECT id FROM subscription WHERE id=:id FOR UPDATE",p("id",id),UUID.class);
        jdbc.update("UPDATE subscription SET status='REVOKED' WHERE id=:id",p("id",id));
        jdbc.update("UPDATE entitlement_grant SET revoked_at=COALESCE(revoked_at,now()),revoke_reason=COALESCE(revoke_reason,'ADMIN_REVOKED') WHERE subscription_id=:id",p("id",id));return subscription(id);
    }
    private final RowMapper<JobResponse> jobMapper=(rs,row)->new JobResponse(rs.getObject("id",UUID.class),rs.getString("type"),rs.getString("status"),rs.getInt("retry_count"),rs.getInt("max_attempts"),time(rs,"run_after"),time(rs,"lease_until"),rs.getString("last_error"),time(rs,"created_at"));
    @Override public PageResponse<JobResponse> jobs(ListQuery q) {
        return page("SELECT j.id,j.type,j.status,j.retry_count,j.max_attempts,j.run_after,j.lease_until,j.last_error,j.created_at ","""
            FROM job j WHERE (:status::varchar IS NULL OR j.status=:status) AND (:search::varchar IS NULL OR j.type=:search OR j.id::text=:search)
            AND (:from::timestamptz IS NULL OR j.created_at>=:from) AND (:to::timestamptz IS NULL OR j.created_at<:to)
            """,parameters(q),q,jobMapper,"j.created_at DESC,j.id DESC");
    }
    @Override public JobResponse retry(UUID id) {
        var job=jdbc.queryForObject("SELECT * FROM job WHERE id=:id FOR UPDATE",p("id",id),jobMapper);
        if(!job.status().equals("FAILED")) throw new BusinessException(409,"JOB_NOT_RETRYABLE","Only failed jobs can be manually retried");
        if(!List.of("IMPORT_PDF","SCORE_ATTEMPT").contains(job.type())) throw new BusinessException(409,"UNSUPPORTED_JOB_TYPE","This job type cannot be retried here");
        jdbc.update("UPDATE job SET status='QUEUED',retry_count=0,lease_token=NULL,lease_until=NULL,lease_owner=NULL,last_error=NULL,run_after=now() WHERE id=:id",p("id",id));
        if(job.type().equals("IMPORT_PDF")) jdbc.update("UPDATE import_batch SET status='QUEUED',error_code=NULL,updated_at=now() WHERE id=(SELECT (payload->>'batchId')::uuid FROM job WHERE id=:id)",p("id",id));
        return jdbc.queryForObject("SELECT * FROM job WHERE id=:id",p("id",id),jobMapper);
    }
    @Override public PageResponse<AuditResponse> audit(ListQuery q) {
        return page("SELECT a.* ","""
            FROM audit_event a WHERE (:userId::uuid IS NULL OR a.actor_id=:userId) AND (:status::varchar IS NULL OR a.entity_type=:status)
            AND (:search::varchar IS NULL OR a.action ILIKE '%'||:search||'%' OR a.entity_id::text=:search OR a.request_id=:search)
            AND (:from::timestamptz IS NULL OR a.occurred_at>=:from) AND (:to::timestamptz IS NULL OR a.occurred_at<:to)
            """,parameters(q),q,(rs,row)->new AuditResponse(rs.getObject("id",UUID.class),rs.getObject("actor_id",UUID.class),rs.getString("action"),rs.getString("entity_type"),rs.getObject("entity_id",UUID.class),rs.getString("reason"),rs.getString("request_id"),time(rs,"occurred_at"),
                rs.getString("before_redacted")==null?null:json.readTree(rs.getString("before_redacted")),rs.getString("after_redacted")==null?null:json.readTree(rs.getString("after_redacted"))),"a.occurred_at DESC,a.id DESC");
    }
    private long count(String sql,MapSqlParameterSource p) { return jdbc.queryForObject(sql,p,Long.class); }
    @Override public Dashboard dashboard(Instant from,Instant to) {
        var p=p("from",java.sql.Timestamp.from(from)).addValue("to",java.sql.Timestamp.from(to));
        var revenue=jdbc.query("""
            WITH orders AS(SELECT currency,COALESCE(sum(amount_minor) FILTER(WHERE status IN ('PAID','REFUNDED')),0) AS gross,
              count(*) FILTER(WHERE status='PENDING') AS pending FROM purchase_order WHERE created_at>=:from AND created_at<:to GROUP BY currency),
            refunds AS(SELECT currency,sum(amount_minor) AS refunds FROM refund_request WHERE status='SUCCEEDED' AND completed_at>=:from AND completed_at<:to GROUP BY currency)
            SELECT COALESCE(o.currency,r.currency) AS currency,COALESCE(o.gross,0) AS gross,COALESCE(r.refunds,0) AS refunds,COALESCE(o.pending,0) AS pending
            FROM orders o FULL OUTER JOIN refunds r ON r.currency=o.currency ORDER BY currency
            """,p,(rs,row)->new Revenue(rs.getString("currency"),rs.getLong("gross"),rs.getLong("refunds"),rs.getLong("gross")-rs.getLong("refunds"),rs.getLong("pending")));
        return new Dashboard(Instant.now(),from,to,count("SELECT count(*) FROM app_user WHERE status='ACTIVE'",p),count("SELECT count(*) FROM app_user WHERE created_at>=:from AND created_at<:to",p),
            count("SELECT count(*) FROM attempt WHERE status='COMPLETED' AND finalized_at>=:from AND finalized_at<:to",p),
            count("SELECT count(*) FROM question q WHERE archived_at IS NULL AND EXISTS(SELECT 1 FROM question_version v WHERE v.question_id=q.id AND v.status='PUBLISHED')",p),
            count("SELECT count(*) FROM job WHERE status='FAILED'",p),count("SELECT count(*) FROM import_batch WHERE status IN ('QUEUED','NEEDS_INPUT','FAILED')",p),
            count("SELECT count(*) FROM stored_file",p),count("SELECT COALESCE(sum(size_bytes),0) FROM stored_file",p),revenue,false);
    }
    @Override public void recordAudit(UUID actor,String action,String entityType,UUID id,String reason,String requestId,Object before,Object after) {
        jdbc.update("""
            INSERT INTO audit_event(id,actor_id,action,entity_type,entity_id,reason,request_id,before_redacted,after_redacted)
            VALUES(gen_random_uuid(),:actor,:action,:type,:id,:reason,:requestId,:before::jsonb,:after::jsonb)
            """,p("actor",actor).addValue("action",action).addValue("type",entityType).addValue("id",id).addValue("reason",reason).addValue("requestId",requestId)
                .addValue("before",before==null?null:json.writeValueAsString(before)).addValue("after",after==null?null:json.writeValueAsString(after)));
    }
}
