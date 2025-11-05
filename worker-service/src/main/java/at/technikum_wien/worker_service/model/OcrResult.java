package at.technikum_wien.worker_service.model;

import lombok.*;

/**
 * Represents the structured output of an OCR operation.
 * This object may later be stored in the database or sent back to another queue.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OcrResult {
    private Long documentId;  // The related document ID
    private String text;      // Extracted OCR text
    private boolean success;  // Whether OCR succeeded
    private String error;     // Error message (if any)
}