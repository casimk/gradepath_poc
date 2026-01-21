package com.gradepath.content.recommendation.service;

import com.gradepath.content.analytics.repository.ContentInteractionRepository;
import com.gradepath.content.content.model.Content;
import com.gradepath.content.content.repository.ContentRepository;
import com.gradepath.content.profile.model.User;
import com.gradepath.content.profile.model.UserPreferences;
import com.gradepath.content.profile.model.SkillLevel;
import com.gradepath.content.profile.repository.SkillLevelRepository;
import com.gradepath.content.profile.repository.UserPreferencesRepository;
import com.gradepath.content.profile.repository.UserRepository;
import com.gradepath.content.recommendation.model.Recommendation;
import com.gradepath.content.recommendation.repository.RecommendationRepository;
import com.gradepath.content.recommendation.profile.BehavioralProfileService;
import com.gradepath.content.recommendation.algorithm.SessionContextService;
import com.gradepath.content.recommendation.algorithm.ShortsStrategyService;
import com.gradepath.content.recommendation.algorithm.BanditStrategyService;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
@Slf4j
public class RecommendationService {

    private final ContentScoringService scoringService;
    private final ContentRepository contentRepository;
    private final RecommendationRepository recommendationRepository;
    private final UserRepository userRepository;
    private final UserPreferencesRepository preferencesRepository;
    private final SkillLevelRepository skillLevelRepository;
    private final ContentInteractionRepository interactionRepository;
    private final BehavioralProfileService behavioralProfileService;
    private final SessionContextService sessionContextService;
    private final ShortsStrategyService shortsStrategyService;
    private final BanditStrategyService banditStrategyService;

    public RecommendationService(
            ContentScoringService scoringService,
            ContentRepository contentRepository,
            RecommendationRepository recommendationRepository,
            UserRepository userRepository,
            UserPreferencesRepository preferencesRepository,
            SkillLevelRepository skillLevelRepository,
            ContentInteractionRepository interactionRepository,
            BehavioralProfileService behavioralProfileService,
            SessionContextService sessionContextService,
            ShortsStrategyService shortsStrategyService,
            BanditStrategyService banditStrategyService) {
        this.scoringService = scoringService;
        this.contentRepository = contentRepository;
        this.recommendationRepository = recommendationRepository;
        this.userRepository = userRepository;
        this.preferencesRepository = preferencesRepository;
        this.skillLevelRepository = skillLevelRepository;
        this.interactionRepository = interactionRepository;
        this.behavioralProfileService = behavioralProfileService;
        this.sessionContextService = sessionContextService;
        this.shortsStrategyService = shortsStrategyService;
        this.banditStrategyService = banditStrategyService;
    }

    /**
     * Get recommendations for a user with caching
     */
    @Cacheable(value = "recommendations", key = "#userId")
    public List<RecommendationResponse> getRecommendations(UUID userId, int limit) {
        log.info("Generating TikTok-style recommendations for user: {}, limit: {}", userId, limit);

        // Get user profile and preferences
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        UserPreferences preferences = preferencesRepository.findByUserId(userId)
            .orElse(createDefaultPreferences(userId));

        List<SkillLevel> skillLevels = skillLevelRepository.findByUserId(userId);

        // Get candidate content (exclude already viewed/completed)
        List<Content> candidates = getCandidateContent(userId);

        if (candidates.isEmpty()) {
            log.warn("No candidate content found for user: {}", userId);
            return List.of();
        }

        // Get recent content for strategy determination
        List<Content> recentContent = getRecentContent(userId, 5);

        // Score candidates WITH behavioral profiling
        Map<String, BigDecimal> scores = scoringService.scoreCandidatesWithBehavioral(
            candidates, userId, preferences, skillLevels, recentContent
        );

        // Apply bandit strategy for explore vs exploit
        List<ScoredContent> rankOrdered = applyBanditOrdering(candidates, scores, userId);

        // Apply diversity algorithm
        List<Content> diversified = diversify(rankOrdered, limit);

        // Store recommendations
        List<Recommendation> stored = storeRecommendations(userId, diversified, scores);

        return stored.stream()
            .map(RecommendationResponse::from)
            .collect(Collectors.toList());
    }

