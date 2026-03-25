package at.technikum_wien.rest_server.controller;

import at.technikum_wien.rest_server.mapper.DocumentMapper;
import at.technikum_wien.rest_server.model.CreateDocumentRequest;
import at.technikum_wien.rest_server.model.Document;
import at.technikum_wien.rest_server.model.DocumentResponse;
import at.technikum_wien.rest_server.model.DocumentTextResponse;
import at.technikum_wien.rest_server.model.UpdateDocumentRequest;
import at.technikum_wien.rest_server.service.DocumentService;
import at.technikum_wien.rest_server.service.MinioService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController // Marks this class as a REST controller (JSON in/out)
@RequestMapping("/api/documents") // Base URL path for all document endpoints
public class DocumentController {

    private static final Logger log = LoggerFactory.getLogger(DocumentController.class);

    private final DocumentService documentService;
    private final DocumentMapper documentMapper; // Maps between Entity and DTO
    private final MinioService minioService;

    @Autowired // Constructor injection of dependencies
    public DocumentController(DocumentService documentService,
                              DocumentMapper documentMapper,
                              MinioService minioService) {
        this.documentService = documentService;
        this.documentMapper = documentMapper;
        this.minioService = minioService;
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
    @Transactional(readOnly = true)
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
    @Transactional(readOnly = true)
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

    // === GET: Stream PDF file by document ID ===
    @GetMapping("/{id}/file")
    @Transactional(readOnly = true)
    public ResponseEntity<StreamingResponseBody> getDocumentFile(@PathVariable Long id) {
        return documentService.getDocumentById(id)
                .map(document -> {
                    String objectKey = document.getMinioObjectKey();
                    if (objectKey == null || objectKey.isBlank()) {
                        objectKey = document.getFileName();
                    }

                    if (objectKey == null || objectKey.isBlank()) {
                        StreamingResponseBody body = outputStream ->
                                outputStream.write("Document file not found.".getBytes(StandardCharsets.UTF_8));
                        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                .contentType(MediaType.TEXT_PLAIN)
                                .body(body);
                    }

                    try {
                        String fileName = document.getFileName() != null
                                ? document.getFileName()
                                : "document-" + document.getId() + ".pdf";

                        String safeFileName = fileName.replace("\"", "'");

                        InputStream inputStream = minioService.downloadDocument(objectKey);
                        StreamingResponseBody body = outputStream -> {
                            try (inputStream) {
                                inputStream.transferTo(outputStream);
                            } catch (Exception e) {
                                log.error("Streaming failed for document {}: {}", id, e.getMessage(), e);
                                throw e;
                            }
                        };

                        return ResponseEntity.ok()
                                .contentType(MediaType.APPLICATION_PDF)
                                .header(HttpHeaders.CONTENT_DISPOSITION,
                                        "inline; filename=\"" + safeFileName + "\"")
                                .body(body);
                    } catch (Exception e) {
                        log.error("Failed to load file for document {}: {}", id, e.getMessage(), e);
                        StreamingResponseBody body = outputStream ->
                                outputStream.write("Failed to fetch document file.".getBytes(StandardCharsets.UTF_8));
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .contentType(MediaType.TEXT_PLAIN)
                                .body(body);
                    }
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // === GET: Retrieve OCR/extracted text for RSVP reader ===
    @GetMapping("/{id}/text")
    @Transactional(readOnly = true)
    public ResponseEntity<?> getDocumentText(@PathVariable Long id) {
        return documentService.getDocumentById(id)
                .map(document -> {
                    String text = document.getOcrText();
                    String error = document.getOcrError();

                    if (error != null && !error.isBlank() && (text == null || text.isBlank())) {
                        return ResponseEntity.status(HttpStatus.CONFLICT)
                                .body(new DocumentTextResponse("", "FAILED", error));
                    }

                    if (text == null || text.isBlank()) {
                        return ResponseEntity.status(HttpStatus.ACCEPTED)
                                .body(new DocumentTextResponse("", "PROCESSING", null));
                    }

                    return ResponseEntity.ok(new DocumentTextResponse(text, "READY", null));
                })
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
