package at.technikum_wien.rest_server.service;

import at.technikum_wien.rest_server.mapper.DocumentMapper;
import at.technikum_wien.rest_server.model.AppUser;
import at.technikum_wien.rest_server.model.UpdateDocumentRequest;
import at.technikum_wien.rest_server.producer.DocumentMessageProducer;
import at.technikum_wien.rest_server.model.Document;
import at.technikum_wien.rest_server.repository.AppUserRepository;
import at.technikum_wien.rest_server.repository.DocumentRepository;
import org.springframework.boot.ApplicationRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.springframework.http.HttpStatus.NOT_FOUND;

/**
 * Handles persistence of document metadata and triggering the OCR workflow.
 */
@Service // Marks this class as a Spring service (business logic layer)
public class DocumentService {

    private static final Logger log = LoggerFactory.getLogger(DocumentService.class);

    private final DocumentRepository documentRepository; // JPA repository for DB access
    private final DocumentMessageProducer messageProducer; // Sends messages to RabbitMQ
    private final MinioService minioService; // Handles file storage in MinIO
    private final DocumentMapper documentMapper; // Maps DTOs to entities
    private final AppUserService appUserService;
    private final AppUserRepository appUserRepository;

    @Autowired // Constructor injection of all dependencies
    public DocumentService(DocumentRepository documentRepository,
                           DocumentMessageProducer messageProducer,
                           MinioService minioService,
                           DocumentMapper documentMapper,
                           AppUserService appUserService,
                           AppUserRepository appUserRepository) {
        this.documentRepository = documentRepository;
        this.messageProducer = messageProducer;
        this.minioService = minioService;
        this.documentMapper = documentMapper;
        this.appUserService = appUserService;
        this.appUserRepository = appUserRepository;
    }

    /**
     * Uploads a file to MinIO and returns its object key.
     */
    public String uploadToMinio(MultipartFile file) {
        log.info("Uploading file '{}' to MinIO...", file.getOriginalFilename());

        // Delegate actual upload to MinioService
        String objectKey = minioService.uploadDocument(file);

        log.info("File uploaded to MinIO with object key: {}", objectKey);
        return objectKey;
    }

    /**
     * Saves a new document record and triggers the OCR worker asynchronously.
     *
     * @param fileName The name of the file.
     * @param fileSize The size of the file in bytes.
     * @param objectKey The MinIO object key.
     * @return The saved Document entity.
     */
    @Transactional // Ensures DB operations happen in a single transaction
    public Document saveDocument(String fileName, long fileSize, String objectKey) {
        AppUser currentUser = appUserService.getCurrentUser();

        // Create new document entity
        Document document = new Document();
        document.setFileName(fileName);
        document.setFileSize(fileSize);
        document.setMinioObjectKey(objectKey);
        document.setOwner(currentUser);
        document.setUploadTimestamp(LocalDateTime.now());
        document.setOcrProcessed(false); // OCR not done yet
        document.setOcrText(null);
        document.setOcrError(null);

        // Persist document metadata in the database
        Document savedDocument = documentRepository.save(document);

        // Send message to RabbitMQ to trigger OCR processing
        try {
            messageProducer.sendOcrProcessingRequest(
                    savedDocument.getId(),
                    savedDocument.getFileName(),
                    savedDocument.getMinioObjectKey()
            );
            log.info("OCR processing request sent for document ID {}", savedDocument.getId());
        } catch (AmqpException e) {
            // If messaging fails, we keep the DB record but log the error
            log.error("[RabbitMQ ERROR] Document ID {} saved but failed to send OCR message. Error: {}",
                    savedDocument.getId(), e.getMessage(), e);
            // Do not rethrow to avoid rolling back the DB transaction
        }

        return savedDocument;
    }

    /**
     * Marks a document as failed during OCR or AI processing.
     */
    @Transactional
    public void markProcessingFailed(Long documentId, String errorMessage) {

        // Load document or fail if it doesn't exist
        Document doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("Document not found: " + documentId));

        // Reset processing flag
        doc.setOcrProcessed(false);
        doc.setOcrError(errorMessage);

        // Save updated state
        documentRepository.save(doc);

