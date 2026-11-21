package com.pasti_bisa.backend.modules.exam.adapter.in.web;

import java.util.UUID;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springdoc.core.annotations.ParameterObject;
import com.pasti_bisa.backend.modules.exam.application.StudentExperienceService;
import com.pasti_bisa.backend.modules.exam.application.dto.StudentExperienceDtos.*;
import com.pasti_bisa.backend.shared.web.PageResponse;

@RestController
@RequestMapping("/api/v1")
@PreAuthorize("hasRole('CUSTOMER')")
public class StudentExperienceController {
    private final StudentExperienceService service;
    public StudentExperienceController(StudentExperienceService service){this.service=service;}
    @GetMapping("/users/current/dashboard") public Dashboard dashboard(Authentication a,@Valid @ParameterObject @ModelAttribute DashboardQuery q){return service.dashboard(a.getName(),q);}
    @GetMapping("/users/current/learning-history") public HistoryPage history(Authentication a,@Valid @ParameterObject @ModelAttribute LearningQuery q){return service.history(a.getName(),q);}
    @GetMapping("/users/current/membership") public Membership membership(Authentication a){return service.membership(a.getName());}
    @GetMapping("/plans") public PageResponse<Plan> plans(@Valid @ParameterObject @ModelAttribute Paging q){return service.plans(q);}
    @GetMapping("/plans/{id}") public Plan plan(@PathVariable UUID id){return service.plan(id);}
    @GetMapping("/catalog/tryouts") public PageResponse<TryoutCard> tryouts(Authentication a,@Valid @ParameterObject @ModelAttribute CatalogQuery q){return service.tryouts(a.getName(),q);}
    @GetMapping("/catalog/practice-topics") public PageResponse<TopicCard> topics(@Valid @ParameterObject @ModelAttribute CatalogQuery q){return service.topics(q);}
    @GetMapping("/leaderboards/cohorts") public PageResponse<Cohort> cohorts(@Valid @ParameterObject @ModelAttribute CohortQuery q){return service.cohorts(q);}
    @GetMapping("/leaderboards/cohorts/{id}/entries") public Rankings rankings(Authentication a,@PathVariable UUID id,@Valid @ParameterObject @ModelAttribute RankingQuery q){return service.rankings(a.getName(),id,q);}
}
