package at.technikum_wien.worker_service.model;

import lombok.AllArgsConstructor;

import lombok.NoArgsConstructor;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;

/**
 * Elasticsearch document representation used by the worker service.
 * Stores OCR text and summary so the document can be found via full-text search.
 */
@Data  // Generates getters, setters, toString, equals, and hashCode
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "documents") // Marks this class as an Elasticsearch index document
public class SearchDocument {

    @Id
    private Long documentId;  // ID of the document (same as in PostgreSQL)

    private String fileName;  // Original filename (used for display and search)

    private String ocrText;   // Full text extracted from the PDF via OCR

    private String summary;   // AI-generated summary of the document
}