package at.technikum_wien.rest_server.service;

import at.technikum_wien.rest_server.model.ApiToken;
import at.technikum_wien.rest_server.model.AppUser;
import at.technikum_wien.rest_server.repository.ApiTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ApiTokenServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-18T10:00:00Z");
    private static final LocalDateTime NOW_LOCAL = LocalDateTime.ofInstant(NOW, ZoneOffset.UTC);

    private ApiTokenRepository repository;
    private ApiTokenService service;
    private AppUser alice;

    @BeforeEach
    void setUp() {
        repository = Mockito.mock(ApiTokenRepository.class);
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        service = new ApiTokenService(repository, Clock.fixed(NOW, ZoneOffset.UTC));
        alice = new AppUser();
        alice.setUsername("alice");
        alice.setTemporary(false);
    }

    @Test
    void create_returnsSecretOnceAndStoresOnlyItsHash() {
        var created = service.create(alice, "Laptop", null);

        String secret = created.secret();
        assertTrue(secret.startsWith("dms_"));
        assertEquals(47, secret.length(), "dms_ + 43 base64url characters (256 bit)");
        ApiToken token = created.token();
        assertEquals(ApiTokenService.hash(secret), token.getSecretHash());
        assertNotEquals(secret, token.getSecretHash());
        assertEquals(secret.substring(0, 10), token.getPrefix());
        assertEquals(NOW_LOCAL.plusDays(90), token.getExpiresAt());
    }

    @Test
    void create_generatesDifferentSecrets() {
        assertNotEquals(service.create(alice, "a", null).secret(), service.create(alice, "b", null).secret());
    }

    @Test
    void create_rejectsGuestAccounts() {
        alice.setTemporary(true);

        assertThrows(ResponseStatusException.class, () -> service.create(alice, "Laptop", null));
        verify(repository, never()).save(any());
    }

    @Test
    void create_validatesNameValidityAndLimit() {
        assertThrows(ResponseStatusException.class, () -> service.create(alice, "  ", null));
        assertThrows(ResponseStatusException.class, () -> service.create(alice, "x".repeat(61), null));
        assertThrows(ResponseStatusException.class, () -> service.create(alice, "Laptop", 0));
        assertThrows(ResponseStatusException.class, () -> service.create(alice, "Laptop", 366));

        when(repository.countByUser(alice)).thenReturn((long) ApiTokenService.MAX_TOKENS_PER_USER);
        assertThrows(ResponseStatusException.class, () -> service.create(alice, "Laptop", null));
    }

    @Test
    void authenticate_acceptsValidTokenAndRecordsLastUse() {
        ApiToken token = tokenExpiringAt(NOW_LOCAL.plusDays(1));
        when(repository.findBySecretHashWithUser(ApiTokenService.hash("dms_secret"))).thenReturn(Optional.of(token));

        assertEquals(Optional.of(token), service.authenticate("dms_secret"));
        assertEquals(NOW_LOCAL, token.getLastUsedAt());
    }

    @Test
    void authenticate_rejectsExpiredToken() {
        ApiToken token = tokenExpiringAt(NOW_LOCAL.minusSeconds(1));
        when(repository.findBySecretHashWithUser(ApiTokenService.hash("dms_secret"))).thenReturn(Optional.of(token));

        assertTrue(service.authenticate("dms_secret").isEmpty());
    }

    @Test
    void authenticate_rejectsUnknownAndMalformedSecrets() {
        when(repository.findBySecretHashWithUser(any())).thenReturn(Optional.empty());

        assertTrue(service.authenticate("dms_unknown").isEmpty());
        assertTrue(service.authenticate("no-prefix").isEmpty());
        assertTrue(service.authenticate(null).isEmpty());
        assertTrue(service.authenticate("dms_" + "x".repeat(200)).isEmpty());
    }

    @Test
    void revoke_onlyFindsTokensOfTheUser() {
        when(repository.findByIdAndUser(5L, alice)).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> service.revoke(alice, 5L));
        verify(repository, never()).delete(any());
    }

    private ApiToken tokenExpiringAt(LocalDateTime expiresAt) {
        ApiToken token = new ApiToken();
        token.setUser(alice);
        token.setExpiresAt(expiresAt);
        return token;
    }
}
