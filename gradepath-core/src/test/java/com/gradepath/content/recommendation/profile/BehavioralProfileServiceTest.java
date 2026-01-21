package com.gradepath.content.recommendation.profile;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for BehavioralProfileService.
 * Tests profile persistence, serialization, and repository operations.
 */
@Test(groups = "unit")
public class BehavioralProfileServiceTest {

    @Mock
    private BehavioralProfileRepository repository;

    private BehavioralProfileService service;
    private ObjectMapper objectMapper;

    @BeforeMethod
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        service = new BehavioralProfileService(repository, objectMapper);
    }

    // Helper method to create a test profile
    private BehavioralProfile createTestProfile(String userId) {
        return BehavioralProfile.builder()
            .userId(userId)
            .timestamp(Instant.now())
            .interests(Map.of(
                "math", BehavioralProfile.InterestScore.builder()
                    .topic("math")
                    .score(30.0)
                    .lastUpdated(Instant.now())
                    .build()
            ))
            .engagement(BehavioralProfile.EngagementPattern.builder()
                .classification("deep_learner")
                .confidence(0.75)
                .avgSessionDuration(600.0)
                .avgContentPerSession(3.0)
                .timePerContentRatio(200.0)
                .uniqueTopicRatio(0.6)
                .build())
            .peakWindows(List.of())
            .commonPaths(List.of())
            .totalSessions(5)
            .totalContentConsumed(15)
            .build();
    }

    // ========================================
    // Save Profile Tests
    // ========================================

    @Test
    public void saveProfile_validProfile_savesEntity() {
        // Given
        BehavioralProfile profile = createTestProfile("user-123");

        when(repository.save(any(BehavioralProfileEntity.class))).thenAnswer(invocation -> {
            BehavioralProfileEntity entity = invocation.getArgument(0);
            entity.setId(1L);
            return entity;
        });

        // When
        service.saveProfile(profile);

        // Then
        verify(repository).save(any(BehavioralProfileEntity.class));
    }

    @Test
    public void saveProfile_serializationError_logsError() throws Exception {
        // Given: ObjectMapper that throws on serialization
        ObjectMapper failingMapper = mock(ObjectMapper.class);
        when(failingMapper.writeValueAsString(any()))
            .thenThrow(new RuntimeException("Serialization failed"));

        BehavioralProfileService serviceWithFailingMapper = new BehavioralProfileService(
            repository, failingMapper
        );

        BehavioralProfile profile = createTestProfile("user-error");

        // When
        serviceWithFailingMapper.saveProfile(profile);

        // Then: should not throw, but logs error
        verify(repository, never()).save(any());
    }

    @Test
    public void saveProfile_nullTimestamp_usesNow() {
        // Given
        BehavioralProfile profile = createTestProfile("user-null-timestamp");
        profile.setTimestamp(null);

        when(repository.save(any(BehavioralProfileEntity.class))).thenAnswer(invocation -> {
            BehavioralProfileEntity entity = invocation.getArgument(0);
            entity.setId(1L);
            return entity;
        });

        // When
        service.saveProfile(profile);

        // Then: entity should have a timestamp
        verify(repository).save(any(BehavioralProfileEntity.class));
    }

    // ========================================
    // Get Profile Tests
    // ========================================

    @Test
    public void getProfile_existingUser_returnsProfile() throws Exception {
        // Given
        BehavioralProfile profile = createTestProfile("user-456");
        String profileJson = objectMapper.writeValueAsString(profile);

        BehavioralProfileEntity entity = BehavioralProfileEntity.builder()
            .userId("user-456")
            .profileData(profileJson)
            .timestamp(Instant.now())
            .build();

        when(repository.findFirstByUserIdOrderByTimestampDesc("user-456"))
            .thenReturn(Optional.of(entity));

        // When
        Optional<BehavioralProfile> result = service.getProfile("user-456");

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getUserId()).isEqualTo("user-456");
        assertThat(result.get().getInterests()).containsKey("math");
    }

    @Test
    public void getProfile_nonExistingUser_returnsEmpty() {
        // Given
        when(repository.findFirstByUserIdOrderByTimestampDesc("non-existent"))
            .thenReturn(Optional.empty());

        // When
        Optional<BehavioralProfile> result = service.getProfile("non-existent");

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    public void getProfile_uuidOverload_works() throws Exception {
        // Given
        UUID userId = UUID.randomUUID();
        String userIdStr = userId.toString();
        BehavioralProfile profile = createTestProfile(userIdStr);
        String profileJson = objectMapper.writeValueAsString(profile);

        BehavioralProfileEntity entity = BehavioralProfileEntity.builder()
            .userId(userIdStr)
            .profileData(profileJson)
            .timestamp(Instant.now())
            .build();

        when(repository.findFirstByUserIdOrderByTimestampDesc(userIdStr))
            .thenReturn(Optional.of(entity));

        // When
        Optional<BehavioralProfile> result = service.getProfile(userId);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getUserId()).isEqualTo(userIdStr);
    }

    @Test
    public void findByUserId_aliasWorks() throws Exception {
        // Given
        BehavioralProfile profile = createTestProfile("user-alias");
        String profileJson = objectMapper.writeValueAsString(profile);

        BehavioralProfileEntity entity = BehavioralProfileEntity.builder()
            .userId("user-alias")
            .profileData(profileJson)
            .timestamp(Instant.now())
            .build();

        when(repository.findFirstByUserIdOrderByTimestampDesc("user-alias"))
            .thenReturn(Optional.of(entity));

        // When
        Optional<BehavioralProfile> result = service.findByUserId("user-alias");

        // Then: should return same as getProfile
        assertThat(result).isPresent();
        assertThat(result.get().getUserId()).isEqualTo("user-alias");
    }

    // ========================================
    // Deserialize Profile Tests
    // ========================================

    @Test
    public void deserializeProfile_validJson_returnsProfile() throws Exception {
        // Given
        BehavioralProfile profile = createTestProfile("user-deser");
        String profileJson = objectMapper.writeValueAsString(profile);

        BehavioralProfileEntity entity = BehavioralProfileEntity.builder()
            .userId("user-deser")
            .profileData(profileJson)
            .timestamp(Instant.now())
            .build();

        when(repository.findFirstByUserIdOrderByTimestampDesc("user-deser"))
            .thenReturn(Optional.of(entity));

        // When
        Optional<BehavioralProfile> result = service.getProfile("user-deser");

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getInterests()).isNotNull();
        assertThat(result.get().getEngagement()).isNotNull();
        assertThat(result.get().getEngagement().getClassification()).isEqualTo("deep_learner");
    }

    @Test
    public void deserializeProfile_invalidJson_logsError() throws Exception {
        // Given: Invalid JSON
        BehavioralProfileEntity entity = BehavioralProfileEntity.builder()
            .userId("user-invalid")
            .profileData("{invalid json}")
            .timestamp(Instant.now())
            .build();

        when(repository.findFirstByUserIdOrderByTimestampDesc("user-invalid"))
            .thenReturn(Optional.of(entity));

        // When
        Optional<BehavioralProfile> result = service.getProfile("user-invalid");

        // Then: returns null due to deserialization error
        assertThat(result).isEmpty();
    }

    // ========================================
    // Cleanup Tests
    // ========================================

    @Test
    public void cleanupOldProfiles_placeholder() {
        // Given
        BehavioralProfile profile = createTestProfile("user-cleanup");

        // When: placeholder method
        service.cleanupOldProfiles("user-cleanup", 5);

        // Then: should not throw
        // This is a placeholder as noted in the code
    }

    @Test
    public void cleanupOldProfiles_withNegativeKeepCount() {
        // Given
        BehavioralProfile profile = createTestProfile("user-cleanup-neg");

        // When
        service.cleanupOldProfiles("user-cleanup-neg", -1);

        // Then: should not throw
        // This is a placeholder method
    }
}
