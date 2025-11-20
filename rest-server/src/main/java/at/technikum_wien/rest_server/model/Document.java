package at.technikum_wien.rest_server.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "documents")
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String fileName;

    @Column(nullable = false)
    private long fileSize;

    @Column(nullable = false)
    private LocalDateTime uploadTimestamp;

    // MinIO object key (e.g. "documents/uuid-filename.pdf")
    @Column(name = "minio_object_key", nullable = false)
    private String minioObjectKey;

    // Summary generated later (Sprint 5)
    @Column(columnDefinition = "TEXT")
    private String summary;

    // Tags (Sprint 6)
    private String tags;

    private Boolean ocrProcessed = false;
    // private Boolean genAiSummarized = false;
}
