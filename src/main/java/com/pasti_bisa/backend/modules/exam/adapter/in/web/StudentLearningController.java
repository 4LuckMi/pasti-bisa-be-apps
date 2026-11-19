package com.pasti_bisa.backend.modules.exam.adapter.in.web;

import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springdoc.core.annotations.ParameterObject;
import com.pasti_bisa.backend.modules.exam.application.StudentLearningService;
import com.pasti_bisa.backend.modules.exam.application.dto.StudentLearningDtos.*;

@RestController
@RequestMapping("/api/v1")
public class StudentLearningController {
    private final StudentLearningService learning;

    public StudentLearningController(StudentLearningService learning) {
        this.learning = learning;
    }

    @GetMapping("/tryouts")
    public List<TryoutResponse> tryouts(@ParameterObject @ModelAttribute CategoryQuery query, Authentication auth) {
        return learning.tryouts(auth.getName(), query);
    }

    @GetMapping("/practice/topics")
    public List<PracticeTopicResponse> topics(@ParameterObject @ModelAttribute CategoryQuery query) {
        return learning.topics(query);
    }

    @GetMapping("/users/current/tryout-quota")
    public QuotaResponse quota(Authentication auth) {
        return learning.quota(auth.getName());
    }

    @GetMapping("/users/current/attempts")
    public List<AttemptHistoryResponse> history(Authentication auth) {
        return learning.history(auth.getName());
    }

    @GetMapping("/users/current/subscription")
    public List<SubscriptionResponse> subscriptions(Authentication auth) {
        return learning.subscriptions(auth.getName());
    }

    @GetMapping("/leaderboard")
    public List<LeaderboardResponse> leaderboard(@ParameterObject @ModelAttribute CategoryQuery query) {
        return learning.leaderboard(query);
    }
}
