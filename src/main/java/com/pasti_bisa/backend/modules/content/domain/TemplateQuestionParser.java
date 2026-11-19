package com.pasti_bisa.backend.modules.content.domain;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import com.pasti_bisa.backend.modules.content.domain.ContentModels.*;

/** Conservative parser for the documented numbered question template; never infers an answer. */
public final class TemplateQuestionParser {
    private static final Pattern QUESTION=Pattern.compile("^(\\d{1,4})[.)]\\s+(.+)$");
    private static final Pattern OPTION=Pattern.compile("^([A-E])[.)]\\s+(.+)$");
    private static final Pattern KEY=Pattern.compile("^(?:Kunci|Answer)\\s*:\\s*([A-E])\\s*$",Pattern.CASE_INSENSITIVE);
    private static final Pattern EXPLANATION=Pattern.compile("^(?:Pembahasan|Explanation)\\s*:\\s*(.*)$",Pattern.CASE_INSENSITIVE);
    private TemplateQuestionParser() {}
    public static List<ParsedCandidate> parse(List<ExtractedPage> pages) {
        var result=new ArrayList<ParsedCandidate>();
        Builder current=null;
        for(ExtractedPage page:pages) {
            int before=result.size();boolean found=false;
            for(String raw:page.text().split("\\R")) {
                String line=raw.trim();if(line.isEmpty()) continue;
                var question=QUESTION.matcher(line);var option=OPTION.matcher(line);var key=KEY.matcher(line);var explanation=EXPLANATION.matcher(line);
                if(question.matches()) {
                    if(current!=null) result.add(current.finish());
                    current=new Builder(page.page(),Integer.parseInt(question.group(1)),question.group(2));found=true;
                } else if(current!=null&&key.matches()) {
                    if(current.key!=null) current.ambiguousKey=true;
                    current.key=key.group(1).toUpperCase(java.util.Locale.ROOT);
                }
                else if(current!=null&&explanation.matches()) { current.explaining=true;current.explanation.append(explanation.group(1)); }
                else if(current!=null&&option.matches()&&!current.explaining) {
                    current.codes.add(option.group(1));current.options.add(new StringBuilder(option.group(2)));
                } else if(current!=null) {
                    if(current.explaining) current.explanation.append('\n').append(line);
                    else if(!current.options.isEmpty()) current.options.getLast().append('\n').append(line);
                    else current.stem.append('\n').append(line);
                }
                if(result.size()>1000) throw new IllegalArgumentException("Too many PDF candidates");
            }
            if(current==null&&!found&&result.size()==before) {
                var content=new Question(page.text(),List.of(),null,false);
                result.add(new ParsedCandidate(page.page(),1,content,List.of("TEMPLATE_NOT_RECOGNIZED","MANUAL_REVIEW_REQUIRED")));
            }
        }
        if(current!=null) result.add(current.finish());
        // source_page + source_item_no is a stable idempotent origin; normalize repeated source numbers per page.
        var counters=new java.util.HashMap<Integer,Integer>();
        return result.stream().map(candidate->new ParsedCandidate(candidate.page(),counters.merge(candidate.page(),1,Integer::sum),
                candidate.content(),candidate.errors())).toList();
    }
    private static class Builder {
        final int page,number;final StringBuilder stem;final List<String> codes=new ArrayList<>();
        final List<StringBuilder> options=new ArrayList<>();final StringBuilder explanation=new StringBuilder();
        String key;boolean explaining,ambiguousKey;
        Builder(int page,int number,String stem) { this.page=page;this.number=number;this.stem=new StringBuilder(stem); }
        ParsedCandidate finish() {
            if(ambiguousKey) key=null;
            var entries=new ArrayList<Option>();
            for(int i=0;i<options.size();i++) entries.add(new Option(codes.get(i),options.get(i).toString(),codes.get(i).equals(key),
                    codes.get(i).equals(key)?BigDecimal.ONE:BigDecimal.ZERO));
            var content=new Question(stem.toString(),entries,explanation.isEmpty()?null:explanation.toString(),false);
            return new ParsedCandidate(page,number,content,QuestionValidator.publicationErrors(content));
        }
    }
}
