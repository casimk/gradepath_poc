package com.gradepath.content.profile.model;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "users")
@EntityListeners(AuditingEntityListener.class)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "external_user_id", unique = true, nullable = false)
    private String externalUserId;

    @Column(name = "email")
    private String email;

    @Column(name = "display_name")
    private String displayName;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    // Constructors
    public User() {}

    private User(Builder builder) {
        this.id = builder.id;
        this.externalUserId = builder.externalUserId;
        this.email = builder.email;
        this.displayName = builder.displayName;
    }

    public static Builder builder() {
        return new Builder();
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getExternalUserId() { return externalUserId; }
    public void setExternalUserId(String externalUserId) { this.externalUserId = externalUserId; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    // Builder class
    public static class Builder {
        private UUID id;
        private String externalUserId;
        private String email;
        private String displayName;

        public Builder id(UUID id) { this.id = id; return this; }
        public Builder externalUserId(String externalUserId) { this.externalUserId = externalUserId; return this; }
        public Builder email(String email) { this.email = email; return this; }
        public Builder displayName(String displayName) { this.displayName = displayName; return this; }

        public User build() {
            return new User(this);
        }
    }
}
