package at.technikum_wien.worker_service.model;

import lombok.*;

/**
 * Simple DTO that represents the result of an OCR operation.
 * It contains either the extracted text or an error if something failed.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OcrResult {

    private Long documentId;  // ID of the document this result belongs to

    private String text;      // The text extracted from the PDF by OCR

    private boolean success;  // True if OCR was successful, false otherwise

    private String error;     // Error message if OCR failed (null if successful)
}