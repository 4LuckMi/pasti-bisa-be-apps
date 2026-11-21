package com.pasti_bisa.backend;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;
import com.pasti_bisa.backend.modules.exam.application.AttemptQuestionRepository;
import com.pasti_bisa.backend.modules.exam.application.AttemptQuestionService;
import com.pasti_bisa.backend.modules.exam.application.StudentLearningRepository;
import com.pasti_bisa.backend.modules.exam.application.StudentLearningService;
import com.pasti_bisa.backend.modules.exam.application.dto.AttemptQuestionDtos.*;
import com.pasti_bisa.backend.modules.exam.application.dto.StudentLearningDtos.*;
import com.pasti_bisa.backend.modules.identity.application.dto.AuthResponse;
import com.pasti_bisa.backend.modules.identity.application.dto.UserProfileResponse;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class StudentDtoTests {
    private final JsonMapper json = JsonMapper.builder().build();

    @Test
    void explicitDtosPreservePublicFieldNamesAndNullFields() {
        UUID id = UUID.randomUUID();
        var dto = new TryoutResponse(id, "TEST", "Test", "UTBK", "TPS", id, 5400, 10, 0, null, null);
        var tree = json.readTree(json.writeValueAsString(dto));
        assertThat(tree.has("blueprint_version_id")).isTrue();
        assertThat(tree.has("blueprintVersionId")).isFalse();
        assertThat(tree.get("latest_score").isNull()).isTrue();
        assertThat(tree.get("question_count").asLong()).isEqualTo(10);
        var auth = new AuthResponse(new UserProfileResponse(id, "student@example.com", "Student", "SISWA", List.of("CUSTOMER")));
        var user = json.readTree(json.writeValueAsString(auth)).get("user");
        assertThat(user.get("userType").asString()).isEqualTo("SISWA");
        assertThat(user.has("passwordHash")).isFalse();
    }

    @Test
    void questionDtosHideAnswerKeysUntilFinalizedIncludingWhenOrderIsEmpty() {
        var repository = mock(AttemptQuestionRepository.class);
        var service = new AttemptQuestionService(repository);
        UUID versionId = UUID.randomUUID();
        UUID optionId = UUID.randomUUID();
        var query = new QuestionQuery(UUID.randomUUID(), 1, "student@example.com");
        when(repository.findOptions(versionId)).thenReturn(List.of(new OptionRow(optionId, "A", json.readTree("{}"), true)));
        when(repository.findQuestion(query)).thenReturn(new QuestionRow(versionId, 1, json.readTree("{}"),
                json.readTree("[]"), optionId, false, 1, false, "Explanation",new QuestionScope(UUID.randomUUID(),"Subtest",null,null)));
        var active = service.question(query);
        assertThat(active.options().getFirst().correct()).isNull();
        assertThat(active.explanation()).isNull();
        assertThat(active.correct()).isNull();
        when(repository.findQuestion(query)).thenReturn(new QuestionRow(versionId, 1, json.readTree("{}"),
                json.readTree("[]"), optionId, false, 1, true, "Explanation",new QuestionScope(UUID.randomUUID(),"Subtest",null,null)));
        var finalized = service.question(query);
        assertThat(finalized.options().getFirst().correct()).isTrue();
        assertThat(finalized.correct()).isTrue();
        assertThat(finalized.explanation()).isEqualTo("Explanation");
    }

    @Test
    void quotaUsesWibDayAtUtcDateBoundary() {
        var repository = mock(StudentLearningRepository.class);
        var clock = Clock.fixed(Instant.parse("2026-11-18T18:00:00Z"), ZoneOffset.UTC);
        when(repository.findUsedQuota("student@example.com", LocalDate.of(2026, 11, 19))).thenReturn(1);
        var quota = new StudentLearningService(repository, clock).quota("student@example.com");
        assertThat(quota.dateWib()).isEqualTo(LocalDate.of(2026, 11, 19));
        assertThat(quota.remaining()).isEqualTo(1);
    }
}
