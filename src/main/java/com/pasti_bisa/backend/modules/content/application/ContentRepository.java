package com.pasti_bisa.backend.modules.content.application;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import com.pasti_bisa.backend.modules.content.application.dto.ContentDtos.*;
import com.pasti_bisa.backend.modules.content.domain.ContentModels.ExtractedPage;
import com.pasti_bisa.backend.modules.content.domain.ContentModels.ParsedCandidate;

public interface ContentRepository {
    List<TaxonomyResponse> taxonomy();
    UUID actor(String email);
    void validateScope(UUID subtestId,UUID topicId);
    Optional<UUID> duplicate(UUID uploader,String checksum,ImportMetadata metadata);
    UUID createImport(UUID actor,StoredUpload file,ImportMetadata metadata);
    ImportResponse importById(UUID id);
    List<ImportResponse> imports(int offset,int limit);
    ImportWork work(UUID batchId);
    void extracted(UUID batchId,List<ExtractedPage> pages,List<ParsedCandidate> candidates);
    void extractionFailed(UUID batchId);
    List<CandidateResponse> candidates(UUID batchId,int offset,int limit);
    void correctCandidate(UUID batchId,UUID candidateId,QuestionContent content,List<String> errors);
    QuestionResponse saveCandidate(UUID actor,UUID batchId,UUID candidateId);
    QuestionResponse createQuestion(UUID actor,CreateQuestionRequest request,UUID candidateId,Integer sourcePage);
    QuestionResponse question(UUID questionId);
    List<QuestionResponse> questions(UUID subtestId,String search,int offset,int limit);
    QuestionResponse newVersion(UUID actor,UUID questionId,QuestionContent content);
    QuestionResponse publish(UUID actor,UUID questionId);
    void archive(UUID actor,UUID questionId);
    String sourcePath(UUID batchId);
}
