package at.technikum_wien.rest_server.service;

import at.technikum_wien.rest_server.model.Comment;
import at.technikum_wien.rest_server.model.Document;
import at.technikum_wien.rest_server.repository.CommentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service // Marks this class as a Spring service
public class CommentService {

    private final CommentRepository commentRepository;   // JPA repository for comments
    private final DocumentService documentService; // Used to enforce document access rules

    @Autowired // Constructor injection of dependencies
    public CommentService(CommentRepository commentRepository,
                          DocumentService documentService) {
        this.commentRepository = commentRepository;
        this.documentService = documentService;
    }

    /**
     * Adds a new comment to a given document.
     */
    public Comment addComment(Long documentId, String content) {

        // Load document or fail if it does not exist
        Document document = documentService.getAccessibleDocumentOrThrow(documentId);

        // Create new comment entity
        Comment comment = new Comment();
        comment.setContent(content);
        comment.setCreatedAt(LocalDateTime.now()); // Set creation timestamp
        comment.setDocument(document);             // Link comment to document

        // Persist comment in the database
        return commentRepository.save(comment);
    }

    /**
     * Returns all comments belonging to a document.
     */
    public List<Comment> getCommentsForDocument(Long documentId) {
        documentService.getAccessibleDocumentOrThrow(documentId);
        return commentRepository.findByDocumentId(documentId);
    }

    /**
     * Deletes a comment by its ID.
     */
    public void deleteComment(Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Comment not found."));

        documentService.getAccessibleDocumentOrThrow(comment.getDocument().getId());
        commentRepository.delete(comment);
    }
}
