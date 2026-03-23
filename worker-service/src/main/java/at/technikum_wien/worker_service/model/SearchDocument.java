package at.technikum_wien.worker_service.model;

import lombok.AllArgsConstructor;

import lombok.NoArgsConstructor;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.Setting;

/**
 * Elasticsearch document representation used by the worker service.
 * Stores OCR text and summary so the document can be found via full-text search.
 */
@Data  // Generates getters, setters, toString, equals, and hashCode
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "documents") // Marks this class as an Elasticsearch index document
@Setting(settingPath = "/elasticsearch/documents-settings.json")
public class SearchDocument {

    @Id
    private Long documentId;  // ID of the document (same as in PostgreSQL)

    @Field(type = FieldType.Text, analyzer = "autocomplete_index", searchAnalyzer = "autocomplete_search")
    private String fileName;  // Original filename (used for display and search)

    @Field(type = FieldType.Text, analyzer = "autocomplete_index", searchAnalyzer = "autocomplete_search")
    private String ocrText;   // Full text extracted from the PDF via OCR

    @Field(type = FieldType.Text, analyzer = "autocomplete_index", searchAnalyzer = "autocomplete_search")
    private String summary;   // AI-generated summary of the document
}
