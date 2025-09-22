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

    // Path or key in MinIO (Sprint 4)
    @Column(nullable = false)
    private String storagePath;

    // Summary generated later (Sprint 5)
    @Column(columnDefinition = "TEXT")
    private String summary;

    // Tags (Sprint 6, could also be separate entity)
    private String tags;
}
