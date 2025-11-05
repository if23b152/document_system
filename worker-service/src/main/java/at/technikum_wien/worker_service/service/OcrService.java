package at.technikum_wien.worker_service.service;

import at.technikum_wien.worker_service.model.OcrResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

/**
 * Performs OCR on PDF or image files using Tesseract.
 */
@Service
public class OcrService {

    private static final Logger log = LoggerFactory.getLogger(OcrService.class);

    private final String tesseractPath;
    private final File tempDir;

    // Spring will inject both beans from OcrConfig
    public OcrService(String tesseractPath, File tempDir) {
        this.tesseractPath = tesseractPath;
        this.tempDir = tempDir;
    }

    public OcrResult performOcr(Long documentId, InputStream inputStream) {
        OcrResult result = new OcrResult();
        result.setDocumentId(documentId);

        File tempFile = null;

        try {
            // Save InputStream to temporary file
            tempFile = File.createTempFile("ocr_", ".pdf", tempDir);
            Files.copy(inputStream, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            log.info("Temporary file created for OCR: {}", tempFile.getAbsolutePath());

            // Initialize Tesseract
            Tesseract tesseract = new Tesseract();
            tesseract.setDatapath(tesseractPath); // Use the bean from OcrConfig
            tesseract.setLanguage("eng"); // Can later extend to "eng+deu" if desired
            tesseract.setOcrEngineMode(1); // LSTM only

            // Perform OCR
            String extractedText = tesseract.doOCR(tempFile);
            result.setText(extractedText);
            result.setSuccess(true);

            // why is this not shown in the output in the terminal
            log.info("OCR successfully completed for document ID {}", documentId);

        } catch (TesseractException e) {
            log.error("Tesseract OCR failed for document ID {}: {}", documentId, e.getMessage(), e);
            result.setSuccess(false);
            result.setError("Tesseract error: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error during OCR for document ID {}: {}", documentId, e.getMessage(), e);
            result.setSuccess(false);
            result.setError("Unexpected error: " + e.getMessage());
        } finally {
            // Cleanup temporary file
            if (tempFile != null && tempFile.exists()) {
                boolean deleted = tempFile.delete();
                log.info("Temporary OCR file deleted: {} -> {}", tempFile.getAbsolutePath(), deleted);
            }
        }

        return result;
    }
}