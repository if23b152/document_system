package at.technikum_wien.worker_service.service;

import at.technikum_wien.worker_service.model.SearchDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;

import static org.mockito.Mockito.*;

public class ElasticsearchServiceTest {

    // This is the Elasticsearch client that will be mocked
    private ElasticsearchOperations elasticsearchOperations;

    // This is the class under test
    private ElasticsearchService elasticsearchService;

    @BeforeEach
    void setUp() {
        // Create a mock for ElasticsearchOperations
        elasticsearchOperations = mock(ElasticsearchOperations.class);

        // Create the service with the mocked dependency
        elasticsearchService = new ElasticsearchService(elasticsearchOperations);
    }

    // When indexing succeeds → save() is called once
    @Test
    void indexDocument_success_callsSave() {
        // Create a fake search document
        SearchDocument document = new SearchDocument(
                1L,
                "file.pdf",
                "some ocr text",
                "some summary"
        );

        // Call the method under test
        elasticsearchService.indexDocument(document);

        // Verify that Elasticsearch save() was called exactly once with this document
        verify(elasticsearchOperations, times(1)).save(document);

        // Verify that no other Elasticsearch calls were made
        verifyNoMoreInteractions(elasticsearchOperations);
    }

    // When Elasticsearch throws → exception is swallowed
    @Test
    void indexDocument_failure_doesNotThrow() {
        // Create a fake search document
        SearchDocument document = new SearchDocument(
                2L,
                "broken.pdf",
                "text",
                "summary"
        );

        // Mock Elasticsearch to throw an exception
        doThrow(new RuntimeException("Elasticsearch down"))
                .when(elasticsearchOperations)
                .save(document);

        // Call the method under test and verify that NO exception escapes
        elasticsearchService.indexDocument(document);

        // Verify that save() was still attempted
        verify(elasticsearchOperations, times(1)).save(document);

        // Verify that no other Elasticsearch calls were made
        verifyNoMoreInteractions(elasticsearchOperations);
    }
}
