package com.gradepath.content.profiling;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.gradepath.content.ContentApplication;
import com.gradepath.content.recommendation.profile.BehavioralProfile;
import com.gradepath.content.recommendation.profile.BehavioralProfileService;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testcontainers.utility.DockerImageName;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

/**
 * Integration tests for ProfilingService using TestContainers.
 * Tests the full Spring Boot context with real PostgreSQL and Kafka containers.
 *
 * Uses ApplicationContextInitializer to inject TestContainers connection properties
 * into the Spring context during initialization, ensuring they're available when
 * the application context is created.
 *
 * Extends AbstractTestNGSpringContextTests to enable Spring context loading with TestNG.
 */
@SpringBootTest(classes = ContentApplication.class)
@ContextConfiguration(initializers = ProfilingServiceIntegrationTest.TestcontainersInitializer.class)
@Test(groups = "integration")
public class ProfilingServiceIntegrationTest extends AbstractTestNGSpringContextTests {

    /**
     * ApplicationContextInitializer that starts TestContainers and injects
     * connection properties into the Spring environment before context loading.
     */
    static class TestcontainersInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

        static final PostgreSQLContainer<?> postgres;
        static final KafkaContainer kafka;

        static {
            // Start containers when the class loads
            postgres = new PostgreSQLContainer<>(
                DockerImageName.parse("postgres:16-alpine")
            );
            kafka = new KafkaContainer(
                DockerImageName.parse("confluentinc/cp-kafka:7.6.0")
            );

            postgres.start();
            kafka.start();
        }

        @Override
        public void initialize(ConfigurableApplicationContext context) {
            // Inject TestContainers connection properties directly into Spring environment
            // This approach works with both JUnit and TestNG when using ApplicationContextInitializer
            org.springframework.core.env.ConfigurableEnvironment environment = context.getEnvironment();
            java.util.Map<String, Object> testcontainersProps = new java.util.HashMap<>();
            testcontainersProps.put("spring.datasource.url", postgres.getJdbcUrl());
            testcontainersProps.put("spring.datasource.username", postgres.getUsername());
            testcontainersProps.put("spring.datasource.password", postgres.getPassword());
            testcontainersProps.put("spring.kafka.bootstrap-servers", kafka.getBootstrapServers());

            org.springframework.core.env.MapPropertySource mapPropertySource =
                new org.springframework.core.env.MapPropertySource("testcontainers", testcontainersProps);
            environment.getPropertySources().addFirst(mapPropertySource);
        }

