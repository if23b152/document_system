package at.technikum_wien.rest_server.listener;

import at.technikum_wien.rest_server.model.ResultMessage;
import at.technikum_wien.rest_server.service.DocumentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class ResultMessageListener {

    private static final Logger log = LoggerFactory.getLogger(ResultMessageListener.class);

    private final DocumentService documentService;

    public ResultMessageListener(DocumentService documentService) {
        this.documentService = documentService;
    }

    /**
     * Listens for OCR + GenAI results coming back from the worker-service.
     */
    @RabbitListener(queues = "result-queue")
    public void handleWorkerResult(ResultMessage message) {
        Long documentId = message.getDocumentId();
        boolean success = message.isSuccess();

        log.info("Received worker result for document {}. Success: {}", documentId, success);

        if (!success) {
            log.error("Worker-service reported failure for document {}: {}", documentId, message.getError());
            documentService.markProcessingFailed(documentId, message.getError());
            return;
        }

        // --- OCR text ---
        String extractedText = message.getExtractedText();
        // --- GenAI summary (may be null if only GenAI failed) ---
        String summary = message.getSummary();

        log.info("Saving OCR text and GenAI summary for document {}", documentId);

        try {
            documentService.saveOcrAndSummary(documentId, summary);
            log.info("Successfully updated document {} with OCR + summary.", documentId);
        } catch (Exception e) {
            log.error("Failed to update document {}: {}", documentId, e.getMessage(), e);
        }
    }
}