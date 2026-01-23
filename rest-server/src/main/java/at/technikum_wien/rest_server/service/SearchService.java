package at.technikum_wien.rest_server.service;

import at.technikum_wien.rest_server.model.SearchDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.elasticsearch.NoSuchIndexException;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service // Marks this class as a Spring service
public class SearchService {

    private static final Logger log = LoggerFactory.getLogger(SearchService.class);
    private final ElasticsearchOperations elasticsearchOperations; // Spring abstraction over Elasticsearch
    private static final String INDEX_NAME = "documents";

    // Constructor injection of Elasticsearch client
    public SearchService(ElasticsearchOperations elasticsearchOperations) {
        this.elasticsearchOperations = elasticsearchOperations;
    }

    /**
     * Searches Elasticsearch and returns matching document IDs.
     */
    public List<Long> searchDocumentIds(String query) {

        try {
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
        } catch (NoSuchIndexException e) {
            log.warn("Elasticsearch index '{}' not found while searching.", INDEX_NAME);
            return List.of();
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().contains("index_not_found_exception")) {
                log.warn("Elasticsearch index '{}' not found while searching.", INDEX_NAME);
                return List.of();
            }
            throw e;
        }
    }

    /**
     * Loads a single document by ID from Elasticsearch (if indexed).
     */
    public Optional<SearchDocument> getDocumentById(Long documentId) {
        try {
            SearchDocument document = elasticsearchOperations.get(
                    documentId.toString(),
                    SearchDocument.class
            );
            return Optional.ofNullable(document);
        } catch (NoSuchIndexException e) {
            log.warn("Elasticsearch index '{}' not found for document {}.", INDEX_NAME, documentId);
            return Optional.empty();
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().contains("index_not_found_exception")) {
                log.warn("Elasticsearch index '{}' not found for document {}.", INDEX_NAME, documentId);
                return Optional.empty();
            }
            throw e;
        }
    }
}
