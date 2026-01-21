package com.gradepath.content.analytics.service;

import com.gradepath.content.analytics.event.ContentCompletedEvent;
import com.gradepath.content.analytics.event.ContentViewedEvent;
import com.gradepath.content.analytics.event.UserReactionEvent;
import com.gradepath.content.analytics.model.ContentInteraction;
import com.gradepath.content.analytics.model.InteractionType;
import com.gradepath.content.analytics.repository.ContentInteractionRepository;
import com.gradepath.content.profile.event.SkillLevelChangedEvent;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
public class EventProcessingService {

    private final ContentInteractionRepository interactionRepository;
    private final ApplicationEventPublisher eventPublisher;

    public EventProcessingService(
            ContentInteractionRepository interactionRepository,
            ApplicationEventPublisher eventPublisher) {
        this.interactionRepository = interactionRepository;
        this.eventPublisher = eventPublisher;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleContentViewed(ContentViewedEvent event) {
        log.info("Handling ContentViewedEvent for user: {}, content: {}",
            event.userId(), event.contentId());

        // Store interaction
        ContentInteraction interaction = ContentInteraction.builder()
            .userId(event.userId())
            .contentId(event.contentId())
            .interactionType(InteractionType.VIEWED)
            .timestamp(Instant.ofEpochMilli(event.timestamp()))
            .sessionId(event.sessionId())
            .build();

        interactionRepository.save(interaction);

        // Additional processing could be added here (e.g., updating engagement metrics in Redis)
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleContentCompleted(ContentCompletedEvent event) {
        log.info("Handling ContentCompletedEvent for user: {}, content: {}, score: {}",
            event.userId(), event.contentId(), event.score());

        // Store interaction with metadata
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("timeSpentSeconds", event.timeSpentSeconds());
        metadata.put("score", event.score());
        metadata.put("passed", event.passed());

        ContentInteraction interaction = ContentInteraction.builder()
            .userId(event.userId())
            .contentId(event.contentId())
            .interactionType(InteractionType.COMPLETED)
            .timestamp(Instant.ofEpochMilli(event.timestamp()))
            .sessionId(event.sessionId())
            .metadata(metadata)
            .build();

        interactionRepository.save(interaction);

        // Trigger skill level update if score is available
        if (event.score() != null) {
            // Extract topic from content - for now using a simple placeholder
            // In a real implementation, you'd fetch the content to get its topic
            String topic = extractTopicFromContentId(event.contentId());
            eventPublisher.publishEvent(new SkillLevelChangedEvent(
                this, event.userId(), topic, event.score()
            ));
        }
    }

    /**
     * Extract topic from content ID - placeholder implementation
     * In production, this would fetch the content entity to get its actual topics
     */
    private String extractTopicFromContentId(String contentId) {
        // Simple heuristic: use part of the contentId as topic
        // In real implementation, query the content table
        return "general"; // Default fallback
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleUserReaction(UserReactionEvent event) {
        log.info("Handling UserReactionEvent for user: {}, content: {}, reaction: {}",
            event.userId(), event.contentId(), event.reactionType());

        // Map reaction type to interaction type
        InteractionType interactionType = switch (event.reactionType().toLowerCase()) {
            case "liked" -> InteractionType.LIKED;
            case "disliked" -> InteractionType.DISLIKED;
            case "bookmarked" -> InteractionType.BOOKMARKED;
            case "shared" -> InteractionType.SHARED;
            default -> InteractionType.VIEWED;
        };

        ContentInteraction interaction = ContentInteraction.builder()
            .userId(event.userId())
            .contentId(event.contentId())
            .interactionType(interactionType)
            .timestamp(Instant.ofEpochMilli(event.timestamp()))
            .sessionId(event.sessionId())
            .build();

        interactionRepository.save(interaction);
    }
}
