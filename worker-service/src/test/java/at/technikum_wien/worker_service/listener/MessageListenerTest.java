package at.technikum_wien.worker_service.listener;

import at.technikum_wien.worker_service.model.OcrRequestMessage;
import at.technikum_wien.worker_service.service.DocumentProcessingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;

public class MessageListenerTest {

    // This is the service that will be mocked
    private DocumentProcessingService documentProcessingService;

    // This is the class under test
    private MessageListener messageListener;

    @BeforeEach
    void setUp() {
        // Create a mock for the DocumentProcessingService
        documentProcessingService = mock(DocumentProcessingService.class);

        // Create the listener with the mocked service
        messageListener = new MessageListener(documentProcessingService);
    }

    // When a message is received → service is called
    @Test
    void handleOcrRequest_callsProcessDocument() {
        // Create a fake OCR request message
        OcrRequestMessage message = new OcrRequestMessage(
                1L,
                "test.pdf",
                "minio/key/test.pdf"
        );

        // Call the method under test (simulates RabbitMQ delivering a message)
        messageListener.handleOcrRequest(message);

        // Verify that the service was called exactly once with the same message
        verify(documentProcessingService, times(1))
                .processDocument(message);

        // Verify that no other interactions happened
        verifyNoMoreInteractions(documentProcessingService);
    }
}
