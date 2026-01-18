package at.technikum_wien.rest_server.controller;

import at.technikum_wien.rest_server.model.Document;
import at.technikum_wien.rest_server.service.DocumentService;
import at.technikum_wien.rest_server.service.SearchService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController // REST controller exposing search endpoint
@RequestMapping("/api/documents") // Shares base path with DocumentController
public class SearchController {

    private final SearchService searchService;     // Handles Elasticsearch queries
    private final DocumentService documentService; // Loads full documents from DB

    // Constructor injection of dependencies
    public SearchController(SearchService searchService,
                            DocumentService documentService) {
        this.searchService = searchService;
        this.documentService = documentService;
    }

    // === GET: Full-text search for documents ===
    @GetMapping("/search")
    public ResponseEntity<List<Document>> searchDocuments(
            @RequestParam("query") String query) {

        // First search in Elasticsearch and get matching document IDs
        List<Long> documentIds = searchService.searchDocumentIds(query);

        // Then load full document entities from the database
        List<Document> documents = documentIds.stream()
                .map(documentService::getDocumentById)
                .flatMap(Optional::stream) // Filters out missing documents safely
                .toList();

        // Return matching documents to the client
        return ResponseEntity.ok(documents);
    }
}