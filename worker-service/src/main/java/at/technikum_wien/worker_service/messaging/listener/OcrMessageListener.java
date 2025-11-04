package at.technikum_wien.worker_service.messaging.listener;

import at.technikum_wien.worker_service.messaging.config.RabbitMQConfig;
import at.technikum_wien.worker_service.model.OcrRequestMessage;

import at.technikum_wien.worker_service.model.OcrResult;
import at.technikum_wien.worker_service.service.MinioService;
import at.technikum_wien.worker_service.service.OcrService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.InputStream;

@Component
public class OcrMessageListener {

    private static final Logger log = LoggerFactory.getLogger(OcrMessageListener.class);

    private final MinioService minioService;
    private final OcrService ocrService;

    public OcrMessageListener(MinioService minioService, OcrService ocrService) {
        this.minioService = minioService;
        this.ocrService = ocrService;
    }

    @RabbitListener(queues = RabbitMQConfig.QUEUE_NAME)
    public void handleOcrRequest(OcrRequestMessage message) {
        try {
            log.info("--- [LISTENER SUCCESS] Received OCR request: {} ---", message);

            // 1. Download PDF from MinIO
            InputStream pdfStream = minioService.downloadFile(message.getMinioObjectKey());

            // 2. Perform OCR
            OcrResult result = ocrService.performOcr(message.getDocumentId(), pdfStream);

            // 3. Log OCR result (later: update DB or forward to GenAI worker)
            if (result.isSuccess()) {
                log.info("--- [OCR SUCCESS] Document ID {} text length: {} ---",
                        result.getDocumentId(),
                        result.getText().length());
            } else {
                log.error("--- [OCR FAILURE] Document ID {} error: {} ---",
                        result.getDocumentId(),
                        result.getError());
            }

        } catch (Exception e) {
            log.error("--- [LISTENER ERROR] Failed to process OCR request: {}. Error: {} ---",
                    message, e.getMessage(), e);
        }
    }
}