package at.technikum_wien.worker_service.config;

import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class that sets up a MinIO client for the worker service.
 * This allows the worker to connect to MinIO and download files for OCR processing.
 */
@Configuration // Marks this class as a configuration source for Spring (like a setup file)
public class MinioConfig {

    // The endpoint URL of the MinIO server (e.g., http://minio:9000)
    // Read from the application.properties file
    @Value("${minio.endpoint}")
    private String endpoint;

    // Access key for authenticating with MinIO
    @Value("${minio.access-key}")
    private String accessKey;

    // Secret key for authenticating with MinIO
    @Value("${minio.secret-key}")
    private String secretKey;

    /**
     * Creates and configures a MinioClient bean.
     * Spring will automatically make this bean available for injection
     * wherever a MinioClient is needed (e.g., in MinioService).
     * The MinioClient comes from the official MinIO Java SDK
     * and is used to perform file operations such as upload and download.
     */
    @Bean // Tells Spring to create and manage this MinioClient as a bean
    public MinioClient minioClient() {
        return MinioClient.builder()
                .endpoint(endpoint) // Set the MinIO server URL
                .credentials(accessKey, secretKey) // Provide access and secret keys
                .build(); // Build and return the client
    }
}
