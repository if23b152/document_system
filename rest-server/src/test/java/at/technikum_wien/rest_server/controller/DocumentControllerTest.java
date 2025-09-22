package at.technikum_wien.rest_server.controller;

import at.technikum_wien.rest_server.model.Document;
import at.technikum_wien.rest_server.service.DocumentService;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Unit tests for DocumentController.
 * Service is mocked → no real database or business logic is used.
 */
public class DocumentControllerTest {
    @Mock
    private DocumentService documentService; // fake service

    @InjectMocks
    private DocumentController documentController; // controller under test

    public DocumentControllerTest() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void uploadDocument_shouldReturnCreated() throws Exception {
        // Arrange → simulate file upload (PDF)
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.pdf", "application/pdf", "dummy content".getBytes()
        );

        // Prepare a fake saved document
        Document saved = new Document();
        saved.setId(1L);
        saved.setFileName("test.pdf");

        when(documentService.saveDocument(anyString(), anyLong(), anyString())).thenReturn(saved);

        // Act → call controller upload endpoint
        ResponseEntity<Document> response = documentController.uploadDocument(file);

        // Assert → HTTP 201 CREATED and correct document returned
        assertThat(response.getStatusCodeValue()).isEqualTo(201);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getId()).isEqualTo(1L);

        // Verify service was called exactly once
        verify(documentService, times(1)).saveDocument(anyString(), anyLong(), anyString());
    }

    @Test
    void getDocumentById_shouldReturnNotFound() {
        // Arrange → service returns nothing
        when(documentService.getDocumentById(99L)).thenReturn(Optional.empty());

        // Act → call controller
        ResponseEntity<Document> response = documentController.getDocumentById(99L);

        // Assert → should return HTTP 404 Not Found
        assertThat(response.getStatusCodeValue()).isEqualTo(404);
    }
}
