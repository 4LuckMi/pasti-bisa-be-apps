package com.pasti_bisa.backend.modules.exam.application.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import jakarta.validation.constraints.*;

/** Camel-case contracts for the current student screens; legacy contracts remain available. */
public final class StudentExperienceDtos {
    private StudentExperienceDtos() {}
    public record DashboardQuery(String category,@Pattern(regexp="TRYOUT|PRACTICE") String mode){
        public DashboardQuery{category=normalize(category);if("ALL".equals(category))category=null;mode=normalize(mode);}
    }
    public record LearningQuery(String category, @Pattern(regexp="TRYOUT|PRACTICE") String mode,
            @Pattern(regexp="IN_PROGRESS|FINALIZING|COMPLETED|ABANDONED|EXPIRED") String status,
            @Size(max=120) String search, UUID tryoutId, UUID topicId,
            LocalDate from, LocalDate to, @Pattern(regexp="ALL|WEEK|MONTH") String period,
            @Min(0) Integer offset, @Min(1) @Max(100) Integer limit) {
        public LearningQuery {
            category=normalize(category); if("ALL".equals(category)) category=null;
            mode=normalize(mode); status=normalize(status); period=normalize(period);
            search=search==null||search.isBlank()?null:search.trim();
            offset=offset==null?0:offset; limit=limit==null?6:limit;
        }
    }
    public record CatalogQuery(String category, @Size(max=120) String search, UUID examTypeId,
            UUID subtestId, Boolean ready, @Pattern(regexp="TITLE|NEWEST") String sort,
            @Min(0) Integer offset, @Min(1) @Max(100) Integer limit) {
        public CatalogQuery { category=normalize(category); if("ALL".equals(category)) category=null;
            search=search==null||search.isBlank()?null:search.trim(); sort=normalize(sort);
            offset=offset==null?0:offset; limit=limit==null?12:limit; }
    }
    public record Paging(@Min(0) Integer offset,@Min(1) @Max(100) Integer limit) {
        public Paging {offset=offset==null?0:offset;limit=limit==null?12:limit;}
    }
    public record CohortQuery(String category, UUID examTypeId, Boolean includeEnded,
            @Min(0) Integer offset,@Min(1) @Max(100) Integer limit) {
        public CohortQuery {category=normalize(category);if("ALL".equals(category))category=null;
            offset=offset==null?0:offset;limit=limit==null?12:limit;}
    }
    public record RankingQuery(@Size(max=180) String targetInstitution,@Size(max=180) String targetProgram,
            @Pattern(regexp="CAMPUS|CIVIL_SERVICE|ACADEMY|SCHOOL") String targetKind,
            @Min(0) Integer offset,@Min(1) @Max(100) Integer limit) {
        public RankingQuery {offset=offset==null?0:offset;limit=limit==null?20:limit;}
        public boolean filtered(){return targetInstitution!=null||targetProgram!=null||targetKind!=null;}
    }
    public record Processing(String state,boolean poll,int retryAfterSeconds,String errorCode) {}
    public record HistoryEntry(UUID attemptId,UUID tryoutId,String title,String category,String mode,String status,
            String terminalReason,UUID topicId,String topicName,Instant startedAt,Instant finalizedAt,
            BigDecimal totalScore,int questionCount,int durationSeconds,Processing processing) {}
    public record HistorySummary(long total,long completed,long inProgress,long scoring,long scoringFailed) {}
    public record HistoryPage(List<HistoryEntry> items,long total,int offset,int limit,HistorySummary summary) {}
    public record ScorePoint(UUID attemptId,Instant completedAt,BigDecimal score) {}
    public record ScoreGroup(UUID examTypeId,String examType,String policyVersion,long count,BigDecimal average,
            List<ScorePoint> trend) {}
    @io.swagger.v3.oas.annotations.media.Schema(name="StudentDashboard")
    public record Dashboard(String category,String mode,long totalTryouts,long totalPractices,long completed,
            long elapsedSeconds,int currentStreakDays,List<ScoreGroup> scoreGroups,List<HistoryEntry> recentActivity,
            StudentLearningDtos.QuotaResponse quota,Membership membership) {}
    public record Benefits(boolean fullLeaderboard) {}
    public record Plan(UUID id,String code,String familyCode,int revision,String name,int months,
            long amountMinor,String currency,Benefits benefits) {}
    public record MembershipPlan(UUID id,String code,String name,int months,long amountMinor,String currency) {}
    public record Membership(String tier,Instant startsAt,Instant endsAt,MembershipPlan plan,
            Benefits benefits,int leaderboardPreviewLimit,int dailyTryoutLimit) {}
    public record TryoutCard(UUID id,String code,String title,String category,UUID examTypeId,String examType,
            UUID blueprintVersionId,int durationSeconds,int questionCount,boolean ready,long attemptCount,
            BigDecimal latestScore,Instant latestCompletedAt) {}
    public record TopicCard(UUID id,String topic,UUID subtestId,String subtestCode,String subtest,
            UUID examTypeId,String examType,String category,long questionCount,boolean ready) {}
    public record Cohort(UUID id,String title,String category,UUID examTypeId,UUID formId,UUID scoringPolicyId,
            Instant periodStart,Instant periodEnd,String attemptPolicy,boolean ended) {}
    public record RankingEntry(int rank,String displayName,BigDecimal totalScore,String targetKind,
            String targetInstitution,String targetProgram,boolean currentUser) {}
    public record Rankings(Cohort cohort,String accessTier,boolean leaderboardOptIn,long totalParticipants,
            long accessibleTotal,int offset,int limit,List<RankingEntry> items,RankingEntry ownRank) {}
    private static String normalize(String value){return value==null||value.isBlank()?null:value.trim().toUpperCase(java.util.Locale.ROOT);}
}
