package com.gradepath.content.recommendation.service;

import com.gradepath.content.content.model.Content;
import com.gradepath.content.content.repository.ContentRepository;
import com.gradepath.content.profile.model.UserPreferences;
import com.gradepath.content.profile.model.SkillLevel;
import com.gradepath.content.profile.repository.SkillLevelRepository;
import com.gradepath.content.recommendation.profile.BehavioralProfile;
import com.gradepath.content.recommendation.profile.BehavioralProfileService;
import com.gradepath.content.recommendation.algorithm.SessionContextService;
import com.gradepath.content.recommendation.algorithm.ShortsStrategyService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ContentScoringService {

    private final ContentRepository contentRepository;
    private final SkillLevelRepository skillLevelRepository;
    private final BehavioralProfileService behavioralProfileService;
    private final SessionContextService sessionContextService;
    private final ShortsStrategyService shortsStrategyService;

    public ContentScoringService(
            ContentRepository contentRepository,
            SkillLevelRepository skillLevelRepository,
            BehavioralProfileService behavioralProfileService,
            SessionContextService sessionContextService,
            ShortsStrategyService shortsStrategyService) {
        this.contentRepository = contentRepository;
        this.skillLevelRepository = skillLevelRepository;
        this.behavioralProfileService = behavioralProfileService;
        this.sessionContextService = sessionContextService;
        this.shortsStrategyService = shortsStrategyService;
    }

    /**
     * Score candidates using hybrid algorithm (70% content-based, 30% collaborative)
     */
    public Map<String, BigDecimal> scoreCandidates(
            List<Content> candidates,
            UUID userId,
            UserPreferences preferences,
            List<SkillLevel> skillLevels) {

        // Content-based scoring
        Map<String, BigDecimal> contentScores = scoreContentBased(candidates, preferences, skillLevels);

        // Collaborative filtering (placeholder - simplified version)
        Map<String, BigDecimal> collabScores = scoreCollaborative(candidates, userId);

        // Hybrid combination: 70% content-based, 30% collaborative
        Map<String, BigDecimal> finalScores = new HashMap<>();
        for (Content content : candidates) {
            String contentId = content.getId();
            BigDecimal contentScore = contentScores.getOrDefault(contentId, BigDecimal.ZERO);
            BigDecimal collabScore = collabScores.getOrDefault(contentId, BigDecimal.ZERO);

            // Weighted combination
            BigDecimal finalScore = contentScore.multiply(new BigDecimal("0.7"))
                .add(collabScore.multiply(new BigDecimal("0.3")));

            finalScores.put(contentId, finalScore);
        }

        return finalScores;
    }

    /**
     * Content-based filtering scoring
     */
    private Map<String, BigDecimal> scoreContentBased(
            List<Content> candidates,
            UserPreferences preferences,
            List<SkillLevel> skillLevels) {

        Map<String, BigDecimal> scores = new HashMap<>();
        Map<String, SkillLevel> skillMap = skillLevels.stream()
            .collect(Collectors.toMap(SkillLevel::getTopic, sl -> sl));

        for (Content content : candidates) {
            BigDecimal score = calculateContentAffinity(content, preferences, skillMap);
            scores.put(content.getId(), score);
        }

        return scores;
    }

    /**
     * Calculate content affinity score based on multiple factors
     */
    private BigDecimal calculateContentAffinity(
            Content content,
            UserPreferences preferences,
            Map<String, SkillLevel> skillLevels) {

        BigDecimal score = BigDecimal.ZERO;

        // Topic affinity (40%)
        score = score.add(topicAffinity(content, preferences, skillLevels).multiply(new BigDecimal("0.4")));

        // Content type preference (20%)
        score = score.add(typeAffinity(content.getType(), preferences).multiply(new BigDecimal("0.2")));

        // Difficulty match (20%) - Zone of Proximal Development
        score = score.add(difficultyScore(content.getDifficultyLevel(), preferences, skillLevels).multiply(new BigDecimal("0.2")));

        // Recency boost (10%) - newer content gets slight boost
        score = score.add(recencyBoost(content.getCreatedAt()).multiply(new BigDecimal("0.1")));

        // Length preference (10%)
        score = score.add(lengthScore(content.getEstimatedDurationMinutes(), preferences).multiply(new BigDecimal("0.1")));

        return score;
    }

    /**
     * Topic affinity - how well content topics match user preferences
     */
    private BigDecimal topicAffinity(
            Content content,
            UserPreferences preferences,
            Map<String, SkillLevel> skillLevels) {

        Map<String, Double> topicPrefs = preferences.getTopicPreferences();
        if (topicPrefs == null || topicPrefs.isEmpty()) {
            return new BigDecimal("0.5"); // Neutral score
        }

        // Extract topics from content
        @SuppressWarnings("unchecked")
        List<String> contentTopics = content.getTopics() != null
            ? (List<String>) content.getTopics().get("topics")
            : List.of();

        if (contentTopics.isEmpty()) {
            return new BigDecimal("0.5");
        }

        // Calculate average affinity for all content topics
        double avgAffinity = contentTopics.stream()
            .mapToDouble(topic -> {
                Double userPref = topicPrefs.get(topic);
                if (userPref != null) {
                    return userPref;
                }

                // Check if user has skill level for this topic
                SkillLevel skill = skillLevels.get(topic);
                if (skill != null && skill.getConfidenceScore() != null) {
                    return skill.getConfidenceScore().doubleValue();
                }

                return 0.5; // Default affinity
            })
            .average()
            .orElse(0.5);

        return BigDecimal.valueOf(avgAffinity);
    }

    /**
     * Content type preference affinity
     */
    private BigDecimal typeAffinity(Content.ContentType contentType, UserPreferences preferences) {
        Map<String, Double> typePrefs = preferences.getContentTypePreferences();
        if (typePrefs == null || typePrefs.isEmpty()) {
            return new BigDecimal("0.5");
        }

        Double affinity = typePrefs.get(contentType.name());
        return affinity != null
            ? BigDecimal.valueOf(affinity)
            : new BigDecimal("0.5");
    }

    /**
     * Difficulty matching using Zone of Proximal Development (ZPD)
     * Content should be slightly above current skill level
     */
    private BigDecimal difficultyScore(
            Integer contentDifficulty,
            UserPreferences preferences,
            Map<String, SkillLevel> skillLevels) {

        if (contentDifficulty == null) {
            return new BigDecimal("0.5");
        }

        // Use user's difficulty preference as baseline
        int userLevel = preferences.getDifficultyPreference() != null
            ? preferences.getDifficultyPreference()
            : 3;

        // ZPD: optimal difficulty is user level + 1
        int optimalDifficulty = userLevel + 1;

        // Calculate distance from optimal
        int distance = Math.abs(contentDifficulty - optimalDifficulty);

        // Score decreases as distance increases
        // Distance 0 = 1.0, Distance 1 = 0.8, Distance 2 = 0.5, Distance 3+ = 0.2
        return switch (distance) {
            case 0 -> new BigDecimal("1.0");
            case 1 -> new BigDecimal("0.8");
            case 2 -> new BigDecimal("0.5");
            default -> new BigDecimal("0.2");
        };
    }

    /**
     * Recency boost - newer content gets slight advantage
     */
    private BigDecimal recencyBoost(java.time.LocalDateTime createdAt) {
        if (createdAt == null) {
            return new BigDecimal("0.5");
        }

        long daysSinceCreation = java.time.Duration.between(createdAt, java.time.LocalDateTime.now()).toDays();

        // Decay over 90 days
        if (daysSinceCreation < 7) {
            return new BigDecimal("1.0"); // Very new
        } else if (daysSinceCreation < 30) {
            return new BigDecimal("0.8"); // Recent
        } else if (daysSinceCreation < 90) {
            return new BigDecimal("0.6"); // Somewhat recent
        } else {
            return new BigDecimal("0.4"); // Older
        }
    }

    /**
     * Length preference scoring
     */
    private BigDecimal lengthScore(Integer durationMinutes, UserPreferences preferences) {
        if (durationMinutes == null) {
            return new BigDecimal("0.5");
        }

        int targetMinutes = preferences.getDailyTimeTargetMinutes() != null
            ? preferences.getDailyTimeTargetMinutes()
            : 30;

        // Prefer content that takes less than daily target
        // Short content (<= target/2) = 1.0
        // Medium content (<= target) = 0.8
        // Long content (> target) = 0.4
        if (durationMinutes <= targetMinutes / 2) {
            return new BigDecimal("1.0");
        } else if (durationMinutes <= targetMinutes) {
            return new BigDecimal("0.8");
        } else {
            return new BigDecimal("0.4");
        }
    }

    /**
     * Collaborative filtering - placeholder implementation
     * In production, this would use user-user or item-item similarity
     */
    @Cacheable(value = "collaborativeScores", key = "#userId")
    private Map<String, BigDecimal> scoreCollaborative(List<Content> candidates, UUID userId) {
        // Placeholder: return neutral scores
        // In production, implement:
        // - User-user collaborative filtering
        // - Item-item collaborative filtering
        // - Matrix factorization
        // - Deep learning approaches

        Map<String, BigDecimal> scores = new HashMap<>();
        for (Content content : candidates) {
            // For now, return random-like scores for variety
            double randomScore = 0.3 + (Math.random() * 0.4); // 0.3 to 0.7
            scores.put(content.getId(), BigDecimal.valueOf(randomScore).setScale(4, RoundingMode.HALF_UP));
        }
        return scores;
    }

    /**
     * Enhanced scoring with TikTok-style behavioral profiling
     * Integrates: behavioral interests, session context, shorts strategy, explore/exploit
     */
    public Map<String, BigDecimal> scoreCandidatesWithBehavioral(
            List<Content> candidates,
            UUID userId,
            UserPreferences preferences,
            List<SkillLevel> skillLevels,
            List<Content> recentContent) {

        log.debug("Scoring {} candidates with behavioral profiling for user: {}", candidates.size(), userId);

        // Get behavioral profile from NestJS
        Optional<BehavioralProfile> behavioralProfile = behavioralProfileService.getProfile(userId);

        // Base scores using traditional methods
        Map<String, BigDecimal> baseScores = scoreCandidates(candidates, userId, preferences, skillLevels);

        // Apply behavioral enhancements
        Map<String, BigDecimal> enhancedScores = new HashMap<>();

        for (Content content : candidates) {
            String contentId = content.getId();
            BigDecimal baseScore = baseScores.getOrDefault(contentId, BigDecimal.valueOf(0.5));

            // Behavioral interest score (30%)
            BigDecimal behavioralInterestScore = calculateBehavioralInterestScore(content, behavioralProfile);

            // Session context score (20%)
            double sessionScore = sessionContextService.calculateSessionScore(content, behavioralProfile);

            // Strategy boost (10%)
            ShortsStrategyService.ContentStrategy strategy = shortsStrategyService.determineStrategy(
                behavioralProfile, recentContent
            );
            double strategyBoost = shortsStrategyService.calculateStrategyBoost(content, strategy);

            // Combine all scores
            // Base: 40%, Behavioral Interest: 30%, Session Context: 20%, Strategy: 10%
            BigDecimal finalScore = baseScore.multiply(new BigDecimal("0.4"))
                .add(behavioralInterestScore.multiply(new BigDecimal("0.3")))
                .add(BigDecimal.valueOf(sessionScore).multiply(new BigDecimal("0.2")))
                .add(BigDecimal.valueOf(strategyBoost));

            // Ensure score is between 0 and 1
            finalScore = finalScore.min(BigDecimal.ONE).max(BigDecimal.ZERO);

            enhancedScores.put(contentId, finalScore);
        }

        return enhancedScores;
    }

    /**
     * Calculate behavioral interest score from NestJS profiler
     */
    private BigDecimal calculateBehavioralInterestScore(
            Content content,
            Optional<BehavioralProfile> behavioralProfile) {

        if (behavioralProfile.isEmpty()) {
            return BigDecimal.valueOf(0.5); // Neutral if no profile
        }

        BehavioralProfile profile = behavioralProfile.get();
        if (profile.getInterests() == null || profile.getInterests().isEmpty()) {
            return BigDecimal.valueOf(0.5);
        }

        // Extract topics from content
        @SuppressWarnings("unchecked")
        List<String> contentTopics = content.getTopics() != null
            ? (List<String>) content.getTopics().get("topics")
            : List.of();

        if (contentTopics.isEmpty()) {
            return BigDecimal.valueOf(0.5);
        }

        // Calculate average interest score for content topics
        double avgInterest = contentTopics.stream()
            .mapToDouble(topic -> {
                BehavioralProfile.InterestScore interest = profile.getInterests().get(topic);
                return interest != null ? interest.getScore() : 0.5;
            })
            .average()
            .orElse(0.5);

        return BigDecimal.valueOf(avgInterest);
    }
}
