package at.technikum_wien.worker_service.service;

import at.technikum_wien.worker_service.model.OcrResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class OcrServiceTest {

    // This is the OCR engine that will be mocked
    private TesseractOcrEngine ocrEngine;

    // This is the class under test
    private OcrService ocrService;

    @BeforeEach
    void setUp() {
        // Create a mock for the OCR engine
        ocrEngine = mock(TesseractOcrEngine.class);

        // Create the service with the mocked engine
        ocrService = new OcrService(ocrEngine);
    }

    // When OCR succeeds → result contains text and success=true
    @Test
    void performOcr_successful_setsSuccessAndText() throws Exception {
        // Define a fake document ID
        Long documentId = 1L;

        // Create a fake file object (does not need to exist)
        File file = new File("dummy.pdf");

        // Define the fake OCR output
        String extractedText = "This is the OCR text";

        // Mock the OCR engine to return text successfully
        when(ocrEngine.doOcr(file)).thenReturn(extractedText);

        // Call the method under test
        OcrResult result = ocrService.performOcr(documentId, file);

        // Verify that the document ID is set correctly
        assertEquals(documentId, result.getDocumentId());

        // Verify that the OCR text is set
        assertEquals(extractedText, result.getText());

        // Verify that success is true
        assertTrue(result.isSuccess());

        // Verify that error is null
        assertNull(result.getError());

        // Verify that the OCR engine was called exactly once
        verify(ocrEngine, times(1)).doOcr(file);
    }

    // When OCR throws exception → result contains error and success=false
    @Test
    void performOcr_failure_setsSuccessFalseAndError() throws Exception {
        // Define a fake document ID
        Long documentId = 2L;

        // Create a fake file object
        File file = new File("broken.pdf");

        // Define the exception message
        String errorMessage = "Tesseract crashed";

        // Mock the OCR engine to throw an exception
        when(ocrEngine.doOcr(file)).thenThrow(new RuntimeException(errorMessage));

        // Call the method under test (exception is handled internally)
        OcrResult result = ocrService.performOcr(documentId, file);

        // Verify that the document ID is still set
        assertEquals(documentId, result.getDocumentId());

        // Verify that success is false
        assertFalse(result.isSuccess());

        // Verify that text is null
        assertNull(result.getText());

        // Verify that error contains the exception message
        assertEquals(errorMessage, result.getError());

        // Verify that the OCR engine was called exactly once
        verify(ocrEngine, times(1)).doOcr(file);
    }
}
