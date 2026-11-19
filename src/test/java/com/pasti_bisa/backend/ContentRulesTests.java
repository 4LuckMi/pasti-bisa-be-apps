package com.pasti_bisa.backend;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;
import com.pasti_bisa.backend.modules.content.domain.ContentModels.*;
import com.pasti_bisa.backend.modules.content.domain.TemplateQuestionParser;
import com.pasti_bisa.backend.modules.exam.domain.ScoringModels.*;
import com.pasti_bisa.backend.modules.exam.domain.ScoreCalculator;
import static org.assertj.core.api.Assertions.*;

class ContentRulesTests {
    @Test void multipleKeysRemainAmbiguous() {
        var parsed=TemplateQuestionParser.parse(List.of(new ExtractedPage(1,
                "1. Dua tambah dua?\nA. 3\nB. 4\nKunci: A\nKunci: B\nPembahasan: Perlu review",false)));
        assertThat(parsed.getFirst().errors()).contains("ANSWER_KEY_MISSING_OR_AMBIGUOUS");
        assertThat(parsed.getFirst().content().options()).noneMatch(Option::correct);
    }
    @Test void numericRulesAreAppliedAndUnsupportedRulesRejectedBeforeScoring() {
        var json=JsonMapper.builder().build();
        var policy=new ScorePolicy(UUID.randomUUID(),"BINARY","test",1,BigDecimal.valueOf(100),
                json.readTree("{\"correctPoints\":4,\"wrongPoints\":-1,\"emptyPoints\":0}"));
        var result=ScoreCalculator.calculate(policy,List.of(new ScoringItem(UUID.randomUUID(),UUID.randomUUID(),true,BigDecimal.ONE,BigDecimal.ONE)));
        assertThat(result.total()).isEqualByComparingTo("100");
        assertThatThrownBy(()->ScoreCalculator.validatePolicy(new ScorePolicy(UUID.randomUUID(),"BINARY","test",1,
                BigDecimal.valueOf(100),json.readTree("{\"irt\":true}")))).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void parserPreservesMissingKeyAndExplanationInsteadOfGuessing() {
        var parsed=TemplateQuestionParser.parse(List.of(new ExtractedPage(1,"1. Dua tambah dua?\nA. 3\nB. 4",false)));
        assertThat(parsed).hasSize(1);
        assertThat(parsed.getFirst().errors()).contains("ANSWER_KEY_MISSING_OR_AMBIGUOUS","EXPLANATION_MISSING");
        assertThat(parsed.getFirst().content().options()).noneMatch(Option::correct);
    }
    @Test void parserRecognizesExplicitKeyAndExplanation() {
        var parsed=TemplateQuestionParser.parse(List.of(new ExtractedPage(1,"1. Dua tambah dua?\nA. 3\nB. 4\nKunci: B\nPembahasan: 2+2=4",false)));
        assertThat(parsed.getFirst().errors()).isEmpty();
        assertThat(parsed.getFirst().content().options().get(1).correct()).isTrue();
    }
    @Test void binaryAndWeightedScoresKeepWrongAndEmptySeparate() {
        var json=JsonMapper.builder().build();UUID subtest=UUID.randomUUID();
        var items=List.of(new ScoringItem(subtest,UUID.randomUUID(),true,BigDecimal.valueOf(5),BigDecimal.valueOf(5)),
                new ScoringItem(subtest,UUID.randomUUID(),false,BigDecimal.valueOf(2),BigDecimal.valueOf(5)),
                new ScoringItem(subtest,null,false,BigDecimal.ZERO,BigDecimal.valueOf(5)));
        var binary=ScoreCalculator.calculate(new ScorePolicy(UUID.randomUUID(),"BINARY","test",1,BigDecimal.valueOf(100),json.readTree("{}")),items);
        assertThat(binary.total()).isEqualByComparingTo("33.3333");
        assertThat(binary.correct()).isEqualTo(1);assertThat(binary.wrong()).isEqualTo(1);assertThat(binary.empty()).isEqualTo(1);
        var weighted=ScoreCalculator.calculate(new ScorePolicy(UUID.randomUUID(),"OPTION_WEIGHT","test",1,BigDecimal.valueOf(100),json.readTree("{}")),items);
        assertThat(weighted.total()).isEqualByComparingTo("46.6667");
    }
}
