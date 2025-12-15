package at.technikum_wien.rest_server.controller;

import at.technikum_wien.rest_server.model.Document;
import at.technikum_wien.rest_server.service.DocumentService;
import at.technikum_wien.rest_server.service.SearchService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/documents")
public class SearchController {

    private final SearchService searchService;
    private final DocumentService documentService;

    public SearchController(SearchService searchService,
                            DocumentService documentService) {
        this.searchService = searchService;
        this.documentService = documentService;
    }

    @GetMapping("/search")
    public ResponseEntity<List<Document>> searchDocuments(
            @RequestParam("query") String query) {

        List<Long> documentIds = searchService.searchDocumentIds(query);

        List<Document> documents = documentIds.stream()
                .map(documentService::getDocumentById)
                .flatMap(Optional::stream)
                .toList();

        return ResponseEntity.ok(documents);
    }
}
