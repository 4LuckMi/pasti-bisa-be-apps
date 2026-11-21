package com.pasti_bisa.backend;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import com.pasti_bisa.backend.modules.content.adapter.out.excel.PoiWorkbookParser;
import com.pasti_bisa.backend.modules.content.application.dto.WorkbookDtos.*;
import com.pasti_bisa.backend.shared.web.BusinessException;
import static org.assertj.core.api.Assertions.*;
class WorkbookParserTests {
    @TempDir Path directory;private final PoiWorkbookParser parser=new PoiWorkbookParser();
    private Path file(String mode)throws Exception{
        Path path=directory.resolve(UUID.randomUUID()+".xlsx");
        try(var workbook=new XSSFWorkbook()){
            var sheet=workbook.createSheet("TIU");var header=sheet.createRow(0);
            for(int i=0;i<PoiWorkbookParser.HEADERS.size();i++)header.createCell(i).setCellValue(PoiWorkbookParser.HEADERS.get(i));
            var row=sheet.createRow(1);String[] values={"Q1","","Pertanyaan","Pilihan A","Pilihan B","","","","A","Pembahasan","EASY","true","1","0"};
            for(int i=0;i<values.length;i++)row.createCell(i).setCellValue(values[i]);
            if(mode.equals("formula"))row.getCell(2).setCellFormula("1+1");
            if(mode.equals("badHeader"))header.getCell(0).setCellValue("wrong_code");
            if(mode.equals("hidden"))workbook.createSheet("HIDDEN");
            if(mode.equals("hidden"))workbook.setSheetHidden(1,true);
            if(mode.equals("weight"))row.getCell(12).setCellValue("-1");
            if(mode.equals("duplicate")){var extra=sheet.createRow(2);for(int i=0;i<values.length;i++)extra.createCell(i).setCellValue(values[i]);}
            try(var output=Files.newOutputStream(path)){workbook.write(output);}
        }return path;
    }
    @Test void validWorkbookPreservesFileBytesAndMapsQuestionFields()throws Exception{
        Path path=file("valid");byte[] before=Files.readAllBytes(path);
        var sheets=parser.parse(path,List.of(new SheetMapping("TIU",UUID.randomUUID())));var row=sheets.getFirst().rows().getFirst();
        assertThat(row.errors()).isEmpty();assertThat(row.row()).isEqualTo(2);assertThat(row.content().options().getFirst().correct()).isTrue();assertThat(row.difficulty()).isEqualTo("EASY");
        assertThat(Files.readAllBytes(path)).isEqualTo(before);
    }
    @Test void formulasAndInvalidWeightsRemainReviewableRowErrors()throws Exception{
        var mappings=List.of(new SheetMapping("TIU",UUID.randomUUID()));
        assertThat(parser.parse(file("formula"),mappings).getFirst().rows().getFirst().errors()).contains("FORMULA_NOT_ALLOWED");
        assertThat(parser.parse(file("weight"),mappings).getFirst().rows().getFirst().errors()).contains("INVALID_WEIGHT");
    }
    @Test void unknownHeadersAndUnmappedHiddenSheetsRejectTheWorkbook()throws Exception{
        var mappings=List.of(new SheetMapping("TIU",UUID.randomUUID()));
        assertThatThrownBy(()->parser.parse(file("badHeader"),mappings)).isInstanceOf(BusinessException.class);
        assertThatThrownBy(()->parser.parse(file("hidden"),mappings)).isInstanceOf(BusinessException.class);
    }
    @Test void oversizedExpandedZipAndMacrosAreRejectedBeforePoiLoadsWorkbook()throws Exception{
        Path path=directory.resolve("expanded.xlsx");
        try(var zip=new ZipOutputStream(Files.newOutputStream(path))){zip.putNextEntry(new ZipEntry("large.xml"));for(int n=0;n<17;n++)zip.write(new byte[1024*1024]);zip.closeEntry();}
        assertThatThrownBy(()->parser.parse(path,List.of())).isInstanceOf(BusinessException.class).extracting(e->((BusinessException)e).code()).isEqualTo("WORKBOOK_LIMIT");
        Path macro=directory.resolve("macro.xlsx");try(var zip=new ZipOutputStream(Files.newOutputStream(macro))){zip.putNextEntry(new ZipEntry("xl/vbaProject.bin"));zip.write(1);zip.closeEntry();}
        assertThatThrownBy(()->parser.parse(macro,List.of())).isInstanceOf(BusinessException.class).extracting(e->((BusinessException)e).code()).isEqualTo("UNSUPPORTED_WORKBOOK");
    }
}
