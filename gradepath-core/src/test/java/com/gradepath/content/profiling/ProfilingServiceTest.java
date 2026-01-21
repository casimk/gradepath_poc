package com.gradepath.content.profiling;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.gradepath.content.profiling.algorithm.EngagementClassifier;
import com.gradepath.content.profiling.algorithm.InterestScorer;
import com.gradepath.content.profiling.algorithm.JourneyAnalyzer;
import com.gradepath.content.recommendation.profile.BehavioralProfile;
import com.gradepath.content.recommendation.profile.BehavioralProfileService;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ProfilingService.
 * Tests Kafka event processing, profile orchestration, and service coordination.
 */
@Test(groups = "unit")
public class ProfilingServiceTest {

    @Mock
    private InterestScorer interestScorer;

    @Mock
    private EngagementClassifier engagementClassifier;

    @Mock
    private JourneyAnalyzer journeyAnalyzer;

    @Mock
    private BehavioralProfileService profileService;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    private ProfilingService profilingService;
    private ObjectMapper objectMapper;

    @BeforeMethod
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        profilingService = new ProfilingService(
            objectMapper,
            interestScorer,
            engagementClassifier,
            journeyAnalyzer,
            profileService,
            kafkaTemplate
        );
    }

    // Helper method to create a fresh profile
    private BehavioralProfile createFreshProfile(String userId) {
        return BehavioralProfile.builder()
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
    }

    // ========================================
    // Process Raw Event Tests
    // ========================================

    @Test
    public void processRawBehavioralEvent_contentJourney_processesEvent() throws Exception {
        // Given
        String message = """
            {
                "topic": "content_journey",
                "userId": "user-123",
                "contentId": "content-abc",
                "action": "completed",
                "topicTags": ["math"],
                "timeInContentSeconds": 120
            }
            """;

        when(profileService.findByUserId("user-123")).thenReturn(Optional.empty());

        // When
        ReflectionTestUtils.invokeMethod(profilingService, "processRawBehavioralEvent", message);

        // Then
        verify(interestScorer).updateInterests(any(), any());
        verify(journeyAnalyzer).analyzeJourney(any(), any());
        verify(profileService).saveProfile(any());
        verify(kafkaTemplate).send(eq("profile-updates"), anyString());
    }

    @Test
    public void processRawBehavioralEvent_sessionLifecycle_processesEvent() throws Exception {
        // Given
        String message = """
            {
                "topic": "session_lifecycle",
                "userId": "user-123",
                "eventType": "session_end",
                "durationSeconds": 300,
                "contentCount": 5
            }
            """;

        when(profileService.findByUserId("user-123")).thenReturn(Optional.empty());

        // When
        ReflectionTestUtils.invokeMethod(profilingService, "processRawBehavioralEvent", message);

        // Then
        verify(engagementClassifier).updateEngagement(any(), any());
        verify(profileService).saveProfile(any());
        verify(kafkaTemplate).send(eq("profile-updates"), anyString());
    }

    @Test
    public void processRawBehavioralEvent_nullTopic_warnsAndReturns() throws Exception {
        // Given
        String message = """
            {
                "userId": "user-123",
                "contentId": "content-abc"
            }
            """;

        // When
        ReflectionTestUtils.invokeMethod(profilingService, "processRawBehavioralEvent", message);

        // Then: should not throw, but also not call any processors
        verify(interestScorer, never()).updateInterests(any(), any());
        verify(engagementClassifier, never()).updateEngagement(any(), any());
    }

    @Test
    public void processRawBehavioralEvent_unknownTopic_warnsAndReturns() throws Exception {
        // Given
        String message = """
            {
                "topic": "unknown",
                "userId": "user-123",
                "contentId": "content-abc"
            }
            """;

        // When
        ReflectionTestUtils.invokeMethod(profilingService, "processRawBehavioralEvent", message);

        // Then: should not throw
        verify(interestScorer, never()).updateInterests(any(), any());
    }

    @Test
    public void processRawBehavioralEvent_malformedJson_logsError() throws Exception {
        // Given
        String message = "invalid json {";

        // When: should not throw
        ReflectionTestUtils.invokeMethod(profilingService, "processRawBehavioralEvent", message);

        // Then: should log error but not throw
        verify(interestScorer, never()).updateInterests(any(), any());
    }

    // ========================================
    // Process Journey Event Tests
    // ========================================

    @Test
    public void processJourneyEvent_createsNewProfile() throws Exception {
        // Given
        JsonNode event = objectMapper.readTree("""
            {
                "topic": "content_journey",
                "userId": "new-user",
                "contentId": "content-123",
                "action": "completed",
                "topicTags": ["math"],
                "timeInContentSeconds": 120
            }
            """);

        when(profileService.findByUserId("new-user")).thenReturn(Optional.empty());

        // When
        ReflectionTestUtils.invokeMethod(profilingService, "processJourneyEvent", event);

        // Then: profile is created with defaults
        ArgumentCaptor<BehavioralProfile> captor = ArgumentCaptor.forClass(BehavioralProfile.class);
        verify(profileService).saveProfile(captor.capture());

        BehavioralProfile savedProfile = captor.getValue();
        assertThat(savedProfile.getUserId()).isEqualTo("new-user");
        assertThat(savedProfile.getTotalContentConsumed()).isEqualTo(1);
    }

    @Test
    public void processJourneyEvent_updatesExistingProfile() throws Exception {
        // Given
        BehavioralProfile existingProfile = createFreshProfile("existing-user");
        existingProfile.setTotalContentConsumed(5);

        JsonNode event = objectMapper.readTree("""
            {
                "topic": "content_journey",
                "userId": "existing-user",
                "contentId": "content-123",
                "action": "completed",
                "topicTags": ["math"],
                "timeInContentSeconds": 120
            }
            """);

        when(profileService.findByUserId("existing-user")).thenReturn(Optional.of(existingProfile));

        // When
        ReflectionTestUtils.invokeMethod(profilingService, "processJourneyEvent", event);

        // Then: existing profile is used and updated
        ArgumentCaptor<BehavioralProfile> captor = ArgumentCaptor.forClass(BehavioralProfile.class);
        verify(profileService).saveProfile(captor.capture());

        BehavioralProfile savedProfile = captor.getValue();
        assertThat(savedProfile.getTotalContentConsumed()).isEqualTo(6); // 5 + 1
    }

    @Test
    public void processJourneyEvent_savesProfile() throws Exception {
        // Given
        JsonNode event = objectMapper.readTree("""
            {
                "topic": "content_journey",
                "userId": "user-123",
                "contentId": "content-abc",
                "action": "completed",
                "topicTags": ["math"],
                "timeInContentSeconds": 120
            }
            """);

        when(profileService.findByUserId("user-123")).thenReturn(Optional.empty());

        // When
        ReflectionTestUtils.invokeMethod(profilingService, "processJourneyEvent", event);

        // Then
        verify(profileService).saveProfile(any());
    }

    @Test
    public void processJourneyEvent_emitsProfileUpdate() throws Exception {
        // Given
        JsonNode event = objectMapper.readTree("""
            {
                "topic": "content_journey",
                "userId": "user-123",
                "contentId": "content-abc",
                "action": "completed",
                "topicTags": ["math"],
                "timeInContentSeconds": 120
            }
            """);

        when(profileService.findByUserId("user-123")).thenReturn(Optional.empty());

        // When
        ReflectionTestUtils.invokeMethod(profilingService, "processJourneyEvent", event);

        // Then
        verify(kafkaTemplate).send(eq("profile-updates"), anyString());
    }

    // ========================================
    // Process Session Event Tests
    // ========================================

    @Test
    public void processSessionEvent_sessionEnd_processes() throws Exception {
        // Given
        JsonNode event = objectMapper.readTree("""
            {
                "topic": "session_lifecycle",
                "userId": "user-123",
                "eventType": "session_end",
                "durationSeconds": 300,
                "contentCount": 5
            }
            """);

        when(profileService.findByUserId("user-123")).thenReturn(Optional.empty());

        // When
        ReflectionTestUtils.invokeMethod(profilingService, "processSessionEvent", event);

        // Then
        verify(engagementClassifier).updateEngagement(any(), any());
        assertThat(ReflectionTestUtils.getField(getProfileFromCache("user-123"), "totalSessions"))
            .isEqualTo(1);
    }

    @Test
    public void processSessionEvent_sessionStart_ignored() throws Exception {
        // Given
        JsonNode event = objectMapper.readTree("""
            {
                "topic": "session_lifecycle",
                "userId": "user-123",
                "eventType": "session_start"
            }
            """);

        when(profileService.findByUserId("user-123")).thenReturn(Optional.empty());

        // When
        ReflectionTestUtils.invokeMethod(profilingService, "processSessionEvent", event);

        // Then: engagement should not be updated for session_start
        verify(engagementClassifier, never()).updateEngagement(any(), any());
    }

    @Test
    public void processSessionEvent_missingDuration_defaultsTo0() throws Exception {
        // Given
        JsonNode event = objectMapper.readTree("""
            {
                "topic": "session_lifecycle",
                "userId": "user-123",
                "eventType": "session_end",
                "contentCount": 5
            }
            """);

        when(profileService.findByUserId("user-123")).thenReturn(Optional.empty());

        // When
        ReflectionTestUtils.invokeMethod(profilingService, "processSessionEvent", event);

        // Then: should process with duration = 0
        verify(engagementClassifier).updateEngagement(any(), any());
    }

    @Test
    public void processSessionEvent_missingContentCount_defaultsTo0() throws Exception {
        // Given
        JsonNode event = objectMapper.readTree("""
            {
                "topic": "session_lifecycle",
                "userId": "user-123",
                "eventType": "session_end",
                "durationSeconds": 300
            }
            """);

        when(profileService.findByUserId("user-123")).thenReturn(Optional.empty());

        // When
        ReflectionTestUtils.invokeMethod(profilingService, "processSessionEvent", event);

        // Then: should process with contentCount = 0
        verify(engagementClassifier).updateEngagement(any(), any());
    }

    // ========================================
    // GetOrCreateProfile Tests
    // ========================================

    @Test
    public void getOrCreateProfile_fromCache() {
        // Given: profile already in cache
        BehavioralProfile cachedProfile = createFreshProfile("cached-user");
        setProfileInCache("cached-user", cachedProfile);

        // When
        BehavioralProfile result = (BehavioralProfile) ReflectionTestUtils.invokeMethod(
            profilingService, "getOrCreateProfile", "cached-user"
        );

        // Then: returns cached profile
        assertThat(result).isSameAs(cachedProfile);
        verify(profileService, never()).findByUserId(anyString());
    }

    @Test
    public void getOrCreateProfile_fromDatabase() {
        // Given
        BehavioralProfile loadedProfile = createFreshProfile("loaded-user");
        when(profileService.findByUserId("loaded-user")).thenReturn(Optional.of(loadedProfile));

        // When
        BehavioralProfile result = (BehavioralProfile) ReflectionTestUtils.invokeMethod(
            profilingService, "getOrCreateProfile", "loaded-user"
        );

        // Then: loads from DB and caches it
        assertThat(result).isSameAs(loadedProfile);
        assertThat(getProfileFromCache("loaded-user")).isSameAs(loadedProfile);
    }

    @Test
    public void getOrCreateProfile_newProfile() {
        // Given: no existing profile
        when(profileService.findByUserId("new-user")).thenReturn(Optional.empty());

        // When
        BehavioralProfile result = (BehavioralProfile) ReflectionTestUtils.invokeMethod(
            profilingService, "getOrCreateProfile", "new-user"
        );

        // Then: creates new profile with defaults
        assertThat(result.getUserId()).isEqualTo("new-user");
        assertThat(result.getEngagement().getClassification()).isEqualTo("unknown");
        assertThat(result.getTotalContentConsumed()).isEqualTo(0);
        assertThat(result.getTotalSessions()).isEqualTo(0);
    }

    // ========================================
    // Emit Profile Update Tests
    // ========================================

    @Test
    public void emitProfileUpdate_kafkaFailure_logsWarning() {
        // Given: Kafka throws exception
        BehavioralProfile profile = createFreshProfile("kafka-fail-user");
        setProfileInCache("kafka-fail-user", profile);

        doThrow(new RuntimeException("Kafka down")).when(kafkaTemplate).send(anyString(), anyString());

        // When: should not throw
        ReflectionTestUtils.invokeMethod(profilingService, "emitProfileUpdate", profile);

        // Then: logs warning but doesn't throw
        // (verify warning was logged - would need to capture logs)
    }

    // ========================================
    // Parse Journey Event Tests
    // ========================================

    @Test
    public void parseJourneyEvent_allFieldsPresent() throws Exception {
        // Given
        JsonNode event = objectMapper.readTree("""
            {
                "journeyId": "j-123",
                "userId": "user-abc",
                "sessionId": "s-456",
                "contentId": "content-789",
                "contentType": "video",
                "action": "completed",
                "sequencePosition": 1,
                "timeInContentSeconds": 120,
                "topicTags": ["math", "algebra"],
                "difficultyLevel": "intermediate",
                "previousContentId": "content-old",
                "timestamp": 1234567890
            }
            """);

        // When
        InterestScorer.RawJourneyEvent result = (InterestScorer.RawJourneyEvent)
            ReflectionTestUtils.invokeMethod(profilingService, "parseJourneyEvent", event);

        // Then: all fields parsed correctly
        assertThat(result.journeyId()).isEqualTo("j-123");
        assertThat(result.userId()).isEqualTo("user-abc");
        assertThat(result.contentId()).isEqualTo("content-789");
        assertThat(result.action()).isEqualTo("completed");
        assertThat(result.topicTags()).containsExactly("math", "algebra");
    }

    @Test
    public void parseJourneyEvent_missingOptionalFields() throws Exception {
        // Given
        JsonNode event = objectMapper.readTree("""
            {
                "userId": "user-xyz",
                "contentId": "content-simple",
                "action": "started"
            }
            """);

        // When
        InterestScorer.RawJourneyEvent result = (InterestScorer.RawJourneyEvent)
            ReflectionTestUtils.invokeMethod(profilingService, "parseJourneyEvent", event);

        // Then: optional fields are null
        assertThat(result.userId()).isEqualTo("user-xyz");
        assertThat(result.timeInContentSeconds()).isNull();
        assertThat(result.topicTags()).isEmpty();
    }

    // ========================================
    // Helper Method Tests
    // ========================================

    @Test
    public void getValue_nullField_returnsNull() throws Exception {
        // Given
        JsonNode node = objectMapper.readTree("""
            {
                "field1": "value1",
                "field2": null
            }
            """);

        // When
        String result = (String) ReflectionTestUtils.invokeMethod(
            profilingService, "getValue", node, "field2"
        );

        // Then: returns null
        assertThat(result).isNull();
    }

    @Test
    public void getValue_missingField_returnsNull() throws Exception {
        // Given
        JsonNode node = objectMapper.readTree("{\"field1\": \"value1\"}");

        // When
        String result = (String) ReflectionTestUtils.invokeMethod(
            profilingService, "getValue", node, "missingField"
        );

        // Then: returns null
        assertThat(result).isNull();
    }

    @Test
    public void getListValue_arrayField_returnsList() throws Exception {
        // Given
        JsonNode node = objectMapper.readTree("{\"tags\": [\"a\", \"b\", \"c\"]}");

        // When
        @SuppressWarnings("unchecked")
        List<String> result = (List<String>) ReflectionTestUtils.invokeMethod(
            profilingService, "getListValue", node, "tags"
        );

        // Then: returns list
        assertThat(result).containsExactly("a", "b", "c");
    }

    @Test
    public void getListValue_missingField_returnsEmptyList() throws Exception {
        // Given
        JsonNode node = objectMapper.readTree("{\"otherField\": \"value\"}");

        // When
        @SuppressWarnings("unchecked")
        List<String> result = (List<String>) ReflectionTestUtils.invokeMethod(
            profilingService, "getListValue", node, "tags"
        );

        // Then: returns empty list
        assertThat(result).isEmpty();
    }

    @Test
    public void getListValue_nonArrayField_returnsEmptyList() throws Exception {
        // Given
        JsonNode node = objectMapper.readTree("{\"tags\": \"not-an-array\"}");

        // When
        @SuppressWarnings("unchecked")
        List<String> result = (List<String>) ReflectionTestUtils.invokeMethod(
            profilingService, "getListValue", node, "tags"
        );

        // Then: returns empty list
        assertThat(result).isEmpty();
    }

    // ========================================
    // Helper Methods
    // ========================================

    private BehavioralProfile getProfileFromCache(String userId) {
        @SuppressWarnings("unchecked")
        Map<String, BehavioralProfile> cache = (Map<String, BehavioralProfile>)
            ReflectionTestUtils.getField(profilingService, "profileCache");
        return cache.get(userId);
    }

    private void setProfileInCache(String userId, BehavioralProfile profile) {
        @SuppressWarnings("unchecked")
        Map<String, BehavioralProfile> cache = (Map<String, BehavioralProfile>)
            ReflectionTestUtils.getField(profilingService, "profileCache");
        cache.put(userId, profile);
    }
}
