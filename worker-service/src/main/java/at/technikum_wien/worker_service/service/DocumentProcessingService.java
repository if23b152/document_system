package at.technikum_wien.worker_service.service;

import at.technikum_wien.worker_service.model.OcrRequestMessage;
import at.technikum_wien.worker_service.model.OcrResult;
import at.technikum_wien.worker_service.model.ResultMessage;
import at.technikum_wien.worker_service.model.SearchDocument;
import at.technikum_wien.worker_service.producer.WorkerResultProducer;
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
    private final GenAiService genAiService;
    private final WorkerResultProducer resultProducer;
    private final File tempDir;
    private final ElasticsearchService elasticsearchService;

    @Autowired
    public DocumentProcessingService(MinioService minioService, OcrService ocrService, GenAiService genAiService,
                                     WorkerResultProducer resultProducer, ElasticsearchService elasticsearchService,
                                     @Qualifier("tempOcrDir") File tempDir) {
        this.minioService = minioService;
        this.ocrService = ocrService;
        this.genAiService = genAiService;
        this.resultProducer = resultProducer;
        this.elasticsearchService = elasticsearchService;
        this.tempDir = tempDir;
    }

    public void processDocument(OcrRequestMessage message) {
        File tempFile = null;
        Long documentId = message.getDocumentId();
        ResultMessage finalResult = null; // Prepare a final result object

        try (InputStream pdfStream = minioService.downloadFile(message.getMinioObjectKey())) {

            tempFile = File.createTempFile("ocr_", ".pdf", tempDir);
            Files.copy(pdfStream, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

            OcrResult ocrResult = ocrService.performOcr(documentId, tempFile);

            if (ocrResult.isSuccess()) {
                log.info("OCR successful for document {}. Starting GenAI summary generation.", documentId);

                String summary;
                try {
                    // --- Sprint 5: Call GenAI Service ---
                    summary = genAiService.generateSummary(ocrResult.getText());
                    log.info("GenAI summary generated for document {}. Summary length: {} characters.", documentId,
                            summary.length());

                    // Full Success Path
                    finalResult = new ResultMessage(documentId, ocrResult.getText(), summary, true, null);

                    // --- Sprint 6: Index document in Elasticsearch ---
                    elasticsearchService.indexDocument(new SearchDocument(documentId, message.getFileName(),
                            ocrResult.getText(), summary));

                } catch (Exception genAiException) {
                    // GenAI Failure Path (OCR was successful, but summary failed)
                    log.error("GenAI summary generation failed for document {}: {}", documentId,
                            genAiException.getMessage(), genAiException);
                    finalResult = new ResultMessage(documentId, ocrResult.getText(),
                            null, // Summary is null
                            false, "GenAI failed: " + genAiException.getMessage()
                    );
                }

            } else {
                // OCR Failure Path
                log.error("OCR failed for document {}: {}", documentId, ocrResult.getError());
                finalResult = new ResultMessage(documentId,
                        null, // Text is null
                        null, // Summary is null
                        false, "OCR failed: " + ocrResult.getError()
                );
            }

        } catch (Exception e) {
            // General Failure Path (e.g., MinIO download, file I/O)
            log.error("Failed to process document {} due to an unexpected error: {}", documentId, e.getMessage(), e);
            finalResult = new ResultMessage(documentId, null, null, false,
                    "Worker processing failed: " + e.getMessage()
            );

        } finally {
            // --- Sprint 5: Send the final result back to REST server ---
            if (finalResult != null) {
                resultProducer.sendResult(finalResult);
                log.info("Final result message sent to REST server for document {}. Success: {}.",
                        documentId, finalResult.isSuccess());
            }

            // Cleanup
            if (tempFile != null && tempFile.exists()) {
                boolean deleted = tempFile.delete();
                if (!deleted) {
                    log.warn("Failed to delete temporary file {}", tempFile.getAbsolutePath());
                }
            }
        }
    }
}
