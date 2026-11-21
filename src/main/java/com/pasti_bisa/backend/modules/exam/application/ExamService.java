package com.pasti_bisa.backend.modules.exam.application;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.pasti_bisa.backend.modules.exam.application.dto.ExamDtos.*;
import com.pasti_bisa.backend.modules.exam.domain.ScoreCalculator;
import com.pasti_bisa.backend.modules.exam.domain.ScoringModels.ScorePolicy;
import com.pasti_bisa.backend.shared.web.BusinessException;

@Service
public class ExamService {
    private final ExamRepository repository;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();
    public ExamService(ExamRepository repository, Clock clock) { this.repository = repository; this.clock = clock; }

    @Transactional
    public SessionResponse startTryout(String email, String key, StartTryoutRequest request) {
        return start(email,key,"TRYOUT",request.tryoutId(),0);
    }
    @Transactional
    public SessionResponse startPractice(String email,String key,StartPracticeRequest request) {
        return start(email,key,"PRACTICE",request.topicId(),request.questionCount());
    }
    private SessionResponse start(String email,String key,String mode,UUID target,int count) {
        if(key==null || !key.matches("[A-Za-z0-9._:-]{8,128}"))
            throw new BusinessException(400,"IDEMPOTENCY_KEY_REQUIRED","Send an Idempotency-Key with 8 to 128 safe characters");
        UUID userId=repository.lockUser(email);
        String hash=hash(mode+":"+target+":"+count);
        var replay=repository.findByKey(userId,key);
        if(replay.isPresent()) {
            if(!hash.equals(replay.get().requestHash())) throw new BusinessException(409,"IDEMPOTENCY_CONFLICT","Key was used for another request");
            return session(expireIfNeeded(repository.lock(replay.get().id())));
        }
        var active=repository.findActive(userId);
        if(active.isPresent()) return session(expireIfNeeded(repository.lock(active.get().id())));
        Instant now=clock.instant();
        LocalDate day=LocalDate.now(clock.withZone(ZoneId.of("Asia/Jakarta")));
        if(mode.equals("TRYOUT") && repository.usedQuota(userId,day)>=2)
            throw new BusinessException(409,"DAILY_QUOTA_EXHAUSTED","Two tryout starts have been used for the current WIB date");
        Blueprint blueprint=mode.equals("TRYOUT")?repository.blueprint(target):repository.practiceBlueprint(target,count);
        if(!List.of("BINARY","OPTION_WEIGHT").contains(blueprint.method()))
            throw new BusinessException(409,"UNSUPPORTED_SCORING_POLICY","This scoring policy is not supported yet");
        try {
            ScoreCalculator.validatePolicy(new ScorePolicy(blueprint.policyId(),blueprint.method(),blueprint.policyCode(),
                    blueprint.policyVersion(),blueprint.scale(),blueprint.rules()));
        } catch(IllegalArgumentException exception) {
            throw new BusinessException(409,"UNSUPPORTED_SCORING_POLICY","The scoring policy contains unsupported rules");
        }
        List<Section> sections=repository.sections(blueprint.id());
        if(sections.isEmpty()) throw new BusinessException(409,"POOL_NOT_READY","Blueprint has no sections");
        var selected=new ArrayList<PlannedItem>();
        var seen=new HashSet<UUID>();
        for(Section section:sections) {
            List<PoolItem> pool=repository.pool(userId,section,mode.equals("TRYOUT"),day);
            if(pool.size()!=section.count()) throw new BusinessException(409,"POOL_NOT_READY","Not enough published questions in a required section");
            Collections.shuffle(pool,random);
            for(PoolItem item:pool) {
                if(!seen.add(item.questionId())) throw new BusinessException(409,"OVERLAPPING_SECTIONS","Blueprint sections repeat a question");
                List<Option> options=new ArrayList<>(repository.options(item.versionId()));
                if(options.size()<2||options.size()>5||options.stream().filter(Option::correct).count()!=1
                        || options.stream().anyMatch(o->o.weight()==null||o.weight().signum()<0)
                        || (blueprint.method().equals("OPTION_WEIGHT")&&options.stream().noneMatch(o->o.weight().signum()>0)))
                    throw new BusinessException(409,"POOL_NOT_READY","A question has incomplete options or scoring weights");
                if(item.shuffleAllowed()) Collections.shuffle(options,random);
                selected.add(new PlannedItem(section.id(),item,options.stream().map(Option::code).toList()));
            }
        }
        UUID id=UUID.randomUUID();
        repository.insertAttempt(id,userId,blueprint,mode,key,hash,now,now.plusSeconds(blueprint.durationSeconds()));
        int position=1;
        for(PlannedItem item:selected) repository.insertItem(id,item.sectionId(),item.item(),position++,item.optionOrder());
        if(mode.equals("TRYOUT")) repository.consumeQuota(userId,id,day);
        return session(repository.lock(id));
    }
    private record PlannedItem(UUID sectionId,PoolItem item,List<String> optionOrder) {}

