package com.pasti_bisa.backend.modules.exam.application;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.pasti_bisa.backend.modules.exam.application.dto.AttemptQuestionDtos.*;

@Service
@Transactional(readOnly = true)
public class AttemptQuestionService {
    private final AttemptQuestionRepository repository;
    public AttemptQuestionService(AttemptQuestionRepository repository) { this.repository = repository; }

    public QuestionResponse question(QuestionQuery query) {
        QuestionRow question = repository.findQuestion(query);
        List<OptionRow> options = repository.findOptions(question.versionId());
        var ordered = new ArrayList<OptionRow>();
        if (question.optionOrder().isArray() && !question.optionOrder().isEmpty()) {
            for (var code : question.optionOrder()) {
                options.stream().filter(option -> option.code().equals(code.asString())).findFirst().ifPresent(ordered::add);
            }
        } else ordered.addAll(options);

        List<OptionResponse> responseOptions = ordered.stream().map(option -> new OptionResponse(
                option.id().toString(), option.code(), option.content(), question.finalized() ? option.correct() : null)).toList();
        UUID selected = question.selectedOptionId();
        Boolean correct = question.finalized() && selected != null
                ? options.stream().anyMatch(option -> option.id().equals(selected) && option.correct()) : null;
        return new QuestionResponse(question.position(), question.stem(), responseOptions,
                selected == null ? null : selected.toString(), question.flagged(), question.revision(), correct,
                question.finalized() ? question.explanation() : null,question.scope());
    }
}
