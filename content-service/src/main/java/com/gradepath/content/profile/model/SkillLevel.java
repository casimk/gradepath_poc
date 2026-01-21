package com.gradepath.content.profile.model;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "skill_levels", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "topic"})
})
@EntityListeners(AuditingEntityListener.class)
public class SkillLevel {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "topic", nullable = false, length = 100)
    private String topic;

    @Column(name = "level", nullable = false)
    private Integer level;

    @Column(name = "confidence_score", precision = 5, scale = 2)
    private BigDecimal confidenceScore;

    @Column(name = "last_assessed_at")
    private Instant lastAssessedAt;

    @CreatedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // Constructors
    public SkillLevel() {}

    private SkillLevel(Builder builder) {
        this.id = builder.id;
        this.user = builder.user;
        this.topic = builder.topic;
        this.level = builder.level;
        this.confidenceScore = builder.confidenceScore;
        this.lastAssessedAt = builder.lastAssessedAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getTopic() { return topic; }
    public void setTopic(String topic) { this.topic = topic; }

    public Integer getLevel() { return level; }
    public void setLevel(Integer level) { this.level = level; }

    public BigDecimal getConfidenceScore() { return confidenceScore; }
    public void setConfidenceScore(BigDecimal confidenceScore) { this.confidenceScore = confidenceScore; }

    public Instant getLastAssessedAt() { return lastAssessedAt; }
    public void setLastAssessedAt(Instant lastAssessedAt) { this.lastAssessedAt = lastAssessedAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    // Builder class
    public static class Builder {
        private UUID id;
        private User user;
        private String topic;
        private Integer level;
        private BigDecimal confidenceScore;
        private Instant lastAssessedAt;

        public Builder id(UUID id) { this.id = id; return this; }
        public Builder user(User user) { this.user = user; return this; }
        public Builder topic(String topic) { this.topic = topic; return this; }
        public Builder level(Integer level) { this.level = level; return this; }
        public Builder confidenceScore(BigDecimal confidenceScore) { this.confidenceScore = confidenceScore; return this; }
        public Builder lastAssessedAt(Instant lastAssessedAt) { this.lastAssessedAt = lastAssessedAt; return this; }

        public SkillLevel build() {
            return new SkillLevel(this);
        }
    }
}
