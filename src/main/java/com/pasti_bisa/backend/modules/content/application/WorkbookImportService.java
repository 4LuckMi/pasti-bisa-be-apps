package com.pasti_bisa.backend.modules.content.application;

import java.io.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import com.pasti_bisa.backend.modules.content.application.dto.WorkbookDtos.*;
import com.pasti_bisa.backend.modules.content.application.dto.ContentDtos.*;
import com.pasti_bisa.backend.modules.operations.application.JobQueue;
import com.pasti_bisa.backend.shared.web.*;

@Service
public class WorkbookImportService {
    private final WorkbookRepository repository;private final ContentRepository content;private final WorkbookParser parser;
    private final ImportFileStorage storage;private final JobQueue jobs;private final TransactionTemplate transactions;
    public WorkbookImportService(WorkbookRepository repository,ContentRepository content,WorkbookParser parser,ImportFileStorage storage,JobQueue jobs,PlatformTransactionManager manager){
        this.repository=repository;this.content=content;this.parser=parser;this.storage=storage;this.jobs=jobs;transactions=new TransactionTemplate(manager);
    }
    @Transactional(readOnly=true) public PageResponse<QuestionSource> sources(SourceQuery q){return repository.sources(q);}
    @Transactional(readOnly=true) public QuestionSource source(UUID id){return repository.source(id);}
    @Transactional public QuestionSource createSource(String email,SourceRequest b){return repository.createSource(content.actor(email),b);}
    @Transactional public QuestionSource sourceStatus(String email,UUID id,SourceStatus b){return repository.status(content.actor(email),id,b);}
    @Transactional public QuestionSource link(String email,UUID id,LinkSourceQuestion b){return repository.link(content.actor(email),id,b);}
    @Transactional(readOnly=true) public PageResponse<SourceItem> sourceItems(UUID id,int offset,int limit){return repository.sourceItems(id,offset,limit);}
    @Transactional(readOnly=true) public byte[] template(UUID id)throws IOException{return parser.template(repository.templateSheets(id));}
    @Transactional(readOnly=true) public List<SheetMapping> templateMapping(UUID id){return repository.templateSheets(id).stream().map(s->new SheetMapping(s.name(),s.subtestId())).toList();}
    @Transactional(readOnly=true) public PageResponse<WorkbookImport> imports(WorkbookQuery q){return repository.imports(q);}
    public WorkbookImport upload(String email,InputStream input,String name,WorkbookMetadata metadata)throws IOException{
        var file=storage.storeWorkbook(input,name);boolean[] reused={false};UUID id;
        String mappingHash=com.pasti_bisa.backend.modules.exam.application.ExamService.hash(metadata.sheets().stream()
            .sorted(Comparator.comparing(SheetMapping::sheetName)).map(s->s.sheetName().length()+":"+s.sheetName()+":"+s.subtestId()).collect(java.util.stream.Collectors.joining("|")));
        try{id=transactions.execute(status->{
            UUID actor=content.actor(email);var duplicate=repository.duplicate(actor,file.sha256(),metadata.questionSetId(),mappingHash);
            if(duplicate.isPresent()){reused[0]=true;return duplicate.get();}
            return repository.createImport(actor,file,metadata,mappingHash);
        });}catch(RuntimeException e){storage.deleteUncommitted(file);throw e;}
        if(reused[0])storage.deleteUncommitted(file);return repository.get(id);
    }
    public void process(JobQueue.Job job)throws IOException{
        UUID id=UUID.fromString(job.payload().get("batchId").asString());var work=repository.work(id);
        try{var parsed=parser.parse(storage.resolve(work.path()),work.mappings());
            transactions.executeWithoutResult(status->{jobs.requireLease(job);repository.extracted(work,parsed);});
        }catch(IOException|RuntimeException e){
            String code=e instanceof BusinessException b?b.code():"WORKBOOK_EXTRACTION_FAILED";
            transactions.executeWithoutResult(status->{jobs.requireLease(job);repository.failed(id,code);});throw e;
        }
    }
    @Transactional(readOnly=true) public WorkbookImport get(UUID id){return repository.get(id);}
    @Transactional(readOnly=true) public PageResponse<ExcelCandidate> candidates(UUID id,UUID sheet,int offset,int limit){return repository.candidates(id,sheet,offset,limit);}
    @Transactional public ExcelCandidate correct(String email,UUID batch,UUID candidate,CandidateCorrection b){return repository.correct(content.actor(email),batch,candidate,b);}
    @Transactional public List<QuestionResponse> save(String email,UUID batch,SaveWorkbookCandidates b){return repository.save(content.actor(email),batch,b);}
}
