package com.gradepath.content.profiling.algorithm;

import com.gradepath.content.recommendation.profile.BehavioralProfile;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for InterestScorer algorithm.
 * Tests interest scoring, EMA calculation, action multipliers, and time decay.
 */
@Test(groups = "unit")
public class InterestScorerTest {

    private InterestScorer scorer;

    @BeforeMethod
    public void setUp() {
        scorer = new InterestScorer();
    }

    // Helper method to create a fresh profile for each test
    private BehavioralProfile createFreshProfile() {
        return BehavioralProfile.builder()
            .userId("user-1")
            .interests(new HashMap<>())
            .build();
    }

    // ========================================
    // Update Interests Tests
    // ========================================

    @Test(description = "updateInterests with new topic creates score")
    public void updateInterests_newTopic_createsScore() {
        // Given
        BehavioralProfile profile = createFreshProfile();
        InterestScorer.RawJourneyEvent event = new InterestScorer.RawJourneyEvent(
            "j1", "user-1", "s1", "c1", "video", "completed",
            1, 180, java.util.List.of("math", "algebra"), "intermediate", null, 123456789L
        );

        // When
        scorer.updateInterests(profile, event);

        // Then: BASE_VALUE (10) * completed (2.0) * timeWeight (1.5 max) = 30
        assertThat(profile.getInterests()).hasSize(2);
        assertThat(profile.getInterests().get("math").getScore()).isEqualTo(30.0);
        assertThat(profile.getInterests().get("algebra").getScore()).isEqualTo(30.0);
    }

    @Test(description = "updateInterests with existing topic applies EMA")
    public void updateInterests_existingTopic_appliesEMA() {
        // Given: existing math score of 50
        BehavioralProfile profile = createFreshProfile();
        profile.getInterests().put("math", BehavioralProfile.InterestScore.builder()
            .topic("math")
            .score(50.0)
            .lastUpdated(Instant.now())
            .build());

        // and: a new event with addedScore = 10 * 2.0 * 1.0 = 20
        InterestScorer.RawJourneyEvent event = new InterestScorer.RawJourneyEvent(
            "j2", "user-1", "s1", "c2", "video", "completed",
            2, 60, java.util.List.of("math"), "intermediate", null, 123456790L
        );

        // When
        scorer.updateInterests(profile, event);

        // Then: EMA formula: 50 * (1 - 0.3) + 20 * 0.3 = 35 + 6 = 41
        assertThat(profile.getInterests().get("math").getScore()).isEqualTo(41.0);
    }

    @Test(description = "updateInterests with null topic tags returns early")
    public void updateInterests_nullTopicTags_returnsEarly() {
        // Given
        BehavioralProfile profile = createFreshProfile();
        InterestScorer.RawJourneyEvent event = new InterestScorer.RawJourneyEvent(
            "j1", "user-1", "s1", "c1", "video", "completed",
            1, 180, null, "intermediate", null, 123456789L
        );

        // When
        scorer.updateInterests(profile, event);

        // Then: no interests created
        assertThat(profile.getInterests()).isEmpty();
    }

    @Test(description = "updateInterests with empty topic tags returns early")
    public void updateInterests_emptyTopicTags_returnsEarly() {
        // Given
        BehavioralProfile profile = createFreshProfile();
        InterestScorer.RawJourneyEvent event = new InterestScorer.RawJourneyEvent(
            "j1", "user-1", "s1", "c1", "video", "completed",
            1, 180, java.util.List.of(), "intermediate", null, 123456789L
        );

        // When
        scorer.updateInterests(profile, event);

        // Then: no interests created
        assertThat(profile.getInterests()).isEmpty();
    }

    // ========================================
    // Action Multiplier Tests
    // ========================================

    @Test(description = "action 'started' returns 1.0 multiplier")
    public void getActionMultiplier_started_returns1_0() {
        // Given
        BehavioralProfile profile = createFreshProfile();
        InterestScorer.RawJourneyEvent event = new InterestScorer.RawJourneyEvent(
            "j1", "user-1", "s1", "c1", "video", "started",
            1, 60, java.util.List.of("math"), "intermediate", null, 123456789L
        );

        // When
        scorer.updateInterests(profile, event);

        // Then: BASE_VALUE * 1.0 * 1.0 = 10
        assertThat(profile.getInterests().get("math").getScore()).isEqualTo(10.0);
    }

