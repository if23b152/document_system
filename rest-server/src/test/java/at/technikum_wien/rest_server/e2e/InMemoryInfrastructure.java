package at.technikum_wien.rest_server.e2e;

import at.technikum_wien.rest_server.producer.DocumentMessageProducer;
import at.technikum_wien.rest_server.service.MinioService;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Replaces MinIO, RabbitMQ and Elasticsearch; database, security and HTTP stay real. */
@TestConfiguration
public class InMemoryInfrastructure {

    @Bean
    @Primary
    MinioService inMemoryMinioService() {
        return new InMemoryMinioService();
    }

    @Bean
    @Primary
    DocumentMessageProducer noOpMessageProducer() {
        return Mockito.mock(DocumentMessageProducer.class);
    }

    // Same bean names as the auto-configured template, which then backs off
    @Bean({"elasticsearchOperations", "elasticsearchTemplate"})
    ElasticsearchOperations elasticsearchOperations() {
        return Mockito.mock(ElasticsearchOperations.class, Mockito.RETURNS_DEEP_STUBS);
    }

    static class InMemoryMinioService extends MinioService {

        private final Map<String, byte[]> objects = new ConcurrentHashMap<>();

        InMemoryMinioService() {
            super(null, "documents");
        }

        @Override
        public String uploadDocument(MultipartFile file) {
            try {
                String key = UUID.randomUUID() + ".pdf";
                objects.put(key, file.getBytes());
                return key;
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        @Override
        public void deleteDocument(String objectKey) {
            objects.remove(objectKey);
        }

        @Override
        public InputStream downloadDocument(String objectKey) {
            return new ByteArrayInputStream(objects.get(objectKey));
        }
    }
}
