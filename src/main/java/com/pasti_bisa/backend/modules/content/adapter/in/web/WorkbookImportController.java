package com.pasti_bisa.backend.modules.content.adapter.in.web;

import java.io.IOException;
import java.util.*;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springdoc.core.annotations.ParameterObject;
import com.pasti_bisa.backend.modules.content.application.WorkbookImportService;
import com.pasti_bisa.backend.modules.content.application.dto.WorkbookDtos.*;
import com.pasti_bisa.backend.modules.content.application.dto.ContentDtos.QuestionResponse;
import com.pasti_bisa.backend.shared.web.*;

@RestController
@RequestMapping("/api/v1/staff")
@PreAuthorize("hasRole('QUESTION_STAFF')")
public class WorkbookImportController {
    public static final String XLSX="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    private final WorkbookImportService service;
    public WorkbookImportController(WorkbookImportService service){this.service=service;}
    @GetMapping("/question-sets") public PageResponse<QuestionSource> sources(@Valid @ParameterObject @ModelAttribute SourceQuery q){return service.sources(q);}
    @PostMapping("/question-sets") @ResponseStatus(HttpStatus.CREATED) public QuestionSource create(Authentication a,@Valid @RequestBody SourceRequest b){return service.createSource(a.getName(),b);}
    @GetMapping("/question-sets/{id}") public QuestionSource source(@PathVariable UUID id){return service.source(id);}
    @GetMapping("/question-sets/{id}/template-mapping") public List<SheetMapping> templateMapping(@PathVariable UUID id){return service.templateMapping(id);}
    @PatchMapping("/question-sets/{id}/status") public QuestionSource status(Authentication a,@PathVariable UUID id,@Valid @RequestBody SourceStatus b){return service.sourceStatus(a.getName(),id,b);}
    @PostMapping("/question-sets/{id}/items") public QuestionSource link(Authentication a,@PathVariable UUID id,@Valid @RequestBody LinkSourceQuestion b){return service.link(a.getName(),id,b);}
    @GetMapping("/question-sets/{id}/items") public PageResponse<SourceItem> items(@PathVariable UUID id,@RequestParam(defaultValue="0") int offset,@RequestParam(defaultValue="20") int limit){
        if(offset<0||limit<1||limit>100)throw new BusinessException(400,"INVALID_FILTER","offset must be nonnegative and limit must be 1 to 100");return service.sourceItems(id,offset,limit);
    }
    @GetMapping(value="/question-sets/{id}/template",produces=XLSX) public ResponseEntity<byte[]> template(@PathVariable UUID id)throws IOException{
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(XLSX)).cacheControl(CacheControl.noStore()).header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=question-template.xlsx").body(service.template(id));
    }
    @PostMapping(value="/workbook-imports",consumes=MediaType.MULTIPART_FORM_DATA_VALUE) @ResponseStatus(HttpStatus.ACCEPTED)
    public WorkbookImport upload(Authentication a,@RequestPart("file") MultipartFile file,@Valid @RequestPart("metadata") WorkbookMetadata metadata)throws IOException{
        if(file.getContentType()!=null&&!List.of(XLSX,"application/octet-stream","application/zip").contains(file.getContentType()))
            throw new BusinessException(400,"XLSX_REQUIRED","Upload an XLSX workbook");
        return service.upload(a.getName(),file.getInputStream(),file.getOriginalFilename(),metadata);
    }
    @GetMapping("/workbook-imports/{id}") public WorkbookImport get(@PathVariable UUID id){return service.get(id);}
    @GetMapping("/workbook-imports") public PageResponse<WorkbookImport> list(@Valid @ParameterObject @ModelAttribute WorkbookQuery q){return service.imports(q);}
    @GetMapping("/workbook-imports/{id}/candidates") public PageResponse<ExcelCandidate> candidates(@PathVariable UUID id,@RequestParam(required=false) UUID sheetId,
            @RequestParam(defaultValue="0") int offset,@RequestParam(defaultValue="20") int limit){
        if(offset<0||limit<1||limit>100)throw new BusinessException(400,"INVALID_FILTER","offset must be nonnegative and limit must be 1 to 100");
        return service.candidates(id,sheetId,offset,limit);
    }
    @PatchMapping("/workbook-imports/{id}/candidates/{candidateId}") public ExcelCandidate correct(Authentication a,@PathVariable UUID id,@PathVariable UUID candidateId,@Valid @RequestBody CandidateCorrection b){return service.correct(a.getName(),id,candidateId,b);}
    @PostMapping("/workbook-imports/{id}/save") public List<QuestionResponse> save(Authentication a,@PathVariable UUID id,@Valid @RequestBody SaveWorkbookCandidates b){return service.save(a.getName(),id,b);}
}
