package at.technikum_wien.worker_service.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

@Configuration
@EnableElasticsearchRepositories(
        basePackages = "at.technikum_wien.worker_service"
)
public class ElasticsearchConfig {
    /*
     * Same configuration as in the REST server.
     *
     * The worker service uses Elasticsearch for indexing documents
     * after OCR and GenAI summary generation.
     *
     * Spring Boot auto-configures the client using:
     * spring.elasticsearch.uris
     */
}
