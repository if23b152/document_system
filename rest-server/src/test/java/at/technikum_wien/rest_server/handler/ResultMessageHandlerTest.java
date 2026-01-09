package at.technikum_wien.rest_server.handler;

import at.technikum_wien.rest_server.model.ResultMessage;
import at.technikum_wien.rest_server.service.DocumentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;

public class ResultMessageHandlerTest {

    private DocumentService documentService;
    private ResultMessageHandler handler;

    @BeforeEach
    void setUp() {
        documentService = mock(DocumentService.class);
        handler = new ResultMessageHandler(documentService);
    }

    // =====================================
    // SUCCESS CASE → calls saveSummary
    // =====================================
    @Test
    void process_success_callsSaveSummary() {
        ResultMessage msg = new ResultMessage();
        msg.setDocumentId(1L);
        msg.setSuccess(true);
        msg.setSummary("Summary text");

        handler.process(msg);

        verify(documentService, times(1))
                .saveSummary(1L, "Summary text");
        verifyNoMoreInteractions(documentService);
    }

    // =====================================
    // FAILURE CASE → calls markProcessingFailed
    // =====================================
    @Test
    void process_failure_callsMarkProcessingFailed() {
        ResultMessage msg = new ResultMessage();
        msg.setDocumentId(2L);
        msg.setSuccess(false);
        msg.setError("OCR failed");

        handler.process(msg);

        verify(documentService, times(1))
                .markProcessingFailed(2L, "OCR failed");
        verifyNoMoreInteractions(documentService);
    }

    // =====================================
    // EXCEPTION CASE → saveSummary throws
    // =====================================
    @Test
    void process_saveSummaryThrows_logsError() {
        ResultMessage msg = new ResultMessage();
        msg.setDocumentId(3L);
        msg.setSuccess(true);
        msg.setSummary("Text");

        doThrow(new RuntimeException("DB down"))
                .when(documentService).saveSummary(3L, "Text");

        handler.process(msg);

        verify(documentService, times(1))
                .saveSummary(3L, "Text");

        // No exception should escape
        verifyNoMoreInteractions(documentService);
    }
}
