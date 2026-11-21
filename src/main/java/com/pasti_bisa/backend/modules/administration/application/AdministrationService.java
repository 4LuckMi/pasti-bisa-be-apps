package com.pasti_bisa.backend.modules.administration.application;

import com.pasti_bisa.backend.shared.web.MutationReason;

import com.pasti_bisa.backend.shared.web.PageResponse;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.pasti_bisa.backend.modules.administration.application.dto.AdministrationDtos.*;
import com.pasti_bisa.backend.shared.web.BusinessException;

@Service
@Transactional(readOnly=true)
public class AdministrationService {
    private final AdministrationRepository repository;
    private final Clock clock;
    public AdministrationService(AdministrationRepository repository,Clock clock) { this.repository=repository;this.clock=clock; }
    private void range(ListQuery q) { if(q.from()!=null && q.to()!=null && !q.to().isAfter(q.from())) throw new BusinessException(400,"INVALID_PERIOD","to must be after from"); }
    public PageResponse<AdminUserResponse> users(ListQuery q) { range(q);return repository.users(q); }
    @Transactional public UserDetail user(String email,UUID id,String reason,String requestId) {
        UUID actor=repository.requireAdmin(email);var user=repository.user(id);
        repository.recordAudit(actor,"USER_SUPPORT_VIEWED","USER",id,reason,requestId,null,null);return user;
    }
    @Transactional public PageResponse<UserAttempt> attempts(String email,UUID id,ListQuery q,String reason,String requestId) {
        range(q);UUID actor=repository.requireAdmin(email);repository.user(id);
        repository.recordAudit(actor,"USER_ATTEMPTS_VIEWED","USER",id,reason,requestId,null,null);return repository.attempts(id,q);
    }
    @Transactional public AdminUserResponse create(String email,InternalUserRequest body,String requestId) {
        repository.lockAccessChanges();UUID actor=repository.requireAdmin(email);var user=repository.createUser(actor,body);
        repository.recordAudit(actor,"INTERNAL_USER_CREATED","USER",user.id(),body.reason(),requestId,null,new RolesRequest(user.roles(),body.reason()));return user;
    }
    @Transactional public AdminUserResponse roles(String email,UUID id,RolesRequest body,String requestId) {
        repository.lockAccessChanges();UUID actor=repository.requireAdmin(email);var before=repository.user(id).account();
        var after=repository.roles(actor,id,body);
        repository.recordAudit(actor,"USER_ROLES_CHANGED","USER",id,body.reason(),requestId,before.roles(),after.roles());return after;
    }
    @Transactional public AdminUserResponse status(String email,UUID id,StatusRequest body,String requestId) {
        repository.lockAccessChanges();UUID actor=repository.requireAdmin(email);var before=repository.user(id).account().status();
        var after=repository.status(actor,id,body);
        repository.recordAudit(actor,"USER_STATUS_CHANGED","USER",id,body.reason(),requestId,before,after.status());return after;
    }
    public List<RoleDefinition> roles() { return List.of(
        new RoleDefinition("CUSTOMER","Siswa",List.of("OWN_PROFILE","OWN_ATTEMPTS","LEARNING_CATALOG")),
        new RoleDefinition("QUESTION_STAFF","Petugas Bank Soal",List.of("QUESTION_BANK","PDF_IMPORT","CONTENT_AUDIT")),
        new RoleDefinition("PLATFORM_ADMIN","Admin Platform",List.of("USERS","ROLES","PRODUCT_CONFIGURATION","FINANCE","OPERATIONS_AUDIT"))); }
    public PageResponse<OrderResponse> orders(ListQuery q) { range(q);return repository.orders(q); }
    public OrderDetail order(UUID id) { return repository.order(id); }
    @Transactional public RefundResponse refund(String email,UUID id,RefundRequest body,String key,String requestId) {
        if(key==null || !key.matches("[A-Za-z0-9._:-]{8,128}")) throw new BusinessException(400,"IDEMPOTENCY_KEY_REQUIRED","Send an Idempotency-Key with 8 to 128 safe characters");
        UUID actor=repository.requireAdmin(email);var result=repository.refund(actor,id,body,key,requestId);
        repository.recordAudit(actor,"REFUND_REQUESTED","ORDER",id,body.reason(),requestId,null,result);return result;
    }
    public PageResponse<SubscriptionResponse> subscriptions(ListQuery q) { range(q);return repository.subscriptions(q); }
    @Transactional public SubscriptionResponse revoke(String email,UUID id,MutationReason body,String requestId) {
        UUID actor=repository.requireAdmin(email);var before=repository.subscription(id);var after=repository.revoke(id);
        repository.recordAudit(actor,"SUBSCRIPTION_REVOKED","SUBSCRIPTION",id,body.reason(),requestId,before,after);return after;
    }
    public PageResponse<JobResponse> jobs(ListQuery q) { range(q);return repository.jobs(q); }
    @Transactional public JobResponse retry(String email,UUID id,MutationReason body,String requestId) {
        UUID actor=repository.requireAdmin(email);var result=repository.retry(id);
        repository.recordAudit(actor,"JOB_RETRIED","JOB",id,body.reason(),requestId,null,result);return result;
    }
    public PageResponse<AuditResponse> audit(ListQuery q) { range(q);return repository.audit(q); }
    public Dashboard dashboard(Instant from,Instant to) {
        if(to==null) to=clock.instant();if(from==null) from=to.minus(30,ChronoUnit.DAYS);
        if(!to.isAfter(from)) throw new BusinessException(400,"INVALID_PERIOD","to must be after from");
        return repository.dashboard(from,to);
    }
    public void reconcile(UUID id) {
        repository.order(id);
        throw new BusinessException(503,"PAYMENT_PROVIDER_NOT_CONFIGURED","Gateway reconciliation is unavailable until a payment provider is configured; order status was not changed");
    }
}
