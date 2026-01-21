package com.gradepath.content.analytics.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

public class ContentEventDto {
    @JsonProperty("eventType")
    private String eventType;

    @JsonProperty("userId")
    private String userId;

    @JsonProperty("sessionId")
    private String sessionId;

    @JsonProperty("contentId")
    private String contentId;

    @JsonProperty("contentType")
    private String contentType;

    @JsonProperty("timeSpentSeconds")
    private Integer timeSpentSeconds;

    @JsonProperty("score")
    private Integer score;

    @JsonProperty("passed")
    private Boolean passed;

    @JsonProperty("metadata")
    private Map<String, Object> metadata;

    @JsonProperty("timestamp")
    private Long timestamp;

    @JsonProperty("platform")
    private String platform;

    @JsonProperty("appVersion")
    private String appVersion;

    // Getters and Setters
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public String getContentId() { return contentId; }
    public void setContentId(String contentId) { this.contentId = contentId; }

    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }

    public Integer getTimeSpentSeconds() { return timeSpentSeconds; }
    public void setTimeSpentSeconds(Integer timeSpentSeconds) { this.timeSpentSeconds = timeSpentSeconds; }

    public Integer getScore() { return score; }
    public void setScore(Integer score) { this.score = score; }

    public Boolean getPassed() { return passed; }
    public void setPassed(Boolean passed) { this.passed = passed; }

    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }

    public Long getTimestamp() { return timestamp; }
    public void setTimestamp(Long timestamp) { this.timestamp = timestamp; }

    public String getPlatform() { return platform; }
    public void setPlatform(String platform) { this.platform = platform; }

    public String getAppVersion() { return appVersion; }
    public void setAppVersion(String appVersion) { this.appVersion = appVersion; }
}
