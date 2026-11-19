package com.pasti_bisa.backend.modules.content.adapter.out.pdf;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.imageio.ImageIO;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import com.pasti_bisa.backend.modules.content.application.PdfExtractor;
import com.pasti_bisa.backend.modules.content.domain.ContentModels.ExtractedPage;

@Component
public class PdfBoxExtractor implements PdfExtractor {
    private final int maxPages;
    public PdfBoxExtractor(@Value("${pastisiap.import.max-pages:100}") int maxPages) { this.maxPages=maxPages; }
    @Override public List<ExtractedPage> extract(Path file) throws IOException {
        long deadline=System.nanoTime()+TimeUnit.MINUTES.toNanos(3);
        try(var document=Loader.loadPDF(file.toFile())) {
            if(document.isEncrypted()) throw new IOException("Encrypted PDFs are not supported");
            if(document.getNumberOfPages()<1||document.getNumberOfPages()>maxPages) throw new IOException("PDF page limit exceeded");
            var stripper=new PDFTextStripper();stripper.setSortByPosition(true);
            var renderer=new PDFRenderer(document);
            var result=new ArrayList<ExtractedPage>();
            int totalChars=0;
            for(int index=0;index<document.getNumberOfPages();index++) {
                if(System.nanoTime()>deadline) throw new IOException("PDF extraction time limit exceeded");
                stripper.setStartPage(index+1);stripper.setEndPage(index+1);
                String text=stripper.getText(document);
                boolean ocr=text.replaceAll("\\s","").length()<30;
                if(ocr) {
                    var box=document.getPage(index).getCropBox();
                    double pixels=box.getWidth()*box.getHeight()*Math.pow(120.0/72,2);
                    if(!Double.isFinite(pixels)||pixels>12000000) throw new IOException("PDF page render limit exceeded");
                    Path temp=Files.createTempDirectory("pastisiap-ocr-");
                    Process process=null;
                    try {
                        Path image=temp.resolve("page.png"),output=temp.resolve("text");
                        ImageIO.write(renderer.renderImageWithDPI(index,120),"png",image.toFile());
                        process=new ProcessBuilder("tesseract",image.toString(),output.toString(),"-l","ind+eng","--psm","6")
                                .redirectError(ProcessBuilder.Redirect.DISCARD).redirectOutput(ProcessBuilder.Redirect.DISCARD).start();
                        if(!process.waitFor(20,TimeUnit.SECONDS)) { process.destroyForcibly();throw new IOException("OCR page timeout"); }
                        if(process.exitValue()!=0) throw new IOException("OCR process failed");
                        text=Files.readString(temp.resolve("text.txt"));
                    } catch(InterruptedException exception) { Thread.currentThread().interrupt();throw new IOException("OCR interrupted",exception); }
                    finally {
                        if(process!=null&&process.isAlive()) process.destroyForcibly();
                        try(var paths=Files.list(temp)) { for(Path path:paths.toList()) Files.deleteIfExists(path); }
                        Files.deleteIfExists(temp);
                    }
                }
                totalChars+=text.length();
                if(text.length()>200000||totalChars>2000000) throw new IOException("Extracted PDF text limit exceeded");
                result.add(new ExtractedPage(index+1,text,ocr));
            }
            return result;
        }
    }
}
