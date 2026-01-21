package com.gradepath.content.analytics.event;

import org.springframework.context.ApplicationEvent;

import java.util.UUID;

public class ContentCompletedEvent extends ApplicationEvent {

    private final UUID userId;
    private final String contentId;
    private final String sessionId;
    private final long timestamp;
    private final Integer timeSpentSeconds;
    private final Integer score;
    private final Boolean passed;

    public ContentCompletedEvent(Object source, UUID userId, String contentId, String sessionId,
                                  long timestamp, Integer timeSpentSeconds, Integer score, Boolean passed) {
        super(source);
        this.userId = userId;
        this.contentId = contentId;
        this.sessionId = sessionId;
        this.timestamp = timestamp;
        this.timeSpentSeconds = timeSpentSeconds;
        this.score = score;
        this.passed = passed;
    }

    public UUID userId() { return userId; }
    public String contentId() { return contentId; }
    public String sessionId() { return sessionId; }
    public long timestamp() { return timestamp; }
    public Integer timeSpentSeconds() { return timeSpentSeconds; }
    public Integer score() { return score; }
    public Boolean passed() { return passed; }
}
