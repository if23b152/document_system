package at.technikum_wien.rest_server.model;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DocumentResponse {

    // Unique ID of the document
    private Long id;

    // Name of the uploaded file
    private String fileName;

    // File size in bytes
    private long fileSize;

    // AI-generated or user-edited summary
    private String summary;

    // Upload timestamp
    private LocalDateTime uploadTimestamp;

    // List of tags associated with the document
    private List<String> tags;
}