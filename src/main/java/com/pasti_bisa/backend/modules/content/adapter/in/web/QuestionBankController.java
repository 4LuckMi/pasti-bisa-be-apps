package com.pasti_bisa.backend.modules.content.adapter.in.web;

import com.pasti_bisa.backend.shared.web.MutationReason;

import com.pasti_bisa.backend.shared.web.PageResponse;

import java.util.List;
import java.util.UUID;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.pasti_bisa.backend.modules.content.application.QuestionBankService;
import com.pasti_bisa.backend.modules.content.application.dto.ContentDtos.*;
import com.pasti_bisa.backend.modules.content.application.dto.QuestionBankDtos.*;
import com.pasti_bisa.backend.shared.web.ApiRequestLoggingFilter;

@RestController
@RequestMapping("/api/v1/staff")
@PreAuthorize("hasRole('QUESTION_STAFF')")
public class QuestionBankController {
    private final QuestionBankService service;
    public QuestionBankController(QuestionBankService service) { this.service=service; }
    private String rid(HttpServletRequest r) { return ApiRequestLoggingFilter.requestId(r); }
    @GetMapping("/dashboard") public StaffDashboard dashboard() { return service.dashboard(); }
    @GetMapping("/question-bank") public PageResponse<QuestionSummary> search(@Valid @ParameterObject @ModelAttribute BankQuery q) { return service.search(q); }
    @GetMapping("/questions/{id}/versions") public List<VersionSummary> versions(@PathVariable UUID id) { return service.versions(id); }
    @GetMapping("/questions/{id}/versions/{versionId}") public QuestionResponse version(@PathVariable UUID id,@PathVariable UUID versionId) { return service.version(id,versionId); }
    @GetMapping("/questions/{id}/draft") public DraftView draft(@PathVariable UUID id) { return service.draft(id); }
    @PutMapping("/questions/{id}/draft") public DraftView update(Authentication a,@PathVariable UUID id,@Valid @RequestBody DraftRequest b,HttpServletRequest r) { return service.update(a.getName(),id,b,rid(r)); }
    @GetMapping("/import-history") public PageResponse<ImportSummary> imports(@Valid @ParameterObject @ModelAttribute ImportQuery q) { return service.imports(q); }
    @PostMapping("/imports/{id}/retry") public ImportResponse retry(Authentication a,@PathVariable UUID id,@Valid @RequestBody MutationReason b,HttpServletRequest r) { return service.retry(a.getName(),id,b.reason(),rid(r)); }
    @PostMapping("/imports/{id}/save") public List<QuestionResponse> save(Authentication a,@PathVariable UUID id,@Valid @RequestBody SaveCandidatesRequest b,HttpServletRequest r) { return service.save(a.getName(),id,b,rid(r)); }
    @GetMapping("/audit") public PageResponse<ContentAudit> audit(@RequestParam(defaultValue="0") @Min(0) int offset,@RequestParam(defaultValue="10") @Min(1) @Max(100) int limit) { return service.audit(offset,limit); }
    @GetMapping("/pool-requirements") public PageResponse<PoolRequirement> requirements(@RequestParam(defaultValue="0") @Min(0) int offset,@RequestParam(defaultValue="10") @Min(1) @Max(100) int limit) { return service.requirements(offset,limit); }
}
