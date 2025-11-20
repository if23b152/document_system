package at.technikum_wien.worker_service.producer;

import at.technikum_wien.worker_service.config.RabbitMQConfig;
import at.technikum_wien.worker_service.model.ResultMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
public class WorkerResultProducer {

    private static final Logger log = LoggerFactory.getLogger(WorkerResultProducer.class);

    private final RabbitTemplate rabbitTemplate;

    public WorkerResultProducer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    /**
     * Sends the OCR/GenAI result back to the REST server via RabbitMQ.
     */
    public void sendResult(ResultMessage message) {
        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.EXCHANGE_NAME,
                    RabbitMQConfig.RESULT_ROUTING_KEY,
                    message
            );

            log.info("=== [WORKER RESULT SENT] Document {} | success: {} ===",
                    message.getDocumentId(),
                    message.isSuccess()
            );

        } catch (AmqpException e) {
            log.error("=== [WORKER RESULT FAILURE] Failed to send result for Document {}: {} ===",
                    message.getDocumentId(),
                    e.getMessage(),
                    e
            );
        }
    }
}