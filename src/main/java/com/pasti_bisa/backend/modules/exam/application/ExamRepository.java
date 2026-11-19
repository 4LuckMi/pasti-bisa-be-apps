package com.pasti_bisa.backend.modules.exam.application;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import com.pasti_bisa.backend.modules.exam.application.dto.ExamDtos.*;
import com.pasti_bisa.backend.modules.exam.domain.ScoreCalculator.Calculation;
import com.pasti_bisa.backend.modules.exam.domain.ScoringModels.*;

public interface ExamRepository {
    UUID lockUser(String email);
    Optional<Attempt> findByKey(UUID userId, String key);
    Optional<Attempt> findActive(UUID userId);
    Attempt lockOwned(UUID id, String email);
    Attempt lock(UUID id);
    Blueprint blueprint(UUID tryoutId);
    Blueprint practiceBlueprint(UUID topicId, int count);
    List<Section> sections(UUID versionId);
    List<PoolItem> pool(UUID userId, Section section, boolean excludeToday, LocalDate day);
    List<Option> options(UUID versionId);
    int usedQuota(UUID userId, LocalDate day);
    void insertAttempt(UUID id, UUID userId, Blueprint blueprint, String mode, String key, String hash, Instant now, Instant deadline);
    void insertItem(UUID attemptId, UUID sectionId, PoolItem item, int position, List<String> optionOrder);
    void consumeQuota(UUID userId, UUID attemptId, LocalDate day);
    List<Navigation> navigation(UUID attemptId);
    AnswerResponse saveAnswer(UUID attemptId, int position, AnswerRequest answer, Instant now);
    void finalizeAttempt(UUID id, String reason, Instant now);
    List<UUID> expired(Instant now);
    ScorePolicy policy(UUID attemptId);
    List<ScoringItem> scoringItems(UUID attemptId);
    void saveScore(Attempt attempt, ScorePolicy policy, Calculation calculation);
    ResultResponse result(UUID attemptId);
    TryoutDetail detail(UUID tryoutId);
}
