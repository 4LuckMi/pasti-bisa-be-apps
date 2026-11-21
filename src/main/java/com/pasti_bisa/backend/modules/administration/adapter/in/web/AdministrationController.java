package com.pasti_bisa.backend.modules.administration.adapter.in.web;

import com.pasti_bisa.backend.shared.web.MutationReason;

import com.pasti_bisa.backend.shared.web.PageResponse;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.pasti_bisa.backend.modules.administration.application.AdministrationService;
import com.pasti_bisa.backend.modules.administration.application.dto.AdministrationDtos.*;
import com.pasti_bisa.backend.shared.web.ApiRequestLoggingFilter;

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('PLATFORM_ADMIN')")
public class AdministrationController {
    private final AdministrationService service;
    public AdministrationController(AdministrationService service) { this.service=service; }
    private String rid(HttpServletRequest request) { return ApiRequestLoggingFilter.requestId(request); }
    @GetMapping("/dashboard") public Dashboard dashboard(@RequestParam(required=false) Instant from,@RequestParam(required=false) Instant to) { return service.dashboard(from,to); }
    @GetMapping("/roles") public List<RoleDefinition> roles() { return service.roles(); }
    @GetMapping("/users") public PageResponse<AdminUserResponse> users(@Valid @ParameterObject @ModelAttribute ListQuery q) { return service.users(q); }
    @PostMapping("/users") @ResponseStatus(HttpStatus.CREATED)
    public AdminUserResponse create(Authentication auth,@Valid @RequestBody InternalUserRequest body,HttpServletRequest request) { return service.create(auth.getName(),body,rid(request)); }
    @GetMapping("/users/{id}") public UserDetail user(Authentication auth,@PathVariable UUID id,@RequestParam @NotBlank @Size(max=500) String reason,HttpServletRequest request) { return service.user(auth.getName(),id,reason,rid(request)); }
    @GetMapping("/users/{id}/attempts") public PageResponse<UserAttempt> attempts(Authentication auth,@PathVariable UUID id,@Valid @ParameterObject @ModelAttribute ListQuery q,@RequestParam @NotBlank @Size(max=500) String reason,HttpServletRequest request) { return service.attempts(auth.getName(),id,q,reason,rid(request)); }
    @PutMapping("/users/{id}/roles") public AdminUserResponse roles(Authentication auth,@PathVariable UUID id,@Valid @RequestBody RolesRequest body,HttpServletRequest request) { return service.roles(auth.getName(),id,body,rid(request)); }
    @PatchMapping("/users/{id}/status") public AdminUserResponse status(Authentication auth,@PathVariable UUID id,@Valid @RequestBody StatusRequest body,HttpServletRequest request) { return service.status(auth.getName(),id,body,rid(request)); }
    @GetMapping("/orders") public PageResponse<OrderResponse> orders(@Valid @ParameterObject @ModelAttribute ListQuery q) { return service.orders(q); }
    @GetMapping("/orders/{id}") public OrderDetail order(@PathVariable UUID id) { return service.order(id); }
    @PostMapping("/orders/{id}/reconcile") public void reconcile(@PathVariable UUID id,@Valid @RequestBody MutationReason body) { service.reconcile(id); }
    @PostMapping("/orders/{id}/refunds") @ResponseStatus(HttpStatus.ACCEPTED)
    public RefundResponse refund(Authentication auth,@PathVariable UUID id,@Valid @RequestBody RefundRequest body,@RequestHeader(value="Idempotency-Key",required=false) String key,HttpServletRequest request) { return service.refund(auth.getName(),id,body,key,rid(request)); }
    @GetMapping("/subscriptions") public PageResponse<SubscriptionResponse> subscriptions(@Valid @ParameterObject @ModelAttribute ListQuery q) { return service.subscriptions(q); }
    @PostMapping("/subscriptions/{id}/revoke") public SubscriptionResponse revoke(Authentication auth,@PathVariable UUID id,@Valid @RequestBody MutationReason body,HttpServletRequest request) { return service.revoke(auth.getName(),id,body,rid(request)); }
    @GetMapping("/jobs") public PageResponse<JobResponse> jobs(@Valid @ParameterObject @ModelAttribute ListQuery q) { return service.jobs(q); }
    @PostMapping("/jobs/{id}/retry") public JobResponse retry(Authentication auth,@PathVariable UUID id,@Valid @RequestBody MutationReason body,HttpServletRequest request) { return service.retry(auth.getName(),id,body,rid(request)); }
    @GetMapping("/audit") public PageResponse<AuditResponse> audit(@Valid @ParameterObject @ModelAttribute ListQuery q) { return service.audit(q); }
}
