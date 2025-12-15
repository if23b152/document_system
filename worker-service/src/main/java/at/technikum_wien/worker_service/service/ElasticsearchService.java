package at.technikum_wien.worker_service.service;

import at.technikum_wien.worker_service.model.SearchDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.stereotype.Service;

@Service
public class ElasticsearchService {

    private static final Logger log = LoggerFactory.getLogger(ElasticsearchService.class);

    private final ElasticsearchOperations elasticsearchOperations;

    public ElasticsearchService(ElasticsearchOperations elasticsearchOperations) {
        this.elasticsearchOperations = elasticsearchOperations;
    }

    public void indexDocument(SearchDocument document) {
        try {
            elasticsearchOperations.save(document);
            log.info("Document {} successfully indexed in Elasticsearch.", document.getDocumentId());
        } catch (Exception e) {
            log.error("Failed to index document {} in Elasticsearch: {}",
                    document.getDocumentId(), e.getMessage(), e);
            // Do NOT throw further → indexing failure should not crash the worker
        }
    }
}