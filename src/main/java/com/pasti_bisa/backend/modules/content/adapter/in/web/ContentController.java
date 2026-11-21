package com.pasti_bisa.backend.modules.content.adapter.in.web;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import jakarta.validation.Valid;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.pasti_bisa.backend.modules.content.application.ContentImportService;
import com.pasti_bisa.backend.modules.content.application.dto.ContentDtos.*;
import com.pasti_bisa.backend.shared.web.BusinessException;

@RestController
@RequestMapping("/api/v1/staff")
@PreAuthorize("hasRole('QUESTION_STAFF')")
public class ContentController {
    private final ContentImportService content;

    public ContentController(ContentImportService content) {
        this.content = content;
    }

    private int limit(int limit) {
        if (limit < 1 || limit > 100)
            throw new IllegalArgumentException("limit must be 1 to 100");
        return limit;
    }

    private int offset(int offset) {
        if (offset < 0)
            throw new IllegalArgumentException("offset cannot be negative");
        return offset;
    }

    @GetMapping("/taxonomy")
    public List<TaxonomyResponse> taxonomy() {
        return content.taxonomy();
    }

    @PostMapping(value = "/imports", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ImportResponse upload(Authentication auth, @RequestPart("file") MultipartFile file,
            @Valid @RequestPart("metadata") ImportMetadata metadata) throws IOException {
        if (file.getContentType() != null
                && !List.of("application/pdf", "application/octet-stream").contains(file.getContentType()))
            throw new BusinessException(400, "PDF_REQUIRED", "Upload a PDF document");
        return content.upload(auth.getName(), file.getInputStream(), file.getOriginalFilename(), metadata);
    }

    @GetMapping("/imports")
    public List<ImportResponse> imports(@RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "20") int limit) {
        return content.list(offset(offset), limit(limit));
    }

    @GetMapping("/imports/{id}")
    public ImportResponse get(@PathVariable UUID id) {
        return content.get(id);
    }

    @GetMapping("/imports/{id}/candidates")
    public List<CandidateResponse> candidates(@PathVariable UUID id,
            @RequestParam(defaultValue = "0") int offset, @RequestParam(defaultValue = "20") int limit) {
        return content.candidates(id, offset(offset), limit(limit));
    }

    @PatchMapping("/imports/{id}/candidates/{candidateId}")
    public void correct(Authentication auth,@PathVariable UUID id, @PathVariable UUID candidateId,
            @Valid @RequestBody QuestionContent body) {
        content.correct(auth.getName(),id, candidateId, body);
    }

    @PostMapping("/imports/{id}/candidates/{candidateId}/save")
    public QuestionResponse save(Authentication auth, @PathVariable UUID id, @PathVariable UUID candidateId) {
        return content.saveCandidate(auth.getName(), id, candidateId);
    }

    @GetMapping("/imports/{id}/source")
    public ResponseEntity<FileSystemResource> source(@PathVariable UUID id) throws IOException {
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF).cacheControl(CacheControl.noStore())
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=source.pdf")
                .body(new FileSystemResource(content.source(id)));
    }

    @PostMapping("/questions")
    @ResponseStatus(HttpStatus.CREATED)
    public QuestionResponse create(Authentication auth, @Valid @RequestBody CreateQuestionRequest body) {
        return content.create(auth.getName(), body);
    }

    @GetMapping("/questions")
    public List<QuestionResponse> questions(@RequestParam(required = false) UUID subtestId,
            @RequestParam(required = false) String search, @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "20") int limit) {
        return content.questions(subtestId, search, offset(offset), limit(limit));
    }

    @GetMapping("/questions/{id}")
    public QuestionResponse question(@PathVariable UUID id) {
        return content.question(id);
    }

    @PostMapping("/questions/{id}/versions")
    public QuestionResponse version(Authentication auth, @PathVariable UUID id,
            @Valid @RequestBody QuestionContent body) {
        return content.newVersion(auth.getName(), id, body);
    }

    @PostMapping("/questions/{id}/publish")
    public QuestionResponse publish(Authentication auth, @PathVariable UUID id) {
        return content.publish(auth.getName(), id);
    }

    @PostMapping("/questions/{id}/archive")
    public void archive(Authentication auth, @PathVariable UUID id) {
        content.archive(auth.getName(), id);
    }
}
