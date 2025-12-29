package at.technikum_wien.rest_server.service;

import at.technikum_wien.rest_server.mapper.DocumentMapper;
import at.technikum_wien.rest_server.model.UpdateDocumentRequest;
import at.technikum_wien.rest_server.producer.DocumentMessageProducer;
import at.technikum_wien.rest_server.model.Document;
import at.technikum_wien.rest_server.repository.DocumentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Handles persistence of document metadata and triggering the OCR workflow.
 */
@Service
public class DocumentService {

    private static final Logger log = LoggerFactory.getLogger(DocumentService.class);

    private final DocumentRepository documentRepository;
    private final DocumentMessageProducer messageProducer;
    private final MinioService minioService; // for MinIO file upload
    private final DocumentMapper documentMapper; // <-- add this

    @Autowired
    public DocumentService(DocumentRepository documentRepository,
                           DocumentMessageProducer messageProducer,
                           MinioService minioService,
                           DocumentMapper documentMapper) { // <-- inject mapper
        this.documentRepository = documentRepository;
        this.messageProducer = messageProducer;
        this.minioService = minioService;
        this.documentMapper = documentMapper;
    }

    /**
     * Uploads a file to MinIO and returns its object key.
     */
    public String uploadToMinio(MultipartFile file) {
        log.info("Uploading file '{}' to MinIO...", file.getOriginalFilename());
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
    @Transactional
    public Document saveDocument(String fileName, long fileSize, String objectKey) {
        Document document = new Document();
        document.setFileName(fileName);
        document.setFileSize(fileSize);
        document.setMinioObjectKey(objectKey); // use object key instead of storagePath
        document.setUploadTimestamp(LocalDateTime.now());
        document.setOcrProcessed(false);
        // document.setGenAiSummarized(false);

        Document savedDocument = documentRepository.save(document);

        // Send message to OCR queue (includes objectKey)
        try {
            messageProducer.sendOcrProcessingRequest(
                    savedDocument.getId(),
                    savedDocument.getFileName(),
                    savedDocument.getMinioObjectKey()
            );
            log.info("OCR processing request sent for document ID {}", savedDocument.getId());
        } catch (AmqpException e) {
            log.error("[RabbitMQ ERROR] Document ID {} saved but failed to send OCR message. Error: {}",
                    savedDocument.getId(), e.getMessage(), e);
            // Do not rethrow to avoid rollback of the DB transaction
        }

        return savedDocument;
    }

    @Transactional
    public void markProcessingFailed(Long documentId, String errorMessage) {
        Document doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("Document not found: " + documentId));

        doc.setOcrProcessed(false); // failure means OCR/summary didn't complete

        documentRepository.save(doc);

        log.error("Document {} marked as failed: {}", documentId, errorMessage);
    }

    // -------------------------------
    // UPDATE METHODS FOR OCR + SUMMARY
    // -------------------------------
    @Transactional
    public void saveSummary(Long documentId, String summary) {
        Document doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("Document not found: " + documentId));

        doc.setOcrProcessed(true);  // OCR completed successfully

        if (summary != null) {
            doc.setSummary(summary);  // GenAI summary
        }

        documentRepository.save(doc);

        log.info("Document {} successfully updated with summary.", documentId);
    }

    public List<Document> getAllDocuments() {
        return documentRepository.findAll();
    }

    public Optional<Document> getDocumentById(Long id) {
        return documentRepository.findById(id);
    }

    @Transactional
    public Optional<Document> updateDocument(Long id, UpdateDocumentRequest dto) {
        return documentRepository.findById(id)
                .map(existing -> {
                    documentMapper.updateDocumentFromDto(dto, existing);
                    return documentRepository.save(existing);
                });
    }

    @Transactional
    public boolean deleteDocument(Long id) {
        if (documentRepository.existsById(id)) {
            documentRepository.deleteById(id);
            return true;
        }
        return false;
    }
}
