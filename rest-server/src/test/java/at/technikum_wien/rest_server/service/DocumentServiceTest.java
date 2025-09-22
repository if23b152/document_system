package at.technikum_wien.rest_server.service;

import at.technikum_wien.rest_server.model.Document;
import at.technikum_wien.rest_server.repository.DocumentRepository;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Unit tests for DocumentService.
 * Repository is mocked → no real database is touched.
 */
public class DocumentServiceTest {
    @Mock
    private DocumentRepository documentRepository; // fake DB repo

    @InjectMocks
    private DocumentService documentService; // service under test

    // Initialize Mockito annotations before each test
    public DocumentServiceTest() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void saveDocument_shouldReturnSavedDocument() {
        // Arrange → prepare a fake document to return from repository
        Document mockDoc = new Document();
        mockDoc.setId(1L);
        mockDoc.setFileName("test.pdf");
        mockDoc.setFileSize(12345L);
        mockDoc.setStoragePath("test.pdf");
        mockDoc.setUploadTimestamp(LocalDateTime.now());

        // Stub repository save() to always return mockDoc
        when(documentRepository.save(any(Document.class))).thenReturn(mockDoc);

        // Act → call service method
        Document saved = documentService.saveDocument("test.pdf", 12345L, "test.pdf");

        // Assert → verify correct values and interactions
        assertThat(saved.getId()).isEqualTo(1L);
        assertThat(saved.getFileName()).isEqualTo("test.pdf");
        verify(documentRepository, times(1)).save(any(Document.class));
    }

    @Test
    void getDocumentById_shouldReturnOptional() {
        // Arrange → prepare fake return value
        Document doc = new Document();
        doc.setId(99L);

        when(documentRepository.findById(99L)).thenReturn(Optional.of(doc));

        // Act → call service
        Optional<Document> result = documentService.getDocumentById(99L);

        // Assert → result should contain document
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(99L);
    }
}
