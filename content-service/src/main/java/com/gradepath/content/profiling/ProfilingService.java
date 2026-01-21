package com.gradepath.content.profiling;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gradepath.content.profiling.algorithm.EngagementClassifier;
import com.gradepath.content.profiling.algorithm.InterestScorer;
import com.gradepath.content.profiling.algorithm.JourneyAnalyzer;
import com.gradepath.content.recommendation.profile.BehavioralProfile;
import com.gradepath.content.recommendation.profile.BehavioralProfileService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Behavioral Profiling Service
 * Core business logic module that processes raw behavioral events from NestJS API Gateway.
 *
 * This service is part of the Spring Modulith core and handles CPU-intensive profiling work:
 * - Interest scoring (graph algorithms)
 * - Engagement classification (statistical analysis)
 * - Journey analysis (Markov chains)
 *
 * Architecture:
 * NestJS (I/O) → Kafka (raw-behavioral-events) → Java Profiling (CPU) → Database
 */
@Service
@Slf4j
public class ProfilingService {

    private final ObjectMapper objectMapper;
    private final InterestScorer interestScorer;
    private final EngagementClassifier engagementClassifier;
    private final JourneyAnalyzer journeyAnalyzer;
    private final BehavioralProfileService profileService;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    // In-memory profile cache for real-time updates
    private final Map<String, BehavioralProfile> profileCache = new HashMap<>();

