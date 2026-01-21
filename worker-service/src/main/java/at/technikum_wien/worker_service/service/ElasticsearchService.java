package at.technikum_wien.worker_service.service;

import at.technikum_wien.worker_service.model.SearchDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.stereotype.Service;

/**
 * Service responsible for storing processed documents in Elasticsearch.
 * It is used by the worker after OCR and summary generation to make documents searchable.
 */
@Service
public class ElasticsearchService {

    // Logger for Elasticsearch-related messages
    private static final Logger log = LoggerFactory.getLogger(ElasticsearchService.class);

    // Spring abstraction used to communicate with Elasticsearch
    private final ElasticsearchOperations elasticsearchOperations;

    // Constructor injection of Elasticsearch operations
    public ElasticsearchService(ElasticsearchOperations elasticsearchOperations) {
        this.elasticsearchOperations = elasticsearchOperations;
    }

    /**
     * Indexes a document in Elasticsearch so it can be found via search.
     */
    public void indexDocument(SearchDocument document) {
        try {
            // Save (index) the document in Elasticsearch
            elasticsearchOperations.save(document);

            // Log success
            log.info("Document {} successfully indexed in Elasticsearch.", document.getDocumentId());
        } catch (Exception e) {
            // Log failure but do not crash the worker
            log.error("Failed to index document {} in Elasticsearch: {}",
                    document.getDocumentId(), e.getMessage(), e);

            // Intentionally not rethrowing: search is optional and should not stop processing
        }
    }
}