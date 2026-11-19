package com.pasti_bisa.backend.modules.exam.application;

import java.time.LocalDate;
import java.util.List;
import com.pasti_bisa.backend.modules.exam.application.dto.StudentLearningDtos.*;

public interface StudentLearningRepository {
    List<TryoutResponse> findTryouts(String email, CategoryQuery query);
    List<PracticeTopicResponse> findTopics(CategoryQuery query);
    int findUsedQuota(String email, LocalDate day);
    List<AttemptHistoryResponse> findHistory(String email);
    List<SubscriptionResponse> findSubscriptions(String email);
    List<LeaderboardResponse> findLeaderboard(CategoryQuery query);
}
