package at.technikum_wien.rest_server.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO representing a message sent to the OCR worker.
 * It contains all necessary data to locate and process the document.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OcrRequestMessage {
    private Long documentId;
    private String fileName;
    private String minioObjectKey;
}