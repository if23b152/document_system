package at.technikum_wien.rest_server.service;

import at.technikum_wien.rest_server.messaging.producer.DocumentMessageProducer;
import at.technikum_wien.rest_server.model.Document;
import at.technikum_wien.rest_server.repository.DocumentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class DocumentService {

    private static final Logger log = LoggerFactory.getLogger(DocumentService.class);

    private final DocumentRepository documentRepository;
    private final DocumentMessageProducer messageProducer; // NEW: Inject the producer

    @Autowired
    public DocumentService(DocumentRepository documentRepository,
                           DocumentMessageProducer messageProducer) { // NEW: Constructor injection
        this.documentRepository = documentRepository;
        this.messageProducer = messageProducer;
    }

    /**
     * Saves a new document record and then sends a message to the queue
     * to trigger the OCR worker.
     * * @param fileName The name of the file.
     * @param fileSize The size of the file in bytes.
     * @param storagePath The path where the file is stored (e.g., MinIO path).
     * @return The saved Document entity.
     */
    @Transactional
    public Document saveDocument(String fileName, long fileSize, String storagePath) {
        Document document = new Document();

        // 1. Persist the document metadata
        document.setFileName(fileName);
        document.setFileSize(fileSize);
        document.setStoragePath(storagePath);
        document.setUploadTimestamp(LocalDateTime.now());
        document.setOcrProcessed(false);
        document.setGenAiSummarized(false);

        Document savedDocument = documentRepository.save(document);

        // 2. Trigger the asynchronous worker with exception handling
        try {
            messageProducer.sendOcrProcessingRequest(savedDocument.getId());
        } catch (AmqpException e) {
            // Failure/exception-handling implemented (AmqpException is the layer-specific exception)
            // CRITICAL LOGGING: Document is saved, but async processing failed to start.
            log.error("===== [SERVICE ERROR] Document ID {} was saved but failed to send to RabbitMQ. Processing requires manual restart! Error: {} =====",
                    savedDocument.getId(), e.getMessage(), e);
            // DO NOT re-throw: Allow the successful database transaction to commit.
        }

        return savedDocument;
    }

    public List<Document> getAllDocuments() {
        return documentRepository.findAll();
    }

    public Optional<Document> getDocumentById(Long id) {
        return documentRepository.findById(id);
    }

    @Transactional
    public Optional<Document> updateDocument(Long id, Document updatedData) {
        return documentRepository.findById(id).map(existing -> {
            existing.setFileName(updatedData.getFileName());
            existing.setTags(updatedData.getTags());
            existing.setSummary(updatedData.getSummary());
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

/*
docker exec -it dms-postgres psql -U postgres
After running this, you should see a prompt like:
postgres=#
This means you are now inside the PostgreSQL CLI.
\c documentdb
\d documents

 */