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
@Service
public class MinioService {

    private static final Logger log = LoggerFactory.getLogger(MinioService.class);

    private final MinioClient minioClient;

    @Value("${minio.bucket}")
    private String bucketName;

    public MinioService(MinioClient minioClient) {
        this.minioClient = minioClient;
    }

    /**
     * Downloads a file from MinIO by its object key.
     *
     * @param objectKey the key of the file in MinIO (usually UUID + filename)
     * @return InputStream of the file (caller must close it)
     */
    public InputStream downloadFile(String objectKey) {
        try {
            log.info("Downloading object '{}' from bucket '{}'", objectKey, bucketName);

            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectKey)
                            .build()
            );

        } catch (MinioException e) {
            log.error("MinIO error while downloading '{}': {}", objectKey, e.getMessage(), e);
            throw new RuntimeException("Failed to download file from MinIO", e);
        } catch (Exception e) {
            log.error("Unexpected error while downloading '{}': {}", objectKey, e.getMessage(), e);
            throw new RuntimeException("Failed to download file from MinIO", e);
        }
    }
}