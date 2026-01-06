package at.technikum_wien.rest_server.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class CommentTest {

    @Test
    void comment_ShouldHaveRequiredFields() {
        Comment comment = new Comment();
        comment.setContent("Test content");
        comment.setCreatedAt(LocalDateTime.now());

        assertEquals("Test content", comment.getContent());
        assertNotNull(comment.getCreatedAt());
        assertNull(comment.getDocument()); // Vor Zuweisung
    }

    @Test
    void comment_ShouldAcceptDocumentAssignment() {
        Comment comment = new Comment();
        Document doc = new Document();
        doc.setId(1L);

        comment.setDocument(doc);
        assertEquals(doc, comment.getDocument());
        assertEquals(1L, comment.getDocument().getId());
    }

    @Test
    void comment_ShouldHandleNullContent() {
        Comment comment = new Comment();
        comment.setContent(null);
        comment.setCreatedAt(LocalDateTime.now());

        assertNull(comment.getContent());
        assertNotNull(comment.getCreatedAt());
    }
}
