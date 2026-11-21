package com.pasti_bisa.backend.modules.exam.application;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import com.pasti_bisa.backend.shared.web.BusinessException;
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
    public PracticeTopicDetail topic(UUID id) { return repository.findTopic(id); }
    public List<AttemptHistoryResponse> history(String email, HistoryQuery query) {
        if ((query.tryoutId()!=null && query.topicId()!=null)
                || (query.tryoutId()!=null && "PRACTICE".equals(query.mode()))
                || (query.topicId()!=null && "TRYOUT".equals(query.mode())))
            throw new BusinessException(400,"INVALID_HISTORY_FILTER","Choose a tryout or practice topic with the matching mode");
        return repository.findHistory(email,query);
    }
    public List<SubscriptionResponse> subscriptions(String email) { return repository.findSubscriptions(email); }
    public List<LeaderboardResponse> leaderboard(String email,CategoryQuery query) { return repository.findLeaderboard(email,query); }

    public QuotaResponse quota(String email) {
        LocalDate day = LocalDate.now(clock.withZone(ZoneId.of("Asia/Jakarta")));
        int used = repository.findUsedQuota(email, day);
        return new QuotaResponse(day, used, 2, Math.max(0, 2 - used));
    }
}
