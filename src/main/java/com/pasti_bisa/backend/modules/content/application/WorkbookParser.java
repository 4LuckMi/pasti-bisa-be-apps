package com.pasti_bisa.backend.modules.content.application;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import com.pasti_bisa.backend.modules.content.application.dto.WorkbookDtos.*;
public interface WorkbookParser {
    List<ParsedSheet> parse(Path path,List<SheetMapping> mappings)throws IOException;
    byte[] template(List<TemplateSheet> sheets)throws IOException;
}
