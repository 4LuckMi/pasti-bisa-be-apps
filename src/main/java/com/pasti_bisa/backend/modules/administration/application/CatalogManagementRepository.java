package com.pasti_bisa.backend.modules.administration.application;

import com.pasti_bisa.backend.shared.web.PageResponse;

import java.util.List;
import java.util.UUID;
import com.pasti_bisa.backend.modules.administration.application.dto.AdministrationDtos.*;
import com.pasti_bisa.backend.modules.administration.application.dto.CatalogManagementDtos.*;

public interface CatalogManagementRepository {
    TaxonomyTree taxonomy();
    TaxonomyItem category(UUID id,CategoryRequest body);
    TaxonomyItem examType(UUID id,ExamTypeRequest body);
    TaxonomyItem subtest(UUID id,SubtestRequest body);
    TaxonomyItem topic(UUID id,TopicRequest body);
    PageResponse<PlanResponse> plans(ListQuery query);
    PlanResponse plan(UUID id);
    PlanResponse createPlan(UUID previous,PlanRequest body);
    PlanResponse planActive(UUID id,boolean active);
    PageResponse<PolicyResponse> policies(ListQuery query);
    PolicyResponse createPolicy(PolicyRequest body);
    PageResponse<BlueprintResponse> blueprints(ListQuery query);
    BlueprintDetail blueprint(UUID id);
    BlueprintResponse createBlueprint(BlueprintRequest body);
    BlueprintVersion createVersion(UUID id,BlueprintVersionRequest body);
    BlueprintVersion version(UUID id,UUID versionId);
    BlueprintDetail publish(UUID id,UUID versionId,PublishRequest body);
    BlueprintDetail status(UUID id,String status);
}
