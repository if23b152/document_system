package at.technikum_wien.worker_service.service;

import at.technikum_wien.worker_service.model.OcrResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;

/**
 * Service responsible only for running OCR on a file and extracting text.
 * It does not handle downloads, uploads, or workflow orchestration.
 */
@Service
public class OcrService {

    // Logger for OCR-related messages
    private static final Logger log = LoggerFactory.getLogger(OcrService.class);

    // Wrapper around Tesseract OCR engine
    private final TesseractOcrEngine ocrEngine;

    // Constructor injection of the OCR engine
    public OcrService(TesseractOcrEngine ocrEngine) {
        this.ocrEngine = ocrEngine;
    }

    /**
     * Runs OCR on the given file and returns the result.
     */
    public OcrResult performOcr(Long documentId, File file) {
        OcrResult result = new OcrResult();
        result.setDocumentId(documentId); // Attach document ID to result

        try {
            // Start OCR process
            log.info("Starting OCR for document {}", documentId);

            // Extract text using Tesseract
            String text = ocrEngine.doOcr(file);

            // Store extracted text and mark as success
            result.setText(text);
            result.setSuccess(true);

            log.info("OCR successful for document {}", documentId);
        } catch (Exception e) {
            // Handle any OCR failure
            log.error("OCR failed for document {}: {}", documentId, e.getMessage(), e);
            result.setSuccess(false);
            result.setError(e.getMessage());
        }

        return result;
    }
}