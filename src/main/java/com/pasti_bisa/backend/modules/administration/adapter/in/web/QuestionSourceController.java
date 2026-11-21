package com.pasti_bisa.backend.modules.administration.adapter.in.web;
import java.util.UUID;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springdoc.core.annotations.ParameterObject;
import com.pasti_bisa.backend.modules.content.application.WorkbookImportService;
import com.pasti_bisa.backend.modules.content.application.dto.WorkbookDtos.*;
import com.pasti_bisa.backend.shared.web.PageResponse;
/** Admin sees source metadata for blueprint composition, never workbook contents or answer keys. */
@RestController
@RequestMapping("/api/v1/admin/question-sets")
@PreAuthorize("hasRole('PLATFORM_ADMIN')")
public class QuestionSourceController {
    private final WorkbookImportService service;
    public QuestionSourceController(WorkbookImportService service){this.service=service;}
    @GetMapping public PageResponse<QuestionSource> list(@Valid @ParameterObject @ModelAttribute SourceQuery q){return service.sources(q);}
    @GetMapping("/{id}") public QuestionSource get(@PathVariable UUID id){return service.source(id);}
}
