package at.technikum_wien.rest_server.controller;

import at.technikum_wien.rest_server.mapper.DocumentMapper;
import at.technikum_wien.rest_server.model.CreateDocumentRequest;
import at.technikum_wien.rest_server.model.Document;
import at.technikum_wien.rest_server.model.DocumentResponse;
import at.technikum_wien.rest_server.model.UpdateDocumentRequest;
import at.technikum_wien.rest_server.service.DocumentService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final DocumentService documentService;
    private final DocumentMapper documentMapper; // inject mapper

    @Autowired
    public DocumentController(DocumentService documentService, DocumentMapper documentMapper) {
        this.documentService = documentService;
        this.documentMapper = documentMapper;
    }

    // === POST ===
    @PostMapping("/upload")
    public ResponseEntity<DocumentResponse> uploadDocument(
            @Valid @ModelAttribute CreateDocumentRequest request) {

        MultipartFile file = request.getFile();

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        if (!"application/pdf".equals(file.getContentType())) {
            return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).build();
        }

        try {
            // Upload to MinIO and get the object key
            String objectKey = documentService.uploadToMinio(file);

            // Save metadata in DB (includes MinIO object key)
            Document saved = documentService.saveDocument(
                    file.getOriginalFilename(),
                    file.getSize(),
                    objectKey
            );

            // Map to response DTO
            DocumentResponse response = documentMapper.toResponse(saved);

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // === GET ALL ===
    @GetMapping
    public ResponseEntity<List<DocumentResponse>> getAllDocuments() {
        List<Document> documents = documentService.getAllDocuments();
        List<DocumentResponse> response = documents.stream()
                .map(documentMapper::toResponse)
                .toList();
        return ResponseEntity.ok(response);
    }

    // === GET BY ID ===
    @GetMapping("/{id}")
    public ResponseEntity<DocumentResponse> getDocumentById(@PathVariable Long id) {
        return documentService.getDocumentById(id)
                .map(documentMapper::toResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // === UPDATE (PUT) ===
    @PutMapping("/{id}")
    public ResponseEntity<DocumentResponse> updateDocument(
            @PathVariable Long id,
            @Valid @RequestBody UpdateDocumentRequest dto) {

        return documentService.updateDocument(id, dto)
                .map(documentMapper::toResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // === DELETE ===
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDocument(@PathVariable Long id) {
        boolean deleted = documentService.deleteDocument(id);
        if (deleted) {
            return ResponseEntity.noContent().build(); // 204 No Content
        } else {
            return ResponseEntity.notFound().build(); // 404 Not Found
        }
    }
}