    public ProfilingService(
            ObjectMapper objectMapper,
            InterestScorer interestScorer,
            EngagementClassifier engagementClassifier,
            JourneyAnalyzer journeyAnalyzer,
            BehavioralProfileService profileService,
            KafkaTemplate<String, Object> kafkaTemplate) {
        this.objectMapper = objectMapper;
        this.interestScorer = interestScorer;
        this.engagementClassifier = engagementClassifier;
        this.journeyAnalyzer = journeyAnalyzer;
        this.profileService = profileService;
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * Consume raw behavioral events from NestJS API Gateway
     * Topic: raw-behavioral-events
     *
     * Event types:
     * - content_journey: User consumed a piece of content
     * - session_lifecycle: User started/ended a session
     */
    @KafkaListener(
        topics = "raw-behavioral-events",
        groupId = "behavioral-profiling-consumer",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void processRawBehavioralEvent(String message) {
        try {
            log.debug("Processing raw behavioral event: {}", message);

            JsonNode json = objectMapper.readTree(message);
            String topic = json.has("topic") ? json.get("topic").asText() : null;

            if (topic == null) {
                log.warn("No topic in event: {}", message);
                return;
            }

            switch (topic) {
                case "content_journey" ->
                    processJourneyEvent(json);
                case "session_lifecycle" ->
                    processSessionEvent(json);
                default ->
                    log.warn("Unknown event topic: {}", topic);
            }

        } catch (Exception e) {
            log.error("Error processing raw behavioral event: {}", message, e);
        }
    }

    /**
     * Process content journey events
     */
    private void processJourneyEvent(JsonNode event) {
        String userId = event.get("userId").asText();

        // Get or create profile
        BehavioralProfile profile = getOrCreateProfile(userId);

        // Parse journey event
        InterestScorer.RawJourneyEvent journeyEvent = parseJourneyEvent(event);

        // Update interest scores
        interestScorer.updateInterests(profile, journeyEvent);

        // Analyze journey patterns
        journeyAnalyzer.analyzeJourney(profile, journeyEvent);

        // Update content consumed count
        profile.setTotalContentConsumed(profile.getTotalContentConsumed() + 1);
        profile.setTimestamp(Instant.now());

        // Save and emit
        saveProfile(profile);
        emitProfileUpdate(profile);

        log.info("Processed journey event for user: {}, content: {}, action: {}",
            userId, journeyEvent.contentId(), journeyEvent.action());
    }

    /**
     * Process session lifecycle events
     */
    private void processSessionEvent(JsonNode event) {
        String userId = event.get("userId").asText();
        String eventType = event.has("eventType") ? event.get("eventType").asText() : null;

        if (!"session_end".equals(eventType)) {
            return; // Only process session_end for engagement metrics
        }

        // Get or create profile
        BehavioralProfile profile = getOrCreateProfile(userId);

        // Parse session metrics
        int durationSeconds = event.has("durationSeconds")
            ? event.get("durationSeconds").asInt()
            : 0;
        int contentCount = event.has("contentCount")
            ? event.get("contentCount").asInt()
            : 0;

        // Update engagement classification
        EngagementClassifier.SessionMetrics metrics = new EngagementClassifier.SessionMetrics(
            durationSeconds,
            contentCount
        );
        engagementClassifier.updateEngagement(profile, metrics);

        // Update session count
        profile.setTotalSessions(profile.getTotalSessions() + 1);
        profile.setTimestamp(Instant.now());

        // Save and emit
        saveProfile(profile);
        emitProfileUpdate(profile);

        log.info("Processed session_end for user: {}, duration: {}s, content: {}",
            userId, durationSeconds, contentCount);
    }

    /**
     * Get or create a behavioral profile for a user
     */
    private BehavioralProfile getOrCreateProfile(String userId) {
        // Check cache first
        BehavioralProfile cached = profileCache.get(userId);
        if (cached != null) {
            return cached;
        }

        // Try to load from database
        BehavioralProfile loaded = profileService.findByUserId(userId).orElse(null);
        if (loaded != null) {
            profileCache.put(userId, loaded);
            return loaded;
        }

        // Create new profile
        BehavioralProfile newProfile = BehavioralProfile.builder()
            .userId(userId)
            .timestamp(Instant.now())
            .interests(new HashMap<>())
            .engagement(BehavioralProfile.EngagementPattern.builder()
                .classification("unknown")
                .confidence(0.0)
                .avgSessionDuration(0.0)
                .avgContentPerSession(0.0)
                .timePerContentRatio(0.0)
                .uniqueTopicRatio(0.0)
                .build())
            .peakWindows(List.of())
            .commonPaths(List.of())
            .totalSessions(0)
            .totalContentConsumed(0)
            .build();

        profileCache.put(userId, newProfile);
        return newProfile;
    }

    /**
     * Parse journey event from JSON
     */
    private InterestScorer.RawJourneyEvent parseJourneyEvent(JsonNode event) {
        return new InterestScorer.RawJourneyEvent(
            getValue(event, "journeyId"),
            getValue(event, "userId"),
            getValue(event, "sessionId"),
            getValue(event, "contentId"),
            getValue(event, "contentType"),
            getValue(event, "action"),
            getIntValue(event, "sequencePosition"),
            getIntValue(event, "timeInContentSeconds"),
            getListValue(event, "topicTags"),
            getValue(event, "difficultyLevel"),
            getValue(event, "previousContentId"),
            getLongValue(event, "timestamp")
        );
    }

    /**
     * Save profile to database
     */
    private void saveProfile(BehavioralProfile profile) {
        profileService.saveProfile(profile);
    }

    /**
     * Emit profile update to Kafka for other services
     * Serializes profile to JSON string before sending
     */
    private void emitProfileUpdate(BehavioralProfile profile) {
        try {
            // Serialize the update to JSON string for Kafka
            String jsonMessage = objectMapper.writeValueAsString(Map.of(
                "userId", profile.getUserId(),
                "profile", profile,
                "timestamp", Instant.now()
            ));

            kafkaTemplate.send("profile-updates", jsonMessage);
            log.debug("Emitted profile update for user: {}", profile.getUserId());
        } catch (Exception e) {
            // Don't fail the entire profiling if Kafka emission fails
            // The profile is already saved in the database
            log.warn("Failed to emit profile update to Kafka for user: {}",
                profile.getUserId(), e);
        }
    }

    // Helper methods for JSON parsing
    private String getValue(JsonNode node, String field) {
        return node.has(field) && !node.get(field).isNull() ? node.get(field).asText() : null;
    }

    private Integer getIntValue(JsonNode node, String field) {
        return node.has(field) && !node.get(field).isNull() ? node.get(field).asInt() : null;
    }

    private Long getLongValue(JsonNode node, String field) {
        return node.has(field) && !node.get(field).isNull() ? node.get(field).asLong() : null;
    }

    @SuppressWarnings("unchecked")
    private java.util.List<String> getListValue(JsonNode node, String field) {
        if (!node.has(field) || node.get(field).isNull()) {
            return List.of();
        }
        var array = node.get(field);
        if (array.isArray()) {
            java.util.List<String> result = new java.util.ArrayList<>();
            for (JsonNode item : array) {
                result.add(item.asText());
            }
            return result;
        }
        return List.of();
    }
}
