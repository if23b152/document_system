package at.technikum_wien.worker_service.listener;

import at.technikum_wien.worker_service.config.RabbitMQConfig;
import at.technikum_wien.worker_service.model.OcrRequestMessage;

import at.technikum_wien.worker_service.model.OcrResult;
import at.technikum_wien.worker_service.service.MinioService;
import at.technikum_wien.worker_service.service.OcrService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.InputStream;

// Marks this class as a Spring-managed component so it can be auto-detected and used
@Component
public class OcrMessageListener {

    // Logger for writing info and error messages to the console or log file
    private static final Logger log = LoggerFactory.getLogger(OcrMessageListener.class);

    // Service for interacting with MinIO (file storage)
    private final MinioService minioService;

    // Service for performing OCR (text extraction from images or PDFs)
    private final OcrService ocrService;

    // Constructor-based dependency injection for MinioService and OcrService
    public OcrMessageListener(MinioService minioService, OcrService ocrService) {
        this.minioService = minioService;
        this.ocrService = ocrService;
    }

    // Method that listens for messages from a RabbitMQ queue
    // The queue name is defined in RabbitMQConfig.QUEUE_NAME
    @RabbitListener(queues = RabbitMQConfig.QUEUE_NAME)
    public void handleOcrRequest(OcrRequestMessage message) {
        try {
            // Log that a new OCR request has been received
            log.info("--- [LISTENER SUCCESS] Received OCR request: {} ---", message);

            // 1. Download the PDF file from MinIO using the object key provided in the message
            InputStream pdfStream = minioService.downloadFile(message.getMinioObjectKey());

            // 2. Perform OCR on the downloaded PDF
            // The OcrService extracts the text content from the document
            OcrResult result = ocrService.performOcr(message.getDocumentId(), pdfStream);

            // 3. Check if the OCR succeeded and log the result
            // Later, this result could also be saved to a database or sent to another worker
            if (result.isSuccess()) {
                log.info("--- [OCR SUCCESS] Document ID {} text length: {} ---",
                        result.getDocumentId(),
                        result.getText().length());
            } else {
                // Log the failure and the reason for it
                log.error("--- [OCR FAILURE] Document ID {} error: {} ---",
                        result.getDocumentId(),
                        result.getError());
            }

        } catch (Exception e) {
            // Catch any unexpected error (network issues, missing file, etc.)
            // and log it for debugging
            log.error("--- [LISTENER ERROR] Failed to process OCR request: {}. Error: {} ---",
                    message, e.getMessage(), e);
        }
    }
}
