package com.pasti_bisa.backend.modules.content.adapter.out.demo;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("demo")
public class DemoDataSeeder implements ApplicationRunner {
    private final NamedParameterJdbcTemplate jdbc;
    private final Path root;
    public DemoDataSeeder(NamedParameterJdbcTemplate jdbc,@Value("${pastisiap.storage.root:./storage}") String root) {
        this.jdbc=jdbc;this.root=Path.of(root);
    }
    public static UUID id(String value) { return UUID.nameUUIDFromBytes(("pastisiap-demo:"+value).getBytes(StandardCharsets.UTF_8)); }
    @Override @Transactional public void run(ApplicationArguments args) throws Exception {
        // API + worker can both start with demo; a transaction advisory lock keeps seeding idempotent.
        jdbc.queryForObject("SELECT pg_advisory_xact_lock(149241)",new MapSqlParameterSource(),Object.class);
        var base=new MapSqlParameterSource("policy",id("policy"));
        jdbc.update("""
                INSERT INTO scoring_policy(id,code,version,method,rules,aggregation,scale,rounding,code_hash,status)
                VALUES (:policy,'DEMO_BINARY',1,'BINARY','{"correctPoints":1,"wrongPoints":0,"emptyPoints":0}',
                        'SUM',100,'HALF_UP','demo-binary-v1','ACTIVE') ON CONFLICT (id) DO NOTHING
                """,base);
        for(String category:List.of("UTBK","CPNS","KEDINASAN","SEKOLAH")) seedCategory(category);
        Path dir=root.resolve("demo");Files.createDirectories(dir);
        Path sample=dir.resolve("sample-questions.pdf");
        if(!Files.exists(sample)) {
            try(var document=new PDDocument()) {
                var page=new PDPage();document.addPage(page);
                try(var stream=new PDPageContentStream(document,page)) {
                    stream.beginText();stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA),11);
                    stream.setLeading(16);stream.newLineAtOffset(45,745);
                    for(String line:List.of("Contoh PastiSiap - bukan soal resmi", "1. Berapa hasil 2 + 3?","A. 4","B. 5","C. 6","D. 7", "Kunci: B", "Pembahasan: Dua ditambah tiga sama dengan lima.","", "2. Berapa hasil 4 x 2?", "A. 6","B. 7","C. 8","D. 9", "Kunci: C", "Pembahasan: Empat dikali dua sama dengan delapan.")) {
                        stream.showText(line);stream.newLine();
                    }
                    stream.endText();
                }
                document.save(sample.toFile());
            }
        }
    }
    private void seedCategory(String category) {
        var p=new MapSqlParameterSource("category",id(category)).addValue("code",category).addValue("type",id(category+":type"))
                .addValue("subtest",id(category+":subtest")).addValue("topic",id(category+":topic"))
                .addValue("blueprint",id(category+":blueprint")).addValue("version",id(category+":blueprint-version"))
                .addValue("section",id(category+":section")).addValue("policy",id("policy"));
        jdbc.update("INSERT INTO exam_category(id,code,name) VALUES (:category,:code,:code) ON CONFLICT (code) DO NOTHING",p);
        p.addValue("category",jdbc.queryForObject("SELECT id FROM exam_category WHERE code=:code",p,UUID.class));
        jdbc.update("INSERT INTO exam_type(id,category_id,code,name) VALUES (:type,:category,'DEMO','Tes Numerik Contoh') ON CONFLICT (category_id,code) DO NOTHING",p);
        p.addValue("type",jdbc.queryForObject("SELECT id FROM exam_type WHERE category_id=:category AND code='DEMO'",p,UUID.class));
        jdbc.update("INSERT INTO subtest(id,exam_type_id,code,name) VALUES (:subtest,:type,'NUMERIK','Penalaran Numerik Contoh') ON CONFLICT (exam_type_id,code) DO NOTHING",p);
        p.addValue("subtest",jdbc.queryForObject("SELECT id FROM subtest WHERE exam_type_id=:type AND code='NUMERIK'",p,UUID.class));
        jdbc.update("INSERT INTO topic(id,subtest_id,name) VALUES (:topic,:subtest,'Aritmetika Dasar Contoh') ON CONFLICT (subtest_id,name) DO NOTHING",p);
        p.addValue("topic",jdbc.queryForObject("SELECT id FROM topic WHERE subtest_id=:subtest AND name='Aritmetika Dasar Contoh'",p,UUID.class));
        for(int number=1;number<=12;number++) {
            UUID question=id(category+":q:"+number),version=id(category+":qv:"+number);
            if(Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM question_version WHERE id=:id)",new MapSqlParameterSource("id",version),Boolean.class))) continue;
            var item=new MapSqlParameterSource("id",question).addValue("version",version).addValue("subtest",p.getValue("subtest"))
                    .addValue("topic",p.getValue("topic")).addValue("code","DEMO-"+category+"-"+number)
                    .addValue("stem","[Contoh "+category+"] Berapa hasil "+number+" + "+number+"?")
                    .addValue("explanation",number+" + "+number+" = "+(number*2)+". Soal dummy, bukan materi resmi.");
            jdbc.update("INSERT INTO question(id,subtest_id,topic_id,code) VALUES (:id,:subtest,:topic,:code)",item);
            jdbc.update("""
                    INSERT INTO question_version(id,question_id,version,stem,explanation,source_year,status,source_title)
                    VALUES (:version,:id,1,jsonb_build_object('text',:stem::text),:explanation,2026,'DRAFT','Dummy PastiSiap')
                    """,item);
            for(int option=0;option<4;option++) {
                item.addValue("optionId",id(category+":option:"+number+":"+option)).addValue("optionCode",String.valueOf((char)('A'+option)))
                        .addValue("text",String.valueOf(number*2+option)).addValue("position",option+1).addValue("correct",option==0).addValue("weight",option==0?1:0);
                jdbc.update("""
                        INSERT INTO question_option(id,question_version_id,option_code,content,original_position,correct_key,score_weight)
                        VALUES (:optionId,:version,:optionCode,jsonb_build_object('text',:text::text),:position,:correct,:weight)
                        """,item);
            }
            jdbc.update("UPDATE question_version SET status='PUBLISHED' WHERE id=:version",item);
        }
        if(!Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM blueprint WHERE id=:blueprint)",p,Boolean.class))) {
            jdbc.update("INSERT INTO blueprint(id,exam_type_id,code,title,demo) VALUES (:blueprint,:type,'DEMO_TRYOUT','Tryout '||:code||' - Contoh',true)",p);
            jdbc.update("""
                    INSERT INTO blueprint_version(id,blueprint_id,version,duration_seconds,scoring_policy,scoring_policy_id)
                    VALUES (:version,:blueprint,1,5400,'{}',:policy)
                    """,p);
            jdbc.update("INSERT INTO blueprint_section(id,blueprint_version_id,subtest_id,position,item_count) VALUES (:section,:version,:subtest,1,3)",p);
            jdbc.update("UPDATE blueprint_version SET status='ACTIVE' WHERE id=:version",p);
            jdbc.update("UPDATE blueprint SET status='ACTIVE',active_version_id=:version WHERE id=:blueprint",p);
        }
    }
}
