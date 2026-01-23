package at.technikum_wien.rest_server.model;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "documents") // Maps this class to the "documents" table in PostgreSQL
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Auto-increment primary key
    private Long id;

    @Column(nullable = false) // Original file name uploaded by the user
    private String fileName;

    @Column(nullable = false) // Size of the file in bytes
    private long fileSize;

    @Column(nullable = false) // Timestamp when the file was uploaded
    private LocalDateTime uploadTimestamp;

    // Key used to locate the file inside MinIO (e.g. "documents/uuid-filename.pdf")
    @Column(name = "minio_object_key", nullable = false)
    private String minioObjectKey;

    // AI-generated summary of the document (filled in Sprint 5)
    @Column(columnDefinition = "TEXT") // Stored as TEXT because it can be long
    private String summary;

    // OCR-extracted text (used by reader and search)
    @Lob
    @Column(columnDefinition = "TEXT")
    private String ocrText;

    // Error details if OCR/processing failed
    @Column(columnDefinition = "TEXT")
    private String ocrError;

    // Comma-separated list of tags
    private String tags;

    // Flag to indicate whether OCR has already been performed
    private Boolean ocrProcessed = false;

    // One document can have many user comments
    @OneToMany(
            mappedBy = "document", // "document" field in Comment owns the relationship
            cascade = CascadeType.ALL, // Automatically persist/delete comments with the document
            orphanRemoval = true // Remove comments if they are no longer linked to a document
    )
    @JsonManagedReference // Prevents infinite JSON recursion when serializing
    private List<Comment> comments = new ArrayList<>();
}
