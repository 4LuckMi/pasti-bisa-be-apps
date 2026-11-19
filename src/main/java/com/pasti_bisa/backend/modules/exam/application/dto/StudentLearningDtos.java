package com.pasti_bisa.backend.modules.exam.application.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;
import com.fasterxml.jackson.annotation.JsonProperty;

/** Explicit read contracts. JsonProperty preserves the original API field names. */
public final class StudentLearningDtos {
    private StudentLearningDtos() {}

    public record CategoryQuery(String category) {}

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
            String details) {}

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
