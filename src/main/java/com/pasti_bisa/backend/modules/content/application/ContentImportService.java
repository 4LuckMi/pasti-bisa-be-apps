package com.pasti_bisa.backend.modules.content.application;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import com.pasti_bisa.backend.modules.content.application.dto.ContentDtos.*;
import com.pasti_bisa.backend.modules.content.domain.QuestionValidator;
import com.pasti_bisa.backend.modules.content.domain.TemplateQuestionParser;
import com.pasti_bisa.backend.modules.operations.application.JobQueue;

@Service
public class ContentImportService {
    private final ContentRepository repository;
    private final ImportFileStorage storage;
    private final PdfExtractor extractor;
    private final JobQueue jobs;
    private final TransactionTemplate transactions;
    public ContentImportService(ContentRepository repository,ImportFileStorage storage,PdfExtractor extractor,JobQueue jobs,PlatformTransactionManager manager) {
        this.repository=repository;this.storage=storage;this.extractor=extractor;this.jobs=jobs;this.transactions=new TransactionTemplate(manager);
    }
    public ImportResponse upload(String email,InputStream input,String originalName,ImportMetadata metadata) throws IOException {
        StoredUpload file=storage.store(input,originalName);
        UUID batch;
        boolean[] reused={false};
        try {
            batch=transactions.execute(status->{
                UUID actor=repository.actor(email);
                repository.validateScope(metadata.subtestId(),metadata.topicId());
                var old=repository.duplicate(actor,file.sha256(),metadata);
                if(old.isPresent()) { reused[0]=true;return old.get(); }
                return repository.createImport(actor,file,metadata);
            });
        } catch(RuntimeException exception) { storage.deleteUncommitted(file);throw exception; }
        if(reused[0]) storage.deleteUncommitted(file);
        return repository.importById(batch);
    }
    public void process(JobQueue.Job job) throws IOException {
        UUID id=UUID.fromString(job.payload().get("batchId").asString());
        ImportWork work=repository.work(id);
        try {
            var pages=extractor.extract(storage.resolve(work.path()));
            var parsed=TemplateQuestionParser.parse(pages);
            transactions.executeWithoutResult(status->{jobs.requireLease(job);repository.extracted(id,pages,parsed);});
        } catch(IOException|RuntimeException exception) {
            transactions.executeWithoutResult(status->{jobs.requireLease(job);repository.extractionFailed(id);});
            throw exception;
        }
    }
    public ImportResponse get(UUID id) { return repository.importById(id); }
    public List<TaxonomyResponse> taxonomy() { return repository.taxonomy(); }
    public List<ImportResponse> list(int offset,int limit) { return repository.imports(offset,limit); }
    public List<CandidateResponse> candidates(UUID id,int offset,int limit) { return repository.candidates(id,offset,limit); }
    @Transactional public void correct(String email,UUID id,UUID candidateId,QuestionContent content) {
        repository.correctCandidate(id,candidateId,content,QuestionValidator.publicationErrors(content.toDomain()));
        repository.auditContent(repository.actor(email),"IMPORT_CANDIDATE_CORRECTED",candidateId);
    }
    @Transactional public QuestionResponse saveCandidate(String email,UUID batchId,UUID candidateId) {
        return repository.saveCandidate(repository.actor(email),batchId,candidateId);
    }
    @Transactional public QuestionResponse create(String email,CreateQuestionRequest request) {
        return repository.createQuestion(repository.actor(email),request,null,null);
    }
    public QuestionResponse question(UUID id) { return repository.question(id); }
    public List<QuestionResponse> questions(UUID subtestId,String search,int offset,int limit) { return repository.questions(subtestId,search,offset,limit); }
    @Transactional public QuestionResponse newVersion(String email,UUID id,QuestionContent content) {
        return repository.newVersion(repository.actor(email),id,content);
    }
    @Transactional public QuestionResponse publish(String email,UUID id) { return repository.publish(repository.actor(email),id); }
    @Transactional public void archive(String email,UUID id) { repository.archive(repository.actor(email),id); }
    public Path source(UUID batchId) throws IOException { return storage.resolve(repository.sourcePath(batchId)); }
}
