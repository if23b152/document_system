package at.technikum_wien.worker_service.model;

import lombok.*;

/**
 * DTO representing a message sent to the OCR worker.
 * It contains all necessary data to locate and process the document.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OcrRequestMessage {
    private Long documentId; // The database ID of the document
    private String fileName; // Original filename (optional, for logs)
    private String minioObjectKey; // The MinIO object key (UUID + filename)
}