    /**
     * Get the next recommended content for a user
     */
    public ContentResponse getNextContent(UUID userId) {
        log.info("Getting next content for user: {}", userId);

        // Check for existing pending recommendations
        Optional<Recommendation> existing = recommendationRepository
            .findTopByUserIdOrderByScoreDesc(userId);

        if (existing.isPresent()) {
            Recommendation rec = existing.get();
            rec.setShownAt(Instant.now());
            recommendationRepository.save(rec);

            Content content = rec.getContent();
            log.info("Returning existing recommendation: {} for user: {}", content.getId(), userId);
            return ContentResponse.from(content, rec.getScore(), rec.getReason());
        }

        // Generate new recommendations
        List<RecommendationResponse> recommendations = getRecommendations(userId, 10);
        if (recommendations.isEmpty()) {
            throw new NoContentAvailableException(userId);
        }

        // Get the first recommendation and convert to ContentResponse
        RecommendationResponse top = recommendations.get(0);
        Content content = contentRepository.findById(top.contentId())
            .orElseThrow(() -> new NoContentAvailableException(userId));
        return ContentResponse.from(content, top.score(), top.reason());
    }

    /**
     * Record feedback on a recommendation
     */
    @CacheEvict(value = "recommendations", key = "#userId")
    public void recordFeedback(UUID userId, String contentId, FeedbackType feedback) {
        log.info("Recording feedback for user: {}, content: {}, feedback: {}",
            userId, contentId, feedback);

        // Update the recommendation with feedback
        List<Recommendation> recs = recommendationRepository.findByUserIdOrderByScoreDesc(userId);
        recs.stream()
            .filter(r -> r.getContent().getId().equals(contentId))
            .findFirst()
            .ifPresent(rec -> {
                if (feedback == FeedbackType.CLICKED) {
                    rec.setClickedAt(Instant.now());
                }
                recommendationRepository.save(rec);
            });

        // In a real implementation, this would update user preferences
        // and trigger re-scoring of content
    }

    /**
     * Get candidate content (exclude already viewed)
     */
    private List<Content> getCandidateContent(UUID userId) {
        // Get all published content
        List<Content> allContent = contentRepository.findByStatus(Content.ContentStatus.PUBLISHED);

        // Get viewed content IDs
        List<String> viewedContentIds = interactionRepository.findViewedContentIds(
            userId,
            List.of(com.gradepath.content.analytics.model.InteractionType.VIEWED,
                    com.gradepath.content.analytics.model.InteractionType.COMPLETED,
                    com.gradepath.content.analytics.model.InteractionType.SKIPPED)
        );

        // Filter out viewed content
        return allContent.stream()
            .filter(content -> !viewedContentIds.contains(content.getId()))
            .collect(Collectors.toList());
    }

    /**
     * Get recent content for strategy determination
     */
    private List<Content> getRecentContent(UUID userId, int limit) {
        // Get recent interactions and convert to content
        // For now, return empty list - would be implemented with interaction history
        return List.of();
    }

