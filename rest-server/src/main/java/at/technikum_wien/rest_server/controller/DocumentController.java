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

@RestController // Marks this class as a REST controller (JSON in/out)
@RequestMapping("/api/documents") // Base URL path for all document endpoints
public class DocumentController {

    private final DocumentService documentService;
    private final DocumentMapper documentMapper; // Maps between Entity and DTO

    @Autowired // Constructor injection of dependencies
    public DocumentController(DocumentService documentService, DocumentMapper documentMapper) {
        this.documentService = documentService;
        this.documentMapper = documentMapper;
    }

    // === POST: Upload a new document ===
    @PostMapping("/upload")
    public ResponseEntity<DocumentResponse> uploadDocument(
            @Valid @ModelAttribute CreateDocumentRequest request) {

        // Extract uploaded file from multipart request
        MultipartFile file = request.getFile();

        // Reject empty file uploads
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        // Only allow PDF uploads
        if (!"application/pdf".equals(file.getContentType())) {
            return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).build();
        }

        try {
            // Upload the file to MinIO and receive its object key
            String objectKey = documentService.uploadToMinio(file);

            // Save document metadata in the database (including MinIO key)
            Document saved = documentService.saveDocument(
                    file.getOriginalFilename(),
                    file.getSize(),
                    objectKey
            );

            // Convert entity to response DTO
            DocumentResponse response = documentMapper.toResponse(saved);

            // Return 201 Created with document metadata
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            // Catch-all error handling (e.g., MinIO or DB failure)
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // === GET: Retrieve all documents ===
    @GetMapping
    public ResponseEntity<List<DocumentResponse>> getAllDocuments() {
        List<Document> documents = documentService.getAllDocuments();

        // Convert entities to DTOs before returning to client
        List<DocumentResponse> response = documents.stream()
                .map(documentMapper::toResponse)
                .toList();

        return ResponseEntity.ok(response);
    }

    // === GET: Retrieve one document by ID ===
    @GetMapping("/{id}")
    public ResponseEntity<DocumentResponse> getDocumentById(@PathVariable Long id) {
        return documentService.getDocumentById(id)
                .map(documentMapper::toResponse) // Convert entity to DTO
                .map(ResponseEntity::ok)          // Return 200 OK
                .orElse(ResponseEntity.notFound().build()); // Or 404 Not Found
    }

    // === PUT: Update document metadata ===
    @PutMapping("/{id}")
    public ResponseEntity<DocumentResponse> updateDocument(
            @PathVariable Long id,
            @Valid @RequestBody UpdateDocumentRequest dto) {

        return documentService.updateDocument(id, dto)
                .map(documentMapper::toResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // === DELETE: Remove a document ===
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