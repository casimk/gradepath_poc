package com.gradepath.content.profiling.algorithm;

import com.gradepath.content.recommendation.profile.BehavioralProfile;
import com.gradepath.content.recommendation.profile.BehavioralProfile.EngagementPattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Classifies user engagement patterns based on session behavior.
 * Ported from TypeScript EngagementClassifier.
 *
 * Classification categories:
 * - binge_consumer: >30 min sessions, >10 content per session
 * - casual_browser: <10 min sessions, <5 content per session
 * - deep_learner: >2 min per content (deep engagement)
 * - explorer: <30 sec per content (browsing mode)
 * - specialist: low topic diversity, focused on specific subjects
 */
@Component
@Slf4j
public class EngagementClassifier {

    // Track recent session metrics per user (LRU cache with max 10 entries per user)
    private final Map<String, List<SessionMetrics>> recentSessions = new LinkedHashMap<>() {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, List<SessionMetrics>> eldest) {
            // Keep cache bounded - remove oldest entries when too many users
            return size() > 1000;
        }
    };

    private static final int MAX_SESSIONS_PER_USER = 10;
    private static final int MIN_SESSIONS_FOR_CLASSIFICATION = 3;

    /**
     * Update engagement based on session end event
     */
    public void updateEngagement(BehavioralProfile profile, SessionMetrics metrics) {
        String userId = profile.getUserId();

        // Store session metrics
        List<SessionMetrics> sessions = recentSessions.computeIfAbsent(
            userId, k -> new ArrayList<>()
        );
        sessions.add(metrics);

        // Keep only last N sessions
        while (sessions.size() > MAX_SESSIONS_PER_USER) {
            sessions.remove(0);
        }

        // Update engagement pattern
        EngagementPattern pattern = classifyEngagement(sessions);
        profile.setEngagement(pattern);

        log.debug("Updated engagement for user {}: classification={}, confidence={}",
            userId, pattern.getClassification(), pattern.getConfidence());
    }

    /**
     * Classify engagement based on recent session metrics
     */
    private EngagementPattern classifyEngagement(List<SessionMetrics> sessions) {
        if (sessions.size() < MIN_SESSIONS_FOR_CLASSIFICATION) {
            return EngagementPattern.builder()
                .classification("unknown")
                .confidence(0.0)
                .avgSessionDuration(0.0)
                .avgContentPerSession(0.0)
                .timePerContentRatio(0.0)
                .uniqueTopicRatio(0.0)
                .build();
        }

        // Calculate averages
        double avgDuration = sessions.stream()
            .mapToDouble(SessionMetrics::duration)
            .average()
            .orElse(0.0);

        double avgContent = sessions.stream()
            .mapToDouble(SessionMetrics::contentCount)
            .average()
            .orElse(0.0);

        double timePerContent = avgDuration / Math.max(avgContent, 1.0);

        // Classification rules based on TikTok-style patterns
        String classification;
        double confidence;

        if (avgDuration > 1800 && avgContent > 10) {
            // >30 min, >10 content = binge consumer
            classification = "binge_consumer";
            confidence = 0.7;
        } else if (avgDuration < 600 && avgContent < 5) {
            // <10 min, <5 content = casual browser
            classification = "casual_browser";
            confidence = 0.7;
        } else if (timePerContent > 120) {
            // >2 min per content = deep learner
            classification = "deep_learner";
            confidence = 0.75;
        } else if (timePerContent < 30) {
            // <30 sec per content = explorer
            classification = "explorer";
            confidence = 0.65;
        } else {
            // Moderate behavior
            classification = "casual_browser";
            confidence = 0.5;
        }

        return EngagementPattern.builder()
            .classification(classification)
            .confidence(confidence)
            .avgSessionDuration(avgDuration)
            .avgContentPerSession(avgContent)
            .timePerContentRatio(timePerContent)
            .uniqueTopicRatio(0.0) // Will be updated by JourneyAnalyzer
            .build();
    }

    /**
     * Update classification based on topic diversity (called by JourneyAnalyzer)
     */
    public void updateBasedOnTopicDiversity(BehavioralProfile profile, double uniqueTopicRatio) {
        EngagementPattern engagement = profile.getEngagement();
        if (engagement == null) {
            return;
        }

        engagement.setUniqueTopicRatio(uniqueTopicRatio);

        // Re-classify based on topic diversity
        if (uniqueTopicRatio > 0.6) {
            engagement.setClassification("explorer");
            engagement.setConfidence(Math.max(engagement.getConfidence(), 0.6));
        } else if (uniqueTopicRatio < 0.3 && profile.getTotalContentConsumed() > 10) {
            engagement.setClassification("specialist");
            engagement.setConfidence(Math.max(engagement.getConfidence(), 0.6));
        }
    }

    /**
     * Get stored session metrics for a user
     */
    public List<SessionMetrics> getUserSessions(String userId) {
        return Collections.unmodifiableList(recentSessions.getOrDefault(userId, List.of()));
    }

    /**
     * Record for session metrics
     */
    public record SessionMetrics(
        double duration,      // Duration in seconds
        int contentCount       // Number of content items consumed
    ) {}

    /**
     * Record for raw session end events from NestJS
     */
    public record SessionEndEvent(
        String sessionId,
        String userId,
        Long startTime,
        Long endTime,
        Integer durationSeconds,
        Integer contentCount,
        String eventType
    ) {}
}
