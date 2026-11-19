package com.pasti_bisa.backend.modules.exam.application.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import tools.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;

public final class ExamDtos {
    private ExamDtos() {}
    public record StartTryoutRequest(@NotNull UUID tryoutId) {}
    public record StartPracticeRequest(@NotNull UUID topicId, @Min(1) @Max(50) int questionCount) {}
    public record AnswerRequest(UUID selectedOptionId, Boolean flagged, @NotNull @Min(0) Integer expectedRevision) {
        public AnswerRequest { if(flagged==null) flagged=false; }
    }
    public record FinalizeRequest(Boolean abandon) {}
    public record AnswerResponse(int position, UUID selectedOptionId, boolean flagged, int revision, Instant receivedAt) {}
    public record Navigation(int position, boolean answered, boolean flagged, int revision) {}
    public record SessionResponse(UUID attemptId, UUID tryoutId, String mode, String status, String terminalReason,
            Instant serverNow, Instant startedAt, Instant deadlineAt, List<Navigation> questions) {}
    public record SectionResponse(UUID id, UUID subtestId, String name, int position, int questionCount, long available) {}
    public record TryoutDetail(UUID id, String title, String category, int durationSeconds, String scoringMethod,
            BigDecimal scale, boolean demo, boolean ready, List<SectionResponse> sections) {}
    public record SectionResult(UUID subtestId, int correct, int wrong, int empty, BigDecimal points,
            BigDecimal maxPoints, BigDecimal accuracy) {}
    public record ResultResponse(UUID attemptId, String status, String terminalReason, String method, String policyVersion,
            BigDecimal totalScore, @Schema(type = "object", implementation = Object.class) JsonNode details, List<SectionResult> sections) {}

    public record Blueprint(UUID id, UUID blueprintId, UUID examTypeId, int durationSeconds, UUID policyId,
            String method, BigDecimal scale, JsonNode rules, String policyCode, int policyVersion) {}
    public record Section(UUID id, UUID subtestId, int position, int count, UUID topicId, Integer sourceYear) {}
    public record PoolItem(UUID questionId, UUID versionId, boolean shuffleAllowed) {}
    public record Option(UUID id, String code, boolean correct, BigDecimal weight) {}
    public record Attempt(UUID id, UUID userId, UUID blueprintVersionId, UUID blueprintId, String mode, String status,
            String requestHash, String terminalReason, Instant startedAt, Instant deadlineAt) {}
}
