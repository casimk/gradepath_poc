package com.gradepath.content.recommendation.profile;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Service
@Slf4j
public class BehavioralProfileService {

    private final BehavioralProfileRepository repository;
    private final ObjectMapper objectMapper;

    public BehavioralProfileService(
            BehavioralProfileRepository repository,
            ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    /**
     * Store or update behavioral profile
     */
    @Transactional
    public void saveProfile(BehavioralProfile profile) {
        try {
            String profileJson = objectMapper.writeValueAsString(profile);

            BehavioralProfileEntity entity = BehavioralProfileEntity.builder()
                .userId(profile.getUserId())
                .profileData(profileJson)
                .timestamp(profile.getTimestamp() != null ? profile.getTimestamp() : Instant.now())
                .build();

            repository.save(entity);
            log.info("Saved behavioral profile for user: {}", profile.getUserId());
        } catch (Exception e) {
            log.error("Error serializing behavioral profile for user: {}", profile.getUserId(), e);
        }
    }

    /**
     * Get latest behavioral profile for user
     */
    public Optional<BehavioralProfile> getProfile(String userId) {
        return repository.findFirstByUserIdOrderByTimestampDesc(userId)
            .map(this::deserializeProfile);
    }

    /**
     * Get latest behavioral profile for user (UUID overload)
     */
    public Optional<BehavioralProfile> getProfile(java.util.UUID userId) {
        return getProfile(userId.toString());
    }

    /**
     * Alias for getProfile - used by ProfilingService
     */
    public Optional<BehavioralProfile> findByUserId(String userId) {
        return getProfile(userId);
    }

    private BehavioralProfile deserializeProfile(BehavioralProfileEntity entity) {
        try {
            return objectMapper.readValue(entity.getProfileData(), BehavioralProfile.class);
        } catch (JsonProcessingException e) {
            log.error("Error deserializing behavioral profile for user: {}", entity.getUserId(), e);
            return null;
        }
    }

    /**
     * Clean up old profiles (keep only latest N per user)
     */
    @Transactional
    public void cleanupOldProfiles(String userId, int keepCount) {
        // Implementation would delete old profiles, keeping only the most recent N
        // For now, this is a placeholder
    }
}
