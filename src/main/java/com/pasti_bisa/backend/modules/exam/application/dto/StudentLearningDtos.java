package com.pasti_bisa.backend.modules.exam.application.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Pattern;

/** Explicit read contracts. JsonProperty preserves the original API field names. */
public final class StudentLearningDtos {
    private StudentLearningDtos() {}

    public record CategoryQuery(String category) {}

    public record HistoryQuery(@Pattern(regexp = "TRYOUT|PRACTICE") String mode, UUID tryoutId, UUID topicId,
            @Min(0) Integer offset, @Min(1) @Max(100) Integer limit) {
        public HistoryQuery {
            mode = mode == null || mode.isBlank() ? null : mode.trim().toUpperCase(java.util.Locale.ROOT);
            if (offset == null) offset = 0;
            if (limit == null) limit = 100;
        }
    }

    public record PracticeTopicDetail(UUID id, String topic, UUID subtestId, String subtestCode, String subtest,
            UUID examTypeId, String examType, String category, long questionCount, int defaultQuestionCount,
            int maxQuestionCount, int secondsPerQuestion, boolean ready) {}

    public record TryoutResponse(
            UUID id,
            String code,
            String title,
            String category,
            @JsonProperty("exam_type") String examType,
            @JsonProperty("blueprint_version_id") UUID blueprintVersionId,
            @JsonProperty("duration_seconds") int durationSeconds,
            @JsonProperty("question_count") long questionCount,
            @JsonProperty("attempt_count") long attemptCount,
            @JsonProperty("latest_score") BigDecimal latestScore,
            @JsonProperty("latest_completed_at") OffsetDateTime latestCompletedAt) {}

    public record PracticeTopicResponse(
            UUID id,
            String topic,
            @JsonProperty("subtest_code") String subtestCode,
            String subtest,
            String category,
            @JsonProperty("question_count") long questionCount) {}

    public record QuotaResponse(LocalDate dateWib, int used, int limit, int remaining) {}

    public record AttemptHistoryResponse(
            UUID id,
            @JsonProperty("tryout_id") UUID tryoutId,
            @JsonProperty("tryout_title") String tryoutTitle,
            String mode,
            String status,
            @JsonProperty("started_at") OffsetDateTime startedAt,
            @JsonProperty("finalized_at") OffsetDateTime finalizedAt,
            @JsonProperty("total_score") BigDecimal totalScore,
            String details,
            @JsonProperty("topic_id") UUID topicId,
            @JsonProperty("topic_name") String topicName,
            String category,
            @JsonProperty("terminal_reason") String terminalReason,
            @JsonProperty("question_count") int questionCount,
            @JsonProperty("duration_seconds") int durationSeconds) {}

    public record SubscriptionResponse(
            UUID id,
            @JsonProperty("starts_at") OffsetDateTime startsAt,
            @JsonProperty("ends_at") OffsetDateTime endsAt,
            String status,
            boolean active) {}

    public record LeaderboardResponse(
            int rank,
            @JsonProperty("display_name") String displayName,
            @JsonProperty("total_score") BigDecimal totalScore,
            String category,
            @JsonProperty("period_start") OffsetDateTime periodStart,
            @JsonProperty("period_end") OffsetDateTime periodEnd) {}
}
