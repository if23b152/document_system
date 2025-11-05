package at.technikum_wien.worker_service.service;

import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.errors.MinioException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.InputStream;

/**
 * Handles file retrieval from MinIO for the worker-service.
 * The worker uses this to download PDF files and pass them to the OCR processor.
 */
/**
 * Handles file retrieval from MinIO for the worker-service.
 * The worker uses this service to download PDF files and pass them to the OCR processor.
 */
@Service // Marks this class as a Spring service (a managed component used for business logic)
public class MinioService {

    // Logger used to print info and error messages to the console or log files
    private static final Logger log = LoggerFactory.getLogger(MinioService.class);

    // The MinioClient is provided by the MinIO Java SDK.
    // It handles all communication with the MinIO server (like upload/download).
    // You usually create it with an endpoint, access key, and secret key.
    private final MinioClient minioClient;

    // Reads the bucket name from the application.properties file
    @Value("${minio.bucket}")
    private String bucketName;

    // Constructor for dependency injection (Spring will provide the MinioClient automatically)
    public MinioService(MinioClient minioClient) {
        this.minioClient = minioClient;
    }

    /**
     * Downloads a file from MinIO by its object key.
     *
     * @param objectKey the key of the file in MinIO (usually UUID + filename)
     * @return InputStream of the file (the caller is responsible for closing it)
     */
    public InputStream downloadFile(String objectKey) {
        try {
            // Log which file is being downloaded and from which bucket
            log.info("Downloading object '{}' from bucket '{}'", objectKey, bucketName);

            // Request the file from MinIO using the MinIO SDK
            // GetObjectArgs is a helper class that builds the request with bucket and object details
            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucketName) // specify the bucket to look in
                            .object(objectKey)  // specify the file name (key)
                            .build()            // build the final request
            );

        } catch (MinioException e) {
            // This block handles errors specific to MinIO (e.g., object not found, permission denied)
            log.error("MinIO error while downloading '{}': {}", objectKey, e.getMessage(), e);
            throw new RuntimeException("Failed to download file from MinIO", e);
        } catch (Exception e) {
            // This block catches any other unexpected errors (network issues, etc.)
            log.error("Unexpected error while downloading '{}': {}", objectKey, e.getMessage(), e);
            throw new RuntimeException("Failed to download file from MinIO", e);
        }
    }
}
