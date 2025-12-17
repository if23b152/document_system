package at.technikum_wien.worker_service.service;

import at.technikum_wien.worker_service.model.SearchDocument;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(MockitoExtension.class)
class ElasticsearchServiceTest {

    @Mock
    private ElasticsearchOperations elasticsearchOperations;

    @InjectMocks
    private ElasticsearchService elasticsearchService;

    @Test
    void indexDocument_shouldSaveDocumentToElasticsearch() {
        // Arrange
        SearchDocument document = new SearchDocument(
                1L,
                "HelloWorld.pdf",
                "Hello World OCR text",
                "Hello World summary"
        );

        // Act
        elasticsearchService.indexDocument(document);

        // Assert
        verify(elasticsearchOperations, times(1)).save(document);
    }
}