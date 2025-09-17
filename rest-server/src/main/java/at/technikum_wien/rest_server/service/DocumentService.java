package at.technikum_wien.rest_server.service;

import at.technikum_wien.rest_server.model.Document;
import at.technikum_wien.rest_server.repository.DocumentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

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
}

/*
docker exec -it dms-postgres psql -U postgres
After running this, you should see a prompt like:
postgres=#
This means you are now inside the PostgreSQL CLI.
\c documentdb
\d documents

 */