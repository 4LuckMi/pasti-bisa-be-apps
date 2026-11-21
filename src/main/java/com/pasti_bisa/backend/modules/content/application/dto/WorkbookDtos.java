package com.pasti_bisa.backend.modules.content.application.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import com.pasti_bisa.backend.modules.content.application.dto.ContentDtos.QuestionContent;

public final class WorkbookDtos {
    private WorkbookDtos(){}
    public record SourceRequest(@NotNull UUID examTypeId,@NotBlank @Pattern(regexp="[A-Z0-9][A-Z0-9_-]{1,79}") String code,
            @Min(1900) @Max(2200) int sourceYear,@NotBlank @Size(max=180) String title,@NotBlank @Size(max=500) String reason){}
    public record SourceStatus(@NotBlank @Pattern(regexp="ACTIVE|ARCHIVED") String status,@NotBlank @Size(max=500) String reason){}
    public record SourceQuery(UUID examTypeId,@Min(1900) @Max(2200) Integer sourceYear,
            @Pattern(regexp="ACTIVE|ARCHIVED") String status,@Size(max=120) String search,@Min(0) Integer offset,@Min(1) @Max(100) Integer limit){
        public SourceQuery{offset=offset==null?0:offset;limit=limit==null?20:limit;}
    }
    public record QuestionSource(UUID id,UUID examTypeId,String code,int sourceYear,String title,String status,long questionCount,long publishedCount){}
    public record LinkSourceQuestion(@NotNull UUID questionId,@NotBlank @Pattern(regexp="[A-Za-z0-9][A-Za-z0-9_.-]{0,79}") String sourceItemCode,@NotBlank @Size(max=500) String reason){}
    public record SourceItem(UUID questionId,UUID subtestId,String sourceItemCode,UUID latestVersionId,String status,String difficulty,boolean archived){}
    public record SheetMapping(@NotBlank @Size(max=31) String sheetName,@NotNull UUID subtestId){}
    public record WorkbookMetadata(@NotNull UUID questionSetId,@NotEmpty @Size(max=30) List<@NotNull @Valid SheetMapping> sheets){}
    public record WorkbookQuery(UUID questionSetId,@Pattern(regexp="QUEUED|READY|NEEDS_INPUT|FAILED") String status,@Min(0) Integer offset,@Min(1) @Max(100) Integer limit){
        public WorkbookQuery{offset=offset==null?0:offset;limit=limit==null?20:limit;}
    }
    public record WorkbookImport(UUID id,UUID questionSetId,UUID fileId,String inputFormat,String status,int candidateCount,
            int validCount,int invalidCount,String errorCode,String jobStatus,Instant createdAt,List<WorkbookSheet> sheets){}
    public record WorkbookSheet(UUID id,String sheetName,UUID subtestId,Integer sheetIndex,String status,int candidateCount,int invalidCount){}
    public record ExcelCandidate(UUID id,UUID sheetId,String sheetName,UUID subtestId,int sourceRow,String sourceItemCode,
            UUID topicId,String difficulty,QuestionContent extractedContent,QuestionContent draftContent,List<String> validationErrors,
            UUID expectedVersionId,UUID resolvedQuestionId,UUID resolvedVersionId){}
    public record CandidateCorrection(@NotBlank @Pattern(regexp="[A-Za-z0-9][A-Za-z0-9_.-]{0,79}") String sourceItemCode,
            UUID topicId,@NotBlank @Pattern(regexp="EASY|MEDIUM|HARD") String difficulty,@NotNull @Valid QuestionContent content){}
    public record SaveWorkbookCandidates(@NotEmpty @Size(max=100) List<@NotNull UUID> candidateIds,@NotBlank @Size(max=500) String reason){}
    public record WorkbookWork(UUID id,String path,UUID sourceId,UUID examTypeId,int year,String title,List<SheetMapping> mappings){}
    public record ParsedSheet(String name,int index,List<ParsedRow> rows){}
    public record ParsedRow(int row,String code,UUID topicId,String difficulty,QuestionContent content,List<String> errors){}
    public record TemplateSheet(String name,UUID subtestId){}
}
