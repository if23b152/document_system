package at.technikum_wien.rest_server.service;

import at.technikum_wien.rest_server.model.SearchDocument;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SearchServiceTest {

    @Mock
    private ElasticsearchOperations elasticsearchOperations;

    @Mock
    private SearchHits<SearchDocument> searchHits;

    @Mock
    private SearchHit<SearchDocument> searchHit;

    @InjectMocks
    private SearchService searchService;

    @Test
    void searchDocumentIds_shouldReturnMatchingDocumentIds() {
        // Arrange
        SearchDocument doc = new SearchDocument();
        doc.setDocumentId(42L);

        when(searchHit.getContent()).thenReturn(doc);
        when(searchHits.getSearchHits()).thenReturn(List.of(searchHit));

        when(elasticsearchOperations.search(
                org.mockito.ArgumentMatchers.<org.springframework.data.elasticsearch.core.query.Query>any(),
                org.mockito.ArgumentMatchers.<Class<SearchDocument>>any()
        )).thenReturn(searchHits);

        // Act
        List<Long> result = searchService.searchDocumentIds("Hello");

        // Assert
        assertEquals(1, result.size());
        assertEquals(42L, result.getFirst());
    }
}
