package at.technikum_wien.rest_server.controller;

import at.technikum_wien.rest_server.service.DocumentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final DocumentService documentService;

    @Autowired
    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @PostMapping("/upload")
    public ResponseEntity<String> uploadDocument(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return new ResponseEntity<>("Please select a file to upload.", HttpStatus.BAD_REQUEST);
        }

        // Ensure the uploaded file is a PDF
        if (!"application/pdf".equals(file.getContentType())) {
            return new ResponseEntity<>("Only PDF files are allowed.", HttpStatus.BAD_REQUEST);
        }

        try {
            documentService.saveDocument(file.getOriginalFilename(), file.getBytes());
            return new ResponseEntity<>("File uploaded successfully: " + file.getOriginalFilename(), HttpStatus.CREATED);
        } catch (IOException e) {
            return new ResponseEntity<>("Failed to upload file: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}