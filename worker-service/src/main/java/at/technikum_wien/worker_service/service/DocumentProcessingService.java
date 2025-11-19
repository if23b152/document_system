package at.technikum_wien.worker_service.service;

import at.technikum_wien.worker_service.model.OcrRequestMessage;
import at.technikum_wien.worker_service.model.OcrResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

@Service
public class DocumentProcessingService {

    private static final Logger log = LoggerFactory.getLogger(DocumentProcessingService.class);

    private final MinioService minioService;
    private final OcrService ocrService;
    private final File tempDir;

    @Autowired
    public DocumentProcessingService(MinioService minioService, OcrService ocrService,
                                     @Qualifier("tempOcrDir") File tempDir) {
        this.minioService = minioService;
        this.ocrService = ocrService;
        this.tempDir = tempDir;
    }

    public void processDocument(OcrRequestMessage message) {
        File tempFile = null;

        try (InputStream pdfStream = minioService.downloadFile(message.getMinioObjectKey())) {

            tempFile = File.createTempFile("ocr_", ".pdf", tempDir);
            Files.copy(pdfStream, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

            OcrResult ocrResult = ocrService.performOcr(message.getDocumentId(), tempFile);

            if (!ocrResult.isSuccess()) {
                log.error("OCR failed for document {}: {}", message.getDocumentId(), ocrResult.getError());
            }

        } catch (Exception e) {
            log.error("Failed to process document {}: {}", message.getDocumentId(), e.getMessage(), e);
        } finally {
            if (tempFile != null && tempFile.exists()) {
                boolean deleted = tempFile.delete();
                if (!deleted) {
                    log.warn("Failed to delete temporary file {}", tempFile.getAbsolutePath());
                }
            }
        }
    }
}
