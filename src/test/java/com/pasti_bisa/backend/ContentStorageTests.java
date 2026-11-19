package com.pasti_bisa.backend;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import com.pasti_bisa.backend.modules.content.adapter.out.storage.LocalImportFileStorage;
import com.pasti_bisa.backend.shared.web.BusinessException;
import static org.assertj.core.api.Assertions.*;

class ContentStorageTests {
    @TempDir Path root;
    private ByteArrayInputStream input(String text) { return new ByteArrayInputStream(text.getBytes(StandardCharsets.US_ASCII)); }
    @Test void invalidSignatureAndOversizeUploadsLeaveNoPartialFile() throws Exception {
        var storage=new LocalImportFileStorage(root.toString(),10);
        assertThatThrownBy(()->storage.store(input("not a pdf"),"x.pdf")).isInstanceOf(BusinessException.class);
        assertThatThrownBy(()->storage.store(input("%PDF-123456789"),"x.pdf")).isInstanceOf(BusinessException.class);
        try(var files=Files.list(root.resolve("imports"))) { assertThat(files.toList()).isEmpty(); }
    }
    @Test void fileNamesCannotControlStoragePaths() throws Exception {
        var storage=new LocalImportFileStorage(root.toString(),100);
        var saved=storage.store(input("%PDF-1.7"),"../../source.pdf");
        assertThat(saved.path()).matches("imports/[0-9a-f-]{36}\\.pdf");
        assertThat(storage.resolve(saved.path())).startsWith(root.toRealPath());
        assertThatThrownBy(()->storage.resolve("../outside.pdf")).isInstanceOf(java.io.IOException.class);
    }
}
