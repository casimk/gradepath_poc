package com.gradepath.content.profile.service;

import com.gradepath.content.profile.event.SkillLevelChangedEvent;
import com.gradepath.content.profile.model.*;
import com.gradepath.content.profile.repository.*;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
@Slf4j
public class UserProfileService {

    private final UserRepository userRepository;
    private final UserProfileRepository profileRepository;
    private final UserPreferencesRepository preferencesRepository;
    private final SkillLevelRepository skillLevelRepository;
    private final ApplicationEventPublisher eventPublisher;

    public UserProfileService(
            UserRepository userRepository,
            UserProfileRepository profileRepository,
            UserPreferencesRepository preferencesRepository,
            SkillLevelRepository skillLevelRepository,
            ApplicationEventPublisher eventPublisher) {
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.preferencesRepository = preferencesRepository;
        this.skillLevelRepository = skillLevelRepository;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Get or create user by external ID
     */
    public User getOrCreateUser(String externalUserId) {
        return userRepository.findByExternalUserId(externalUserId)
            .orElseGet(() -> {
                User user = User.builder()
                    .externalUserId(externalUserId)
                    .build();
                return userRepository.save(user);
            });
    }

    /**
     * Get user by UUID
     */
    public User getUser(UUID userId) {
        return userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
    }

    /**
     * Get user profile with caching
     */
    @Cacheable(value = "userProfile", key = "#userId")
    public UserProfileResponse getProfile(UUID userId) {
        User user = getUser(userId);
        UserProfile profile = profileRepository.findByUserId(userId)
            .orElse(UserProfile.builder().userId(userId).build());
        UserPreferences preferences = preferencesRepository.findByUserId(userId)
            .orElse(createDefaultPreferences(userId));
        List<SkillLevel> skills = skillLevelRepository.findByUserId(userId);

        return UserProfileResponse.from(user, profile, preferences, skills);
    }

    /**
     * Update user profile
     */
    public UserProfile updateProfile(UUID userId, UserProfileRequest request) {
        UserProfile profile = profileRepository.findByUserId(userId)
            .orElse(UserProfile.builder().userId(userId).build());

        profile.setInterests(request.interests());
        profile.setLearningGoals(request.learningGoals());
        profile.setPreferredLearningStyle(request.preferredLearningStyle());
        profile.setLanguage(request.language());

        return profileRepository.save(profile);
    }

    /**
     * Update user preferences
     */
    public UserPreferences updatePreferences(UUID userId, UserPreferencesRequest request) {
        UserPreferences preferences = preferencesRepository.findByUserId(userId)
            .orElse(createDefaultPreferences(userId));

        preferences.setDifficultyPreference(request.difficultyPreference());
        preferences.setContentTypePreferences(request.contentTypePreferences());
        preferences.setTopicPreferences(request.topicPreferences());
        preferences.setDailyTimeTargetMinutes(request.dailyTimeTargetMinutes());

        return preferencesRepository.save(preferences);
    }

    /**
     * Handle skill level changed event
     */
    @EventListener
    public void handleSkillLevelChanged(SkillLevelChangedEvent event) {
        log.info("Handling SkillLevelChangedEvent for user: {}, topic: {}, score: {}",
            event.userId(), event.topic(), event.newScore());

        SkillLevel skill = skillLevelRepository
            .findByUserIdAndTopic(event.userId(), event.topic())
            .orElse(SkillLevel.builder()
                .user(getUser(event.userId()))
                .topic(event.topic())
                .level(50) // Default starting level
                .confidenceScore(BigDecimal.ZERO)
                .build());

        // Update skill level based on assessment
        int newLevel = calculateNewLevel(skill.getLevel(), event.newScore());
        BigDecimal newConfidence = increaseConfidence(skill.getConfidenceScore());

        skill.setLevel(newLevel);
        skill.setConfidenceScore(newConfidence);
        skill.setLastAssessedAt(Instant.now());

        skillLevelRepository.save(skill);

        log.info("Updated skill level for user: {}, topic: {}, level: {} -> {}, confidence: {} -> {}",
            event.userId(), event.topic(), skill.getLevel(), newLevel,
            skill.getConfidenceScore(), newConfidence);
    }

    /**
     * Create default preferences for a user
     */
    private UserPreferences createDefaultPreferences(UUID userId) {
        UserPreferences preferences = UserPreferences.builder()
            .userId(userId)
            .difficultyPreference(3) // Medium difficulty
            .dailyTimeTargetMinutes(30)
            .build();
        return preferencesRepository.save(preferences);
    }

    /**
     * Calculate new skill level based on performance
     */
    private int calculateNewLevel(int currentLevel, Integer newScore) {
        if (newScore == null) {
            return currentLevel;
        }

        // Simple algorithm: move towards the score
        // If score > current level, increase level
        // If score < current level, decrease level
        // Use weighted average to smooth transitions
        return (int) Math.round(currentLevel * 0.7 + newScore * 0.3);
    }

    /**
     * Increase confidence score slightly
     */
    private BigDecimal increaseConfidence(BigDecimal currentConfidence) {
        double current = currentConfidence != null ? currentConfidence.doubleValue() : 0.0;
        double increase = 0.05; // Increase by 5% each time
        double newConfidence = Math.min(1.0, current + increase);
        return BigDecimal.valueOf(newConfidence);
    }

    // Response DTO
    public record UserProfileResponse(
        UUID userId,
        String email,
        String displayName,
        List<String> interests,
        List<String> learningGoals,
        String preferredLearningStyle,
        String language,
        Integer difficultyPreference,
        java.util.Map<String, Double> contentTypePreferences,
        java.util.Map<String, Double> topicPreferences,
        Integer dailyTimeTargetMinutes,
        List<SkillDto> skills
    ) {
        static UserProfileResponse from(User user, UserProfile profile, UserPreferences preferences, List<SkillLevel> skills) {
            return new UserProfileResponse(
                user.getId(),
                user.getEmail(),
                user.getDisplayName(),
                profile.getInterests(),
                profile.getLearningGoals(),
                profile.getPreferredLearningStyle(),
                profile.getLanguage(),
                preferences.getDifficultyPreference(),
                preferences.getContentTypePreferences(),
                preferences.getTopicPreferences(),
                preferences.getDailyTimeTargetMinutes(),
                skills.stream().map(SkillDto::from).toList()
            );
        }
    }

    public record SkillDto(
        String topic,
        Integer level,
        BigDecimal confidenceScore,
        Instant lastAssessedAt
    ) {
        static SkillDto from(SkillLevel skill) {
            return new SkillDto(
                skill.getTopic(),
                skill.getLevel(),
                skill.getConfidenceScore(),
                skill.getLastAssessedAt()
            );
        }
    }

    // Request DTOs
    public record UserProfileRequest(
        List<String> interests,
        List<String> learningGoals,
        String preferredLearningStyle,
        String language
    ) {}

    public record UserPreferencesRequest(
        Integer difficultyPreference,
        java.util.Map<String, Double> contentTypePreferences,
        java.util.Map<String, Double> topicPreferences,
        Integer dailyTimeTargetMinutes
    ) {}
}
