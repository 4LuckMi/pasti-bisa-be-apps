package com.pasti_bisa.backend.modules.operations.adapter.in.worker;

import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import com.pasti_bisa.backend.modules.exam.application.ExamService;
import com.pasti_bisa.backend.modules.content.application.ContentImportService;
import com.pasti_bisa.backend.modules.operations.application.JobQueue;

@Configuration
@EnableScheduling
@ConditionalOnProperty(name="pastisiap.worker.enabled",havingValue="true")
public class LearningWorker {
    private static final Logger log=LoggerFactory.getLogger(LearningWorker.class);
    private final JobQueue queue;
    private final ExamService exams;
    private final ContentImportService imports;
    public LearningWorker(JobQueue queue,ExamService exams,ContentImportService imports) { this.queue=queue;this.exams=exams;this.imports=imports; }

    @Scheduled(fixedDelayString="${pastisiap.worker.poll-interval:PT2S}")
    public void tick() {
        try {
            for(UUID id:exams.expired()) {
                try { exams.expire(id); } catch(Exception exception) { log.error("Deadline finalization failed attemptId={}",id,exception); }
            }
            var claim=queue.claim();
            if(claim.isEmpty()) return;
            var job=claim.get();
            MDC.put("requestId","job:"+job.id());
            try {
                switch(job.type()) {
                    case "SCORE_ATTEMPT" -> exams.score(UUID.fromString(job.payload().get("attemptId").asString()));
                    case "IMPORT_PDF" -> imports.process(job);
                    default -> throw new IllegalArgumentException("Unknown persistent job type");
                }
                queue.succeeded(job);
                log.info("Job succeeded jobId={} type={}",job.id(),job.type());
            } catch(Exception exception) {
                queue.failed(job,exception);
                log.error("Job failed jobId={} type={}",job.id(),job.type(),exception);
            } finally { MDC.remove("requestId"); }
        } catch(Exception exception) { log.error("Worker polling failed",exception); }
    }
}
