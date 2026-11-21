package com.pasti_bisa.backend.modules.exam.application;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import com.pasti_bisa.backend.modules.exam.application.dto.StudentLearningDtos.*;

public interface StudentLearningRepository {
    List<TryoutResponse> findTryouts(String email, CategoryQuery query);
    List<PracticeTopicResponse> findTopics(CategoryQuery query);
    PracticeTopicDetail findTopic(UUID topicId);
    int findUsedQuota(String email, LocalDate day);
    List<AttemptHistoryResponse> findHistory(String email, HistoryQuery query);
    List<SubscriptionResponse> findSubscriptions(String email);
    List<LeaderboardResponse> findLeaderboard(CategoryQuery query);
}
