package at.technikum_wien.rest_server.service;

import at.technikum_wien.rest_server.model.Document;
import at.technikum_wien.rest_server.repository.DocumentRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ContentHashBackfillTest {

    private final DocumentRepository repository = Mockito.mock(DocumentRepository.class);
    private final MinioService minio = Mockito.mock(MinioService.class);
    private final ContentHashBackfill backfill = new ContentHashBackfill(repository, minio);

    @Test
    void fillsInMissingHashesFromStoredFiles() {
        Document doc = document("key-1");
        when(repository.findByContentSha256IsNull()).thenReturn(List.of(doc));
        when(minio.downloadDocument("key-1"))
                .thenReturn(new ByteArrayInputStream("abc".getBytes(StandardCharsets.US_ASCII)));

        backfill.run(null);

        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad", doc.getContentSha256());
        verify(repository).save(doc);
    }

    @Test
    void missingFileIsSkippedWithoutStoppingTheOthers() {
        Document broken = document("gone");
        Document ok = document("key-2");
        when(repository.findByContentSha256IsNull()).thenReturn(List.of(broken, ok));
        when(minio.downloadDocument("gone")).thenThrow(new RuntimeException("NoSuchKey"));
        when(minio.downloadDocument("key-2"))
                .thenReturn(new ByteArrayInputStream("abc".getBytes(StandardCharsets.US_ASCII)));

        backfill.run(null);

        assertNull(broken.getContentSha256());
        verify(repository, never()).save(broken);
        verify(repository).save(ok);
    }

    @Test
    void nothingToDoWhenAllDocumentsHaveHashes() {
        when(repository.findByContentSha256IsNull()).thenReturn(List.of());

        backfill.run(null);

        verify(minio, never()).downloadDocument(any());
    }

    private static Document document(String objectKey) {
        Document document = new Document();
        document.setMinioObjectKey(objectKey);
        return document;
    }
}
