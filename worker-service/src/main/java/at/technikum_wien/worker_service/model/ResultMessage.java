package at.technikum_wien.worker_service.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// Using record is also idiomatic for DTOs in modern Java:
// public record ResultMessage(Long documentId, String summary) {}
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResultMessage {
    private Long documentId;

    private String text;      // Extracted OCR text
    private String summary;

    private boolean success;  // Whether OCR succeeded
    private String error;     // Error message (if any)
}