package at.technikum_wien.rest_server.service;

import at.technikum_wien.rest_server.model.Comment;
import at.technikum_wien.rest_server.model.Document;
import at.technikum_wien.rest_server.repository.CommentRepository;
import at.technikum_wien.rest_server.repository.DocumentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @Mock private CommentRepository commentRepository;
    @Mock private DocumentRepository documentRepository;

    @InjectMocks private CommentService commentService;

    @Test
    void addComment_Success() {
        Long docId = 1L;
        Document doc = new Document();
        doc.setId(docId);

        when(documentRepository.findById(docId)).thenReturn(Optional.of(doc));
        when(commentRepository.save(any(Comment.class))).thenAnswer(invocation -> {
            Comment comment = invocation.getArgument(0);
            comment.setId(99L);
            return comment;
        });

        Comment saved = commentService.addComment(docId, "Toller Kommentar");

        assertNotNull(saved);
        assertEquals("Toller Kommentar", saved.getContent());
        assertEquals(doc, saved.getDocument());
        verify(commentRepository, times(1)).save(any());
        verify(documentRepository, times(1)).findById(docId);
    }

    @Test
    void addComment_DocumentNotFound_ThrowsException() {
        when(documentRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () ->
                commentService.addComment(99L, "Inhalt")
        );
        verify(documentRepository).findById(99L);
        verify(commentRepository, never()).save(any());
    }

    @Test
    void getCommentsForDocument_ShouldCallRepository() {
        commentService.getCommentsForDocument(1L);
        verify(commentRepository).findByDocumentId(1L);
    }

    @Test
    void deleteComment_ShouldCallRepository() {
        commentService.deleteComment(10L);
        verify(commentRepository).deleteById(10L);
    }
}
