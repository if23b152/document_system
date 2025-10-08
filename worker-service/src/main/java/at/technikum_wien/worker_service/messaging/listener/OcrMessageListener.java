package at.technikum_wien.worker_service.messaging.listener;

import at.technikum_wien.worker_service.messaging.config.RabbitMQConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Message listener for the OCR processing queue.
 * This component consumes messages published by the document-service.
 */
@Component
public class OcrMessageListener {

    private static final Logger log = LoggerFactory.getLogger(OcrMessageListener.class);

    /**
     * Listens for messages on the defined OCR queue.
     * The message payload is expected to be the ID of the document needing OCR processing.
     * * @param documentId The ID of the document received as a String payload.
     */
    @RabbitListener(queues = RabbitMQConfig.QUEUE_NAME)
    public void handleOcrRequest(String documentId) {
        // Use a try-catch to ensure that if processing fails, RabbitMQ receives an acknowledgement
        // and does not redeliver the message infinitely.
        try {
            // --- SPRINT 3 REQUIREMENT: Simply log the receipt of the message (SUCCESS LOGGING) ---
            log.info("--- [LISTENER SUCCESS] Received OCR request for Document ID: {}. Starting worker processing. ---", documentId);

            // In SPRINT 4, the logic here will change to:
            // 1. Fetch document from DB using the ID.
            // 2. Call ocrProcessingService.process(document).

            // Placeholder for future SPRINT 4 logic (e.g., throwing an error if ID is invalid)
            // if (documentId.length() < 1) {
            //     throw new IllegalArgumentException("Received empty document ID.");
            // }

        } catch (Exception e) {
            // Failure/exception-handling integrated (catching any unexpected runtime error)
            // CRITICAL LOGGING: This failure must be logged for manual intervention.
            log.error("--- [LISTENER ERROR] Failed to process OCR request for Document ID: {}. Message will NOT be re-queued. Error: {} ---",
                    documentId, e.getMessage(), e);

            // DO NOT re-throw: By returning normally, Spring AMQP acknowledges the message,
            // even though processing failed, preventing infinite redelivery.
        }
    }
}