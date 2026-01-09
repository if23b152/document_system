package at.technikum_wien.rest_server.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DocumentTest {

    @Test
    void document_ShouldInitializeWithEmptyComments() {
        Document doc = new Document();
        assertNotNull(doc.getComments());
        assertTrue(doc.getComments().isEmpty());
    }

    @Test
    void document_ShouldHandleGettersAndSetters() {
        Document doc = new Document();
        doc.setFileName("test.pdf");
        doc.setOcrProcessed(true);
        doc.setTags("java,test");

        assertEquals("test.pdf", doc.getFileName());
        assertTrue(doc.getOcrProcessed());
        assertEquals("java,test", doc.getTags());
    }
    @Test
    void addComment_ShouldAddToCommentsList() {
        Document doc = new Document();
        Comment comment = new Comment();
        comment.setContent("Test");

        doc.getComments().add(comment);

        assertEquals(1, doc.getComments().size());
        assertEquals(comment, doc.getComments().get(0));
    }

    @Test
    void setOcrProcessed_ShouldUpdateFlag() {
        Document doc = new Document();
        doc.setOcrProcessed(true);
        assertTrue(doc.getOcrProcessed());

        doc.setOcrProcessed(false);
        assertFalse(doc.getOcrProcessed());
    }
}
