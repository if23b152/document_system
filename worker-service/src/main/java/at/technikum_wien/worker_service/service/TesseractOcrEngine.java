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
 * Low-level wrapper around the Tesseract OCR library.
 * This class is only responsible for taking a file and extracting text from it.
 */
@Component // Makes this class a Spring-managed component
public class TesseractOcrEngine {

    // Logger for OCR engine related messages
    private static final Logger log = LoggerFactory.getLogger(TesseractOcrEngine.class);

    // Path to the folder that contains Tesseract language data (tessdata)
    private final String tessdataPath;

    // Injects the tessdata path from OcrConfig (bean named "tesseractPath")
    @Autowired
    public TesseractOcrEngine(@Qualifier("tesseractPath") String tessdataPath) {
        this.tessdataPath = tessdataPath;
    }

    /**
     * Runs Tesseract OCR on the given file and returns the extracted text.
     */
    public String doOcr(File file) throws TesseractException {
        // Create a new Tesseract instance
        Tesseract tesseract = new Tesseract();

        // Tell Tesseract where its language files are located
        tesseract.setDatapath(tessdataPath);

        // Set OCR language to English
        tesseract.setLanguage("eng");

        // Use the modern LSTM OCR engine
        tesseract.setOcrEngineMode(1);

        // Log which file is being processed
        log.info("Performing OCR on file: {}", file.getAbsolutePath());

        // Run OCR and return the extracted text
        return tesseract.doOCR(file);
    }
}