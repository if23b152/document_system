package at.technikum_wien.rest_server.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// Using record is also idiomatic for DTOs in modern Java:
// public record ResultMessage(Long documentId, String summary) {}

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResultMessage {

    // ID of the document in the database
    private Long documentId;

    // Full OCR-extracted text (optional, can be used for indexing)
    private String extractedText;

    // AI-generated summary
    private String summary;

    // Flag indicating whether OCR + summary generation succeeded
    private boolean success;

    // Error message if processing failed
    private String error;
}