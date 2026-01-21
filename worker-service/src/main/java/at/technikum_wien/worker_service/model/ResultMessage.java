package at.technikum_wien.worker_service.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO sent from the worker back to the REST server via RabbitMQ.
 * Contains either the OCR + summary result or an error description.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResultMessage {

    private Long documentId;  // ID of the processed document

    private String text;      // Extracted OCR text (null if failed)

    private String summary;   // AI-generated summary (null if failed)

    private boolean success;  // Indicates whether the processing was successful

    private String error;     // Error message if something went wrong
}