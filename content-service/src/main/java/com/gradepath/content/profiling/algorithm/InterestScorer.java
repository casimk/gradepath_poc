package com.gradepath.content.profiling.algorithm;

import com.gradepath.content.recommendation.profile.BehavioralProfile;
import com.gradepath.content.recommendation.profile.BehavioralProfile.InterestScore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Calculates and updates interest scores based on user content interactions.
 * Ported from TypeScript InterestScorer.
 *
 * Algorithm:
 * - Base score of 10 per interaction
 * - Action multipliers: started=1.0, completed=2.0, revisited=3.0, abandoned=0.5
 * - Time weight: normalizes time spent, max 1.5x multiplier
 * - Decay: 7-day half-life for old scores
 * - EMA: Exponential moving average with alpha=0.3 for score updates
 */
@Component
@Slf4j
public class InterestScorer {

    // Action multipliers based on TikTok-style weights
    private static final double MULTIPLIER_STARTED = 1.0;
    private static final double MULTIPLIER_COMPLETED = 2.0;
    private static final double MULTIPLIER_REVISITED = 3.0;
    private static final double MULTIPLIER_ABANDONED = 0.5;

    // Decay function: half-life of 7 days
    private static final double HALF_LIFE_DAYS = 7.0;

    // EMA alpha for score updates
    private static final double EMA_ALPHA = 0.3;

    // Minimum score threshold
    private static final double MIN_SCORE_THRESHOLD = 1.0;

    // Base value for each interaction
    private static final double BASE_VALUE = 10.0;

    /**
     * Update interests based on a content journey event
     */
    public void updateInterests(BehavioralProfile profile, RawJourneyEvent event) {
        var topicTags = event.topicTags();
        if (topicTags == null || topicTags.isEmpty()) {
            return;
        }

        var action = event.action();
        var timeInContentSeconds = event.timeInContentSeconds() != null
            ? event.timeInContentSeconds()
            : 0;

        // Update interest for each topic tag
        for (String topic : topicTags) {
            double actionMultiplier = getActionMultiplier(action);
            double timeWeight = Math.min(timeInContentSeconds / 60.0, 1.5);
            double recencyDecay = 1.0; // Fresh events have no decay

            double score = BASE_VALUE * actionMultiplier * timeWeight * recencyDecay;

            updateTopicScore(profile, topic, score);
        }

        // Apply decay to all interests
        if (profile.getInterests() != null) {
            applyDecay(profile.getInterests());
        }
    }

    /**
     * Get the action multiplier for a given action type
     */
    private double getActionMultiplier(String action) {
        return switch (action.toLowerCase()) {
            case "started" -> MULTIPLIER_STARTED;
            case "completed" -> MULTIPLIER_COMPLETED;
            case "revisited" -> MULTIPLIER_REVISITED;
            case "abandoned" -> MULTIPLIER_ABANDONED;
            default -> 1.0;
        };
    }

    /**
     * Update a single topic score using exponential moving average
     */
    private void updateTopicScore(BehavioralProfile profile, String topic, double addedScore) {
        Map<String, InterestScore> interests = profile.getInterests();
        if (interests == null) {
            interests = new HashMap<>();
            profile.setInterests(interests);
        }

        InterestScore existing = interests.get(topic);

        if (existing != null) {
            // Combine with existing score using exponential moving average
            double newScore = existing.getScore() * (1 - EMA_ALPHA) + addedScore * EMA_ALPHA;
            existing.setScore(newScore);
            existing.setLastUpdated(Instant.now());
        } else {
            InterestScore newInterest = InterestScore.builder()
                .topic(topic)
                .score(addedScore)
                .lastUpdated(Instant.now())
                .build();
            interests.put(topic, newInterest);
        }
    }

    /**
     * Apply time-based decay to all interest scores
     * Uses 7-day half-life: score = score * 0.5^(days/7)
     */
    private void applyDecay(Map<String, InterestScore> interests) {
        Instant now = Instant.now();

        // Apply decay
        interests.values().forEach(interest -> {
            long daysSinceUpdate = Duration.between(interest.getLastUpdated(), now).toDays();
            double decayFactor = Math.pow(0.5, daysSinceUpdate / HALF_LIFE_DAYS);
            double newScore = interest.getScore() * decayFactor;
            interest.setScore(newScore);
        });

        // Remove interests that have decayed below threshold
        interests.entrySet().removeIf(entry -> entry.getValue().getScore() < MIN_SCORE_THRESHOLD);
    }

    /**
     * Record for raw journey events from NestJS
     */
    public record RawJourneyEvent(
        String journeyId,
        String userId,
        String sessionId,
        String contentId,
        String contentType,
        String action,
        Integer sequencePosition,
        Integer timeInContentSeconds,
        java.util.List<String> topicTags,
        String difficultyLevel,
        String previousContentId,
        Long timestamp
    ) {}
}
