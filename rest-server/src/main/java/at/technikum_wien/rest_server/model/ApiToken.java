package at.technikum_wien.rest_server.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Personal access token for clients without a browser session (e.g. the Explorer integration).
 * Only the SHA-256 of the secret is stored; the secret itself is shown once when it is created.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "api_tokens",
        indexes = @Index(name = "idx_api_tokens_secret_hash", columnList = "secret_hash", unique = true))
public class ApiToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    // Label chosen by the user, e.g. "Laptop – Explorer"
    @Column(nullable = false, length = 60)
    private String name;

    @Column(name = "secret_hash", nullable = false, length = 64)
    private String secretHash;

    // First characters of the secret, so the user can recognise a token in the list
    @Column(nullable = false, length = 12)
    private String prefix;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    private LocalDateTime lastUsedAt;
}
