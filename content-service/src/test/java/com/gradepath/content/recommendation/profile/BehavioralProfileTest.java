package com.gradepath.content.recommendation.profile;

import org.testng.annotations.Test;
import org.testcontainers.shaded.org.apache.commons.lang3.builder.EqualsBuilder;
import org.testcontainers.shaded.org.apache.commons.lang3.builder.HashCodeBuilder;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for BehavioralProfile model.
 * Tests Lombok-generated builders, getters, setters, equals, hashCode.
 */
@Test(groups = "unit")
public class BehavioralProfileTest {

    // ========================================
    // Builder Tests
    // ========================================

    @Test
    public void builder_createsValidProfile() {
        // Given
        Map<String, BehavioralProfile.InterestScore> interests = new HashMap<>();
        interests.put("math", BehavioralProfile.InterestScore.builder()
            .topic("math")
            .score(30.0)
            .lastUpdated(Instant.now())
            .build());

        BehavioralProfile.EngagementPattern engagement = BehavioralProfile.EngagementPattern.builder()
            .classification("deep_learner")
            .confidence(0.75)
            .avgSessionDuration(600.0)
            .avgContentPerSession(3.0)
            .timePerContentRatio(200.0)
            .uniqueTopicRatio(0.6)
            .build();

        // When
        BehavioralProfile profile = BehavioralProfile.builder()
            .userId("user-123")
            .timestamp(Instant.now())
            .interests(interests)
            .engagement(engagement)
            .peakWindows(List.of())
            .commonPaths(List.of())
            .totalSessions(5)
            .totalContentConsumed(15)
            .build();

        // Then: all fields set correctly
        assertThat(profile.getUserId()).isEqualTo("user-123");
        assertThat(profile.getInterests()).hasSize(1);
        assertThat(profile.getEngagement().getClassification()).isEqualTo("deep_learner");
        assertThat(profile.getTotalSessions()).isEqualTo(5);
        assertThat(profile.getTotalContentConsumed()).isEqualTo(15);
    }

    @Test
    public void builder_defaultValues_emptyCollections() {
        // Given
        BehavioralProfile profile = BehavioralProfile.builder()
            .userId("user-defaults")
            .build();

        // Then: collections have default empty values
        assertThat(profile.getInterests()).isNotNull(); // Has @Builder.Default
        assertThat(profile.getInterests()).isEmpty();
        assertThat(profile.getPeakWindows()).isNotNull(); // Has @Builder.Default
        assertThat(profile.getPeakWindows()).isEmpty();
        assertThat(profile.getCommonPaths()).isNotNull(); // Has @Builder.Default
        assertThat(profile.getCommonPaths()).isEmpty();
    }

    // ========================================
    // Nested Builder Tests
    // ========================================

    @Test
    public void InterestScoreBuilder_createsValid() {
        // When
        BehavioralProfile.InterestScore score = BehavioralProfile.InterestScore.builder()
            .topic("math")
            .score(30.0)
            .lastUpdated(Instant.now())
            .build();

        // Then
        assertThat(score.getTopic()).isEqualTo("math");
        assertThat(score.getScore()).isEqualTo(30.0);
        assertThat(score.getLastUpdated()).isNotNull();
    }

    @Test
    public void EngagementPatternBuilder_createsValid() {
        // When
        BehavioralProfile.EngagementPattern pattern = BehavioralProfile.EngagementPattern.builder()
            .classification("deep_learner")
            .confidence(0.75)
            .avgSessionDuration(600.0)
            .avgContentPerSession(3.0)
            .timePerContentRatio(200.0)
            .uniqueTopicRatio(0.6)
            .build();

        // Then
        assertThat(pattern.getClassification()).isEqualTo("deep_learner");
        assertThat(pattern.getConfidence()).isEqualTo(0.75);
        assertThat(pattern.getAvgSessionDuration()).isEqualTo(600.0);
        assertThat(pattern.getUniqueTopicRatio()).isEqualTo(0.6);
    }

    @Test
    public void ContentTransitionBuilder_createsValid() {
        // When
        BehavioralProfile.ContentTransition transition = BehavioralProfile.ContentTransition.builder()
            .fromContent("content-1")
            .toContent("content-2")
            .frequency(5)
            .probability(0.25)
            .build();

        // Then
        assertThat(transition.getFromContent()).isEqualTo("content-1");
        assertThat(transition.getToContent()).isEqualTo("content-2");
        assertThat(transition.getFrequency()).isEqualTo(5);
        assertThat(transition.getProbability()).isEqualTo(0.25);
    }

    @Test
    public void PeakWindowBuilder_createsValid() {
        // When
        BehavioralProfile.PeakWindow window = BehavioralProfile.PeakWindow.builder()
            .hour(14)
            .day("Monday")
            .score(0.85)
            .build();

        // Then
        assertThat(window.getHour()).isEqualTo(14);
        assertThat(window.getDay()).isEqualTo("Monday");
        assertThat(window.getScore()).isEqualTo(0.85);
    }

    // ========================================
    // Lombok Generated Methods Tests
    // ========================================

    @Test
    public void lombok_gettersSetters_work() {
        // Given
        BehavioralProfile profile = BehavioralProfile.builder()
            .userId("user-getset")
            .build();

        // When: using setters
        profile.setTotalSessions(10);
        profile.setTotalContentConsumed(25);

        // Then: getters return set values
        assertThat(profile.getTotalSessions()).isEqualTo(10);
        assertThat(profile.getTotalContentConsumed()).isEqualTo(25);
    }

    @Test
    public void lombok_equalsHashCode_work() {
        // Given
        Instant sameTimestamp = Instant.now();
        BehavioralProfile profile1 = BehavioralProfile.builder()
            .userId("user-equals")
            .timestamp(sameTimestamp)
            .build();

        BehavioralProfile profile2 = BehavioralProfile.builder()
            .userId("user-equals")
            .timestamp(sameTimestamp)
            .build();

        BehavioralProfile profile3 = BehavioralProfile.builder()
            .userId("user-different")
            .timestamp(sameTimestamp)
            .build();

        // Then: equals and hashCode work correctly
        assertThat(profile1).isEqualTo(profile2);
        assertThat(profile1.hashCode()).isEqualTo(profile2.hashCode());
        assertThat(profile1).isNotEqualTo(profile3);
    }

    @Test
    public void lombok_toString_works() {
        // Given
        BehavioralProfile profile = BehavioralProfile.builder()
            .userId("user-tostring")
            .build();

        // When
        String result = profile.toString();

        // Then: toString contains userId
        assertThat(result).contains("user-tostring");
    }
}
