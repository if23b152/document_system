package at.technikum_wien.worker_service.service;

import at.technikum_wien.worker_service.model.OcrResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;

/**
 * Performs OCR using TesseractOcrEngine.
 * Only responsible for converting a file to text.
 * All orchestration (downloads, temp file management) is handled elsewhere.
 */
@Service
public class OcrService {

    private static final Logger log = LoggerFactory.getLogger(OcrService.class);

    private final TesseractOcrEngine ocrEngine;

    public OcrService(TesseractOcrEngine ocrEngine) {
        this.ocrEngine = ocrEngine;
    }

    public OcrResult performOcr(Long documentId, File file) {
        OcrResult result = new OcrResult();
        result.setDocumentId(documentId);

        try {
            log.info("Starting OCR for document {}", documentId);
            String text = ocrEngine.doOcr(file);
            result.setText(text);
            result.setSuccess(true);
            log.info("OCR successful for document {}", documentId);
        } catch (Exception e) {
            log.error("OCR failed for document {}: {}", documentId, e.getMessage(), e);
            result.setSuccess(false);
            result.setError(e.getMessage());
        }

        return result;
    }
}