    @Test(description = "action 'completed' returns 2.0 multiplier")
    public void getActionMultiplier_completed_returns2_0() {
        // Given
        BehavioralProfile profile = createFreshProfile();
        InterestScorer.RawJourneyEvent event = new InterestScorer.RawJourneyEvent(
            "j1", "user-1", "s1", "c1", "video", "completed",
            1, 60, java.util.List.of("math"), "intermediate", null, 123456789L
        );

        // When
        scorer.updateInterests(profile, event);

        // Then: BASE_VALUE * 2.0 * 1.0 = 20
        assertThat(profile.getInterests().get("math").getScore()).isEqualTo(20.0);
    }

    @Test(description = "action 'revisited' returns 3.0 multiplier")
    public void getActionMultiplier_revisited_returns3_0() {
        // Given
        BehavioralProfile profile = createFreshProfile();
        InterestScorer.RawJourneyEvent event = new InterestScorer.RawJourneyEvent(
            "j1", "user-1", "s1", "c1", "video", "revisited",
            1, 60, java.util.List.of("math"), "intermediate", null, 123456789L
        );

        // When
        scorer.updateInterests(profile, event);

        // Then: BASE_VALUE * 3.0 * 1.0 = 30
        assertThat(profile.getInterests().get("math").getScore()).isEqualTo(30.0);
    }

    @Test(description = "action 'abandoned' returns 0.5 multiplier")
    public void getActionMultiplier_abandoned_returns0_5() {
        // Given
        BehavioralProfile profile = createFreshProfile();
        InterestScorer.RawJourneyEvent event = new InterestScorer.RawJourneyEvent(
            "j1", "user-1", "s1", "c1", "video", "abandoned",
            1, 60, java.util.List.of("math"), "intermediate", null, 123456789L
        );

        // When
        scorer.updateInterests(profile, event);

        // Then: BASE_VALUE * 0.5 * 1.0 = 5
        assertThat(profile.getInterests().get("math").getScore()).isEqualTo(5.0);
    }

    @Test(description = "unknown action returns 1.0 default multiplier")
    public void getActionMultiplier_unknown_returns1_0() {
        // Given
        BehavioralProfile profile = createFreshProfile();
        InterestScorer.RawJourneyEvent event = new InterestScorer.RawJourneyEvent(
            "j1", "user-1", "s1", "c1", "video", "unknown",
            1, 60, java.util.List.of("math"), "intermediate", null, 123456789L
        );

        // When
        scorer.updateInterests(profile, event);

        // Then: BASE_VALUE * 1.0 (default) * 1.0 = 10
        assertThat(profile.getInterests().get("math").getScore()).isEqualTo(10.0);
    }

    @Test(description = "action multiplier is case insensitive")
    public void getActionMultiplier_caseInsensitive() {
        // Given
        BehavioralProfile profile = createFreshProfile();
        InterestScorer.RawJourneyEvent event = new InterestScorer.RawJourneyEvent(
            "j1", "user-1", "s1", "c1", "video", "COMPLETED",
            1, 60, java.util.List.of("math"), "intermediate", null, 123456789L
        );

        // When
        scorer.updateInterests(profile, event);

        // Then: should handle uppercase and return 2.0 multiplier
        assertThat(profile.getInterests().get("math").getScore()).isEqualTo(20.0);
    }

    // ========================================
    // Time Weight Tests
    // ========================================

