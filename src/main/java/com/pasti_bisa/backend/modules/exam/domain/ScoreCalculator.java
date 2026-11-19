package com.pasti_bisa.backend.modules.exam.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;
import com.pasti_bisa.backend.modules.exam.domain.ScoringModels.*;

/** Deterministic product index, not an official UTBK/IRT score. */
public final class ScoreCalculator {
    private ScoreCalculator() {}
    public record Calculation(BigDecimal total, List<SectionScore> sections, int correct, int wrong, int empty) {}

    public static void validatePolicy(ScorePolicy policy) {
        if(!List.of("BINARY","OPTION_WEIGHT").contains(policy.method()) || policy.scale()==null || policy.scale().signum()<=0
                || policy.rules()==null || !policy.rules().isObject()) throw new IllegalArgumentException("Unsupported scoring policy");
        for(String field:policy.rules().propertyNames()) {
            if(!List.of("correctPoints","wrongPoints","emptyPoints").contains(field)
                    || (policy.method().equals("OPTION_WEIGHT")&&!field.equals("emptyPoints")))
                throw new IllegalArgumentException("Unsupported scoring rule");
            number(policy,field,BigDecimal.ZERO);
        }
        if(policy.method().equals("BINARY")&&number(policy,"correctPoints",BigDecimal.ONE).signum()<=0)
            throw new IllegalArgumentException("Correct points must be positive");
    }

    public static Calculation calculate(ScorePolicy policy, List<ScoringItem> items) {
        if (items.isEmpty()) throw new IllegalArgumentException("Cannot score an empty attempt");
        validatePolicy(policy);
        var groups = new LinkedHashMap<UUID, List<ScoringItem>>();
        items.forEach(item -> groups.computeIfAbsent(item.subtestId(), ignored -> new ArrayList<>()).add(item));
        var sections = new ArrayList<SectionScore>();
        BigDecimal total = BigDecimal.ZERO, max = BigDecimal.ZERO;
        int correct = 0, wrong = 0, empty = 0;
        for (var group : groups.entrySet()) {
            int c = 0, w = 0, e = 0;
            BigDecimal points = BigDecimal.ZERO, maximum = BigDecimal.ZERO;
            for (ScoringItem item : group.getValue()) {
                boolean answered = item.selectedOptionId() != null;
                if (!answered) e++; else if (item.correct()) c++; else w++;
                BigDecimal itemPoints = !answered ? number(policy, "emptyPoints", BigDecimal.ZERO)
                        : policy.method().equals("OPTION_WEIGHT") ? item.selectedWeight()
                        : item.correct() ? number(policy, "correctPoints", BigDecimal.ONE) : number(policy, "wrongPoints", BigDecimal.ZERO);
                points = points.add(itemPoints);
                maximum = maximum.add(policy.method().equals("OPTION_WEIGHT") ? item.maxWeight() : number(policy, "correctPoints", BigDecimal.ONE));
            }
            BigDecimal accuracy = BigDecimal.valueOf(c * 100L).divide(BigDecimal.valueOf(group.getValue().size()), 4, RoundingMode.HALF_UP);
            sections.add(new SectionScore(group.getKey(), c, w, e, points, maximum, accuracy));
            total = total.add(points); max = max.add(maximum); correct += c; wrong += w; empty += e;
        }
        if (max.signum() <= 0) throw new IllegalArgumentException("Scoring maximum must be positive");
        return new Calculation(total.multiply(policy.scale()).divide(max, 4, RoundingMode.HALF_UP), sections, correct, wrong, empty);
    }

    private static BigDecimal number(ScorePolicy policy, String field, BigDecimal fallback) {
        var value = policy.rules().get(field);
        return value == null ? fallback : new BigDecimal(value.isNumber()?value.toString():value.asString());
    }
}
