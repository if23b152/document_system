package at.technikum_wien.rest_server.service;

import io.minio.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.UUID;

/**
 * Service for interacting with MinIO object storage.
 * Handles uploading and downloading of PDF documents.
 */
@Service // Marks this as a Spring service component
public class MinioService {

    private final MinioClient minioClient; // Official MinIO Java client
    private final String bucketName;       // Bucket where documents are stored
    private static final Logger log = LoggerFactory.getLogger(MinioService.class);

    // Inject MinIO client and bucket name from configuration
    public MinioService(MinioClient minioClient, @Value("${minio.bucket}") String bucketName) {
        this.minioClient = minioClient;
        this.bucketName = bucketName;
    }

    /**
     * Uploads a document to MinIO object storage.
     *
     * @param file MultipartFile to upload
     * @return Generated object key (stored in DB and sent to workers)
     */
    public String uploadDocument(MultipartFile file) {
        try {
            // Ensure that the configured bucket exists
            boolean exists = minioClient.bucketExists(
                    BucketExistsArgs.builder().bucket(bucketName).build()
            );
            if (!exists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
            }

            // Generate a unique object key (UUID + original filename)
            String objectKey = UUID.randomUUID() + "-" + file.getOriginalFilename();

            // Upload the file stream to MinIO
            try (InputStream is = file.getInputStream()) {
                minioClient.putObject(
                        PutObjectArgs.builder()
                                .bucket(bucketName)
                                .object(objectKey)
                                .stream(is, file.getSize(), -1)
                                .contentType(file.getContentType())
                                .build()
                );
            }

            // Return object key so it can be stored in DB
            return objectKey;
        } catch (Exception e) {
            // Wrap any MinIO or IO error into a runtime exception
            throw new RuntimeException("Failed to upload file to MinIO", e);
        }
    }

    /**
     * Deletes a document from MinIO storage.
     *
     * @param objectKey The object key of the file to delete
     */
    public void deleteDocument(String objectKey) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectKey)
                            .build()
            );
            log.info("Deleted file '{}' from MinIO bucket '{}'.", objectKey, bucketName);
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete file from MinIO: " + objectKey, e);
        }
    }
}