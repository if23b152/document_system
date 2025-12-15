package at.technikum_wien.worker_service.model;

import lombok.AllArgsConstructor;

import lombok.NoArgsConstructor;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;

@Data  // Generates getters, setters, toString, equals, hashCode
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "documents")
public class SearchDocument {
    @Id
    private Long documentId;
    private String fileName;
    private String ocrText;
    private String summary;
}