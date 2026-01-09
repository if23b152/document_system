package at.technikum_wien.worker_service.listener;

import at.technikum_wien.worker_service.config.RabbitMQConfig;
import at.technikum_wien.worker_service.model.OcrRequestMessage;

import at.technikum_wien.worker_service.service.DocumentProcessingService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

// Marks this class as a Spring-managed component, so it can be auto-detected and used
@Component
public class MessageListener {

    private final DocumentProcessingService documentProcessingService;

    public MessageListener(DocumentProcessingService documentProcessingService) {
        this.documentProcessingService = documentProcessingService;
    }

    @RabbitListener(queues = RabbitMQConfig.QUEUE_NAME)
    public void handleOcrRequest(OcrRequestMessage message) {
        documentProcessingService.processDocument(message);
    }
}