package at.technikum_wien.rest_server.controller;

import at.technikum_wien.rest_server.model.Comment;
import at.technikum_wien.rest_server.service.CommentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController // REST controller for comment-related endpoints
@RequestMapping("/api/comments") // Base path for all comment APIs
public class CommentController {

    private final CommentService commentService; // Business logic for comments

    @Autowired // Constructor injection
    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    // === POST: Add a new comment to a document ===
    @PostMapping("/document/{documentId}")
    public ResponseEntity<Comment> addComment(
            @PathVariable Long documentId,
            @RequestBody String content) {

        // Create and persist new comment
        Comment saved = commentService.addComment(documentId, content);

        // Return saved comment
        return ResponseEntity.ok(saved);
    }

    // === GET: Retrieve all comments for a document ===
    @GetMapping("/document/{documentId}")
    public ResponseEntity<List<Comment>> getComments(@PathVariable Long documentId) {
        return ResponseEntity.ok(
                commentService.getCommentsForDocument(documentId)
        );
    }

    // === DELETE: Remove a comment by its ID ===
    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> deleteComment(@PathVariable Long commentId) {

        // Delete comment from database
        commentService.deleteComment(commentId);

        // Return 204 No Content
        return ResponseEntity.noContent().build();
    }
}