package com.pasti_bisa.backend.modules.content.application.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;

public final class ContentDtos {
    private ContentDtos() {}
    public record TaxonomyResponse(UUID categoryId,String categoryCode,String categoryName,UUID examTypeId,
            String examTypeCode,String examTypeName,UUID subtestId,String subtestName,UUID topicId,String topicName) {}
    public record ImportMetadata(@NotNull UUID subtestId,UUID topicId,@NotBlank @Size(max=180) String sourceTitle,
            @Min(1900) @Max(2200) Integer sourceYear) {}
    public record OptionDraft(@NotBlank @Pattern(regexp="[A-E]") String code,@NotBlank @Size(max=10000) String text,
            boolean correct,@NotNull @DecimalMin("0") @DecimalMax("1000") BigDecimal weight) {}
    public record QuestionContent(@NotBlank @Size(max=20000) String stemText,@NotNull @Size(min=2,max=5) List<@NotNull @Valid OptionDraft> options,
            @Size(max=20000) String explanation,boolean shuffleAllowed) {
        public com.pasti_bisa.backend.modules.content.domain.ContentModels.Question toDomain() {
            return new com.pasti_bisa.backend.modules.content.domain.ContentModels.Question(stemText,
                    options==null?null:options.stream().map(o->new com.pasti_bisa.backend.modules.content.domain.ContentModels.Option(
                        o.code(),o.text(),o.correct(),o.weight())).toList(),explanation,shuffleAllowed);
        }
    }
    public record CreateQuestionRequest(@NotNull UUID subtestId,UUID topicId,@NotBlank @Size(max=180) String sourceTitle,
            @Min(1900) @Max(2200) Integer sourceYear,@NotNull @Valid QuestionContent content) {}
    public record CandidateResponse(UUID id,int sourcePage,int sourceItemNo,QuestionContent extractedContent,
            QuestionContent draftContent,List<String> validationErrors,UUID questionId) {}
    public record ImportResponse(UUID id,String status,UUID fileId,UUID subtestId,UUID topicId,String sourceTitle,
            Integer sourceYear,Integer pageCount,int candidateCount,int validCount,int invalidCount,String errorCode,
            String jobStatus,String jobErrorCode,Instant createdAt) {}
    @Schema(name = "StaffQuestionResponse")
    public record QuestionResponse(UUID id,UUID versionId,String code,int version,String status,UUID subtestId,
            UUID topicId,Integer sourceYear,String sourceTitle,Integer sourcePage,QuestionContent content) {}
    public record StoredUpload(UUID id,String path,String originalName,String sha256,long size) {}
    public record ImportWork(UUID id,String path,UUID subtestId,UUID topicId) {}
}