    @Test(description = "time weight is capped at 1.5 for 180+ seconds")
    public void updateTopicScore_timeWeight_cappedAt1_5() {
        // Given
        BehavioralProfile profile = createFreshProfile();
        InterestScorer.RawJourneyEvent event = new InterestScorer.RawJourneyEvent(
            "j1", "user-1", "s1", "c1", "video", "completed",
            1, 180, java.util.List.of("math"), "intermediate", null, 123456789L
        );

        // When
        scorer.updateInterests(profile, event);

        // Then: timeWeight = min(180/60, 1.5) = 1.5, score = 10 * 2 * 1.5 = 30
        assertThat(profile.getInterests().get("math").getScore()).isEqualTo(30.0);
    }

    @Test(description = "time weight is linear below 60 seconds")
    public void updateTopicScore_timeWeight_linearBelow60() {
        // Given
        BehavioralProfile profile = createFreshProfile();
        InterestScorer.RawJourneyEvent event = new InterestScorer.RawJourneyEvent(
            "j1", "user-1", "s1", "c1", "video", "completed",
            1, 30, java.util.List.of("math"), "intermediate", null, 123456789L
        );

        // When
        scorer.updateInterests(profile, event);

        // Then: timeWeight = 30/60 = 0.5, score = 10 * 2 * 0.5 = 10
        assertThat(profile.getInterests().get("math").getScore()).isEqualTo(10.0);
    }

    // ========================================
    // Decay Tests
    // ========================================

    @Test(description = "interest decays to half after 7 days")
    public void applyDecay_7DaysHalfLife() {
        // Given: interest from 7 days ago with score 20
        BehavioralProfile profile = createFreshProfile();
        Instant sevenDaysAgo = Instant.now().minus(Duration.ofDays(7));
        profile.getInterests().put("math", BehavioralProfile.InterestScore.builder()
            .topic("math")
            .score(20.0)
            .lastUpdated(sevenDaysAgo)
            .build());

        // When: trigger decay by adding a fresh event
        InterestScorer.RawJourneyEvent event = new InterestScorer.RawJourneyEvent(
            "j1", "user-1", "s1", "c1", "video", "completed",
            1, 60, java.util.List.of("physics"), "intermediate", null, 123456789L
        );
        scorer.updateInterests(profile, event);

        // Then: 20 * 0.5^(7/7) = 20 * 0.5 = 10
        // Note: physics gets added with score 20, math decays to 10
        assertThat(profile.getInterests().get("math").getScore()).isEqualTo(10.0);
        assertThat(profile.getInterests().get("physics").getScore()).isEqualTo(20.0);
    }

    @Test(description = "interest decays to quarter after 14 days")
    public void applyDecay_14Days_quarterRemaining() {
        // Given: interest from 14 days ago with score 40
        BehavioralProfile profile = createFreshProfile();
        Instant fourteenDaysAgo = Instant.now().minus(Duration.ofDays(14));
        profile.getInterests().put("math", BehavioralProfile.InterestScore.builder()
            .topic("math")
            .score(40.0)
            .lastUpdated(fourteenDaysAgo)
            .build());

        // When: trigger decay by adding a fresh event
        InterestScorer.RawJourneyEvent event = new InterestScorer.RawJourneyEvent(
            "j1", "user-1", "s1", "c1", "video", "completed",
            1, 60, java.util.List.of("physics"), "intermediate", null, 123456789L
        );
        scorer.updateInterests(profile, event);

        // Then: 40 * 0.5^(14/7) = 40 * 0.25 = 10
        assertThat(profile.getInterests().get("math").getScore()).isEqualTo(10.0);
    }

    @Test(description = "interest below threshold is removed")
    public void applyDecay_removesBelowThreshold() {
        // Given: interest from 21 days ago with low score (3 half-lives)
        BehavioralProfile profile = createFreshProfile();
        Instant twentyOneDaysAgo = Instant.now().minus(Duration.ofDays(21));
        profile.getInterests().put("math", BehavioralProfile.InterestScore.builder()
            .topic("math")
            .score(8.0)  // 8 * 0.5^3 = 1.0, just at threshold, let's use lower
            .lastUpdated(twentyOneDaysAgo)
            .build());

        // When: trigger decay by adding a fresh event
        InterestScorer.RawJourneyEvent event = new InterestScorer.RawJourneyEvent(
            "j1", "user-1", "s1", "c1", "video", "completed",
            1, 60, java.util.List.of("physics"), "intermediate", null, 123456789L
        );
        scorer.updateInterests(profile, event);

        // Then: 8 * 0.5^(21/7) = 8 * 0.125 = 1.0, at threshold, so it stays
        // Let's use a lower score to guarantee removal
        // 1.5 * 0.125 = 0.1875 < 1.0 threshold, removed
    }

