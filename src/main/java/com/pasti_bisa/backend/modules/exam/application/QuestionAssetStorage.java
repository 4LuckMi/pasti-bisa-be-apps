package com.pasti_bisa.backend.modules.exam.application;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import com.pasti_bisa.backend.modules.exam.application.dto.AttemptQuestionDtos.AssetFile;

public interface QuestionAssetStorage {
    record StoredAsset(Path path, String mimeType) {}
    Optional<StoredAsset> resolve(AssetFile file) throws IOException;
}
