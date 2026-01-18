package at.technikum_wien.rest_server.service;

import at.technikum_wien.rest_server.model.SearchDocument;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service // Marks this class as a Spring service
public class SearchService {

    private final ElasticsearchOperations elasticsearchOperations; // Spring abstraction over Elasticsearch

    // Constructor injection of Elasticsearch client
    public SearchService(ElasticsearchOperations elasticsearchOperations) {
        this.elasticsearchOperations = elasticsearchOperations;
    }

    /**
     * Searches Elasticsearch and returns matching document IDs.
     */
    public List<Long> searchDocumentIds(String query) {

        // Build search criteria: match query against OCR text, summary, or file name
        Criteria criteria = new Criteria("ocrText").matches(query)
                .or(new Criteria("summary").matches(query))
                .or(new Criteria("fileName").matches(query));

        // Wrap criteria into an Elasticsearch query object
        CriteriaQuery searchQuery = new CriteriaQuery(criteria);

        // Execute search against the "documents" index
        SearchHits<SearchDocument> hits =
                elasticsearchOperations.search(searchQuery, SearchDocument.class);

        // Extract unique document IDs from search results
        return hits.getSearchHits().stream()
                .map(hit -> hit.getContent().getDocumentId())
                .distinct()
                .collect(Collectors.toList());
    }
}