package com.pasti_bisa.backend.modules.administration.application.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

public final class CatalogManagementDtos {
    private CatalogManagementDtos() {}
    public record CategoryRequest(@NotBlank @Pattern(regexp="[A-Z][A-Z0-9_]{1,39}") String code,@NotBlank @Size(max=120) String name,
            @NotNull Boolean active,@NotBlank @Size(max=500) String reason) {}
    public record ExamTypeRequest(@NotNull UUID categoryId,@NotBlank @Pattern(regexp="[A-Z][A-Z0-9_]{1,39}") String code,
            @NotBlank @Size(max=120) String name,@NotNull Boolean active,@NotBlank @Size(max=500) String reason) {}
    public record SubtestRequest(@NotNull UUID examTypeId,@NotBlank @Pattern(regexp="[A-Z][A-Z0-9_]{1,39}") String code,
            @NotBlank @Size(max=120) String name,@NotNull Boolean active,@NotBlank @Size(max=500) String reason) {}
    public record TopicRequest(@NotNull UUID subtestId,@NotBlank @Size(max=160) String name,@NotNull Boolean active,@NotBlank @Size(max=500) String reason) {}
    public record TaxonomyItem(UUID id,UUID parentId,String code,String name,boolean active) {}
    public record TaxonomyTree(List<TaxonomyItem> categories,List<TaxonomyItem> examTypes,List<TaxonomyItem> subtests,List<TaxonomyItem> topics) {}
    public record PlanBenefits(boolean fullLeaderboard) {}
    public record PlanRequest(@NotBlank @Pattern(regexp="[A-Z][A-Z0-9_]{1,59}") String code,@NotBlank @Size(max=120) String name,
            @Min(1) @Max(36) int months,@Min(0) @Max(1000000000000L) long amountMinor,@NotBlank @Pattern(regexp="[A-Z]{3}") String currency,
            @NotNull @Valid PlanBenefits benefits,@NotBlank @Size(max=500) String reason) {}
    public record PlanResponse(UUID id,String code,String familyCode,int revision,String name,int months,long amountMinor,String currency,PlanBenefits benefits,boolean active) {}
    public record ActiveRequest(@NotNull Boolean active,@NotBlank @Size(max=500) String reason) {}
    public record ScoreRules(@DecimalMin("0.0001") @DecimalMax("1000") BigDecimal correctPoints,
            @DecimalMin("-1000") @DecimalMax("1000") BigDecimal wrongPoints,@DecimalMin("-1000") @DecimalMax("1000") BigDecimal emptyPoints) {}
    public record PolicyRequest(@NotBlank @Pattern(regexp="[A-Z][A-Z0-9_]{1,79}") String code,
            @NotBlank @Pattern(regexp="BINARY|OPTION_WEIGHT") String method,@NotNull @DecimalMin("0.0001") @DecimalMax("1000000") BigDecimal scale,
            @NotNull @Valid ScoreRules rules,@NotBlank @Size(max=500) String reason) {}
    public record PolicyResponse(UUID id,String code,int version,String method,BigDecimal scale,ScoreRules rules,String status) {}
    public record BlueprintRequest(@NotNull UUID examTypeId,@NotBlank @Pattern(regexp="[A-Z][A-Z0-9_-]{1,59}") String code,
            @NotBlank @Size(max=180) String title,@NotBlank @Size(max=500) String reason) {}
    public record SectionRequest(@NotNull UUID subtestId,@Min(1) @Max(500) int questionCount,UUID topicId,@Min(1900) @Max(2200) Integer sourceYear) {}
    public record BlueprintVersionRequest(@Min(60) @Max(86400) int durationSeconds,@NotNull UUID scoringPolicyId,
            @NotEmpty @Size(max=30) List<@NotNull @Valid SectionRequest> sections,@NotBlank @Size(max=500) String reason) {}
    public record PublishRequest(UUID expectedActiveVersionId,@NotBlank @Size(max=500) String reason) {}
    public record BlueprintStatusRequest(@NotBlank @Pattern(regexp="ACTIVE|ARCHIVED") String status,@NotBlank @Size(max=500) String reason) {}
    public record BlueprintResponse(UUID id,UUID examTypeId,String code,String title,String status,UUID activeVersionId) {}
    public record BlueprintSection(UUID id,int position,UUID subtestId,String subtestName,int questionCount,UUID topicId,Integer sourceYear,long available) {}
    public record BlueprintVersion(UUID id,int version,String status,int durationSeconds,UUID scoringPolicyId,List<BlueprintSection> sections,boolean ready) {}
    public record BlueprintDetail(BlueprintResponse blueprint,List<BlueprintVersion> versions) {}
}
