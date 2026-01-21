package com.gradepath.content.profile.model;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "user_profiles")
public class UserProfile {

    @Id
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "interests", columnDefinition = "jsonb")
    private List<String> interests;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "learning_goals", columnDefinition = "jsonb")
    private List<String> learningGoals;

    @Column(name = "preferred_learning_style")
    private String preferredLearningStyle;

    @Column(name = "language", length = 10)
    private String language = "en";

    // Constructors
    public UserProfile() {}

    private UserProfile(Builder builder) {
        this.userId = builder.userId;
        this.user = builder.user;
        this.interests = builder.interests;
        this.learningGoals = builder.learningGoals;
        this.preferredLearningStyle = builder.preferredLearningStyle;
        this.language = builder.language;
    }

    public static Builder builder() {
        return new Builder();
    }

    // Getters and Setters
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public List<String> getInterests() { return interests; }
    public void setInterests(List<String> interests) { this.interests = interests; }

    public List<String> getLearningGoals() { return learningGoals; }
    public void setLearningGoals(List<String> learningGoals) { this.learningGoals = learningGoals; }

    public String getPreferredLearningStyle() { return preferredLearningStyle; }
    public void setPreferredLearningStyle(String preferredLearningStyle) { this.preferredLearningStyle = preferredLearningStyle; }

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }

    // Builder class
    public static class Builder {
        private UUID userId;
        private User user;
        private List<String> interests;
        private List<String> learningGoals;
        private String preferredLearningStyle;
        private String language = "en";

        public Builder userId(UUID userId) { this.userId = userId; return this; }
        public Builder user(User user) { this.user = user; return this; }
        public Builder interests(List<String> interests) { this.interests = interests; return this; }
        public Builder learningGoals(List<String> learningGoals) { this.learningGoals = learningGoals; return this; }
        public Builder preferredLearningStyle(String preferredLearningStyle) { this.preferredLearningStyle = preferredLearningStyle; return this; }
        public Builder language(String language) { this.language = language; return this; }

        public UserProfile build() {
            return new UserProfile(this);
        }
    }
}