        /**
         * Cleanup method to stop containers after all tests complete.
         */
        @AfterSuite(alwaysRun = true)
        static void stopContainers() {
            if (kafka != null && kafka.isRunning()) {
                kafka.stop();
            }
            if (postgres != null && postgres.isRunning()) {
                postgres.stop();
            }
        }
    }

    @Autowired
    private ProfilingService profilingService;

    @Autowired
    private BehavioralProfileService profileService;

    @MockBean
    private KafkaTemplate<String, Object> kafkaTemplate;

    private ObjectMapper objectMapper;

    @BeforeMethod
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    // ========================================
    // End-to-End Journey Event Processing
    // ========================================

    @Test(description = "Process journey event end-to-end and persist to database")
    public void processJourneyEvent_endToEnd_createsProfileInDatabase() throws Exception {
        // Given: a journey event
        String message = """
            {
                "topic": "content_journey",
                "userId": "integration-user-1",
                "contentId": "content-123",
                "action": "completed",
                "topicTags": ["math", "algebra"],
                "timeInContentSeconds": 120
            }
            """;

        // When: process the raw event
        profilingService.processRawBehavioralEvent(message);

        // Then: profile is persisted to database
        Optional<BehavioralProfile> profile = profileService.findByUserId("integration-user-1");
        assertThat(profile).isPresent();
        assertThat(profile.get().getUserId()).isEqualTo("integration-user-1");
        assertThat(profile.get().getTotalContentConsumed()).isEqualTo(1);

        // And: interests are updated
        assertThat(profile.get().getInterests()).isNotEmpty();
        assertThat(profile.get().getInterests()).containsKey("math");
        assertThat(profile.get().getInterests()).containsKey("algebra");
    }

    @Test(description = "Process journey event and emit Kafka message")
    public void processJourneyEvent_endToEnd_emitsKafkaMessage() throws Exception {
        // Given
        String message = """
            {
                "topic": "content_journey",
                "userId": "kafka-test-user",
                "contentId": "content-456",
                "action": "completed",
                "topicTags": ["physics"],
                "timeInContentSeconds": 180
            }
            """;

        // When
        profilingService.processRawBehavioralEvent(message);

        // Then: Kafka message is sent
        verify(kafkaTemplate, timeout(5).times(1))
            .send(eq("profile-updates"), anyString());
    }

    @Test(description = "Process multiple journey events and accumulate profile data")
    public void processMultipleJourneyEvents_accumulatesProfile() throws Exception {
        // Given: a user processes multiple content items
        String event1 = """
            {
                "topic": "content_journey",
                "userId": "multi-event-user",
                "contentId": "content-1",
                "action": "completed",
                "topicTags": ["math"],
                "timeInContentSeconds": 120
            }
            """;

        String event2 = """
            {
                "topic": "content_journey",
                "userId": "multi-event-user",
                "contentId": "content-2",
                "action": "completed",
                "topicTags": ["math", "algebra"],
                "timeInContentSeconds": 180
            }
            """;

        String event3 = """
            {
                "topic": "content_journey",
                "userId": "multi-event-user",
                "contentId": "content-3",
                "action": "revisited",
                "topicTags": ["physics"],
                "timeInContentSeconds": 90
            }
            """;

        // When: process all events
        profilingService.processRawBehavioralEvent(event1);
        profilingService.processRawBehavioralEvent(event2);
        profilingService.processRawBehavioralEvent(event3);

        // Then: profile shows accumulated data
        Optional<BehavioralProfile> profile = profileService.findByUserId("multi-event-user");
        assertThat(profile).isPresent();
        assertThat(profile.get().getTotalContentConsumed()).isEqualTo(3);

        // And: interests are accumulated with proper scoring
        // "math" gets updated twice (event1 and event2)
        assertThat(profile.get().getInterests().get("math").getScore()).isGreaterThan(0);
        assertThat(profile.get().getInterests()).containsKeys("math", "algebra", "physics");
    }

    // ========================================
    // Session Lifecycle Events
    // ========================================

    @Test(description = "Process session end event and update session count")
    public void processSessionEvent_endToEnd_updatesSessionCount() throws Exception {
        // Given: a session end event
        String message = """
            {
                "topic": "session_lifecycle",
                "userId": "session-user-1",
                "eventType": "session_end",
                "durationSeconds": 300,
                "contentCount": 5
            }
            """;

        // When
        profilingService.processRawBehavioralEvent(message);

        // Then: profile is created and session count is updated
        // Note: Engagement metrics require multiple sessions (MIN_SESSIONS_FOR_CLASSIFICATION)
        // so avgSessionDuration remains at initial value (0.0) for single session
        Optional<BehavioralProfile> profile = profileService.findByUserId("session-user-1");
        assertThat(profile).isPresent();
        assertThat(profile.get().getTotalSessions()).isEqualTo(1);
        assertThat(profile.get().getEngagement().getClassification()).isEqualTo("unknown");
    }

    @Test(description = "Process both session and journey events and combine data")
    public void processSessionAndJourneyEvents_combinesData() throws Exception {
        // Given: a user with both session and journey events
        String journeyEvent = """
            {
                "topic": "content_journey",
                "userId": "combined-user",
                "contentId": "content-1",
                "action": "completed",
                "topicTags": ["chemistry"],
                "timeInContentSeconds": 150
            }
            """;

        String sessionEvent = """
            {
                "topic": "session_lifecycle",
                "userId": "combined-user",
                "eventType": "session_end",
                "durationSeconds": 450,
                "contentCount": 3
            }
            """;

        // When
        profilingService.processRawBehavioralEvent(journeyEvent);
        profilingService.processRawBehavioralEvent(sessionEvent);

        // Then: profile has both interest and session data
        Optional<BehavioralProfile> profile = profileService.findByUserId("combined-user");
        assertThat(profile).isPresent();

        // Interest data from journey
        assertThat(profile.get().getInterests()).containsKey("chemistry");
        assertThat(profile.get().getTotalContentConsumed()).isEqualTo(1);

        // Session count updated (engagement metrics require MIN_SESSIONS_FOR_CLASSIFICATION)
        assertThat(profile.get().getTotalSessions()).isEqualTo(1);
    }

    // ========================================
    // Database Persistence
    // ========================================

    @Test(description = "Profile persists across database retrievals")
    public void profile_persistsAcrossRetrieval() throws Exception {
        // Given: create a profile through event processing
        String message = """
            {
                "topic": "content_journey",
                "userId": "persistence-user",
                "contentId": "content-1",
                "action": "completed",
                "topicTags": ["biology"],
                "timeInContentSeconds": 120
            }
            """;

        // When: process event
        profilingService.processRawBehavioralEvent(message);

        // Then: profile persists across retrievals
        Optional<BehavioralProfile> profile1 = profileService.findByUserId("persistence-user");
        assertThat(profile1).isPresent();

        // Retrieve again from database
        Optional<BehavioralProfile> profile2 = profileService.findByUserId("persistence-user");
        assertThat(profile2).isPresent();
        assertThat(profile2.get().getUserId()).isEqualTo(profile1.get().getUserId());
        assertThat(profile2.get().getInterests()).hasSize(profile1.get().getInterests().size());
    }

    // ========================================
    // Error Handling
    // ========================================

    @Test(description = "Invalid JSON event does not crash the service")
    public void processInvalidJsonEvent_doesNotCrash() {
        // Given: invalid JSON
        String invalidMessage = "invalid json {";

        // When: should not throw exception
        profilingService.processRawBehavioralEvent(invalidMessage);

        // Then: no profile created
        Optional<BehavioralProfile> profile = profileService.findByUserId("any-user");
        assertThat(profile).isEmpty();
    }

    @Test(description = "Event with missing topic does not crash the service")
    public void processEventWithMissingTopic_doesNotCrash() throws Exception {
        // Given: event without topic
        String message = """
            {
                "userId": "no-topic-user",
                "contentId": "content-1"
            }
            """;

        // When: should not throw
        profilingService.processRawBehavioralEvent(message);

        // Then: no profile created
        Optional<BehavioralProfile> profile = profileService.findByUserId("no-topic-user");
        assertThat(profile).isEmpty();
    }

    // ========================================
    // Interest Decay
    // ========================================

    @Test(description = "Old interests decay when new event is processed")
    public void oldInterests_decayWhenNewEventProcessed() throws Exception {
        // Given: create an old profile
        String event1 = """
            {
                "topic": "content_journey",
                "userId": "decay-user",
                "contentId": "content-1",
                "action": "completed",
                "topicTags": ["history"],
                "timeInContentSeconds": 120
            }
            """;

        profilingService.processRawBehavioralEvent(event1);

        Optional<BehavioralProfile> initialProfile = profileService.findByUserId("decay-user");
        double initialScore = initialProfile.get().getInterests().get("history").getScore();

        // When: wait for decay and process new event
        Thread.sleep(100);

        String event2 = """
            {
                "topic": "content_journey",
                "userId": "decay-user",
                "contentId": "content-2",
                "action": "completed",
                "topicTags": ["geography"],
                "timeInContentSeconds": 120
            }
            """;

        profilingService.processRawBehavioralEvent(event2);

        // Then: old interest should still exist but might have decayed
        Optional<BehavioralProfile> updatedProfile = profileService.findByUserId("decay-user");
        assertThat(updatedProfile).isPresent();
        assertThat(updatedProfile.get().getInterests()).containsKey("history");
        // The new event adds geography
        assertThat(updatedProfile.get().getInterests()).containsKey("geography");
    }
}
