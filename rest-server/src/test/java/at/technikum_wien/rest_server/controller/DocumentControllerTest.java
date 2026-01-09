package at.technikum_wien.rest_server.controller;

import at.technikum_wien.rest_server.mapper.DocumentMapper;
import at.technikum_wien.rest_server.model.Document;
import at.technikum_wien.rest_server.model.DocumentResponse;
import at.technikum_wien.rest_server.service.DocumentService;
import at.technikum_wien.rest_server.model.UpdateDocumentRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DocumentController.class)
class DocumentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DocumentService documentService;

    @MockitoBean
    private DocumentMapper documentMapper;

    @Autowired
    private ObjectMapper objectMapper;

    // ===========================
    // SUCCESS CASE
    // ===========================
    @Test
    void uploadDocument_success_returns201() throws Exception {
        // Arrange
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.pdf",
                "application/pdf",
                "dummy pdf content".getBytes()
        );

        Document savedDocument = new Document();
        savedDocument.setId(1L);
        savedDocument.setFileName("test.pdf");
        savedDocument.setFileSize(123L);
        savedDocument.setMinioObjectKey("minio-key");
        savedDocument.setUploadTimestamp(LocalDateTime.now());

        DocumentResponse responseDto = new DocumentResponse();
        responseDto.setId(1L);
        responseDto.setFileName("test.pdf");
        responseDto.setFileSize(123L);

        Mockito.when(documentService.uploadToMinio(Mockito.any()))
                .thenReturn("minio-key");

        Mockito.when(documentService.saveDocument(
                Mockito.eq("test.pdf"),
                Mockito.anyLong(),
                Mockito.eq("minio-key")
        )).thenReturn(savedDocument);

        Mockito.when(documentMapper.toResponse(savedDocument))
                .thenReturn(responseDto);

        // Act & Assert
        mockMvc.perform(
                        multipart("/api/documents/upload")
                                .file(file)
                                .contentType(MediaType.MULTIPART_FORM_DATA)
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.fileName").value("test.pdf"))
                .andExpect(jsonPath("$.fileSize").value(123));
    }

    // ===========================
    // EMPTY FILE → 400
    // ===========================
    @Test
    void uploadDocument_emptyFile_returns400() throws Exception {
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file",
                "test.pdf",
                "application/pdf",
                new byte[0]
        );

        mockMvc.perform(
                        multipart("/api/documents/upload")
                                .file(emptyFile)
                                .contentType(MediaType.MULTIPART_FORM_DATA)
                )
                .andExpect(status().isBadRequest());
    }

    // ===========================
    // WRONG CONTENT TYPE → 415
    // ===========================
    @Test
    void uploadDocument_notPdf_returns415() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.txt",
                "text/plain",
                "not a pdf".getBytes()
        );

        mockMvc.perform(
                        multipart("/api/documents/upload")
                                .file(file)
                                .contentType(MediaType.MULTIPART_FORM_DATA)
                )
                .andExpect(status().isUnsupportedMediaType());
    }

    // ===========================
    // EXCEPTION → 500
    // ===========================
    @Test
    void uploadDocument_exception_returns500() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.pdf",
                "application/pdf",
                "dummy".getBytes()
        );

        Mockito.when(documentService.uploadToMinio(Mockito.any()))
                .thenThrow(new RuntimeException("MinIO down"));

        mockMvc.perform(
                        multipart("/api/documents/upload")
                                .file(file)
                                .contentType(MediaType.MULTIPART_FORM_DATA)
                )
                .andExpect(status().isInternalServerError());
    }

    // =====================================================
    // === GET ALL
    // =====================================================
    @Test
    void getAllDocuments_returnsList() throws Exception {
        Document d1 = new Document();
        d1.setId(1L);
        Document d2 = new Document();
        d2.setId(2L);

        DocumentResponse r1 = new DocumentResponse(1L, "a.pdf", 10, null, LocalDateTime.now(), List.of());
        DocumentResponse r2 = new DocumentResponse(2L, "b.pdf", 20, null, LocalDateTime.now(), List.of());

        Mockito.when(documentService.getAllDocuments()).thenReturn(List.of(d1, d2));
        Mockito.when(documentMapper.toResponse(d1)).thenReturn(r1);
        Mockito.when(documentMapper.toResponse(d2)).thenReturn(r2);

        mockMvc.perform(get("/api/documents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[1].id").value(2));
    }

    // =====================================================
    // === GET BY ID
    // =====================================================
    @Test
    void getDocumentById_found_returns200() throws Exception {
        Document doc = new Document();
        doc.setId(1L);

        DocumentResponse response = new DocumentResponse(1L, "a.pdf", 10, null, LocalDateTime.now(), List.of());

        Mockito.when(documentService.getDocumentById(1L)).thenReturn(Optional.of(doc));
        Mockito.when(documentMapper.toResponse(doc)).thenReturn(response);

        mockMvc.perform(get("/api/documents/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void getDocumentById_notFound_returns404() throws Exception {
        Mockito.when(documentService.getDocumentById(99L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/documents/99"))
                .andExpect(status().isNotFound());
    }

    // =====================================================
    // === UPDATE
    // =====================================================
    @Test
    void updateDocument_found_returns200() throws Exception {
        UpdateDocumentRequest dto = new UpdateDocumentRequest(
                "new.pdf",
                "summary",
                List.of("tag1", "tag2")
        );

        Document updated = new Document();
        updated.setId(1L);

        DocumentResponse response = new DocumentResponse(1L, "new.pdf", 10, "summary", LocalDateTime.now(), List.of("tag1", "tag2"));

        Mockito.when(documentService.updateDocument(Mockito.eq(1L), Mockito.any()))
                .thenReturn(Optional.of(updated));

        Mockito.when(documentMapper.toResponse(updated)).thenReturn(response);

        mockMvc.perform(
                        put("/api/documents/1")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(dto))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fileName").value("new.pdf"))
                .andExpect(jsonPath("$.tags.length()").value(2));
    }

    @Test
    void updateDocument_notFound_returns404() throws Exception {
        UpdateDocumentRequest dto = new UpdateDocumentRequest("x.pdf", null, null);

        Mockito.when(documentService.updateDocument(Mockito.eq(99L), Mockito.any()))
                .thenReturn(Optional.empty());

        mockMvc.perform(
                        put("/api/documents/99")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(dto))
                )
                .andExpect(status().isNotFound());
    }

    // =====================================================
    // === DELETE
    // =====================================================
    @Test
    void deleteDocument_found_returns204() throws Exception {
        Mockito.when(documentService.deleteDocument(1L)).thenReturn(true);

        mockMvc.perform(delete("/api/documents/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteDocument_notFound_returns404() throws Exception {
        Mockito.when(documentService.deleteDocument(99L)).thenReturn(false);

        mockMvc.perform(delete("/api/documents/99"))
                .andExpect(status().isNotFound());
    }
}
