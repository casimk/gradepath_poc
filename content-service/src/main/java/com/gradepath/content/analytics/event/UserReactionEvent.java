package com.gradepath.content.analytics.event;

import org.springframework.context.ApplicationEvent;

import java.util.UUID;

public class UserReactionEvent extends ApplicationEvent {

    private final UUID userId;
    private final String contentId;
    private final String sessionId;
    private final long timestamp;
    private final String reactionType; // liked, disliked, bookmarked, shared

    public UserReactionEvent(Object source, UUID userId, String contentId, String sessionId,
                            long timestamp, String reactionType) {
        super(source);
        this.userId = userId;
        this.contentId = contentId;
        this.sessionId = sessionId;
        this.timestamp = timestamp;
        this.reactionType = reactionType;
    }

    public UUID userId() { return userId; }
    public String contentId() { return contentId; }
    public String sessionId() { return sessionId; }
    public long timestamp() { return timestamp; }
    public String reactionType() { return reactionType; }
}
