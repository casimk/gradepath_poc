package com.gradepath.content.recommendation.algorithm;

import com.gradepath.content.content.model.Content;
import com.gradepath.content.recommendation.profile.BehavioralProfile;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Shorts-aware recommendation strategy
 * Handles short-form content (30-60s) differently from long-form
 */
@Service
@Slf4j
public class ShortsStrategyService {

    private static final int SHORTS_THRESHOLD_SECONDS = 90; // 1.5 minutes
    private static final int SHORTS_MIN_SECONDS = 20;

    /**
     * Determine content type strategy based on user behavior
     */
    public ContentStrategy determineStrategy(Optional<BehavioralProfile> profile, List<Content> recentContent) {
        if (profile.isEmpty()) {
            return ContentStrategy.BALANCED;
        }

        BehavioralProfile p = profile.get();
        if (p.getEngagement() == null) {
            return ContentStrategy.BALANCED;
        }

        // Detect if user is in "snack mode" (consuming shorts)
        boolean inShortsMode = isInShortsMode(recentContent);
        String classification = p.getEngagement().getClassification();

        // Strategy selection
        if (inShortsMode && classification.equals("explorer")) {
            return ContentStrategy.DISCOVERY_SHORTS; // Mixed topics, new discoveries
        } else if (inShortsMode) {
            return ContentStrategy.SHORTS_ONLY; // Short content for quick consumption
        } else if (classification.equals("deep_learner")) {
            return ContentStrategy.DEEP_DIVE; // Long-form, same topic
        } else if (classification.equals("specialist")) {
            return ContentStrategy.TOPIC_FOCUSED; // Narrow topic focus
        } else {
            return ContentStrategy.BALANCED;
        }
    }

    /**
     * Check if user is in "shorts mode" (consuming short content)
     */
    private boolean isInShortsMode(List<Content> recentContent) {
        if (recentContent == null || recentContent.isEmpty()) {
            return false;
        }

        // Check if last 3 items were all shorts
        long shortsCount = recentContent.stream()
            .limit(3)
            .filter(this::isShort)
            .count();

        return shortsCount >= 2;
    }

    /**
     * Check if content is a short
     */
    public boolean isShort(Content content) {
        Integer durationMinutes = content.getEstimatedDurationMinutes();
        if (durationMinutes != null) {
            int durationSeconds = durationMinutes * 60;
            return durationSeconds >= SHORTS_MIN_SECONDS && durationSeconds <= SHORTS_THRESHOLD_SECONDS;
        }

        // QUIZ and EXERCISE are typically short content types
        return content.getType() == Content.ContentType.QUIZ ||
               content.getType() == Content.ContentType.EXERCISE;
    }

    /**
     * Filter content based on strategy
     */
    public List<Content> filterByStrategy(List<Content> candidates, ContentStrategy strategy) {
        return candidates.stream()
            .filter(content -> matchesStrategy(content, strategy))
            .toList();
    }

    /**
     * Check if content matches the strategy
     */
    private boolean matchesStrategy(Content content, ContentStrategy strategy) {
        return switch (strategy) {
            case SHORTS_ONLY, DISCOVERY_SHORTS -> isShort(content);
            case DEEP_DIVE, TOPIC_FOCUSED, BALANCED -> true; // All content types allowed
        };
    }

    /**
     * Calculate strategy-specific score boost
     */
    public double calculateStrategyBoost(Content content, ContentStrategy strategy) {
        if (matchesStrategy(content, strategy)) {
            return switch (strategy) {
                case SHORTS_ONLY -> isShort(content) ? 0.3 : 0.0;
                case DISCOVERY_SHORTS -> isShort(content) ? 0.4 : 0.1;
                case DEEP_DIVE -> !isShort(content) ? 0.3 : 0.0;
                case TOPIC_FOCUSED -> 0.2;
                case BALANCED -> 0.1;
            };
        }
        return 0.0;
    }

    /**
     * Get strategy explanation
     */
    public String getStrategyReason(ContentStrategy strategy) {
        return switch (strategy) {
            case SHORTS_ONLY -> "Quick content perfect for your current session";
            case DISCOVERY_SHORTS -> "Exploring new topics with short content";
            case DEEP_DIVE -> "Deep dive into topics you care about";
            case TOPIC_FOCUSED -> "Focused content matching your interests";
            case BALANCED -> "Personalized mix for you";
        };
    }

    public enum ContentStrategy {
        SHORTS_ONLY,           // Only short content (snack mode)
        DISCOVERY_SHORTS,      // Shorts for exploration (new topics)
        DEEP_DIVE,             // Long-form, single topic
        TOPIC_FOCUSED,         // Narrow topic focus
        BALANCED               // Mix of all types
    }
}
