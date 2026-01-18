package at.technikum_wien.rest_server.service;

import at.technikum_wien.rest_server.model.Comment;
import at.technikum_wien.rest_server.model.Document;
import at.technikum_wien.rest_server.repository.CommentRepository;
import at.technikum_wien.rest_server.repository.DocumentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service // Marks this class as a Spring service
public class CommentService {

    private final CommentRepository commentRepository;   // JPA repository for comments
    private final DocumentRepository documentRepository; // Used to load the owning document

    @Autowired // Constructor injection of dependencies
    public CommentService(CommentRepository commentRepository,
                          DocumentRepository documentRepository) {
        this.commentRepository = commentRepository;
        this.documentRepository = documentRepository;
    }

    /**
     * Adds a new comment to a given document.
     */
    public Comment addComment(Long documentId, String content) {

        // Load document or fail if it does not exist
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found"));

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
        return commentRepository.findByDocumentId(documentId);
    }

    /**
     * Deletes a comment by its ID.
     */
    public void deleteComment(Long commentId) {
        commentRepository.deleteById(commentId);
    }
}