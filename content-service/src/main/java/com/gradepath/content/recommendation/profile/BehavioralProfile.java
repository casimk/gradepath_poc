package com.gradepath.content.recommendation.profile;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;
import java.util.List;

/**
 * Behavioral profile received from NestJS backend
 * Contains real-time user behavior patterns for recommendations
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BehavioralProfile {
    private String userId;
    private Instant timestamp;

    // Interest scores from behavioral profiler
    @Builder.Default
    private Map<String, InterestScore> interests = Map.of();

    // Engagement pattern classification
    private EngagementPattern engagement;

    // Peak performance windows
    @Builder.Default
    private List<PeakWindow> peakWindows = List.of();

    // Content transition patterns (Markov chain)
    @Builder.Default
    private List<ContentTransition> commonPaths = List.of();

    private int totalSessions;
    private int totalContentConsumed;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InterestScore {
        private String topic;
        private double score;
        private Instant lastUpdated;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EngagementPattern {
        private String classification; // binge_consumer, casual_browser, deep_learner, explorer, specialist
        private double confidence;
        private double avgSessionDuration;
        private double avgContentPerSession;
        private double timePerContentRatio;
        private double uniqueTopicRatio;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PeakWindow {
        private int hour;
        private String day;
        private double score;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ContentTransition {
        private String fromContent;
        private String toContent;
        private int frequency;
        private double probability;
    }
}
