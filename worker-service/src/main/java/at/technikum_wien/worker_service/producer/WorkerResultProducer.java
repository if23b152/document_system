package at.technikum_wien.worker_service.producer;

import at.technikum_wien.worker_service.config.RabbitMQConfig;
import at.technikum_wien.worker_service.model.ResultMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

/**
 * Sends processing results from the worker back to the REST server via RabbitMQ.
 * This is used after OCR and AI summarization finished (success or failure).
 */
@Service
public class WorkerResultProducer {

    // Logger for printing info and error messages
    private static final Logger log = LoggerFactory.getLogger(WorkerResultProducer.class);

    // Spring helper class for sending messages to RabbitMQ
    private final RabbitTemplate rabbitTemplate;

    // RabbitTemplate is injected by Spring
    public WorkerResultProducer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    /**
     * Sends the OCR/GenAI result back to the REST server via RabbitMQ.
     */
    public void sendResult(ResultMessage message) {
        try {
            // Send the message to the exchange using the result routing key
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.EXCHANGE_NAME,
                    RabbitMQConfig.RESULT_ROUTING_KEY,
                    message
            );

            // Log success
            int textLength = message.getText() != null ? message.getText().length() : 0;
            log.info("=== [WORKER RESULT SENT] Document {} | success: {} | textLength: {} ===",
                    message.getDocumentId(),
                    message.isSuccess(),
                    textLength
            );

        } catch (AmqpException e) {
            // Log failure (we do not crash the worker because of this)
            log.error("=== [WORKER RESULT FAILURE] Failed to send result for Document {}: {} ===",
                    message.getDocumentId(),
                    e.getMessage(),
                    e
            );
        }
    }
}
