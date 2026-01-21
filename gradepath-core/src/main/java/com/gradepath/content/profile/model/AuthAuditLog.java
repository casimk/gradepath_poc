package com.gradepath.content.profile.model;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

/**
 * Entity for security audit logging.
 * Tracks all authentication-related events for security monitoring and compliance.
 */
@Entity
@Table(name = "auth_audit_log", indexes = {
    @Index(name = "idx_auth_audit_log_user_id", columnList = "user_id"),
    @Index(name = "idx_auth_audit_log_event_type", columnList = "event_type")
})
@EntityListeners(AuditingEntityListener.class)
public class AuthAuditLog {

    public enum EventType {
        REGISTER,
        LOGIN,
        LOGOUT,
        TOKEN_REFRESH,
        OAUTH_LOGIN,
        PASSWORD_CHANGE,
        PASSWORD_RESET,
        EMAIL_VERIFICATION,
        ACCOUNT_LOCKED,
        ACCOUNT_UNLOCKED,
        TOKEN_REVOKED,
        SESSION_CREATED,
        SESSION_DESTROYED
    }

    public enum AuthMethod {
        PASSWORD,
        GOOGLE,
        GITHUB,
        APPLE,
        FACEBOOK,
        TOKEN_REFRESH
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "event_type", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private EventType eventType;

    @Column(name = "auth_method", length = 50)
    @Enumerated(EnumType.STRING)
    private AuthMethod authMethod;

    @Column(name = "success", nullable = false)
    private Boolean success;

    @Column(name = "failure_reason", length = 255)
    private String failureReason;

    @Column(name = "ip_address", columnDefinition = "inet")
    private String ipAddress;

    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    @Column(name = "metadata", columnDefinition = "jsonb")
    private String metadata;

    @CreatedDate
    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    // Constructors
    public AuthAuditLog() {}

    private AuthAuditLog(Builder builder) {
        this.id = builder.id;
        this.userId = builder.userId;
        this.eventType = builder.eventType;
        this.authMethod = builder.authMethod;
        this.success = builder.success;
        this.failureReason = builder.failureReason;
        this.ipAddress = builder.ipAddress;
        this.userAgent = builder.userAgent;
        this.metadata = builder.metadata;
        this.occurredAt = builder.occurredAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public EventType getEventType() { return eventType; }
    public void setEventType(EventType eventType) { this.eventType = eventType; }

    public AuthMethod getAuthMethod() { return authMethod; }
    public void setAuthMethod(AuthMethod authMethod) { this.authMethod = authMethod; }

    public Boolean getSuccess() { return success; }
    public void setSuccess(Boolean success) { this.success = success; }

    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }

    public String getMetadata() { return metadata; }
    public void setMetadata(String metadata) { this.metadata = metadata; }

    public Instant getOccurredAt() { return occurredAt; }
    public void setOccurredAt(Instant occurredAt) { this.occurredAt = occurredAt; }

    // Builder class
    public static class Builder {
        private UUID id;
        private UUID userId;
        private EventType eventType;
        private AuthMethod authMethod;
        private Boolean success;
        private String failureReason;
        private String ipAddress;
        private String userAgent;
        private String metadata;
        private Instant occurredAt;

        public Builder id(UUID id) { this.id = id; return this; }
        public Builder userId(UUID userId) { this.userId = userId; return this; }
        public Builder eventType(EventType eventType) { this.eventType = eventType; return this; }
        public Builder authMethod(AuthMethod authMethod) { this.authMethod = authMethod; return this; }
        public Builder success(Boolean success) { this.success = success; return this; }
        public Builder failureReason(String failureReason) { this.failureReason = failureReason; return this; }
        public Builder ipAddress(String ipAddress) { this.ipAddress = ipAddress; return this; }
        public Builder userAgent(String userAgent) { this.userAgent = userAgent; return this; }
        public Builder metadata(String metadata) { this.metadata = metadata; return this; }
        public Builder occurredAt(Instant occurredAt) { this.occurredAt = occurredAt; return this; }

        public AuthAuditLog build() {
            return new AuthAuditLog(this);
        }
    }
}
