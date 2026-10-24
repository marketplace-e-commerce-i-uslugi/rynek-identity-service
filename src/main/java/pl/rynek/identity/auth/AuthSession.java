package pl.rynek.identity.auth;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Jedno zalogowanie na urządzeniu. Access token niesie jej id w claimie {@code sid}. */
@Entity
@Table(name = "sessions")
public class AuthSession {

    @Id
    private UUID id;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "refresh_token_hash")
    private String refreshTokenHash;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    protected AuthSession() {
    }

    AuthSession(UUID userId, String refreshTokenHash, Instant now, Instant expiresAt) {
        this.id = UUID.randomUUID();
        this.userId = userId;
        this.refreshTokenHash = refreshTokenHash;
        this.createdAt = now;
        this.expiresAt = expiresAt;
    }

    boolean isActive(Instant now) {
        return revokedAt == null && now.isBefore(expiresAt);
    }

    /** Rotacja: stary refresh token przestaje działać, sesja trwa dalej z nowym terminem. */
    void rotate(String refreshTokenHash, Instant expiresAt) {
        this.refreshTokenHash = refreshTokenHash;
        this.expiresAt = expiresAt;
    }

    void revoke(Instant now) {
        if (revokedAt == null) {
            revokedAt = now;
        }
    }

    UUID getId() {
        return id;
    }

    UUID getUserId() {
        return userId;
    }
}