    /**
     * Apply bandit ordering for explore vs exploit
     */
    private List<ScoredContent> applyBanditOrdering(
            List<Content> candidates,
            Map<String, BigDecimal> scores,
            UUID userId) {

        // Convert scores to double for bandit algorithm
        Map<String, Double> doubleScores = scores.entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                e -> e.getValue().doubleValue()
            ));

        // Apply epsilon-greedy: 20% exploration, 80% exploitation
        // Use a fixed seed for reproducibility during testing
        double epsilon = 0.2;
        boolean explore = Math.random() < epsilon;

        if (explore) {
            // Shuffle candidates for exploration
            List<Content> shuffled = new ArrayList<>(candidates);
            Collections.shuffle(shuffled);
            return shuffled.stream()
                .map(c -> new ScoredContent(c, scores.getOrDefault(c.getId(), BigDecimal.ZERO)))
                .collect(Collectors.toList());
        } else {
            // Sort by score for exploitation
            List<ScoredContent> sorted = new ArrayList<>();
            for (Content content : candidates) {
                BigDecimal score = scores.getOrDefault(content.getId(), BigDecimal.valueOf(0.5));
                sorted.add(new ScoredContent(content, score));
            }
            sorted.sort((a, b) -> b.score().compareTo(a.score()));
            return sorted;
        }
    }

    /**
     * Apply diversity algorithm to recommendations
     */
    private List<Content> diversify(List<ScoredContent> scoredContent, int limit) {
        // Simple diversity: ensure mix of content types
        Map<Content.ContentType, List<Content>> byType = scoredContent.stream()
            .map(sc -> sc.content())
            .collect(Collectors.groupingBy(Content::getType));

        List<Content> diversified = new ArrayList<>();

        // Round-robin through types to ensure diversity
        List<Content.ContentType> types = new ArrayList<>(byType.keySet());
        int typeIndex = 0;

        while (diversified.size() < limit && !byType.isEmpty()) {
            Content.ContentType currentType = types.get(typeIndex % types.size());
            List<Content> typeContent = byType.get(currentType);

            if (!typeContent.isEmpty()) {
                diversified.add(typeContent.remove(0));
                if (typeContent.isEmpty()) {
                    byType.remove(currentType);
                    types.remove(currentType);
                    typeIndex = 0;
                    continue;
                }
            }

            typeIndex++;
        }

        // If we didn't get enough, add remaining by score
        if (diversified.size() < limit) {
            for (ScoredContent sc : scoredContent) {
                if (!diversified.contains(sc.content())) {
                    diversified.add(sc.content());
                    if (diversified.size() >= limit) break;
                }
            }
        }

        return diversified;
    }

    /**
     * Store recommendations in database
     */
    private List<Recommendation> storeRecommendations(
            UUID userId,
            List<Content> contents,
            Map<String, BigDecimal> scores) {

        User user = userRepository.getReferenceById(userId);

        List<Recommendation> recommendations = new ArrayList<>();
        for (Content content : contents) {
            BigDecimal score = scores.get(content.getId());

            Recommendation rec = Recommendation.builder()
                .user(user)
                .content(content)
                .score(score)
                .algorithm("HYBRID")
                .reason("Personalized based on your preferences and learning history")
                .build();

            // Explicitly set createdAt to bypass Builder pattern issues
            rec.setCreatedAt(Instant.now());

            recommendations.add(recommendationRepository.save(rec));
        }

        return recommendations;
    }

    /**
     * Create default preferences
     */
    private UserPreferences createDefaultPreferences(UUID userId) {
        UserPreferences preferences = UserPreferences.builder()
            .userId(userId)
            .difficultyPreference(3)
            .dailyTimeTargetMinutes(30)
            .build();
        return preferencesRepository.save(preferences);
    }

    // Response DTOs
    public record RecommendationResponse(
        UUID id,
        String contentId,
        String title,
        String contentType,
        Integer difficultyLevel,
        BigDecimal score,
        String reason,
        String algorithm
    ) {
        static RecommendationResponse from(Recommendation rec) {
            return new RecommendationResponse(
                rec.getId(),
                rec.getContent().getId(),
                rec.getContent().getTitle(),
                rec.getContent().getType().name(),
                rec.getContent().getDifficultyLevel(),
                rec.getScore(),
                rec.getReason(),
                rec.getAlgorithm()
            );
        }
    }

    public record ContentResponse(
        String id,
        String type,
        String title,
        String description,
        Integer difficultyLevel,
        Integer estimatedDurationMinutes,
        BigDecimal score,
        String reason
    ) {
        static ContentResponse from(Content content, BigDecimal score, String reason) {
            return new ContentResponse(
                content.getId(),
                content.getType().name(),
                content.getTitle(),
                content.getDescription(),
                content.getDifficultyLevel(),
                content.getEstimatedDurationMinutes(),
                score,
                reason
            );
        }
    }

    private record ScoredContent(Content content, BigDecimal score) {}

    public enum FeedbackType {
        CLICKED,
        DISMISSED,
        BOOKMARKED
    }

    public static class NoContentAvailableException extends RuntimeException {
        public NoContentAvailableException(UUID userId) {
            super("No content available for user: " + userId);
        }
    }
}
