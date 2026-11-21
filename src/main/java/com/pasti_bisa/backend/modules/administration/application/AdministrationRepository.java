package com.pasti_bisa.backend.modules.administration.application;

import com.pasti_bisa.backend.shared.web.MutationReason;

import com.pasti_bisa.backend.shared.web.PageResponse;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import com.pasti_bisa.backend.modules.administration.application.dto.AdministrationDtos.*;

public interface AdministrationRepository {
    UUID requireAdmin(String email);
    PageResponse<AdminUserResponse> users(ListQuery query);
    UserDetail user(UUID id);
    PageResponse<UserAttempt> attempts(UUID user,ListQuery query);
    AdminUserResponse createUser(UUID actor,InternalUserRequest request);
    AdminUserResponse roles(UUID actor,UUID id,RolesRequest request);
    AdminUserResponse status(UUID actor,UUID id,StatusRequest request);
    PageResponse<OrderResponse> orders(ListQuery query);
    OrderDetail order(UUID id);
    RefundResponse refund(UUID actor,UUID order,RefundRequest request,String key,String requestId);
    PageResponse<SubscriptionResponse> subscriptions(ListQuery query);
    SubscriptionResponse subscription(UUID id);
    SubscriptionResponse revoke(UUID id);
    PageResponse<JobResponse> jobs(ListQuery query);
    JobResponse retry(UUID id);
    PageResponse<AuditResponse> audit(ListQuery query);
    Dashboard dashboard(Instant from,Instant to);
    void recordAudit(UUID actor,String action,String entityType,UUID entityId,String reason,String requestId,Object before,Object after);
    void lockAccessChanges();
}
