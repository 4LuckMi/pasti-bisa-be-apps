package com.pasti_bisa.backend.modules.exam.application;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import com.pasti_bisa.backend.modules.exam.application.dto.AttemptQuestionDtos.QuestionQuery;
import com.pasti_bisa.backend.modules.exam.application.QuestionAssetStorage.StoredAsset;

@Service
public class QuestionAssetService {
    private final AttemptQuestionRepository repository;
    private final QuestionAssetStorage storage;
    public QuestionAssetService(AttemptQuestionRepository repository, QuestionAssetStorage storage) {
        this.repository = repository;
        this.storage = storage;
    }

    public Optional<StoredAsset> find(QuestionQuery query, UUID assetId) throws IOException {
        var file = repository.findAsset(query, assetId);
        return file.isEmpty() ? Optional.empty() : storage.resolve(file.get());
    }
}
