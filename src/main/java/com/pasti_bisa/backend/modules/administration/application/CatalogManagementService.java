package com.pasti_bisa.backend.modules.administration.application;

import com.pasti_bisa.backend.shared.web.MutationReason;

import com.pasti_bisa.backend.shared.web.PageResponse;

import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.pasti_bisa.backend.modules.administration.application.dto.AdministrationDtos.*;
import com.pasti_bisa.backend.modules.administration.application.dto.CatalogManagementDtos.*;

@Service
@Transactional(readOnly=true)
public class CatalogManagementService {
    private final CatalogManagementRepository catalog;
    private final AdministrationRepository administration;
    public CatalogManagementService(CatalogManagementRepository catalog,AdministrationRepository administration) { this.catalog=catalog;this.administration=administration; }
    private UUID actor(String email) { return administration.requireAdmin(email); }
    private void audit(UUID actor,String action,String type,UUID id,String reason,String rid,Object after) { administration.recordAudit(actor,action,type,id,reason,rid,null,after); }
    public TaxonomyTree taxonomy() { return catalog.taxonomy(); }
    @Transactional public TaxonomyItem category(String email,UUID id,CategoryRequest body,String rid) { UUID actor=actor(email);var value=catalog.category(id,body);audit(actor,"CATEGORY_SAVED","TAXONOMY",value.id(),body.reason(),rid,value);return value; }
    @Transactional public TaxonomyItem examType(String email,UUID id,ExamTypeRequest body,String rid) { UUID actor=actor(email);var value=catalog.examType(id,body);audit(actor,"EXAM_TYPE_SAVED","TAXONOMY",value.id(),body.reason(),rid,value);return value; }
    @Transactional public TaxonomyItem subtest(String email,UUID id,SubtestRequest body,String rid) { UUID actor=actor(email);var value=catalog.subtest(id,body);audit(actor,"SUBTEST_SAVED","TAXONOMY",value.id(),body.reason(),rid,value);return value; }
    @Transactional public TaxonomyItem topic(String email,UUID id,TopicRequest body,String rid) { UUID actor=actor(email);var value=catalog.topic(id,body);audit(actor,"TOPIC_SAVED","TAXONOMY",value.id(),body.reason(),rid,value);return value; }
    public PageResponse<PlanResponse> plans(ListQuery q) { return catalog.plans(q); }
    public PlanResponse plan(UUID id) { return catalog.plan(id); }
    @Transactional public PlanResponse createPlan(String email,UUID previous,PlanRequest body,String rid) {
        UUID actor=actor(email);var result=catalog.createPlan(previous,body);audit(actor,"PLAN_VERSION_CREATED","PLAN",result.id(),body.reason(),rid,result);return result;
    }
    @Transactional public PlanResponse planActive(String email,UUID id,ActiveRequest body,String rid) {
        UUID actor=actor(email);var result=catalog.planActive(id,body.active());audit(actor,"PLAN_AVAILABILITY_CHANGED","PLAN",id,body.reason(),rid,result);return result;
    }
    public PageResponse<PolicyResponse> policies(ListQuery q) { return catalog.policies(q); }
    @Transactional public PolicyResponse policy(String email,PolicyRequest body,String rid) {
        UUID actor=actor(email);var result=catalog.createPolicy(body);audit(actor,"SCORING_POLICY_VERSION_CREATED","SCORING_POLICY",result.id(),body.reason(),rid,result);return result;
    }
    public PageResponse<BlueprintResponse> blueprints(ListQuery q) { return catalog.blueprints(q); }
    public BlueprintDetail blueprint(UUID id) { return catalog.blueprint(id); }
    @Transactional public BlueprintResponse createBlueprint(String email,BlueprintRequest body,String rid) {
        UUID actor=actor(email);var result=catalog.createBlueprint(body);audit(actor,"BLUEPRINT_CREATED","BLUEPRINT",result.id(),body.reason(),rid,result);return result;
    }
    @Transactional public BlueprintVersion version(String email,UUID id,BlueprintVersionRequest body,String rid) {
        UUID actor=actor(email);var result=catalog.createVersion(id,body);audit(actor,"BLUEPRINT_VERSION_CREATED","BLUEPRINT",id,body.reason(),rid,result);return result;
    }
    public BlueprintVersion readiness(UUID id,UUID versionId) { return catalog.version(id,versionId); }
    @Transactional public BlueprintDetail publish(String email,UUID id,UUID versionId,PublishRequest body,String rid) {
        UUID actor=actor(email);var before=catalog.blueprint(id).blueprint();var result=catalog.publish(id,versionId,body);
        administration.recordAudit(actor,"BLUEPRINT_PUBLISHED","BLUEPRINT",id,body.reason(),rid,before,result.blueprint());return result;
    }
    @Transactional public BlueprintDetail status(String email,UUID id,BlueprintStatusRequest body,String rid) {
        UUID actor=actor(email);var result=catalog.status(id,body.status());audit(actor,"BLUEPRINT_STATUS_CHANGED","BLUEPRINT",id,body.reason(),rid,result.blueprint());return result;
    }
}
