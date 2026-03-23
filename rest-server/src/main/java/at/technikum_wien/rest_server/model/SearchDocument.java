package at.technikum_wien.rest_server.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.Setting;

@Data  // Generates getters, setters, toString, equals, hashCode
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "documents") // Maps this class to the "documents" index in Elasticsearch
@Setting(settingPath = "/elasticsearch/documents-settings.json")
public class SearchDocument {

    @Id // Document ID used inside Elasticsearch (same as DB document ID)
    private Long documentId;

    // Original file name (indexed for search)
    @Field(type = FieldType.Text, analyzer = "autocomplete_index", searchAnalyzer = "autocomplete_search")
    private String fileName;

    // Full OCR-extracted text (main searchable content)
    @Field(type = FieldType.Text, analyzer = "autocomplete_index", searchAnalyzer = "autocomplete_search")
    private String ocrText;

    // AI-generated summary (also searchable)
    @Field(type = FieldType.Text, analyzer = "autocomplete_index", searchAnalyzer = "autocomplete_search")
    private String summary;
}
