package at.technikum_wien.rest_server.model;

import jakarta.persistence.*;
import lombok.*;

import java.sql.Timestamp;

@Getter @Setter @Entity
@Table(name = "documents")
public class Document {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String fileName;

    @Lob
    @Column(nullable = false)
    private byte[] fileData;

    @Column(name = "upload_timestamp", nullable = false)
    private Timestamp uploadTimestamp;
}