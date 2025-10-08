package at.technikum_wien.rest_server.messaging.producer;

import at.technikum_wien.rest_server.messaging.config.RabbitMQConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Service responsible for publishing messages to the RabbitMQ exchange.
 * This producer is triggered after a document is successfully saved to the database.
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
     * Publishes the document ID to the OCR queue for asynchronous processing.
     * The worker service will consume this message and start the OCR process.
     *
     * @param documentId The ID of the newly saved document.
     */
    public void sendOcrProcessingRequest(Long documentId) throws AmqpException {
        // Convert the Long ID to a String for the message payload
        String messagePayload = String.valueOf(documentId);

        try {
            // Send the message to the defined exchange with the specific routing key
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.EXCHANGE_NAME,
                    RabbitMQConfig.ROUTING_KEY,
                    messagePayload
            );

            // Logging in remarkable/critical positions integrated (SUCCESS)
            log.info("===== [PRODUCER SUCCESS] Message sent successfully. Exchange: {}, Key: {}, Payload: {}=====",
                    RabbitMQConfig.EXCHANGE_NAME, RabbitMQConfig.ROUTING_KEY, messagePayload);

        } catch (AmqpException e) {
            // Failure/exception-handling with layer-specific exceptions (AmqpException is Spring AMQP layer-specific)
            log.error("===== [PRODUCER FAILURE] Failed to send OCR message for document ID: {}. Error: {} =====",
                    documentId, e.getMessage(), e);

            // Re-throw the layer-specific exception to be handled by the DocumentService
            throw e;
        }
    }
}
