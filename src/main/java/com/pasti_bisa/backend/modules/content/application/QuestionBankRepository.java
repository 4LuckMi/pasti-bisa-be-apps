package com.pasti_bisa.backend.modules.content.application;

import com.pasti_bisa.backend.shared.web.MutationReason;

import java.util.List;
import java.util.UUID;
import com.pasti_bisa.backend.shared.web.PageResponse;
import com.pasti_bisa.backend.modules.content.application.dto.QuestionBankDtos.*;
import com.pasti_bisa.backend.modules.content.application.dto.ContentDtos.*;

public interface QuestionBankRepository {
    PageResponse<QuestionSummary> search(BankQuery query);
    List<VersionSummary> versions(UUID questionId);
    DraftView draft(UUID questionId);
    DraftView update(UUID actor,UUID questionId,DraftRequest request);
    PageResponse<ImportSummary> imports(ImportQuery query);
    ImportResponse retry(UUID actor,UUID batchId);
    StaffDashboard dashboard();
    PageResponse<ContentAudit> audit(int offset,int limit);
    PageResponse<PoolRequirement> requirements(int offset,int limit);
    void audit(UUID actor,String action,UUID id,String reason,String requestId);
}