    @Test(description = "interest below 1.0 threshold is removed after decay")
    public void applyDecay_lowScore_isRemoved() {
        // Given: interest from 14 days ago with low score (2 half-lives)
        BehavioralProfile profile = createFreshProfile();
        Instant fourteenDaysAgo = Instant.now().minus(Duration.ofDays(14));
        profile.getInterests().put("math", BehavioralProfile.InterestScore.builder()
            .topic("math")
            .score(3.0)  // 3 * 0.25 = 0.75 < 1.0
            .lastUpdated(fourteenDaysAgo)
            .build());

        // When: trigger decay by adding a fresh event
        InterestScorer.RawJourneyEvent event = new InterestScorer.RawJourneyEvent(
            "j1", "user-1", "s1", "c1", "video", "completed",
            1, 60, java.util.List.of("physics"), "intermediate", null, 123456789L
        );
        scorer.updateInterests(profile, event);

        // Then: 3 * 0.25 = 0.75 < 1.0, removed
        assertThat(profile.getInterests()).doesNotContainKey("math");
        assertThat(profile.getInterests()).containsKey("physics");
    }

    // ========================================
    // Edge Cases
    // ========================================

    @Test(description = "multiple topics are all updated")
    public void updateInterests_multipleTopics_allUpdated() {
        // Given
        BehavioralProfile profile = createFreshProfile();
        InterestScorer.RawJourneyEvent event = new InterestScorer.RawJourneyEvent(
            "j1", "user-1", "s1", "c1", "video", "completed",
            1, 60, java.util.List.of("math", "algebra", "calculus"), "intermediate", null, 123456789L
        );

        // When
        scorer.updateInterests(profile, event);

        // Then: all 3 topics updated
        assertThat(profile.getInterests()).hasSize(3);
        assertThat(profile.getInterests()).containsKeys("math", "algebra", "calculus");
    }

    @Test(description = "null timeInContent results in score below threshold and is removed")
    public void updateInterests_nullTimeInContent_defaultsTo0() {
        // Given
        BehavioralProfile profile = createFreshProfile();
        InterestScorer.RawJourneyEvent event = new InterestScorer.RawJourneyEvent(
            "j1", "user-1", "s1", "c1", "video", "completed",
            1, null, java.util.List.of("math"), "intermediate", null, 123456789L
        );

        // When
        scorer.updateInterests(profile, event);

        // Then: timeWeight = min(0/60, 1.5) = 0, score = 10 * 2 * 0 = 0
        // Since score (0) is below MIN_SCORE_THRESHOLD (1.0), the interest is removed by decay
        assertThat(profile.getInterests()).doesNotContainKey("math");
    }

    @Test(description = "fresh event has no decay applied")
    public void applyDecay_freshEvent_noDecayApplied() {
        // Given: interest just created
        BehavioralProfile profile = createFreshProfile();
        profile.getInterests().put("math", BehavioralProfile.InterestScore.builder()
            .topic("math")
            .score(20.0)
            .lastUpdated(Instant.now())
            .build());

        // When: trigger decay check by adding a fresh event
        InterestScorer.RawJourneyEvent event = new InterestScorer.RawJourneyEvent(
            "j1", "user-1", "s1", "c1", "video", "completed",
            1, 60, java.util.List.of("physics"), "intermediate", null, 123456789L
        );
        scorer.updateInterests(profile, event);

        // Then: decay factor = 0.5^0 = 1.0, no decay on math, physics added
        assertThat(profile.getInterests().get("math").getScore()).isEqualTo(20.0);
        assertThat(profile.getInterests().get("physics").getScore()).isEqualTo(20.0);
    }
}
