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
    private Long id;
    private String fileName;
    private long fileSize;
    private String summary;
    private LocalDateTime uploadTimestamp;
    private List<String> tags;
}
