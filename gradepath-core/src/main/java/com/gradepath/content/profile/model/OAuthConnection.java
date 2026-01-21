package com.gradepath.content.profile.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

/**
 * Entity for OAuth social login connections.
 * Stores OAuth provider information and tokens for users who authenticate via social providers.
 */
@Entity
@Table(name = "oauth_connections", indexes = {
    @Index(name = "idx_oauth_connections_user_id", columnList = "user_id"),
    @Index(name = "idx_oauth_connections_provider", columnList = "provider")
}, uniqueConstraints = {
    @UniqueConstraint(name = "uk_oauth_provider_user_id", columnNames = {"provider", "provider_user_id"})
})
@EntityListeners(AuditingEntityListener.class)
public class OAuthConnection {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "provider", nullable = false, length = 50)
    private String provider;

    @Column(name = "provider_user_id", nullable = false, length = 255)
    private String providerUserId;

    @Column(name = "provider_email", length = 255)
    private String providerEmail;

    @Column(name = "access_token", columnDefinition = "TEXT")
    private String accessToken;

    @Column(name = "refresh_token", columnDefinition = "TEXT")
    private String refreshToken;

    @Column(name = "token_expires_at")
    private Instant tokenExpiresAt;

    @Column(name = "profile_data", columnDefinition = "jsonb")
    private String profileData;

    @CreatedDate
    @Column(name = "linked_at", nullable = false, updatable = false)
    private Instant linkedAt;

    @Column(name = "last_used_at")
    private Instant lastUsedAt;

    // Constructors
    public OAuthConnection() {}

    private OAuthConnection(Builder builder) {
        this.id = builder.id;
        this.userId = builder.userId;
        this.provider = builder.provider;
        this.providerUserId = builder.providerUserId;
        this.providerEmail = builder.providerEmail;
        this.accessToken = builder.accessToken;
        this.refreshToken = builder.refreshToken;
        this.tokenExpiresAt = builder.tokenExpiresAt;
        this.profileData = builder.profileData;
        this.linkedAt = builder.linkedAt;
        this.lastUsedAt = builder.lastUsedAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }

    public String getProviderUserId() { return providerUserId; }
    public void setProviderUserId(String providerUserId) { this.providerUserId = providerUserId; }

    public String getProviderEmail() { return providerEmail; }
    public void setProviderEmail(String providerEmail) { this.providerEmail = providerEmail; }

    public String getAccessToken() { return accessToken; }
    public void setAccessToken(String accessToken) { this.accessToken = accessToken; }

    public String getRefreshToken() { return refreshToken; }
    public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }

    public Instant getTokenExpiresAt() { return tokenExpiresAt; }
    public void setTokenExpiresAt(Instant tokenExpiresAt) { this.tokenExpiresAt = tokenExpiresAt; }

    public String getProfileData() { return profileData; }
    public void setProfileData(String profileData) { this.profileData = profileData; }

    public Instant getLinkedAt() { return linkedAt; }
    public void setLinkedAt(Instant linkedAt) { this.linkedAt = linkedAt; }

    public Instant getLastUsedAt() { return lastUsedAt; }
    public void setLastUsedAt(Instant lastUsedAt) { this.lastUsedAt = lastUsedAt; }

    // Builder class
    public static class Builder {
        private UUID id;
        private UUID userId;
        private String provider;
        private String providerUserId;
        private String providerEmail;
        private String accessToken;
        private String refreshToken;
        private Instant tokenExpiresAt;
        private String profileData;
        private Instant linkedAt;
        private Instant lastUsedAt;

        public Builder id(UUID id) { this.id = id; return this; }
        public Builder userId(UUID userId) { this.userId = userId; return this; }
        public Builder provider(String provider) { this.provider = provider; return this; }
        public Builder providerUserId(String providerUserId) { this.providerUserId = providerUserId; return this; }
        public Builder providerEmail(String providerEmail) { this.providerEmail = providerEmail; return this; }
        public Builder accessToken(String accessToken) { this.accessToken = accessToken; return this; }
        public Builder refreshToken(String refreshToken) { this.refreshToken = refreshToken; return this; }
        public Builder tokenExpiresAt(Instant tokenExpiresAt) { this.tokenExpiresAt = tokenExpiresAt; return this; }
        public Builder profileData(String profileData) { this.profileData = profileData; return this; }
        public Builder linkedAt(Instant linkedAt) { this.linkedAt = linkedAt; return this; }
        public Builder lastUsedAt(Instant lastUsedAt) { this.lastUsedAt = lastUsedAt; return this; }

        public OAuthConnection build() {
            return new OAuthConnection(this);
        }
    }
}
