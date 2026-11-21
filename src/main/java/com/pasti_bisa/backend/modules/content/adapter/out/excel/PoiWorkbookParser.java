package com.pasti_bisa.backend.modules.content.adapter.out.excel;

import java.io.*;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.*;
import java.util.zip.ZipFile;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;
import com.pasti_bisa.backend.modules.content.application.WorkbookParser;
import com.pasti_bisa.backend.modules.content.application.dto.WorkbookDtos.*;
import com.pasti_bisa.backend.modules.content.application.dto.ContentDtos.*;
import com.pasti_bisa.backend.modules.content.domain.QuestionValidator;
import com.pasti_bisa.backend.shared.web.BusinessException;

/** Bounded OOXML reader. Formula evaluation, external references and macros are never used. */
@Component
public class PoiWorkbookParser implements WorkbookParser {
    public static final List<String> HEADERS=List.of("question_code","topic_id","question_text","option_a","option_b","option_c","option_d","option_e",
            "correct_answer","explanation","difficulty","shuffle_allowed","weight_a","weight_b","weight_c","weight_d","weight_e");
    private static final int MAX_ROWS=2000,MAX_CELLS=40000,MAX_EXPANDED_BYTES=16*1024*1024;
    private static void invalid(String code,String message){throw new BusinessException(400,code,message);}
    private void preflight(Path path)throws IOException{
        int total=0,entries=0,rows=0,cells=0;
        try(var zip=new ZipFile(path.toFile())){
            var parts=zip.entries();
            while(parts.hasMoreElements()){
                var part=parts.nextElement();String name=part.getName().toLowerCase(Locale.ROOT);
                if(++entries>512)invalid("WORKBOOK_LIMIT","Workbook contains too many parts");
                if(name.contains("vbaproject")||name.contains("externallinks/"))invalid("UNSUPPORTED_WORKBOOK","Macros and external workbook references are not allowed");
                if(name.startsWith("xl/media/")||name.startsWith("xl/drawings/")||name.startsWith("xl/embeddings/"))
                    invalid("UNSUPPORTED_WORKBOOK","Embedded images, drawings and objects are not supported by the text question template");
                try(var input=zip.getInputStream(part);var output=new ByteArrayOutputStream()){
                    byte[] buffer=new byte[8192];int n;
                    while((n=input.read(buffer))!=-1){total+=n;if(total>MAX_EXPANDED_BYTES)invalid("WORKBOOK_LIMIT","Expanded workbook exceeds 16 MiB");output.write(buffer,0,n);}
                    if(name.startsWith("xl/worksheets/")&&name.endsWith(".xml")||name.endsWith(".rels")){
                        var factory=XMLInputFactory.newFactory();factory.setProperty(XMLInputFactory.SUPPORT_DTD,false);
                        factory.setProperty("javax.xml.stream.isSupportingExternalEntities",false);
                        var reader=factory.createXMLStreamReader(new ByteArrayInputStream(output.toByteArray()));
                        try{while(reader.hasNext()){
                            if(reader.next()!=XMLStreamConstants.START_ELEMENT)continue;
                            String tag=reader.getLocalName();
                            if(tag.equals("row")&&++rows>MAX_ROWS+30)invalid("WORKBOOK_LIMIT","Workbook supports at most 2000 data rows");
                            if(tag.equals("c")&&++cells>MAX_CELLS)invalid("WORKBOOK_LIMIT","Workbook contains too many cells");
                            if("External".equalsIgnoreCase(reader.getAttributeValue(null,"TargetMode")))invalid("UNSUPPORTED_WORKBOOK","External relationships are not allowed");
                        }}finally{reader.close();}
                    }
                }
            }
        }catch(BusinessException e){throw e;}catch(Exception e){throw new BusinessException(400,"INVALID_WORKBOOK","File is not a valid supported XLSX workbook");}
    }
    @Override public List<ParsedSheet> parse(Path path,List<SheetMapping> mappings)throws IOException{
        preflight(path);
        try(var workbook=new XSSFWorkbook(org.apache.poi.openxml4j.opc.OPCPackage.open(path.toFile(),org.apache.poi.openxml4j.opc.PackageAccess.READ))){
            if(workbook.getNumberOfSheets()<1||workbook.getNumberOfSheets()>30)invalid("WORKBOOK_LIMIT","Use 1 to 30 sheets");
            var expected=new HashSet<>(mappings.stream().map(SheetMapping::sheetName).toList());
            if(expected.size()!=mappings.size()||expected.size()!=workbook.getNumberOfSheets())invalid("SHEET_MAPPING_MISMATCH","Map every sheet exactly once");
            var result=new ArrayList<ParsedSheet>();int rowCount=0;
            for(int index=0;index<workbook.getNumberOfSheets();index++){
                Sheet sheet=workbook.getSheetAt(index);
                if(!expected.contains(sheet.getSheetName())||workbook.isSheetHidden(index)||workbook.isSheetVeryHidden(index))
                    invalid("SHEET_MAPPING_MISMATCH","Every visible worksheet must have an explicit mapping; hidden sheets are not supported");
                if(sheet.getNumMergedRegions()!=0)invalid("UNSUPPORTED_WORKBOOK","Merged cells are not supported");
                var formatter=new DataFormatter(Locale.ROOT);var columns=new LinkedHashMap<String,Integer>();
                Row header=sheet.getRow(0);if(header==null)invalid("INVALID_HEADERS","Every sheet requires a header in row 1");
                for(Cell cell:header){
                    if(cell.getCellType()==CellType.FORMULA)invalid("INVALID_HEADERS","Header formulas are not allowed");
                    String name=formatter.formatCellValue(cell).trim().toLowerCase(Locale.ROOT);
                    if(name.isBlank())continue;
                    if(!HEADERS.contains(name)||columns.putIfAbsent(name,cell.getColumnIndex())!=null)invalid("INVALID_HEADERS","Unknown or duplicate column: "+name);
                }
                if(!columns.keySet().containsAll(List.of("question_code","question_text","option_a","option_b","correct_answer","explanation")))
                    invalid("INVALID_HEADERS","Missing a required column in sheet "+sheet.getSheetName());
                var rows=new ArrayList<ParsedRow>();
                for(Row row:sheet){
                    if(row.getRowNum()==0)continue;
                    var errors=new ArrayList<String>();var values=new HashMap<String,String>();boolean nonEmpty=false;
                    for(Cell cell:row){
                        if(cell.getCellType()==CellType.FORMULA){errors.add("FORMULA_NOT_ALLOWED");nonEmpty=true;}
                        else if(cell.getCellType()==CellType.ERROR){errors.add("CELL_ERROR");nonEmpty=true;}
                        else if(!formatter.formatCellValue(cell).isBlank()){
                            nonEmpty=true;if(!columns.containsValue(cell.getColumnIndex()))errors.add("UNMAPPED_CELL");
                        }
                    }
                    if(!nonEmpty)continue;
                    if(++rowCount>MAX_ROWS)invalid("WORKBOOK_LIMIT","Workbook supports at most 2000 data rows");
                    for(var column:columns.entrySet()){
                        Cell cell=row.getCell(column.getValue());String value=cell==null||cell.getCellType()==CellType.FORMULA?"":formatter.formatCellValue(cell).trim();
                        if(value.length()>20000){errors.add("CELL_TOO_LONG");value=value.substring(0,20000);}values.put(column.getKey(),value);
                    }
                    String code=values.getOrDefault("question_code","");
                    if(!code.matches("[A-Za-z0-9][A-Za-z0-9_.-]{0,79}"))errors.add("INVALID_QUESTION_CODE");
                    if(code.length()>80)code=code.substring(0,80);
                    UUID topic=null;String topicValue=values.getOrDefault("topic_id","");
                    try{if(!topicValue.isBlank())topic=UUID.fromString(topicValue);}catch(IllegalArgumentException e){errors.add("INVALID_TOPIC_ID");}
                    String difficulty=values.getOrDefault("difficulty","").toUpperCase(Locale.ROOT);
                    if(difficulty.isBlank())difficulty="MEDIUM";
                    if(!List.of("EASY","MEDIUM","HARD").contains(difficulty)){errors.add("INVALID_DIFFICULTY");difficulty=null;}
                    String correct=values.getOrDefault("correct_answer","").toUpperCase(Locale.ROOT);
                    if(!correct.matches("[A-E]"))errors.add("INVALID_ANSWER_KEY");
                    var options=new ArrayList<OptionDraft>();
                    for(char letter='A';letter<='E';letter++){
                        String suffix=String.valueOf(Character.toLowerCase(letter)),text=values.getOrDefault("option_"+suffix,"");
                        String weightValue=values.getOrDefault("weight_"+suffix,"");boolean key=correct.equals(String.valueOf(letter));
                        if(text.isBlank()){if(!weightValue.isBlank())errors.add("WEIGHT_WITHOUT_OPTION");continue;}
                        if(text.length()>10000)errors.add("OPTION_TOO_LONG");
                        BigDecimal weight=key?BigDecimal.ONE:BigDecimal.ZERO;
                        try{if(!weightValue.isBlank())weight=new BigDecimal(weightValue);if(weight.signum()<0||weight.compareTo(BigDecimal.valueOf(1000))>0||weight.scale()>4)throw new NumberFormatException();}
                        catch(NumberFormatException e){errors.add("INVALID_WEIGHT");weight=BigDecimal.ZERO;}
                        options.add(new OptionDraft(String.valueOf(letter),text,key,weight));
                    }
                    String shuffleValue=values.getOrDefault("shuffle_allowed","").toLowerCase(Locale.ROOT);
                    if(!List.of("","true","false").contains(shuffleValue))errors.add("INVALID_SHUFFLE_FLAG");
                    var content=new QuestionContent(values.getOrDefault("question_text",""),options,values.getOrDefault("explanation",""),!shuffleValue.equals("false"));
                    errors.addAll(QuestionValidator.publicationErrors(content.toDomain()));
                    rows.add(new ParsedRow(row.getRowNum()+1,code,topic,difficulty,content,errors.stream().distinct().toList()));
                }
                if(rows.isEmpty())invalid("EMPTY_SHEET","Each mapped sheet must contain at least one question");
                result.add(new ParsedSheet(sheet.getSheetName(),index,rows));
            }
            return result;
        }catch(BusinessException e){throw e;}catch(Exception e){throw new BusinessException(400,"INVALID_WORKBOOK","File is not a valid supported XLSX workbook");}
    }
    @Override public byte[] template(List<TemplateSheet> sheets)throws IOException{
        try(var workbook=new XSSFWorkbook();var output=new ByteArrayOutputStream()){
            var style=workbook.createCellStyle();style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            var font=workbook.createFont();font.setBold(true);font.setColor(IndexedColors.WHITE.getIndex());style.setFont(font);
            for(var definition:sheets){
                var sheet=workbook.createSheet(definition.name());var row=sheet.createRow(0);
                for(int index=0;index<HEADERS.size();index++){var cell=row.createCell(index);cell.setCellValue(HEADERS.get(index));cell.setCellStyle(style);sheet.setColumnWidth(index,(index==2||index==9?60:24)*256);}
                sheet.createFreezePane(0,1);sheet.setAutoFilter(new CellRangeAddress(0,0,0,HEADERS.size()-1));
                var helper=sheet.getDataValidationHelper();
                sheet.addValidationData(helper.createValidation(helper.createExplicitListConstraint(new String[]{"EASY","MEDIUM","HARD"}),new CellRangeAddressList(1,MAX_ROWS,10,10)));
                sheet.addValidationData(helper.createValidation(helper.createExplicitListConstraint(new String[]{"A","B","C","D","E"}),new CellRangeAddressList(1,MAX_ROWS,8,8)));
            }
            workbook.write(output);return output.toByteArray();
        }
    }
}
