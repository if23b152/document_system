package at.technikum_wien.rest_server.service;

import at.technikum_wien.rest_server.model.Document;
import at.technikum_wien.rest_server.repository.DocumentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.List;

/**
 * Documents uploaded before the content hash was recorded have no {@code contentSha256}.
 * On startup their files are read from MinIO once and the hash is filled in, so they can be
 * found by hash as well. Failures (e.g. a missing object) are logged and retried on the next start.
 */
@Component
public class ContentHashBackfill implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ContentHashBackfill.class);

    private final DocumentRepository documentRepository;
    private final MinioService minioService;

    public ContentHashBackfill(DocumentRepository documentRepository, MinioService minioService) {
        this.documentRepository = documentRepository;
        this.minioService = minioService;
    }

    @Override
    public void run(ApplicationArguments args) {
        List<Document> missing = documentRepository.findByContentSha256IsNull();
        if (missing.isEmpty()) {
            return;
        }
        log.info("Computing content hashes for {} existing document(s)", missing.size());

        int done = 0;
        for (Document document : missing) {
            try (InputStream content = minioService.downloadDocument(document.getMinioObjectKey())) {
                document.setContentSha256(ContentHash.sha256(content));
                documentRepository.save(document);
                done++;
            } catch (Exception e) {
                log.warn("Could not hash document {}: {}", document.getId(), e.getMessage());
            }
        }
        log.info("Content hashes filled in for {} of {} document(s)", done, missing.size());
    }
}
