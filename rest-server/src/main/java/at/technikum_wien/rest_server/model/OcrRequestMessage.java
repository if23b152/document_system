package at.technikum_wien.rest_server.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO representing a message sent to the OCR worker.
 * It contains all necessary data to locate and process the document.
 */
@Data // Generates getters, setters, toString, equals, hashCode
@NoArgsConstructor
@AllArgsConstructor
public class OcrRequestMessage {

    // Database ID of the document
    private Long documentId;

    // Original filename (for logging or debugging purposes)
    private String fileName;

    // Object key used to retrieve the file from MinIO
    private String minioObjectKey;
}