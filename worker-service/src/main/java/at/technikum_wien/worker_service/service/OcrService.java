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
 * Performs OCR (Optical Character Recognition) on PDF or image files using Tesseract.
 * Converts images or scanned PDFs into machine-readable text.
 */
@Service // Marks this class as a Spring service (for business logic managed by Spring)
public class OcrService {

    // Logger used to print messages and errors to the console or log file
    private static final Logger log = LoggerFactory.getLogger(OcrService.class);

    // Path to the Tesseract "tessdata" directory (where language files are stored)
    private final String tesseractPath;

    // Temporary folder where input files are stored during OCR processing
    private final File tempDir;

    // Constructor injection: Spring automatically provides tesseractPath and tempDir (from OcrConfig)
    public OcrService(String tesseractPath, File tempDir) {
        this.tesseractPath = tesseractPath;
        this.tempDir = tempDir;
    }

    /**
     * Performs OCR on the given input file stream.
     *
     * @param documentId   ID of the document (used for logging and tracking)
     * @param inputStream  The file input stream (e.g., a PDF downloaded from MinIO)
     * @return OcrResult   Contains the extracted text or an error message
     */
    public OcrResult performOcr(Long documentId, InputStream inputStream) {
        // Create a result object to hold OCR output and metadata
        OcrResult result = new OcrResult();
        result.setDocumentId(documentId);

        File tempFile = null; // Temporary local copy of the file used for OCR

        try {
            // 1. Save InputStream (the PDF) to a temporary file so Tesseract can read it
            tempFile = File.createTempFile("ocr_", ".pdf", tempDir);
            Files.copy(inputStream, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            log.info("Temporary file created for OCR: {}", tempFile.getAbsolutePath());

            // 2. Initialize the Tesseract OCR engine
            Tesseract tesseract = new Tesseract();
            tesseract.setDatapath(tesseractPath); // Location of trained language data files
            tesseract.setLanguage("eng"); // Use English; can be changed or combined (e.g. "eng+deu")
            tesseract.setOcrEngineMode(1); // Use the LSTM OCR engine (modern, more accurate)

            // 3. Perform OCR and extract text from the temporary file
            String extractedText = tesseract.doOCR(tempFile);
            result.setText(extractedText);
            result.setSuccess(true);

            // This log message confirms OCR success; may not always show immediately depending on logging settings
            log.info("OCR successfully completed for document ID {}", documentId);

        } catch (TesseractException e) {
            // Handles OCR-specific errors (e.g., Tesseract misconfiguration or corrupt file)
            log.error("Tesseract OCR failed for document ID {}: {}", documentId, e.getMessage(), e);
            result.setSuccess(false);
            result.setError("Tesseract error: " + e.getMessage());
        } catch (Exception e) {
            // Handles any unexpected runtime errors (e.g., I/O issues)
            log.error("Unexpected error during OCR for document ID {}: {}", documentId, e.getMessage(), e);
            result.setSuccess(false);
            result.setError("Unexpected error: " + e.getMessage());
        } finally {
            // 4. Delete the temporary file after OCR is done to free up space
            if (tempFile != null && tempFile.exists()) {
                boolean deleted = tempFile.delete();
                log.info("Temporary OCR file deleted: {} -> {}", tempFile.getAbsolutePath(), deleted);
            }
        }

        // Return the OCR result (text content or error message)
        return result;
    }
}
