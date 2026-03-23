package at.technikum_wien.rest_server.config;

import at.technikum_wien.rest_server.model.Document;
import at.technikum_wien.rest_server.model.SearchDocument;
import at.technikum_wien.rest_server.repository.DocumentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;

@Configuration
public class ElasticsearchIndexInitializer {

    private static final Logger log = LoggerFactory.getLogger(ElasticsearchIndexInitializer.class);

    @Bean
    public ApplicationRunner ensureElasticsearchIndex(ElasticsearchOperations operations,
                                                      DocumentRepository documentRepository) {
        return args -> {
            IndexOperations indexOps = operations.indexOps(SearchDocument.class);

            if (indexOps.exists()) {
                indexOps.delete();
                log.info("Deleted existing Elasticsearch index for SearchDocument so the latest analyzer settings apply.");
            }

            indexOps.create();
            indexOps.putMapping();
            log.info("Created Elasticsearch index for SearchDocument.");

            for (Document document : documentRepository.findAll()) {
                operations.save(new SearchDocument(
                        document.getId(),
                        document.getFileName(),
                        document.getOcrText(),
                        document.getSummary()
                ));
            }

            log.info("Reindexed {} documents into Elasticsearch.", documentRepository.count());
        };
    }
}
