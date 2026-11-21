package com.pasti_bisa.backend.modules.administration.adapter.in.web;

import com.pasti_bisa.backend.shared.web.PageResponse;

import java.util.UUID;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.pasti_bisa.backend.modules.administration.application.CatalogManagementService;
import com.pasti_bisa.backend.modules.administration.application.dto.AdministrationDtos.*;
import com.pasti_bisa.backend.modules.administration.application.dto.CatalogManagementDtos.*;
import com.pasti_bisa.backend.shared.web.ApiRequestLoggingFilter;

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('PLATFORM_ADMIN')")
public class CatalogManagementController {
    private final CatalogManagementService service;
    public CatalogManagementController(CatalogManagementService service) { this.service=service; }
    private String rid(HttpServletRequest r) { return ApiRequestLoggingFilter.requestId(r); }
    @GetMapping("/taxonomy") public TaxonomyTree taxonomy() { return service.taxonomy(); }
    @PostMapping("/categories") @ResponseStatus(HttpStatus.CREATED) public TaxonomyItem createCategory(Authentication a,@Valid @RequestBody CategoryRequest b,HttpServletRequest r) { return service.category(a.getName(),null,b,rid(r)); }
    @PatchMapping("/categories/{id}") public TaxonomyItem category(Authentication a,@PathVariable UUID id,@Valid @RequestBody CategoryRequest b,HttpServletRequest r) { return service.category(a.getName(),id,b,rid(r)); }
    @PostMapping("/exam-types") @ResponseStatus(HttpStatus.CREATED) public TaxonomyItem createExamType(Authentication a,@Valid @RequestBody ExamTypeRequest b,HttpServletRequest r) { return service.examType(a.getName(),null,b,rid(r)); }
    @PatchMapping("/exam-types/{id}") public TaxonomyItem examType(Authentication a,@PathVariable UUID id,@Valid @RequestBody ExamTypeRequest b,HttpServletRequest r) { return service.examType(a.getName(),id,b,rid(r)); }
    @PostMapping("/subtests") @ResponseStatus(HttpStatus.CREATED) public TaxonomyItem createSubtest(Authentication a,@Valid @RequestBody SubtestRequest b,HttpServletRequest r) { return service.subtest(a.getName(),null,b,rid(r)); }
    @PatchMapping("/subtests/{id}") public TaxonomyItem subtest(Authentication a,@PathVariable UUID id,@Valid @RequestBody SubtestRequest b,HttpServletRequest r) { return service.subtest(a.getName(),id,b,rid(r)); }
    @PostMapping("/topics") @ResponseStatus(HttpStatus.CREATED) public TaxonomyItem createTopic(Authentication a,@Valid @RequestBody TopicRequest b,HttpServletRequest r) { return service.topic(a.getName(),null,b,rid(r)); }
    @PatchMapping("/topics/{id}") public TaxonomyItem topic(Authentication a,@PathVariable UUID id,@Valid @RequestBody TopicRequest b,HttpServletRequest r) { return service.topic(a.getName(),id,b,rid(r)); }
    @GetMapping("/plans") public PageResponse<PlanResponse> plans(@Valid @ParameterObject @ModelAttribute ListQuery q) { return service.plans(q); }
    @GetMapping("/plans/{id}") public PlanResponse plan(@PathVariable UUID id) { return service.plan(id); }
    @PostMapping("/plans") @ResponseStatus(HttpStatus.CREATED) public PlanResponse createPlan(Authentication a,@Valid @RequestBody PlanRequest b,HttpServletRequest r) { return service.createPlan(a.getName(),null,b,rid(r)); }
    @PostMapping("/plans/{id}/versions") @ResponseStatus(HttpStatus.CREATED) public PlanResponse planVersion(Authentication a,@PathVariable UUID id,@Valid @RequestBody PlanRequest b,HttpServletRequest r) { return service.createPlan(a.getName(),id,b,rid(r)); }
    @PatchMapping("/plans/{id}/availability") public PlanResponse availability(Authentication a,@PathVariable UUID id,@Valid @RequestBody ActiveRequest b,HttpServletRequest r) { return service.planActive(a.getName(),id,b,rid(r)); }
    @GetMapping("/scoring-policies") public PageResponse<PolicyResponse> policies(@Valid @ParameterObject @ModelAttribute ListQuery q) { return service.policies(q); }
    @PostMapping("/scoring-policies") @ResponseStatus(HttpStatus.CREATED) public PolicyResponse policy(Authentication a,@Valid @RequestBody PolicyRequest b,HttpServletRequest r) { return service.policy(a.getName(),b,rid(r)); }
    @GetMapping("/blueprints") public PageResponse<BlueprintResponse> blueprints(@Valid @ParameterObject @ModelAttribute ListQuery q) { return service.blueprints(q); }
    @GetMapping("/blueprints/{id}") public BlueprintDetail blueprint(@PathVariable UUID id) { return service.blueprint(id); }
    @PostMapping("/blueprints") @ResponseStatus(HttpStatus.CREATED) public BlueprintResponse createBlueprint(Authentication a,@Valid @RequestBody BlueprintRequest b,HttpServletRequest r) { return service.createBlueprint(a.getName(),b,rid(r)); }
    @PostMapping("/blueprints/{id}/versions") @ResponseStatus(HttpStatus.CREATED) public BlueprintVersion version(Authentication a,@PathVariable UUID id,@Valid @RequestBody BlueprintVersionRequest b,HttpServletRequest r) { return service.version(a.getName(),id,b,rid(r)); }
    @GetMapping("/blueprints/{id}/versions/{versionId}/readiness") public BlueprintVersion readiness(@PathVariable UUID id,@PathVariable UUID versionId) { return service.readiness(id,versionId); }
    @PostMapping("/blueprints/{id}/versions/{versionId}/publish") public BlueprintDetail publish(Authentication a,@PathVariable UUID id,@PathVariable UUID versionId,@Valid @RequestBody PublishRequest b,HttpServletRequest r) { return service.publish(a.getName(),id,versionId,b,rid(r)); }
    @PatchMapping("/blueprints/{id}/status") public BlueprintDetail status(Authentication a,@PathVariable UUID id,@Valid @RequestBody BlueprintStatusRequest b,HttpServletRequest r) { return service.status(a.getName(),id,b,rid(r)); }
}
