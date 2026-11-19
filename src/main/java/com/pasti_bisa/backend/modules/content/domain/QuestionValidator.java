package com.pasti_bisa.backend.modules.content.domain;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import com.pasti_bisa.backend.modules.content.domain.ContentModels.Question;

public final class QuestionValidator {
    private QuestionValidator() {}
    public static List<String> publicationErrors(Question content) {
        var errors=new ArrayList<String>();
        if(content.stemText()==null||content.stemText().isBlank()) errors.add("STEM_MISSING");
        if(content.stemText()!=null&&content.stemText().length()>20000) errors.add("STEM_TOO_LONG");
        if(content.options()==null||content.options().size()<2||content.options().size()>5) errors.add("OPTIONS_INCOMPLETE");
        var codes=new HashSet<String>();
        if(content.options()!=null) {
            for(var option:content.options()) {
                if(option.code()==null||!option.code().matches("[A-E]")||!codes.add(option.code())) errors.add("OPTION_CODE_INVALID");
                if(option.text()==null||option.text().isBlank()||option.text().length()>10000) errors.add("OPTION_TEXT_INVALID");
                if(option.weight()==null||option.weight().signum()<0) errors.add("OPTION_WEIGHT_INVALID");
            }
            if(content.options().stream().filter(o->o.correct()).count()!=1) errors.add("ANSWER_KEY_MISSING_OR_AMBIGUOUS");
        }
        if(content.explanation()==null||content.explanation().isBlank()) errors.add("EXPLANATION_MISSING");
        return errors.stream().distinct().toList();
    }
}
