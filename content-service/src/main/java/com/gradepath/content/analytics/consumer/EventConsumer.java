package com.gradepath.content.analytics.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gradepath.content.analytics.dto.ContentEventDto;
import com.gradepath.content.analytics.event.ContentCompletedEvent;
import com.gradepath.content.analytics.event.ContentViewedEvent;
import com.gradepath.content.analytics.event.UserReactionEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@Slf4j
public class EventConsumer {

    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Autowired
    public EventConsumer(ObjectMapper objectMapper, ApplicationEventPublisher eventPublisher) {
        this.objectMapper = objectMapper;
        this.eventPublisher = eventPublisher;
    }

    @KafkaListener(
        topics = "content-interactions",
        groupId = "content-analytics-consumer",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeContentInteraction(String message) {
        try {
            log.debug("Received message from Kafka: {}", message);

            JsonNode json = objectMapper.readTree(message);
            String eventType = json.has("eventType") ? json.get("eventType").asText() : "";

            ContentEventDto event = objectMapper.treeToValue(json, ContentEventDto.class);

            switch (eventType) {
                case "content_viewed" -> handleContentViewed(event);
                case "content_completed" -> handleContentCompleted(event);
                case "content_reaction" -> handleContentReaction(event);
                case "assessment_completed" -> handleAssessmentCompleted(event);
                default -> log.warn("Unknown event type: {}", eventType);
            }
        } catch (Exception e) {
            log.error("Error processing Kafka message: {}", message, e);
        }
    }

    private void handleContentViewed(ContentEventDto event) {
        log.info("Processing content_viewed event for user: {}, content: {}",
            event.getUserId(), event.getContentId());

        try {
            UUID userId = UUID.fromString(event.getUserId());
            String contentId = event.getContentId();
            long timestamp = event.getTimestamp() != null ? event.getTimestamp() : System.currentTimeMillis();

            ContentViewedEvent viewedEvent = new ContentViewedEvent(
                this, userId, contentId, event.getSessionId(), timestamp
            );
            eventPublisher.publishEvent(viewedEvent);
        } catch (IllegalArgumentException e) {
            log.error("Invalid UUID in event: {}", event, e);
        }
    }

    private void handleContentCompleted(ContentEventDto event) {
        log.info("Processing content_completed event for user: {}, content: {}, score: {}",
            event.getUserId(), event.getContentId(), event.getScore());

        try {
            UUID userId = UUID.fromString(event.getUserId());
            String contentId = event.getContentId();
            long timestamp = event.getTimestamp() != null ? event.getTimestamp() : System.currentTimeMillis();

            ContentCompletedEvent completedEvent = new ContentCompletedEvent(
                this, userId, contentId, event.getSessionId(), timestamp,
                event.getTimeSpentSeconds(), event.getScore(), event.getPassed()
            );
            eventPublisher.publishEvent(completedEvent);
        } catch (IllegalArgumentException e) {
            log.error("Invalid UUID in event: {}", event, e);
        }
    }

    private void handleContentReaction(ContentEventDto event) {
        log.info("Processing content_reaction event for user: {}, content: {}",
            event.getUserId(), event.getContentId());

        try {
            UUID userId = UUID.fromString(event.getUserId());
            String contentId = event.getContentId();
            long timestamp = event.getTimestamp() != null ? event.getTimestamp() : System.currentTimeMillis();

            // Extract reaction type from metadata
            String reactionType = "liked";
            if (event.getMetadata() != null && event.getMetadata().containsKey("reaction")) {
                reactionType = String.valueOf(event.getMetadata().get("reaction"));
            }

            UserReactionEvent reactionEvent = new UserReactionEvent(
                this, userId, contentId, event.getSessionId(), timestamp, reactionType
            );
            eventPublisher.publishEvent(reactionEvent);
        } catch (IllegalArgumentException e) {
            log.error("Invalid UUID in event: {}", event, e);
        }
    }

    private void handleAssessmentCompleted(ContentEventDto event) {
        // Treat assessment completed as content completed
        handleContentCompleted(event);
    }
}
