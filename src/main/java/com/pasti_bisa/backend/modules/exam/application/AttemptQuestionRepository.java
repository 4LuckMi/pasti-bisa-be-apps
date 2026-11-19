package com.pasti_bisa.backend.modules.exam.application;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import com.pasti_bisa.backend.modules.exam.application.dto.AttemptQuestionDtos.*;

public interface AttemptQuestionRepository {
    QuestionRow findQuestion(QuestionQuery query);
    List<OptionRow> findOptions(UUID questionVersionId);
    Optional<AssetFile> findAsset(QuestionQuery query, UUID assetId);
}
