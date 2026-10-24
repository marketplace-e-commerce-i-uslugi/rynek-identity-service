package pl.rynek.identity.auth;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Refresh token zużyty przy rotacji (tylko skrót). Służy do wykrycia jego ponownego użycia. */
@Entity
@Table(name = "used_refresh_tokens")
class UsedRefreshToken {

    @Id
    @Column(name = "token_hash")
    private String tokenHash;

    @Column(name = "session_id")
    private UUID sessionId;

    @Column(name = "used_at")
    private Instant usedAt;

    protected UsedRefreshToken() {
    }

    UsedRefreshToken(String tokenHash, UUID sessionId, Instant usedAt) {
        this.tokenHash = tokenHash;
        this.sessionId = sessionId;
        this.usedAt = usedAt;
    }

    UUID getSessionId() {
        return sessionId;
    }

    Instant getUsedAt() {
        return usedAt;
    }
}
