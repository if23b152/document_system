package at.technikum_wien.rest_server.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
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