package at.technikum_wien.rest_server.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "comments") // Maps this class to the "comments" table in the database
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Auto-increment primary key
    private Long id;

    @Column(nullable = false, columnDefinition = "TEXT") // Comment text content
    private String content;

    @Column(nullable = false) // Timestamp when the comment was created
    private LocalDateTime createdAt;

    // Many comments can belong to one document
    @ManyToOne(optional = false) // Each comment must be linked to a document
    @JoinColumn(name = "document_id") // Foreign key column in the comments table
    @JsonBackReference // Prevents infinite JSON recursion when serializing
    private Document document;
}