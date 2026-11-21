package com.pasti_bisa.backend.modules.exam.application;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import com.pasti_bisa.backend.modules.exam.application.dto.StudentExperienceDtos.*;
import com.pasti_bisa.backend.shared.web.PageResponse;

public interface StudentExperienceRepository {
    HistoryPage history(String email,LearningQuery query,Instant from,Instant to);
    Dashboard dashboard(String email,LearningQuery query,Instant now);
    Membership membership(String email,Instant now);
    PageResponse<Plan> plans(Paging paging);
    Plan plan(UUID id);
    PageResponse<TryoutCard> tryouts(String email,CatalogQuery query);
    PageResponse<TopicCard> topics(CatalogQuery query);
    PageResponse<Cohort> cohorts(CohortQuery query,Instant now);
    Rankings rankings(String email,UUID cohortId,RankingQuery query,Instant now);
}
