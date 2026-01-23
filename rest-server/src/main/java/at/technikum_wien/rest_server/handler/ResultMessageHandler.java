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

            // Save OCR result state and error (if any text exists, still persist it)
            documentService.saveOcrResult(
                    documentId,
                    message.getText(),
                    message.getSummary(),
                    false,
                    message.getError()
            );
            int textLength = message.getText() != null ? message.getText().length() : 0;
            log.info("Stored failed OCR result for document {} (textLength={}).",
                    documentId, textLength);
            return;
        }

        try {
            // Save OCR text + summary and mark OCR as processed
            documentService.saveOcrResult(
                    documentId,
                    message.getText(),
                    message.getSummary(),
                    true,
                    null
            );
            int textLength = message.getText() != null ? message.getText().length() : 0;
            log.info("Successfully updated document {} with OCR + summary. textLength={}",
                    documentId, textLength);

        } catch (Exception e) {
            // Log any unexpected error during DB update
            log.error("Failed to update document {}: {}", documentId, e.getMessage(), e);
        }
    }
}
