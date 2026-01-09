package at.technikum_wien.rest_server.service;

import at.technikum_wien.rest_server.model.SearchDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class SearchServiceTest {

    // This is the Elasticsearch client that will be mocked
    private ElasticsearchOperations elasticsearchOperations;

    // This is the class under test
    private SearchService searchService;

    @BeforeEach
    void setUp() {
        // Create a mock for ElasticsearchOperations
        elasticsearchOperations = mock(ElasticsearchOperations.class);

        // Create the service with the mocked dependency
        searchService = new SearchService(elasticsearchOperations);
    }

    // When Elasticsearch returns hits → IDs are returned
    @Test
    void searchDocumentIds_returnsDistinctIds() {
        // Create first fake search document
        SearchDocument doc1 = new SearchDocument(1L, "a.pdf", "text", "summary");

        // Create second fake search document with SAME id (to test distinct)
        SearchDocument doc2 = new SearchDocument(1L, "a.pdf", "text2", "summary2");

        // Create third fake search document with different id
        SearchDocument doc3 = new SearchDocument(2L, "b.pdf", "text", "summary");

        // Create mocked SearchHit objects
        SearchHit<SearchDocument> hit1 = mock(SearchHit.class);
        SearchHit<SearchDocument> hit2 = mock(SearchHit.class);
        SearchHit<SearchDocument> hit3 = mock(SearchHit.class);

        // Define what each SearchHit should return
        when(hit1.getContent()).thenReturn(doc1);
        when(hit2.getContent()).thenReturn(doc2);
        when(hit3.getContent()).thenReturn(doc3);

        // Create mocked SearchHits container
        SearchHits<SearchDocument> searchHits = mock(SearchHits.class);

        // Return a list of hits (including duplicate ID)
        when(searchHits.getSearchHits()).thenReturn(List.of(hit1, hit2, hit3));

        // Mock the Elasticsearch search call
        when(elasticsearchOperations.search(any(CriteriaQuery.class), eq(SearchDocument.class)))
                .thenReturn(searchHits);

        // Call the method under test
        List<Long> result = searchService.searchDocumentIds("test");

        // Verify that we got only distinct IDs
        assertEquals(2, result.size());
        assertEquals(List.of(1L, 2L), result);

        // Verify that Elasticsearch was called exactly once
        verify(elasticsearchOperations, times(1))
                .search(any(CriteriaQuery.class), eq(SearchDocument.class));
    }

    // When Elasticsearch returns no hits → empty list
    @Test
    void searchDocumentIds_noHits_returnsEmptyList() {
        // Create empty SearchHits mock
        SearchHits<SearchDocument> searchHits = mock(SearchHits.class);

        // Return empty list of hits
        when(searchHits.getSearchHits()).thenReturn(List.of());

        // Mock Elasticsearch search call
        when(elasticsearchOperations.search(any(CriteriaQuery.class), eq(SearchDocument.class)))
                .thenReturn(searchHits);

        // Call the method under test
        List<Long> result = searchService.searchDocumentIds("does-not-exist");

        // Verify result is empty
        assertEquals(0, result.size());

        // Verify Elasticsearch was still called
        verify(elasticsearchOperations, times(1))
                .search(any(CriteriaQuery.class), eq(SearchDocument.class));
    }
}
