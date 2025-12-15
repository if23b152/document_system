package at.technikum_wien.rest_server.handler;

import at.technikum_wien.rest_server.model.ResultMessage;
import at.technikum_wien.rest_server.service.DocumentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ResultMessageHandler {

    private static final Logger log = LoggerFactory.getLogger(ResultMessageHandler.class);

    private final DocumentService documentService;

    public ResultMessageHandler(DocumentService documentService) {
        this.documentService = documentService;
    }

    public void process(ResultMessage message) {
        Long documentId = message.getDocumentId();

        if (!message.isSuccess()) {
            log.error("Worker-service reported failure for document {}: {}",
                    documentId, message.getError());
            documentService.markProcessingFailed(documentId, message.getError());
            return;
        }

        try {
            documentService.saveSummary(documentId, message.getSummary());
            log.info("Successfully updated document {} with OCR + summary.", documentId);
        } catch (Exception e) {
            log.error("Failed to update document {}: {}", documentId, e.getMessage(), e);
        }
    }
}
