package at.technikum_wien.rest_server.producer;

import at.technikum_wien.rest_server.config.RabbitMQConfig;
import at.technikum_wien.rest_server.model.OcrRequestMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Publishes messages to RabbitMQ after a document is saved.
 */
@Service
public class DocumentMessageProducer {

    private static final Logger log = LoggerFactory.getLogger(DocumentMessageProducer.class);

    private final RabbitTemplate rabbitTemplate;

    @Autowired
    public DocumentMessageProducer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    /**
     * Publishes a message with document metadata to the OCR queue for asynchronous processing.
     *
     * @param documentId The database ID of the document.
     * @param fileName The original filename.
     * @param minioObjectKey The MinIO object key (path in bucket).
     */
    public void sendOcrProcessingRequest(Long documentId, String fileName, String minioObjectKey) throws AmqpException {
        OcrRequestMessage message = new OcrRequestMessage(documentId, fileName, minioObjectKey);

        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.EXCHANGE_NAME,
                    RabbitMQConfig.OCR_ROUTING_KEY,
                    message
            );

            log.info("""
                    [PRODUCER SUCCESS] OCR message sent:
                    Exchange: {}
                    RoutingKey: {}
                    Payload: {}
                    """, RabbitMQConfig.EXCHANGE_NAME, RabbitMQConfig.OCR_ROUTING_KEY, message);

        } catch (AmqpException e) {
            log.error("[PRODUCER FAILURE] Failed to send OCR message for document ID {}: {}", documentId, e.getMessage(), e);
            throw e;
        }
    }
}
