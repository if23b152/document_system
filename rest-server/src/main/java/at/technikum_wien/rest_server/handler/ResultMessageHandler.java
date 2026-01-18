package at.technikum_wien.rest_server.handler;

import at.technikum_wien.rest_server.model.ResultMessage;
import at.technikum_wien.rest_server.service.DocumentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service // Spring service that processes worker results
public class ResultMessageHandler {

    private static final Logger log = LoggerFactory.getLogger(ResultMessageHandler.class);

    private final DocumentService documentService; // Used to update document in DB

    // Constructor injection
    public ResultMessageHandler(DocumentService documentService) {
        this.documentService = documentService;
    }

    // Process a result message from the OCR/GenAI worker
    public void process(ResultMessage message) {
        Long documentId = message.getDocumentId();

        // Handle failure case reported by worker
        if (!message.isSuccess()) {
            log.error("Worker-service reported failure for document {}: {}",
                    documentId, message.getError());

            // Mark the document as failed in DB (for retry or alert)
            documentService.markProcessingFailed(documentId, message.getError());
            return;
        }

        try {
            // Save AI-generated summary and mark OCR as processed
            documentService.saveSummary(documentId, message.getSummary());
            log.info("Successfully updated document {} with OCR + summary.", documentId);

        } catch (Exception e) {
            // Log any unexpected error during DB update
            log.error("Failed to update document {}: {}", documentId, e.getMessage(), e);
        }
    }
}