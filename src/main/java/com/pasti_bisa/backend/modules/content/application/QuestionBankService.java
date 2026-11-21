package com.pasti_bisa.backend.modules.content.application;

import com.pasti_bisa.backend.shared.web.MutationReason;

import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.pasti_bisa.backend.shared.web.PageResponse;
import com.pasti_bisa.backend.modules.content.application.dto.QuestionBankDtos.*;
import com.pasti_bisa.backend.modules.content.application.dto.ContentDtos.*;

@Service
@Transactional(readOnly=true)
public class QuestionBankService {
    private final QuestionBankRepository bank;
    private final ContentRepository content;
    public QuestionBankService(QuestionBankRepository bank,ContentRepository content) { this.bank=bank;this.content=content; }
    public PageResponse<QuestionSummary> search(BankQuery q) { return bank.search(q); }
    public List<VersionSummary> versions(UUID id) { content.question(id);return bank.versions(id); }
    public QuestionResponse version(UUID id,UUID versionId) { return content.questionVersion(id,versionId); }
    public DraftView draft(UUID id) { return bank.draft(id); }
    @Transactional public DraftView update(String email,UUID id,DraftRequest body,String requestId) {
        UUID actor=content.actor(email);var result=bank.update(actor,id,body);bank.audit(actor,"QUESTION_DRAFT_UPDATED",id,body.reason(),requestId);return result;
    }
    public PageResponse<ImportSummary> imports(ImportQuery q) { return bank.imports(q); }
    @Transactional public ImportResponse retry(String email,UUID id,String reason,String requestId) {
        UUID actor=content.actor(email);var result=bank.retry(actor,id);bank.audit(actor,"IMPORT_RETRIED",id,reason,requestId);return result;
    }
    @Transactional public List<QuestionResponse> save(String email,UUID id,SaveCandidatesRequest body,String requestId) {
        UUID actor=content.actor(email);
        var values=body.candidateIds().stream().distinct().sorted().map(candidate->content.saveCandidate(actor,id,candidate)).toList();
        bank.audit(actor,"IMPORT_CANDIDATES_SAVED",id,body.reason(),requestId);return values;
    }
    public StaffDashboard dashboard() { return bank.dashboard(); }
    public PageResponse<ContentAudit> audit(int offset,int limit) { return bank.audit(offset,limit); }
    public PageResponse<PoolRequirement> requirements(int offset,int limit) { return bank.requirements(offset,limit); }
}
