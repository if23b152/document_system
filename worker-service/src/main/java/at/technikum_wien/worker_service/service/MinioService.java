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
 * Service responsible for downloading files from MinIO for the worker.
 * The worker uses this to fetch PDFs before running OCR.
 */
@Service // Marks this class as a Spring service component
public class MinioService {

    // Logger for printing info and error messages
    private static final Logger log = LoggerFactory.getLogger(MinioService.class);

    // MinIO client used to communicate with the object storage server
    private final MinioClient minioClient;

    // Name of the bucket where documents are stored (from application.properties)
    @Value("${minio.bucket}")
    private String bucketName;

    // Constructor injection of the MinIO client
    public MinioService(MinioClient minioClient) {
        this.minioClient = minioClient;
    }

    /**
     * Downloads a file from MinIO using its object key.
     *
     * @param objectKey the unique key of the file in MinIO
     * @return InputStream of the file (caller must close it)
     */
    public InputStream downloadFile(String objectKey) {
        try {
            // Log which file is being downloaded
            log.info("Downloading object '{}' from bucket '{}'", objectKey, bucketName);

            // Request the object from MinIO
            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucketName) // bucket where the file is stored
                            .object(objectKey)  // object key (filename in MinIO)
                            .build()            // build the request
            );

        } catch (MinioException e) {
            // Handle MinIO-specific errors (e.g., file not found, permission issues)
            log.error("MinIO error while downloading '{}': {}", objectKey, e.getMessage(), e);
            throw new RuntimeException("Failed to download file from MinIO", e);
        } catch (Exception e) {
            // Handle any other unexpected errors (network, IO, etc.)
            log.error("Unexpected error while downloading '{}': {}", objectKey, e.getMessage(), e);
            throw new RuntimeException("Failed to download file from MinIO", e);
        }
    }
}
