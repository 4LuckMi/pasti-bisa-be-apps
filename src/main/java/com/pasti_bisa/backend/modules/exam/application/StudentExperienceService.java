package com.pasti_bisa.backend.modules.exam.application;

import java.time.*;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.pasti_bisa.backend.modules.exam.application.dto.StudentExperienceDtos.*;
import com.pasti_bisa.backend.shared.web.PageResponse;
import com.pasti_bisa.backend.shared.web.BusinessException;

@Service
@Transactional(readOnly=true,isolation=org.springframework.transaction.annotation.Isolation.REPEATABLE_READ)
public class StudentExperienceService {
    private static final ZoneId WIB=ZoneId.of("Asia/Jakarta");
    private final StudentExperienceRepository repository; private final Clock clock;
    public StudentExperienceService(StudentExperienceRepository repository,Clock clock){this.repository=repository;this.clock=clock;}
    public HistoryPage history(String email,LearningQuery q){
        if((q.tryoutId()!=null&&q.topicId()!=null)||(q.tryoutId()!=null&&"PRACTICE".equals(q.mode()))
                ||(q.topicId()!=null&&"TRYOUT".equals(q.mode()))) bad("Conflicting attempt scope");
        LocalDate from=q.from(),to=q.to();
        if(q.period()!=null&&!"ALL".equals(q.period())){
            if(from!=null||to!=null)bad("Choose period or date range");
            to=LocalDate.now(clock.withZone(WIB));from="WEEK".equals(q.period())?to.minusDays(6):to.withDayOfMonth(1);
        }
        if(from!=null&&to!=null&&from.isAfter(to))bad("from must be on or before to");
        return repository.history(email,q,from==null?null:from.atStartOfDay(WIB).toInstant(),
                to==null?null:to.plusDays(1).atStartOfDay(WIB).toInstant());
    }
    public Dashboard dashboard(String email,DashboardQuery q){
        return repository.dashboard(email,new LearningQuery(q.category(),q.mode(),null,null,null,null,null,null,null,0,6),clock.instant());
    }
    public Membership membership(String email){return repository.membership(email,clock.instant());}
    public PageResponse<Plan> plans(Paging q){return repository.plans(q);}
    public Plan plan(UUID id){return repository.plan(id);}
    public PageResponse<TryoutCard> tryouts(String email,CatalogQuery q){return repository.tryouts(email,q);}
    public PageResponse<TopicCard> topics(CatalogQuery q){return repository.topics(q);}
    public PageResponse<Cohort> cohorts(CohortQuery q){return repository.cohorts(q,clock.instant());}
    public Rankings rankings(String email,UUID id,RankingQuery q){return repository.rankings(email,id,q,clock.instant());}
    private static void bad(String message){throw new BusinessException(400,"INVALID_FILTER",message);}
}
