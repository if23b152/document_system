package at.technikum_wien.rest_server.service;

import at.technikum_wien.rest_server.model.ApiToken;
import at.technikum_wien.rest_server.model.AppUser;
import at.technikum_wien.rest_server.repository.ApiTokenRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NOT_FOUND;

/**
 * Creates, checks and revokes personal access tokens.
 * Tokens look like "dms_" + 43 random base64url characters (256 bit); only their SHA-256 is stored.
 */
@Service
public class ApiTokenService {

    public static final String PREFIX = "dms_";
    static final int MAX_TOKENS_PER_USER = 10;
    static final int DEFAULT_VALID_DAYS = 90;
    static final int MAX_VALID_DAYS = 365;
    // Avoid a database write on every request: last use is recorded at most every few minutes
    private static final Duration LAST_USED_RESOLUTION = Duration.ofMinutes(5);

    private final ApiTokenRepository repository;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();

    public ApiTokenService(ApiTokenRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    /** A newly created token: the entity plus the secret, which is never stored and shown once. */
    public record CreatedToken(ApiToken token, String secret) {
    }

    @Transactional
    public CreatedToken create(AppUser user, String name, Integer validDays) {
        if (Boolean.TRUE.equals(user.getTemporary())) {
            throw new ResponseStatusException(FORBIDDEN, "Guest accounts cannot create access tokens.");
        }
        String label = name == null ? "" : name.strip();
        if (label.isEmpty() || label.length() > 60) {
            throw new ResponseStatusException(BAD_REQUEST, "Token name must be 1-60 characters.");
        }
        int days = validDays == null ? DEFAULT_VALID_DAYS : validDays;
        if (days < 1 || days > MAX_VALID_DAYS) {
            throw new ResponseStatusException(BAD_REQUEST, "Validity must be 1-" + MAX_VALID_DAYS + " days.");
        }
        if (repository.countByUser(user) >= MAX_TOKENS_PER_USER) {
            throw new ResponseStatusException(BAD_REQUEST, "Too many tokens – revoke an old one first.");
        }

        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String secret = PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        LocalDateTime now = LocalDateTime.now(clock);
        ApiToken token = new ApiToken();
        token.setUser(user);
        token.setName(label);
        token.setSecretHash(hash(secret));
        token.setPrefix(secret.substring(0, 10));
        token.setCreatedAt(now);
        token.setExpiresAt(now.plusDays(days));
        return new CreatedToken(repository.save(token), secret);
    }

    /** Returns the token for a presented secret if it exists and has not expired. */
    @Transactional
    public Optional<ApiToken> authenticate(String secret) {
        if (secret == null || !secret.startsWith(PREFIX) || secret.length() > 100) {
            return Optional.empty();
        }
        LocalDateTime now = LocalDateTime.now(clock);
        return repository.findBySecretHashWithUser(hash(secret))
                .filter(token -> token.getExpiresAt().isAfter(now))
                .map(token -> {
                    if (token.getLastUsedAt() == null
                            || token.getLastUsedAt().isBefore(now.minus(LAST_USED_RESOLUTION))) {
                        token.setLastUsedAt(now);
                    }
                    return token;
                });
    }

    public List<ApiToken> list(AppUser user) {
        return repository.findByUserOrderByCreatedAtDesc(user);
    }

    @Transactional
    public void revoke(AppUser user, Long tokenId) {
        ApiToken token = repository.findByIdAndUser(tokenId, user)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Token not found."));
        repository.delete(token);
    }

    @Transactional
    public void revokeAll(AppUser user) {
        repository.deleteAllByUser(user);
    }

    static String hash(String secret) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(secret.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is always available in the JDK", e);
        }
    }
}
