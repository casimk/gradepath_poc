package com.gradepath.content.profile.event;

import org.springframework.context.ApplicationEvent;

import java.util.UUID;

public class SkillLevelChangedEvent extends ApplicationEvent {

    private final UUID userId;
    private final String topic;
    private final Integer newScore;

    public SkillLevelChangedEvent(Object source, UUID userId, String topic, Integer newScore) {
        super(source);
        this.userId = userId;
        this.topic = topic;
        this.newScore = newScore;
    }

    public UUID userId() { return userId; }
    public String topic() { return topic; }
    public Integer newScore() { return newScore; }
}
