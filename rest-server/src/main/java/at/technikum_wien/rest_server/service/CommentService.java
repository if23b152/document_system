package at.technikum_wien.rest_server.service;

import at.technikum_wien.rest_server.model.Comment;
import at.technikum_wien.rest_server.model.Document;
import at.technikum_wien.rest_server.repository.CommentRepository;
import at.technikum_wien.rest_server.repository.DocumentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class CommentService {

    private final CommentRepository commentRepository;
    private final DocumentRepository documentRepository;

    @Autowired
    public CommentService(CommentRepository commentRepository,
                          DocumentRepository documentRepository) {
        this.commentRepository = commentRepository;
        this.documentRepository = documentRepository;
    }

    public Comment addComment(Long documentId, String content) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found"));

        Comment comment = new Comment();
        comment.setContent(content);
        comment.setCreatedAt(LocalDateTime.now());
        comment.setDocument(document);

        return commentRepository.save(comment);
    }

    public List<Comment> getCommentsForDocument(Long documentId) {
        return commentRepository.findByDocumentId(documentId);
    }

    public void deleteComment(Long commentId) {
        commentRepository.deleteById(commentId);
    }
}
