package com.gradepath.content.recommendation.model;

import com.gradepath.content.content.model.Content;
import com.gradepath.content.profile.model.User;
import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "recommendations")
@EntityListeners(AuditingEntityListener.class)
public class Recommendation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "content_id", nullable = false)
    private Content content;

    @Column(name = "score", nullable = false, precision = 10, scale = 4)
    private BigDecimal score;

    @Column(name = "algorithm", length = 100)
    private String algorithm;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "shown_at")
    private Instant shownAt;

    @Column(name = "clicked_at")
    private Instant clickedAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    // Constructors
    public Recommendation() {}

    private Recommendation(Builder builder) {
        this.id = builder.id;
        this.user = builder.user;
        this.content = builder.content;
        this.score = builder.score;
        this.algorithm = builder.algorithm;
        this.reason = builder.reason;
        this.shownAt = builder.shownAt;
        this.clickedAt = builder.clickedAt;
        this.createdAt = builder.createdAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public Content getContent() { return content; }
    public void setContent(Content content) { this.content = content; }

    public BigDecimal getScore() { return score; }
    public void setScore(BigDecimal score) { this.score = score; }

    public String getAlgorithm() { return algorithm; }
    public void setAlgorithm(String algorithm) { this.algorithm = algorithm; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getShownAt() { return shownAt; }
    public void setShownAt(Instant shownAt) { this.shownAt = shownAt; }

    public Instant getClickedAt() { return clickedAt; }
    public void setClickedAt(Instant clickedAt) { this.clickedAt = clickedAt; }

    // Builder class
    public static class Builder {
        private UUID id;
        private User user;
        private Content content;
        private BigDecimal score;
        private String algorithm;
        private String reason;
        private Instant shownAt;
        private Instant clickedAt;
        private Instant createdAt;

        public Builder id(UUID id) { this.id = id; return this; }
        public Builder user(User user) { this.user = user; return this; }
        public Builder content(Content content) { this.content = content; return this; }
        public Builder score(BigDecimal score) { this.score = score; return this; }
        public Builder algorithm(String algorithm) { this.algorithm = algorithm; return this; }
        public Builder reason(String reason) { this.reason = reason; return this; }
        public Builder shownAt(Instant shownAt) { this.shownAt = shownAt; return this; }
        public Builder clickedAt(Instant clickedAt) { this.clickedAt = clickedAt; return this; }

        public Recommendation build() {
            if (createdAt == null) {
                this.createdAt = Instant.now();
            }
            return new Recommendation(this);
        }
    }
}
