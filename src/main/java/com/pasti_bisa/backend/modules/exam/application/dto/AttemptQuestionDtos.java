package com.pasti_bisa.backend.modules.exam.application.dto;

import java.util.List;
import java.util.UUID;
import io.swagger.v3.oas.annotations.media.Schema;
import tools.jackson.databind.JsonNode;

public final class AttemptQuestionDtos {
    private AttemptQuestionDtos() {}

    public record QuestionQuery(UUID attemptId, int position, String email) {}

    public record OptionResponse(String id, String code,
            @Schema(type = "object", implementation = Object.class) JsonNode content, Boolean correct) {}
    public record QuestionScope(UUID subtestId,String subtestName,UUID topicId,String topicName) {}

    @Schema(name = "AttemptQuestionResponse")
    public record QuestionResponse(
            int position,
            @Schema(type = "object", implementation = Object.class) JsonNode stem,
            List<OptionResponse> options,
            String selectedOptionId,
            boolean flagged,
            int revision,
            Boolean correct,
            String explanation,
            QuestionScope scope) {}

    // Database projections stay internal; services control which fields reach the response.
    public record QuestionRow(
            UUID versionId,
            int position,
            JsonNode stem,
            JsonNode optionOrder,
            UUID selectedOptionId,
            boolean flagged,
            int revision,
            boolean finalized,
            String explanation,
            QuestionScope scope) {}

    public record OptionRow(UUID id, String code, JsonNode content, boolean correct) {}
    public record AssetFile(String relativePath, String mimeType) {}
}