    @Transactional
    public SessionResponse get(String email,UUID id) { return session(expireIfNeeded(repository.lockOwned(id,email))); }
    @Transactional
    public SessionResponse active(String email) {
        UUID user=repository.lockUser(email);
        return repository.findActive(user).map(a->session(expireIfNeeded(repository.lock(a.id())))).orElse(null);
    }
    @Transactional
    public AnswerResponse answer(String email,UUID id,int position,AnswerRequest request) {
        Attempt attempt=repository.lockOwned(id,email);
        if(!attempt.status().equals("IN_PROGRESS") || !clock.instant().isBefore(attempt.deadlineAt()))
            throw new BusinessException(409,"ATTEMPT_CLOSED","The attempt is closed or the server deadline has passed");
        return repository.saveAnswer(id,position,request,clock.instant());
    }
    @Transactional
    public SessionResponse finalize(String email,UUID id,boolean abandon) {
        Attempt attempt=repository.lockOwned(id,email);
        if(attempt.status().equals("IN_PROGRESS")) {
            String reason=!clock.instant().isBefore(attempt.deadlineAt())?"TIMED_OUT":abandon?"ABANDONED":"SUBMITTED";
            repository.finalizeAttempt(id,reason,clock.instant());
        }
        return session(repository.lock(id));
    }
    @Transactional
    public void expire(UUID id) { expireIfNeeded(repository.lock(id)); }
    public List<UUID> expired() { return repository.expired(clock.instant()); }
    private Attempt expireIfNeeded(Attempt attempt) {
        if(attempt.status().equals("IN_PROGRESS") && !clock.instant().isBefore(attempt.deadlineAt())) {
            repository.finalizeAttempt(attempt.id(),"TIMED_OUT",clock.instant());
            return repository.lock(attempt.id());
        }
        return attempt;
    }
    @Transactional
    public void score(UUID id) {
        Attempt attempt=repository.lock(id);
        if(attempt.status().equals("COMPLETED")) return;
        if(!attempt.status().equals("FINALIZING")) throw new IllegalStateException("Attempt must be finalized before scoring");
        var policy=repository.policy(id);
        repository.saveScore(attempt,policy,ScoreCalculator.calculate(policy,repository.scoringItems(id)));
    }
    @Transactional
    public ResultResponse result(String email,UUID id) {
        expireIfNeeded(repository.lockOwned(id,email));
        return repository.result(id);
    }
    public TryoutDetail detail(UUID id) { return repository.detail(id); }
    private SessionResponse session(Attempt attempt) {
        return new SessionResponse(attempt.id(),attempt.blueprintId(),attempt.mode(),attempt.status(),attempt.terminalReason(),
                clock.instant(),attempt.startedAt(),attempt.deadlineAt(),repository.navigation(attempt.id()),attempt.finalizedAt(),
                repository.context(attempt.blueprintVersionId(),"PRACTICE".equals(attempt.mode())));
    }
    public static String hash(String value) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch(NoSuchAlgorithmException exception) { throw new IllegalStateException(exception); }
    }
}
