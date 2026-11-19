package com.pasti_bisa.backend.modules.content.application;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import com.pasti_bisa.backend.modules.content.domain.ContentModels.ExtractedPage;

public interface PdfExtractor {
    List<ExtractedPage> extract(Path file) throws IOException;
}
