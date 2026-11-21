package com.pasti_bisa.backend.modules.content.application;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import com.pasti_bisa.backend.modules.content.application.dto.ContentDtos.StoredUpload;

public interface ImportFileStorage {
    StoredUpload store(InputStream stream,String originalName) throws IOException;
    StoredUpload storeWorkbook(InputStream stream,String originalName) throws IOException;
    Path resolve(String relativePath) throws IOException;
    void deleteUncommitted(StoredUpload upload) throws IOException;
}
