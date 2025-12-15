package at.technikum_wien.rest_server.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

@Configuration
@EnableElasticsearchRepositories(
        basePackages = "at.technikum_wien.rest_server"
)
public class ElasticsearchConfig {
    /*
     * We do not need to manually define a client bean here.
     *
     * Spring Boot automatically creates:
     * - ElasticsearchClient
     * - ElasticsearchOperations
     *
     * based on:
     * spring.elasticsearch.uris
     *
     * This keeps the configuration simple and clean.
     */
}