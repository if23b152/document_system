package at.technikum_wien.worker_service.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

/**
 * Configuration class for enabling Elasticsearch repositories in the worker service.
 * The worker uses Elasticsearch to index OCR text and summaries for search.
 */
@Configuration
@EnableElasticsearchRepositories(
        basePackages = "at.technikum_wien.worker_service"
)
public class ElasticsearchConfig {

    /*
     * No manual client configuration is needed here.
     *
     * Spring Boot automatically creates:
     *  - ElasticsearchClient
     *  - ElasticsearchOperations
     *
     * based on the property:
     *  spring.elasticsearch.uris
     *
     * This is the same setup as in the REST server.
     */
}