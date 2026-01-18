package at.technikum_wien.rest_server.config;

import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for MinIO client setup.
 * Reads connection details from application.properties.
 */
@Configuration // Marks this class as a Spring configuration
public class MinioConfig {

    @Value("${minio.url}") // MinIO server URL
    private String minioUrl;

    @Value("${minio.access-key}") // Access key for authentication
    private String accessKey;

    @Value("${minio.secret-key}") // Secret key for authentication
    private String secretKey;

    // Creates and exposes MinioClient as a Spring Bean
    @Bean
    public MinioClient minioClient() {
        return MinioClient.builder()
                .endpoint(minioUrl)
                .credentials(accessKey, secretKey)
                .build();
    }
}