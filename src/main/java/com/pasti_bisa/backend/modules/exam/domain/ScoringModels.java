package com.pasti_bisa.backend.modules.exam.domain;

import java.math.BigDecimal;
import java.util.UUID;
import tools.jackson.databind.JsonNode;

public final class ScoringModels {
    private ScoringModels() {}
    public record ScoringItem(UUID subtestId,UUID selectedOptionId,boolean correct,BigDecimal selectedWeight,BigDecimal maxWeight) {}
    public record ScorePolicy(UUID id,String method,String code,int version,BigDecimal scale,JsonNode rules) {}
    public record SectionScore(UUID subtestId,int correct,int wrong,int empty,BigDecimal points,BigDecimal maxPoints,BigDecimal accuracy) {}
}
