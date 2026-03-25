package at.technikum_wien.rest_server.controller;

import at.technikum_wien.rest_server.mapper.DocumentMapper;
import at.technikum_wien.rest_server.model.Document;
import at.technikum_wien.rest_server.model.DocumentResponse;
import at.technikum_wien.rest_server.service.DocumentService;
import at.technikum_wien.rest_server.service.SearchService;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController // REST controller exposing search endpoint
@RequestMapping("/api/documents") // Shares base path with DocumentController
public class SearchController {

    private final SearchService searchService;     // Handles Elasticsearch queries
    private final DocumentService documentService; // Loads full documents from DB
    private final DocumentMapper documentMapper;

    // Constructor injection of dependencies
    public SearchController(SearchService searchService,
                            DocumentService documentService,
                            DocumentMapper documentMapper) {
        this.searchService = searchService;
        this.documentService = documentService;
        this.documentMapper = documentMapper;
    }

    // === GET: Full-text search for documents ===
    @GetMapping("/search")
    @Transactional(readOnly = true)
    public ResponseEntity<List<DocumentResponse>> searchDocuments(
            @RequestParam("query") String query) {

        // First search in Elasticsearch and get matching document IDs
        List<Long> documentIds = searchService.searchDocumentIds(query);

        // Then load full document entities from the database
        List<Document> documents = documentService.getAccessibleDocumentsByIds(documentIds);

        List<DocumentResponse> response = documents.stream()
                .map(documentMapper::toResponse)
                .toList();

        // Return matching documents to the client
        return ResponseEntity.ok(response);
    }
}
