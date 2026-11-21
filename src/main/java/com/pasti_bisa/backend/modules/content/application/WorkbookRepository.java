package com.pasti_bisa.backend.modules.content.application;
import java.util.*;
import com.pasti_bisa.backend.modules.content.application.dto.WorkbookDtos.*;
import com.pasti_bisa.backend.modules.content.application.dto.ContentDtos.*;
import com.pasti_bisa.backend.shared.web.PageResponse;
public interface WorkbookRepository {
    PageResponse<QuestionSource> sources(SourceQuery query);
    QuestionSource source(UUID id);
    QuestionSource createSource(UUID actor,SourceRequest body);
    QuestionSource status(UUID actor,UUID id,SourceStatus body);
    QuestionSource link(UUID actor,UUID id,LinkSourceQuestion body);
    PageResponse<SourceItem> sourceItems(UUID id,int offset,int limit);
    List<TemplateSheet> templateSheets(UUID sourceId);
    UUID createImport(UUID actor,StoredUpload file,WorkbookMetadata metadata,String mappingHash);
    Optional<UUID> duplicate(UUID actor,String hash,UUID sourceId,String mappingHash);
    WorkbookImport get(UUID id);
    PageResponse<WorkbookImport> imports(WorkbookQuery query);
    WorkbookWork work(UUID id);
    void extracted(WorkbookWork work,List<ParsedSheet> sheets);
    void failed(UUID id,String code);
    PageResponse<ExcelCandidate> candidates(UUID id,UUID sheetId,int offset,int limit);
    ExcelCandidate correct(UUID actor,UUID batch,UUID candidate,CandidateCorrection body);
    List<QuestionResponse> save(UUID actor,UUID batch,SaveWorkbookCandidates body);
}
