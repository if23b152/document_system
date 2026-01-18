package at.technikum_wien.rest_server.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

@Configuration // Marks this as a Spring configuration class
@EnableElasticsearchRepositories(
        basePackages = "at.technikum_wien.rest_server" // Enables Spring Data Elasticsearch repositories
)
public class ElasticsearchConfig {

    /*
     * We do not need to manually define an Elasticsearch client bean here.
     *
     * Spring Boot automatically creates:
     * - ElasticsearchClient
     * - ElasticsearchOperations
     *
     * Based on the property:
     * - spring.elasticsearch.uris
     *
     * This keeps the configuration minimal and clean.
     */
}