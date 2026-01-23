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
    // SUCCESS CASE - calls saveOcrResult
    // =====================================
    @Test
    void process_success_callsSaveOcrResult() {
        ResultMessage msg = new ResultMessage();
        msg.setDocumentId(1L);
        msg.setSuccess(true);
        msg.setText("OCR text");
        msg.setSummary("Summary text");

        handler.process(msg);

        verify(documentService, times(1))
                .saveOcrResult(1L, "OCR text", "Summary text", true, null);
        verifyNoMoreInteractions(documentService);
    }

    // =====================================
    // FAILURE CASE - calls saveOcrResult
    // =====================================
    @Test
    void process_failure_callsSaveOcrResult() {
        ResultMessage msg = new ResultMessage();
        msg.setDocumentId(2L);
        msg.setSuccess(false);
        msg.setError("OCR failed");
        msg.setText(null);
        msg.setSummary(null);

        handler.process(msg);

        verify(documentService, times(1))
                .saveOcrResult(2L, null, null, false, "OCR failed");
        verifyNoMoreInteractions(documentService);
    }

    // =====================================
    // EXCEPTION CASE - saveOcrResult throws
    // =====================================
    @Test
    void process_saveOcrResultThrows_logsError() {
        ResultMessage msg = new ResultMessage();
        msg.setDocumentId(3L);
        msg.setSuccess(true);
        msg.setText("OCR text");
        msg.setSummary("Text");

        doThrow(new RuntimeException("DB down"))
                .when(documentService).saveOcrResult(3L, "OCR text", "Text", true, null);

        handler.process(msg);

        verify(documentService, times(1))
                .saveOcrResult(3L, "OCR text", "Text", true, null);

        // No exception should escape
        verifyNoMoreInteractions(documentService);
    }
}
