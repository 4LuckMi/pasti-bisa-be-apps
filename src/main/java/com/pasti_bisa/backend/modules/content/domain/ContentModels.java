package com.pasti_bisa.backend.modules.content.domain;

import java.math.BigDecimal;
import java.util.List;

/** Content rules operate on plain values, independent of HTTP validation and persistence. */
public final class ContentModels {
    private ContentModels() {}
    public record Option(String code,String text,boolean correct,BigDecimal weight) {}
    public record Question(String stemText,List<Option> options,String explanation,boolean shuffleAllowed) {}
    public record ExtractedPage(int page,String text,boolean ocr) {}
    public record ParsedCandidate(int page,int number,Question content,List<String> errors) {}
}
