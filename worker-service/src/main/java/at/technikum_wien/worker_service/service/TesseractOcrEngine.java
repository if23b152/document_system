package at.technikum_wien.worker_service.service;

import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.io.File;

/**
 * Encapsulates direct Tesseract OCR operations.
 */
@Component
public class TesseractOcrEngine {

    private static final Logger log = LoggerFactory.getLogger(TesseractOcrEngine.class);

    private final String tessdataPath;

    @Autowired
    public TesseractOcrEngine(@Qualifier("tesseractPath") String tessdataPath) {
        this.tessdataPath = tessdataPath;
    }

    public String doOcr(File file) throws TesseractException {
        Tesseract tesseract = new Tesseract();
        tesseract.setDatapath(tessdataPath);
        tesseract.setLanguage("eng");
        tesseract.setOcrEngineMode(1); // LSTM engine
        log.info("Performing OCR on file: {}", file.getAbsolutePath());
        return tesseract.doOCR(file);
    }
}
