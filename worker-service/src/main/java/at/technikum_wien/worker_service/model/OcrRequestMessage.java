package at.technikum_wien.worker_service.model;

import lombok.*;

/**
 * DTO representing a message sent from the REST server to the worker.
 * Contains all information needed to fetch and process the document.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OcrRequestMessage {
    private Long documentId;      // Database ID of the document
    private String fileName;       // Original filename (mainly for logging/debugging)
    private String minioObjectKey; // Key (UUID + filename) used to download the file from MinIO
}