package com.gradepath.content.analytics.event;

import org.springframework.context.ApplicationEvent;

import java.util.UUID;

public class ContentViewedEvent extends ApplicationEvent {

    private final UUID userId;
    private final String contentId;
    private final String sessionId;
    private final long timestamp;

    public ContentViewedEvent(Object source, UUID userId, String contentId, String sessionId, long timestamp) {
        super(source);
        this.userId = userId;
        this.contentId = contentId;
        this.sessionId = sessionId;
        this.timestamp = timestamp;
    }

    public UUID userId() { return userId; }
    public String contentId() { return contentId; }
    public String sessionId() { return sessionId; }
    public long timestamp() { return timestamp; }
}
