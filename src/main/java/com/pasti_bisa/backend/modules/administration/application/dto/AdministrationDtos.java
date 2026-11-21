package com.pasti_bisa.backend.modules.administration.application.dto;

import com.pasti_bisa.backend.shared.web.MutationReason;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import jakarta.validation.constraints.*;

public final class AdministrationDtos {
    private AdministrationDtos() {}
    public record ListQuery(@Size(max=180) String search,@Size(max=32) String status,UUID userId,
            Instant from,Instant to,@Min(0) Integer offset,@Min(1) @Max(100) Integer limit) {
        public ListQuery { if(offset==null) offset=0;if(limit==null) limit=20; }
    }
    public record StatusRequest(@NotBlank @Pattern(regexp="ACTIVE|SUSPENDED") String status,@NotBlank @Size(max=500) String reason) {}
    public record RolesRequest(@NotEmpty @Size(max=3) List<@NotNull @Pattern(regexp="CUSTOMER|QUESTION_STAFF|PLATFORM_ADMIN") String> roles,
            @NotBlank @Size(max=500) String reason) {}
    public record InternalUserRequest(@NotBlank @Email @Size(max=320) String email,@NotBlank @Size(max=120) String displayName,
            @NotEmpty @Size(max=2) List<@NotNull @Pattern(regexp="QUESTION_STAFF|PLATFORM_ADMIN") String> roles,
            @NotBlank @Size(max=500) String reason) {}
    public record AdminUserResponse(UUID id,String email,String displayName,String userType,String status,
            List<String> roles,Instant createdAt,Instant updatedAt) {}
    public record SupportProfile(String schoolName,String schoolGrade,String targetKind,String institution,String program,Integer targetScore) {}
    public record UserDetail(AdminUserResponse account,SupportProfile profile,long attemptCount,long orderCount) {}
    public record UserAttempt(UUID id,String title,String mode,String status,String terminalReason,Instant startedAt,Instant finalizedAt,java.math.BigDecimal score) {}
    public record RoleDefinition(String code,String name,List<String> permissions) {}
    public record OrderResponse(UUID id,UUID userId,String userEmail,UUID planId,String planCode,int months,long amountMinor,
            String currency,String status,String provider,String providerOrderId,Instant createdAt,Instant expiresAt) {}
    public record PaymentEventResponse(UUID id,String provider,String providerEventId,Long amountMinor,boolean verified,String signatureStatus,Instant receivedAt) {}
    public record OrderDetail(OrderResponse order,List<PaymentEventResponse> events,List<RefundResponse> refunds) {}
    public record RefundRequest(@Min(1) long amountMinor,@NotBlank @Size(max=500) String reason) {}
    public record RefundResponse(UUID id,UUID orderId,long amountMinor,String currency,String status,Instant createdAt,Instant completedAt) {}
    @io.swagger.v3.oas.annotations.media.Schema(name="AdminSubscriptionResponse")
    public record SubscriptionResponse(UUID id,UUID userId,Instant startsAt,Instant endsAt,String status,List<GrantResponse> grants) {}
    public record GrantResponse(UUID id,UUID orderId,Instant startsAt,Instant endsAt,Instant revokedAt) {}
    public record JobResponse(UUID id,String type,String status,int attempts,int maxAttempts,Instant runAfter,Instant leaseUntil,String errorCode,Instant createdAt) {}
    public record AuditResponse(UUID id,UUID actorId,String action,String entityType,UUID entityId,String reason,String requestId,Instant occurredAt,
            tools.jackson.databind.JsonNode before,tools.jackson.databind.JsonNode after) {}
    public record Revenue(String currency,long paidGrossMinor,long refundedMinor,long netMinor,long pendingOrders) {}
    public record Dashboard(Instant generatedAt,Instant from,Instant to,long activeAccounts,long newAccounts,long completedSessions,
            long publishedQuestions,long failedJobs,long pendingImports,long storedFiles,long storedBytes,
            List<Revenue> revenue,boolean paymentProviderConfigured) {}
}
