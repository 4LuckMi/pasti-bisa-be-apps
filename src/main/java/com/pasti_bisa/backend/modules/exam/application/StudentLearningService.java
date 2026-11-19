package com.pasti_bisa.backend.modules.exam.application;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import com.pasti_bisa.backend.modules.exam.application.dto.StudentLearningDtos.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class StudentLearningService {
    private final StudentLearningRepository repository;
    private final Clock clock;

    public StudentLearningService(StudentLearningRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    public List<TryoutResponse> tryouts(String email, CategoryQuery query) { return repository.findTryouts(email, query); }
    public List<PracticeTopicResponse> topics(CategoryQuery query) { return repository.findTopics(query); }
    public List<AttemptHistoryResponse> history(String email) { return repository.findHistory(email); }
    public List<SubscriptionResponse> subscriptions(String email) { return repository.findSubscriptions(email); }
    public List<LeaderboardResponse> leaderboard(CategoryQuery query) { return repository.findLeaderboard(query); }

    public QuotaResponse quota(String email) {
        LocalDate day = LocalDate.now(clock.withZone(ZoneId.of("Asia/Jakarta")));
        int used = repository.findUsedQuota(email, day);
        return new QuotaResponse(day, used, 2, Math.max(0, 2 - used));
    }
}
