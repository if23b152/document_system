package at.technikum_wien.rest_server.listener;

import at.technikum_wien.rest_server.handler.ResultMessageHandler;
import at.technikum_wien.rest_server.model.ResultMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;

public class ResultMessageListenerTest {

    private ResultMessageHandler handler;
    private ResultMessageListener listener;

    @BeforeEach
    void setUp() {
        handler = mock(ResultMessageHandler.class);
        listener = new ResultMessageListener(handler);
    }

    @Test
    void handleWorkerResult_callsProcess() {
        // Arrange
        ResultMessage message = new ResultMessage();
        message.setDocumentId(42L);
        message.setSuccess(true);
        message.setSummary("Test summary");

        // Act
        listener.handleWorkerResult(message);

        // Assert
        verify(handler, times(1)).process(message);
        verifyNoMoreInteractions(handler);
    }
}
