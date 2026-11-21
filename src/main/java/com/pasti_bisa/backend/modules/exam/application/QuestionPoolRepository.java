package com.pasti_bisa.backend.modules.exam.application;
import java.time.LocalDate;
import java.util.*;
import com.pasti_bisa.backend.modules.exam.application.dto.ExamDtos.*;
/** Shared eligibility/readiness rules used by starts, catalog cards, and admin publication. */
public interface QuestionPoolRepository {
    List<Section> sections(UUID versionId);
    long available(UUID sectionId);
    List<PoolItem> select(UUID userId,Section section,boolean excludeToday,LocalDate day);
}
