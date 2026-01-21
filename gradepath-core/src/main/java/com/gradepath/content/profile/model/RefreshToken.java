package com.gradepath.content.profile.model;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

/**
 * Entity for refresh tokens used for token rotation.
 * Stores refresh tokens with expiration and revocation tracking.
 */
@Entity
@Table(name = "refresh_tokens", indexes = {
    @Index(name = "idx_refresh_tokens_user_id", columnList = "user_id"),
    @Index(name = "idx_refresh_tokens_token_hash", columnList = "token_hash")
})
@EntityListeners(AuditingEntityListener.class)
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "token", nullable = false, unique = true, length = 500)
    private String token;

    @Column(name = "token_hash", nullable = false, length = 255)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "revoked_reason", length = 100)
    private String revokedReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "replaced_by_token", foreignKey = @ForeignKey(name = "fk_refresh_token_replacement"))
    private RefreshToken replacedByToken;

    // Constructors
    public RefreshToken() {}

    private RefreshToken(Builder builder) {
        this.id = builder.id;
        this.userId = builder.userId;
        this.token = builder.token;
        this.tokenHash = builder.tokenHash;
        this.expiresAt = builder.expiresAt;
        this.createdAt = builder.createdAt;
        this.revokedAt = builder.revokedAt;
        this.revokedReason = builder.revokedReason;
        this.replacedByToken = builder.replacedByToken;
    }

    public static Builder builder() {
        return new Builder();
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public String getTokenHash() { return tokenHash; }
    public void setTokenHash(String tokenHash) { this.tokenHash = tokenHash; }

    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getRevokedAt() { return revokedAt; }
    public void setRevokedAt(Instant revokedAt) { this.revokedAt = revokedAt; }

    public String getRevokedReason() { return revokedReason; }
    public void setRevokedReason(String revokedReason) { this.revokedReason = revokedReason; }

    public RefreshToken getReplacedByToken() { return replacedByToken; }
    public void setReplacedByToken(RefreshToken replacedByToken) { this.replacedByToken = replacedByToken; }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    public boolean isRevoked() {
        return revokedAt != null;
    }

    public boolean isValid() {
        return !isExpired() && !isRevoked();
    }

    // Builder class
    public static class Builder {
        private UUID id;
        private UUID userId;
        private String token;
        private String tokenHash;
        private Instant expiresAt;
        private Instant createdAt;
        private Instant revokedAt;
        private String revokedReason;
        private RefreshToken replacedByToken;

        public Builder id(UUID id) { this.id = id; return this; }
        public Builder userId(UUID userId) { this.userId = userId; return this; }
        public Builder token(String token) { this.token = token; return this; }
        public Builder tokenHash(String tokenHash) { this.tokenHash = tokenHash; return this; }
        public Builder expiresAt(Instant expiresAt) { this.expiresAt = expiresAt; return this; }
        public Builder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }
        public Builder revokedAt(Instant revokedAt) { this.revokedAt = revokedAt; return this; }
        public Builder revokedReason(String revokedReason) { this.revokedReason = revokedReason; return this; }
        public Builder replacedByToken(RefreshToken replacedByToken) { this.replacedByToken = replacedByToken; return this; }

        public RefreshToken build() {
            return new RefreshToken(this);
        }
    }
}
