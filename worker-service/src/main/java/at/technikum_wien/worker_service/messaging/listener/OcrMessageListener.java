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
        // --- SPRINT 3 REQUIREMENT: Simply log the receipt of the message ---
        log.info("--- SPRINT 3 SUCCESS ---");
        log.info("Received OCR request for Document ID: {}. Triggering worker processing.", documentId);

        // In SPRINT 4, the logic here will change to:
        // 1. Fetch document from DB using the ID.
        // 2. Call ocrProcessingService.process(document).

        // For now, the message is consumed, and the worker confirms successful flow.
    }
}