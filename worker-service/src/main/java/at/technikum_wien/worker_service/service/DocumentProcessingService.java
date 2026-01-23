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

/**
 * Central orchestration service of the worker.
 * It processes documents received from RabbitMQ:
 * download from MinIO → OCR → AI summary → Elasticsearch indexing → send result back.
 */
@Service
public class DocumentProcessingService {

    private static final Logger log = LoggerFactory.getLogger(DocumentProcessingService.class);

    private final MinioService minioService;                 // Downloads files from MinIO
    private final OcrService ocrService;                     // Performs OCR on PDF files
    private final GenAiService genAiService;                 // Generates AI summaries from text
    private final WorkerResultProducer resultProducer;       // Sends results back to the REST server
    private final File tempDir;                              // Directory for temporary OCR files
    private final ElasticsearchService elasticsearchService; // Indexes documents for search

    @Autowired
    public DocumentProcessingService(MinioService minioService,
                                     OcrService ocrService,
                                     GenAiService genAiService,
                                     WorkerResultProducer resultProducer,
                                     ElasticsearchService elasticsearchService,
                                     @Qualifier("tempOcrDir") File tempDir) {
        this.minioService = minioService;
        this.ocrService = ocrService;
        this.genAiService = genAiService;
        this.resultProducer = resultProducer;
        this.elasticsearchService = elasticsearchService;
        this.tempDir = tempDir;
    }

    // Main entry point for processing a document received from the queue
    public void processDocument(OcrRequestMessage message) {
        File tempFile = null;                       // Temporary local copy of the PDF
        Long documentId = message.getDocumentId();  // ID of the document being processed
        ResultMessage finalResult = null;           // Final result sent back to REST server

        log.info("Starting processing for document {} (minioObjectKey={})",
                documentId, message.getMinioObjectKey());

        try (InputStream pdfStream = minioService.downloadFile(message.getMinioObjectKey())) {

            // Create a temporary file to store the downloaded PDF
            tempFile = File.createTempFile("ocr_", ".pdf", tempDir);
            Files.copy(pdfStream, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

            // Run OCR on the PDF file
            OcrResult ocrResult = ocrService.performOcr(documentId, tempFile);

            if (ocrResult.isSuccess()) {
                log.info("OCR successful for document {}. Starting GenAI summary generation.", documentId);

                String summary;
                try {
                    // Generate AI summary from extracted text
                    summary = genAiService.generateSummary(ocrResult.getText());
                    log.info("GenAI summary generated for document {}. Summary length: {} characters.",
                            documentId, summary.length());

                    // Build success result (OCR + summary)
                    finalResult = new ResultMessage(documentId, ocrResult.getText(), summary, true, null);

                    // Index the document in Elasticsearch for search
                    elasticsearchService.indexDocument(
                            new SearchDocument(documentId, message.getFileName(),
                                    ocrResult.getText(), summary)
                    );

                } catch (Exception genAiException) {
                    // Case: OCR succeeded, but summary generation failed
                    log.error("GenAI summary generation failed for document {}: {}",
                            documentId, genAiException.getMessage(), genAiException);

                    finalResult = new ResultMessage(
                            documentId,
                            ocrResult.getText(),   // OCR text is available
                            null,                  // Summary is missing
                            false,
                            "GenAI failed: " + genAiException.getMessage()
                    );
                }

            } else {
                // Case: OCR itself failed
                log.error("OCR failed for document {}: {}", documentId, ocrResult.getError());

                finalResult = new ResultMessage(
                        documentId,
                        null,   // No OCR text
                        null,   // No summary
                        false,
                        "OCR failed: " + ocrResult.getError()
                );
            }

        } catch (Exception e) {
            // Case: MinIO download, file I/O, or unexpected failure
            log.error("Failed to process document {} due to an unexpected error: {}",
                    documentId, e.getMessage(), e);

            finalResult = new ResultMessage(
                    documentId,
                    null,
                    null,
                    false,
                    "Worker processing failed: " + e.getMessage()
            );

        } finally {
            // Always send the result back to the REST server (success or failure)
            if (finalResult != null) {
                resultProducer.sendResult(finalResult);
                log.info("Final result message sent to REST server for document {}. Success: {}.",
                        documentId, finalResult.isSuccess());
            }

            // Delete the temporary file from the disk
            if (tempFile != null && tempFile.exists()) {
                boolean deleted = tempFile.delete();
                if (!deleted) {
                    log.warn("Failed to delete temporary file {}", tempFile.getAbsolutePath());
                }
            }
        }
    }
}
