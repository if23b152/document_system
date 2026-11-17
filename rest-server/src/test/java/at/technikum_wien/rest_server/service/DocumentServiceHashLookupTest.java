package at.technikum_wien.rest_server.service;

import at.technikum_wien.rest_server.mapper.DocumentMapper;
import at.technikum_wien.rest_server.model.AppUser;
import at.technikum_wien.rest_server.model.Document;
import at.technikum_wien.rest_server.producer.DocumentMessageProducer;
import at.technikum_wien.rest_server.repository.AppUserRepository;
import at.technikum_wien.rest_server.repository.DocumentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** The hash lookup must respect document ownership like every other read. */
class DocumentServiceHashLookupTest {

    private static final String HASH = "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad";

    private DocumentRepository repository;
    private AppUserService appUserService;
    private DocumentService service;

    @BeforeEach
    void setUp() {
        repository = Mockito.mock(DocumentRepository.class);
        appUserService = Mockito.mock(AppUserService.class);
        service = new DocumentService(repository, Mockito.mock(DocumentMessageProducer.class),
                Mockito.mock(MinioService.class), Mockito.mock(DocumentMapper.class),
                appUserService, Mockito.mock(AppUserRepository.class));
    }

    @Test
    void regularUser_onlySearchesOwnDocuments() {
        AppUser alice = new AppUser();
        alice.setUsername("alice");
        Document own = new Document();
        when(appUserService.currentUserIsAdmin()).thenReturn(false);
        when(appUserService.getCurrentUser()).thenReturn(alice);
        when(repository.findFirstByContentSha256AndOwnerUsernameOrderByUploadTimestampDesc(HASH, "alice"))
                .thenReturn(Optional.of(own));

        assertEquals(Optional.of(own), service.getDocumentByContentHash(HASH));
        verify(repository, never()).findFirstByContentSha256OrderByUploadTimestampDesc(HASH);
    }

    @Test
    void regularUser_doesNotSeeOtherUsersDocuments() {
        AppUser bob = new AppUser();
        bob.setUsername("bob");
        when(appUserService.currentUserIsAdmin()).thenReturn(false);
        when(appUserService.getCurrentUser()).thenReturn(bob);
        when(repository.findFirstByContentSha256AndOwnerUsernameOrderByUploadTimestampDesc(HASH, "bob"))
                .thenReturn(Optional.empty());

        assertTrue(service.getDocumentByContentHash(HASH).isEmpty());
    }

    @Test
    void admin_searchesAllDocuments() {
        Document any = new Document();
        when(appUserService.currentUserIsAdmin()).thenReturn(true);
        when(repository.findFirstByContentSha256OrderByUploadTimestampDesc(HASH)).thenReturn(Optional.of(any));

        assertEquals(Optional.of(any), service.getDocumentByContentHash(HASH));
    }
}
