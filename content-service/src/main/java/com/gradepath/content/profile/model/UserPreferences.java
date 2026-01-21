package com.gradepath.content.profile.model;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "user_preferences")
public class UserPreferences {

    @Id
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "difficulty_preference")
    private Integer difficultyPreference;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "content_type_preferences", columnDefinition = "jsonb")
    private Map<String, Double> contentTypePreferences;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "topic_preferences", columnDefinition = "jsonb")
    private Map<String, Double> topicPreferences;

    @Column(name = "daily_time_target_minutes")
    private Integer dailyTimeTargetMinutes = 30;

    // Constructors
    public UserPreferences() {}

    private UserPreferences(Builder builder) {
        this.userId = builder.userId;
        this.user = builder.user;
        this.difficultyPreference = builder.difficultyPreference;
        this.contentTypePreferences = builder.contentTypePreferences;
        this.topicPreferences = builder.topicPreferences;
        this.dailyTimeTargetMinutes = builder.dailyTimeTargetMinutes;
    }

    public static Builder builder() {
        return new Builder();
    }

    // Getters and Setters
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public Integer getDifficultyPreference() { return difficultyPreference; }
    public void setDifficultyPreference(Integer difficultyPreference) { this.difficultyPreference = difficultyPreference; }

    public Map<String, Double> getContentTypePreferences() { return contentTypePreferences; }
    public void setContentTypePreferences(Map<String, Double> contentTypePreferences) { this.contentTypePreferences = contentTypePreferences; }

    public Map<String, Double> getTopicPreferences() { return topicPreferences; }
    public void setTopicPreferences(Map<String, Double> topicPreferences) { this.topicPreferences = topicPreferences; }

    public Integer getDailyTimeTargetMinutes() { return dailyTimeTargetMinutes; }
    public void setDailyTimeTargetMinutes(Integer dailyTimeTargetMinutes) { this.dailyTimeTargetMinutes = dailyTimeTargetMinutes; }

    // Builder class
    public static class Builder {
        private UUID userId;
        private User user;
        private Integer difficultyPreference;
        private Map<String, Double> contentTypePreferences;
        private Map<String, Double> topicPreferences;
        private Integer dailyTimeTargetMinutes = 30;

        public Builder userId(UUID userId) { this.userId = userId; return this; }
        public Builder user(User user) { this.user = user; return this; }
        public Builder difficultyPreference(Integer difficultyPreference) { this.difficultyPreference = difficultyPreference; return this; }
        public Builder contentTypePreferences(Map<String, Double> contentTypePreferences) { this.contentTypePreferences = contentTypePreferences; return this; }
        public Builder topicPreferences(Map<String, Double> topicPreferences) { this.topicPreferences = topicPreferences; return this; }
        public Builder dailyTimeTargetMinutes(Integer dailyTimeTargetMinutes) { this.dailyTimeTargetMinutes = dailyTimeTargetMinutes; return this; }

        public UserPreferences build() {
            return new UserPreferences(this);
        }
    }
}
