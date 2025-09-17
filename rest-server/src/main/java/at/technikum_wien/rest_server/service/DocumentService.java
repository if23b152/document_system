package at.technikum_wien.rest_server.service;

import at.technikum_wien.rest_server.model.Document;
import at.technikum_wien.rest_server.repository.DocumentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class DocumentService {

    private final DocumentRepository documentRepository;

    @Autowired
    public DocumentService(DocumentRepository documentRepository) {
        this.documentRepository = documentRepository;
    }

    @Transactional
    public Document saveDocument(String fileName, long fileSize, String storagePath) {
        Document document = new Document();
        document.setFileName(fileName);
        document.setFileSize(fileSize);
        document.setStoragePath(storagePath);
        document.setUploadTimestamp(LocalDateTime.now());
        return documentRepository.save(document);
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
}

/*
docker exec -it dms-postgres psql -U postgres
After running this, you should see a prompt like:
postgres=#
This means you are now inside the PostgreSQL CLI.
\c documentdb
\d documents

 */