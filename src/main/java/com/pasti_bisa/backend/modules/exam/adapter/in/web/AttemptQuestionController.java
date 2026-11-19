package com.pasti_bisa.backend.modules.exam.adapter.in.web;

import java.io.IOException;
import java.util.UUID;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.pasti_bisa.backend.modules.exam.application.AttemptQuestionService;
import com.pasti_bisa.backend.modules.exam.application.QuestionAssetService;
import com.pasti_bisa.backend.modules.exam.application.dto.AttemptQuestionDtos.QuestionQuery;
import com.pasti_bisa.backend.modules.exam.application.dto.AttemptQuestionDtos.QuestionResponse;

@RestController
@RequestMapping("/api/v1/users/current/attempts/{attemptId}/questions")
public class AttemptQuestionController {
    private final AttemptQuestionService questions;
    private final QuestionAssetService assets;

    public AttemptQuestionController(AttemptQuestionService questions, QuestionAssetService assets) {
        this.questions = questions;
        this.assets = assets;
    }

    @GetMapping("/{position}")
    public QuestionResponse question(@PathVariable UUID attemptId, @PathVariable int position, Authentication auth) {
        return questions.question(new QuestionQuery(attemptId, position, auth.getName()));
    }

    @GetMapping("/{position}/assets/{assetId}")
    public ResponseEntity<FileSystemResource> readAsset(@PathVariable UUID attemptId, @PathVariable int position,
            @PathVariable UUID assetId, Authentication auth) throws IOException {
        var file = assets.find(new QuestionQuery(attemptId, position, auth.getName()), assetId);
        if (file.isEmpty())
            return ResponseEntity.notFound().build();
        var asset = file.get();
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(asset.mimeType()))
                .cacheControl(CacheControl.noStore())
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline().filename(asset.path().getFileName().toString()).build().toString())
                .body(new FileSystemResource(asset.path()));
    }
}
