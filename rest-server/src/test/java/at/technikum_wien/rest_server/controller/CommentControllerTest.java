package at.technikum_wien.rest_server.controller;

import at.technikum_wien.rest_server.model.Comment;
import at.technikum_wien.rest_server.model.Document;
import at.technikum_wien.rest_server.service.CommentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CommentController.class)
class CommentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CommentService commentService;

    @Autowired
    private ObjectMapper objectMapper;

    // =====================================================
    // === ADD COMMENT (POST) ===
    // =====================================================
    @Test
    void addComment_returns200() throws Exception {
        Long documentId = 1L;
        String content = "This is a comment";

        Document doc = new Document();
        doc.setId(documentId);

        Comment savedComment = new Comment();
        savedComment.setId(10L);
        savedComment.setContent(content);
        savedComment.setCreatedAt(LocalDateTime.now());
        savedComment.setDocument(doc);

        Mockito.when(commentService.addComment(documentId, content))
                .thenReturn(savedComment);

        mockMvc.perform(post("/api/comments/document/{documentId}", documentId)
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("This is a comment"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.content").value("This is a comment"));
    }

    // =====================================================
    // === GET COMMENTS FOR DOCUMENT (GET) ===
    // =====================================================
    @Test
    void getComments_returnsList() throws Exception {
        Long documentId = 1L;

        Document doc = new Document();
        doc.setId(documentId);

        Comment c1 = new Comment(1L, "First", LocalDateTime.now(), doc);
        Comment c2 = new Comment(2L, "Second", LocalDateTime.now(), doc);

        List<Comment> comments = List.of(c1, c2);

        Mockito.when(commentService.getCommentsForDocument(documentId))
                .thenReturn(comments);

        mockMvc.perform(get("/api/comments/document/{documentId}", documentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].content").value("First"))
                .andExpect(jsonPath("$[1].id").value(2))
                .andExpect(jsonPath("$[1].content").value("Second"));
    }

    // =====================================================
    // === DELETE COMMENT ===
    // =====================================================
    @Test
    void deleteComment_returns204() throws Exception {
        Long commentId = 5L;

        // The service method returns void, so just verify the call
        Mockito.doNothing().when(commentService).deleteComment(commentId);

        mockMvc.perform(delete("/api/comments/{commentId}", commentId))
                .andExpect(status().isNoContent());
    }
}
