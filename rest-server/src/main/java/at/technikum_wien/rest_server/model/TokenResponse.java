package at.technikum_wien.rest_server.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;

/**
 * A personal access token as shown to its owner. {@code secret} is only set in the response
 * to the creation request – afterwards it cannot be retrieved again.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record TokenResponse(
        Long id,
        String name,
        String prefix,
        LocalDateTime createdAt,
        LocalDateTime expiresAt,
        LocalDateTime lastUsedAt,
        String secret) {

    public static TokenResponse of(ApiToken token) {
        return of(token, null);
    }

    public static TokenResponse of(ApiToken token, String secret) {
        return new TokenResponse(token.getId(), token.getName(), token.getPrefix(), token.getCreatedAt(),
                token.getExpiresAt(), token.getLastUsedAt(), secret);
    }
}
