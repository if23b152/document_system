package at.technikum_wien.rest_server.service;

import at.technikum_wien.rest_server.model.Comment;
import at.technikum_wien.rest_server.model.Document;
import at.technikum_wien.rest_server.repository.CommentRepository;
import at.technikum_wien.rest_server.repository.DocumentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class CommentServiceTest {

    // This is the repository for comments (will be mocked)
    private CommentRepository commentRepository;

    // This is the repository for documents (will be mocked)
    private DocumentRepository documentRepository;

    // This is the class under test
    private CommentService commentService;

    @BeforeEach
    void setUp() {
        // Create mock for CommentRepository
        commentRepository = mock(CommentRepository.class);

        // Create mock for DocumentRepository
        documentRepository = mock(DocumentRepository.class);

        // Create service with mocked dependencies
        commentService = new CommentService(commentRepository, documentRepository);
    }

    // addComment → document exists → comment is saved
    @Test
    void addComment_documentExists_savesComment() {
        // Define a fake document ID
        Long documentId = 1L;

        // Define the comment content
        String content = "Hello world";

        // Create a fake Document entity
        Document document = new Document();
        document.setId(documentId);

        // Mock repository to return the document
        when(documentRepository.findById(documentId))
                .thenReturn(Optional.of(document));

        // Capture the Comment that is saved and return it
        when(commentRepository.save(any(Comment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Call the method under test
        Comment saved = commentService.addComment(documentId, content);

        // Verify that the saved comment has the correct content
        assertEquals(content, saved.getContent());

        // Verify that the saved comment references the correct document
        assertEquals(document, saved.getDocument());

        // Verify that createdAt was set
        assertNotNull(saved.getCreatedAt());

        // Verify that documentRepository was called
        verify(documentRepository, times(1)).findById(documentId);

        // Verify that commentRepository.save was called
        verify(commentRepository, times(1)).save(any(Comment.class));
    }

    // addComment → document does NOT exist → exception
    @Test
    void addComment_documentMissing_throwsException() {
        // Define a non-existing document ID
        Long documentId = 99L;

        // Mock repository to return empty
        when(documentRepository.findById(documentId))
                .thenReturn(Optional.empty());

        // Call method and expect RuntimeException
        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> commentService.addComment(documentId, "test")
        );

        // Verify exception message
        assertEquals("Document not found", ex.getMessage());

        // Verify that save was NEVER called
        verify(commentRepository, never()).save(any());

        // Verify that documentRepository was called
        verify(documentRepository, times(1)).findById(documentId);
    }
}
