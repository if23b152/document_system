package at.technikum_wien.rest_server.controller;

import at.technikum_wien.rest_server.model.Comment;
import at.technikum_wien.rest_server.service.CommentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/comments")
public class CommentController {

    private final CommentService commentService;

    @Autowired
    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    // === Add comment to document ===
    @PostMapping("/document/{documentId}")
    public ResponseEntity<Comment> addComment(
            @PathVariable Long documentId,
            @RequestBody String content) {

        Comment saved = commentService.addComment(documentId, content);
        return ResponseEntity.ok(saved);
    }

    // === Get comments for document ===
    @GetMapping("/document/{documentId}")
    public ResponseEntity<List<Comment>> getComments(@PathVariable Long documentId) {
        return ResponseEntity.ok(
                commentService.getCommentsForDocument(documentId)
        );
    }

    // === Delete comment ===
    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> deleteComment(@PathVariable Long commentId) {
        commentService.deleteComment(commentId);
        return ResponseEntity.noContent().build();
    }
}
