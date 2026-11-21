package com.pasti_bisa.backend.modules.content.application.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import com.pasti_bisa.backend.modules.content.application.dto.ContentDtos.*;

public final class QuestionBankDtos {
    private QuestionBankDtos() {}
    public record BankQuery(@Size(max=180) String search,UUID categoryId,UUID examTypeId,UUID subtestId,UUID topicId,
            @Min(1900) @Max(2200) Integer sourceYear,@Pattern(regexp="DRAFT|PUBLISHED|ARCHIVED") String status,
            UUID batchId,@Min(0) Integer offset,@Min(1) @Max(100) Integer limit) {
        public BankQuery { if(offset==null) offset=0;if(limit==null) limit=10; }
    }
    public record QuestionSummary(UUID id,String code,UUID versionId,int version,String status,long revision,
            String category,String examType,String subtest,String topic,Integer sourceYear,String sourceTitle,String stemPreview,UUID batchId) {}
    public record VersionSummary(UUID id,int version,String status,long revision,UUID createdBy,Instant createdAt) {}
    public record DraftView(QuestionResponse question,long revision) {}
    public record DraftRequest(@NotNull UUID expectedVersionId,@NotNull @Min(0) Long expectedRevision,@NotNull @Valid QuestionContent content,
            @NotBlank @Size(max=500) String reason) {}
    public record ImportQuery(@Size(max=180) String search,UUID subtestId,UUID uploaderId,
            @Pattern(regexp="QUEUED|READY|NEEDS_INPUT|FAILED") String status,@Min(0) Integer offset,@Min(1) @Max(100) Integer limit) {
        public ImportQuery { if(offset==null) offset=0;if(limit==null) limit=10; }
    }
    public record ImportSummary(ImportResponse batch,String originalName,UUID uploaderId) {}
    public record SaveCandidatesRequest(@NotEmpty @Size(max=100) List<@NotNull UUID> candidateIds,@NotBlank @Size(max=500) String reason) {}
    public record StaffDashboard(long publishedQuestions,long draftQuestions,long archivedQuestions,long imports,long importsNeedingInput,long failedImports) {}
    public record ContentAudit(UUID id,UUID actorId,String action,UUID entityId,String reason,String requestId,Instant occurredAt) {}
    public record PoolRequirement(UUID blueprintId,String blueprintTitle,UUID versionId,int version,int position,
            UUID subtestId,String subtestName,int required,long publishedAvailable) {}
}