        log.error("Document {} marked as failed: {}", documentId, errorMessage);
    }

    // -------------------------------
    // UPDATE METHODS FOR OCR + SUMMARY
    // -------------------------------

    /**
     * Saves OCR text, summary, and processing state.
     */
    @Transactional
    public void saveOcrResult(Long documentId, String ocrText, String summary, boolean success, String errorMessage) {

        // Load document or fail
        Document doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("Document not found: " + documentId));

        // Mark OCR as completed if we have text
        boolean hasText = ocrText != null && !ocrText.isBlank();
        doc.setOcrProcessed(hasText);
        doc.setOcrText(ocrText);
        doc.setOcrError(errorMessage);

        // Store summary if available
        if (summary != null) {
            doc.setSummary(summary);
        }

        // Persist changes
        documentRepository.save(doc);

        log.info("Document {} updated with OCR text (len={}) and summary (len={}). Success: {}",
                documentId,
                ocrText != null ? ocrText.length() : 0,
                summary != null ? summary.length() : 0,
                success);
    }

    // === Simple CRUD methods ===

    public List<Document> getAllDocuments() {
        if (appUserService.currentUserIsAdmin()) {
            return documentRepository.findAllByOrderByUploadTimestampDesc();
        }
        return documentRepository.findByOwnerUsernameOrderByUploadTimestampDesc(
                appUserService.getCurrentUser().getUsername()
        );
    }

    public Optional<Document> getDocumentById(Long id) {
        if (appUserService.currentUserIsAdmin()) {
            return documentRepository.findById(id);
        }
        return documentRepository.findByIdAndOwnerUsername(
                id,
                appUserService.getCurrentUser().getUsername()
        );
    }

    public Document getAccessibleDocumentOrThrow(Long id) {
        return getDocumentById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Document not found."));
    }

    public List<Document> getAccessibleDocumentsByIds(List<Long> ids) {
        return ids.stream()
                .map(this::getDocumentById)
                .flatMap(Optional::stream)
                .toList();
    }

    @Transactional
    public Optional<Document> updateDocument(Long id, UpdateDocumentRequest dto) {
        return getDocumentById(id)
                .map(existing -> {
                    // Apply changes from DTO to entity
                    documentMapper.updateDocumentFromDto(dto, existing);
                    return documentRepository.save(existing);
                });
    }

    @Transactional
    public boolean deleteDocument(Long id) {
        // Fetch the document to get its MinIO object key
        Optional<Document> documentOpt = getDocumentById(id);

        if (documentOpt.isEmpty()) {
            return false; // Document does not exist
        }

        Document document = documentOpt.get();

        try {
            // Delete the file from MinIO
            minioService.deleteDocument(document.getMinioObjectKey());
        } catch (Exception e) {
            log.error("Failed to delete document file from MinIO for document {}: {}", id, e.getMessage(), e);
            // Depending on requirements, you could either:
            // a) return false (abort deletion)
            // b) continue and delete DB record anyway (we choose b here)
        }

        // Delete the document record from the DB (comments cascade automatically)
        documentRepository.deleteById(id);
        log.info("Deleted document {} from database and MinIO.", id);
        return true;
    }

    @Transactional
    public void deleteDocumentsForOwner(AppUser owner) {
        documentRepository.findCleanupDataByOwnerId(owner.getId())
                .forEach(this::deleteStoredDocument);
    }

    @Bean
    public ApplicationRunner documentOwnershipBootstrapRunner() {
        return args -> {
            cleanupTemporaryUsers();
            assignUnownedDocumentsToAdmin();
        };
    }

    @Transactional
    public void assignUnownedDocumentsToAdmin() {
        appUserRepository.findByUsername("admin").ifPresent(documentRepository::assignOwnerToUnownedDocuments);
    }

    @Transactional
    public void cleanupTemporaryUsers() {
        appUserRepository.findByTemporaryTrue().forEach(user -> {
            deleteDocumentsForOwner(user);
            appUserRepository.delete(user);
        });
    }

    private void deleteStoredDocument(DocumentRepository.DocumentCleanupProjection document) {
        try {
            minioService.deleteDocument(document.getMinioObjectKey());
        } catch (Exception e) {
            log.error("Failed to delete temporary document file {}: {}", document.getId(), e.getMessage(), e);
        }
        documentRepository.deleteById(document.getId());
    }

}
