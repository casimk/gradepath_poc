package com.gradepath.content.profiling.algorithm;

import com.gradepath.content.recommendation.profile.BehavioralProfile;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for JourneyAnalyzer algorithm.
 * Tests Markov chain content transitions, topic tracking, and path prediction.
 */
@Test(groups = "unit")
public class JourneyAnalyzerTest {

    private JourneyAnalyzer analyzer;

    @BeforeMethod
    public void setUp() {
        analyzer = new JourneyAnalyzer();
        analyzer.clearTransitions();
        analyzer.clearUserTopics();
    }

    // Helper method to create a fresh profile for each test
    private BehavioralProfile createFreshProfile(String userId) {
        return BehavioralProfile.builder()
            .userId(userId)
            .timestamp(java.time.Instant.now())
            .interests(Map.of())
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
    // Analyze Journey Tests
    // ========================================

    @Test(description = "analyzeJourney tracks topic tags for user")
    public void analyzeJourney_tracksTopicTags() {
        // Given
        BehavioralProfile profile = createFreshProfile("user-1");
        InterestScorer.RawJourneyEvent event = new InterestScorer.RawJourneyEvent(
            "j1", "user-1", "s1", "c1", "video", "completed",
            1, 60, List.of("math", "algebra"), "intermediate", null, 123456789L
        );

        // When
        analyzer.analyzeJourney(profile, event);

        // Then: topics are tracked for the user
        assertThat(analyzer.getUserTopics("user-1")).contains("math", "algebra");
    }

    @Test(description = "analyzeJourney with null topic tags does not throw error")
    public void analyzeJourney_nullTopicTags_noError() {
        // Given
        BehavioralProfile profile = createFreshProfile("user-null-topics");
        InterestScorer.RawJourneyEvent event = new InterestScorer.RawJourneyEvent(
            "j1", "user-null-topics", "s1", "c1", "video", "completed",
            1, 60, null, "intermediate", null, 123456789L
        );

        // When: should not throw
        analyzer.analyzeJourney(profile, event);

        // Then: topics treated as empty
        assertThat(analyzer.getUserTopics("user-null-topics")).isEmpty();
    }

    @Test(description = "analyzeJourney with empty topic tags does not throw error")
    public void analyzeJourney_emptyTopicTags_noError() {
        // Given
        BehavioralProfile profile = createFreshProfile("user-empty-topics");
        InterestScorer.RawJourneyEvent event = new InterestScorer.RawJourneyEvent(
            "j1", "user-empty-topics", "s1", "c1", "video", "completed",
            1, 60, List.of(), "intermediate", null, 123456789L
        );

        // When: should not throw
        analyzer.analyzeJourney(profile, event);

        // Then: topics treated as empty
        assertThat(analyzer.getUserTopics("user-empty-topics")).isEmpty();
    }

    // ========================================
    // Transition Tracking Tests
    // ========================================

    @Test(description = "trackTransition increments frequency for same transition")
    public void trackTransition_incrementsFrequency() {
        // Given
        BehavioralProfile profile = createFreshProfile("user-transition");
        InterestScorer.RawJourneyEvent event1 = new InterestScorer.RawJourneyEvent(
            "j1", "user-transition", "s1", "c2", "video", "completed",
            1, 60, List.of("math"), "intermediate", "c1", 123456789L
        );
        InterestScorer.RawJourneyEvent event2 = new InterestScorer.RawJourneyEvent(
            "j2", "user-transition", "s1", "c2", "video", "completed",
            2, 60, List.of("math"), "intermediate", "c1", 123456790L
        );

        // When: same transition twice
        analyzer.analyzeJourney(profile, event1);
        analyzer.analyzeJourney(profile, event2);

        // Then: frequency is 2
        Map<String, Map<String, Integer>> matrix = analyzer.getTransitionMatrix();
        assertThat(matrix.get("c1").get("c2")).isEqualTo(2);
    }

    @Test(description = "trackTransition creates new entry for first transition")
    public void trackTransition_newTransition_createsEntry() {
        // Given
        BehavioralProfile profile = createFreshProfile("user-new-trans");
        InterestScorer.RawJourneyEvent event = new InterestScorer.RawJourneyEvent(
            "j1", "user-new-trans", "s1", "c2", "video", "completed",
            1, 60, List.of("math"), "intermediate", "c1", 123456789L
        );

        // When: first A→B transition
        analyzer.analyzeJourney(profile, event);

        // Then: creates entry with frequency = 1
        Map<String, Map<String, Integer>> matrix = analyzer.getTransitionMatrix();
        assertThat(matrix.get("c1").get("c2")).isEqualTo(1);
    }

    @Test(description = "updateCommonPaths sorts transitions by frequency descending")
    public void updateCommonPaths_sortsByFrequency() {
        // Given
        BehavioralProfile profile = createFreshProfile("user-sort");
        InterestScorer.RawJourneyEvent event1 = new InterestScorer.RawJourneyEvent(
            "j1", "user-sort", "s1", "c2", "video", "completed",
            1, 60, List.of("math"), "intermediate", "c1", 123456789L
        );
        InterestScorer.RawJourneyEvent event2 = new InterestScorer.RawJourneyEvent(
            "j2", "user-sort", "s1", "c2", "video", "completed",
            2, 60, List.of("math"), "intermediate", "c1", 123456790L
        );
        InterestScorer.RawJourneyEvent event3 = new InterestScorer.RawJourneyEvent(
            "j3", "user-sort", "s1", "c3", "video", "completed",
            3, 60, List.of("math"), "intermediate", "c1", 123456791L
        );
        InterestScorer.RawJourneyEvent event4 = new InterestScorer.RawJourneyEvent(
            "j4", "user-sort", "s1", "c3", "video", "completed",
            4, 60, List.of("math"), "intermediate", "c1", 123456792L
        );
        InterestScorer.RawJourneyEvent event5 = new InterestScorer.RawJourneyEvent(
            "j5", "user-sort", "s1", "c3", "video", "completed",
            5, 60, List.of("math"), "intermediate", "c1", 123456793L
        );

        // When: c1→c2 (2 times), c1→c3 (3 times)
        analyzer.analyzeJourney(profile, event1);
        analyzer.analyzeJourney(profile, event2);
        analyzer.analyzeJourney(profile, event3);
        analyzer.analyzeJourney(profile, event4);
        analyzer.analyzeJourney(profile, event5);

        // Then: sorted by frequency (c3 before c2)
        List<BehavioralProfile.ContentTransition> paths = profile.getCommonPaths();
        assertThat(paths).hasSize(2);
        assertThat(paths.get(0).getToContent()).isEqualTo("c3");
        assertThat(paths.get(0).getFrequency()).isEqualTo(3);
        assertThat(paths.get(1).getToContent()).isEqualTo("c2");
        assertThat(paths.get(1).getFrequency()).isEqualTo(2);
    }

    @Test(description = "updateCommonPaths limits to top 20 paths")
    public void updateCommonPaths_limitsToTop20() {
        // Given: create 25 transitions from c1 to different content, each with frequency 2
        BehavioralProfile profile = createFreshProfile("user-limit");
        for (int i = 1; i <= 25; i++) {
            String toContent = "c" + i;
            // Create 2 events for each target to get frequency=2
            InterestScorer.RawJourneyEvent event1 = new InterestScorer.RawJourneyEvent(
                "j" + i + "a", "user-limit", "s1", toContent, "video", "completed",
                i, 60, List.of("math"), "intermediate", "c1", 123456789L + i
            );
            InterestScorer.RawJourneyEvent event2 = new InterestScorer.RawJourneyEvent(
                "j" + i + "b", "user-limit", "s1", toContent, "video", "completed",
                i, 60, List.of("math"), "intermediate", "c1", 123456814L + i
            );
            analyzer.analyzeJourney(profile, event1);
            analyzer.analyzeJourney(profile, event2);
        }

        // When: only 20 should be kept
        List<BehavioralProfile.ContentTransition> paths = profile.getCommonPaths();

        // Then: only 20 paths (limited by MAX_COMMON_PATHS)
        assertThat(paths).hasSize(20);
    }

    @Test(description = "updateCommonPaths filters by minimum threshold")
    public void updateCommonPaths_filtersByThreshold() {
        // Given
        BehavioralProfile profile = createFreshProfile("user-threshold");
        InterestScorer.RawJourneyEvent event = new InterestScorer.RawJourneyEvent(
            "j1", "user-threshold", "s1", "c2", "video", "completed",
            1, 60, List.of("math"), "intermediate", "c1", 123456789L
        );

        // When: only 1 transition (below threshold of 2)
        analyzer.analyzeJourney(profile, event);

        // Then: transition excluded (frequency < 2)
        assertThat(profile.getCommonPaths()).isEmpty();
    }

    // ========================================
    // Prediction Tests
    // ========================================

    @Test(description = "predictNext returns top 3 predictions by frequency")
    public void predictNext_withTransitions() {
        // Given: A→B×5, A→C×3, A→D×1
        BehavioralProfile profile = createFreshProfile("user-predict");
        for (int i = 0; i < 5; i++) {
            InterestScorer.RawJourneyEvent event = new InterestScorer.RawJourneyEvent(
                "j1", "user-predict", "s1", "B", "video", "completed",
                1, 60, List.of("math"), "intermediate", "A", 123456789L + i
            );
            analyzer.analyzeJourney(profile, event);
        }
        for (int i = 0; i < 3; i++) {
            InterestScorer.RawJourneyEvent event = new InterestScorer.RawJourneyEvent(
                "j2", "user-predict", "s1", "C", "video", "completed",
                2, 60, List.of("math"), "intermediate", "A", 123456800L + i
            );
            analyzer.analyzeJourney(profile, event);
        }
        InterestScorer.RawJourneyEvent event = new InterestScorer.RawJourneyEvent(
            "j3", "user-predict", "s1", "D", "video", "completed",
            3, 60, List.of("math"), "intermediate", "A", 123456803L
        );
        analyzer.analyzeJourney(profile, event);

        // When
        List<String> predictions = analyzer.predictNext("A");

        // Then: returns [B, C] (D is excluded because frequency < 2)
        assertThat(predictions).hasSize(2);
        assertThat(predictions).containsExactly("B", "C");
    }

    @Test(description = "predictNext returns empty list when no transitions exist")
    public void predictNext_noTransitions_emptyList() {
        // Given: no transitions from content
        BehavioralProfile profile = createFreshProfile("user-no-trans");

        // When
        List<String> predictions = analyzer.predictNext("unknown-content");

        // Then: returns empty list
        assertThat(predictions).isEmpty();
    }

    @Test(description = "predictNext returns empty list for null fromContent")
    public void predictNext_nullFromContent_emptyList() {
        // Given
        BehavioralProfile profile = createFreshProfile("user-null-from");

        // When
        List<String> predictions = analyzer.predictNext(null);

        // Then: returns empty list
        assertThat(predictions).isEmpty();
    }

    // ========================================
    // Topic Diversity Tests
    // ========================================

    @Test(description = "calculateUniqueTopicRatio returns 1.0 for diverse content")
    public void calculateUniqueTopicRatio_diverseContent() {
        // Given: 5 topics, 5 content
        BehavioralProfile profile = createFreshProfile("user-diverse");
        profile.setTotalContentConsumed(5);

        for (int i = 0; i < 5; i++) {
            String topic = "topic" + i;
            InterestScorer.RawJourneyEvent event = new InterestScorer.RawJourneyEvent(
                "j" + i, "user-diverse", "s1", "c" + i, "video", "completed",
                1, 60, List.of(topic), "intermediate", null, 123456789L + i
            );
            analyzer.analyzeJourney(profile, event);
        }

        // Then: ratio = 5 / 5 = 1.0
        assertThat(profile.getEngagement().getUniqueTopicRatio()).isEqualTo(1.0);
    }

    @Test(description = "calculateUniqueTopicRatio returns 0.2 for repetitive content")
    public void calculateUniqueTopicRatio_repetitiveContent() {
        // Given: 2 topics, 10 content
        BehavioralProfile profile = createFreshProfile("user-repetitive");
        profile.setTotalContentConsumed(10);

        for (int i = 0; i < 10; i++) {
            String topic = (i % 2 == 0) ? "math" : "algebra";
            InterestScorer.RawJourneyEvent event = new InterestScorer.RawJourneyEvent(
                "j" + i, "user-repetitive", "s1", "c" + i, "video", "completed",
                1, 60, List.of(topic), "intermediate", null, 123456789L + i
            );
            analyzer.analyzeJourney(profile, event);
        }

        // Then: ratio = 2 / 10 = 0.2
        assertThat(profile.getEngagement().getUniqueTopicRatio()).isEqualTo(0.2);
    }

    @Test(description = "calculateUniqueTopicRatio returns 0 when no content consumed")
    public void calculateUniqueTopicRatio_zeroContent_noDivisionByZero() {
        // Given: 0 content consumed
        BehavioralProfile profile = createFreshProfile("user-zero-content");
        profile.setTotalContentConsumed(0);

        InterestScorer.RawJourneyEvent event = new InterestScorer.RawJourneyEvent(
            "j1", "user-zero-content", "s1", "c1", "video", "completed",
            1, 60, List.of("math"), "intermediate", null, 123456789L
        );
        analyzer.analyzeJourney(profile, event);

        // Then: ratio stays at 0.0 because totalContentConsumed = 0, so the ratio is not updated
        // (The implementation checks totalContentConsumed > 0 before updating)
        assertThat(profile.getEngagement().getUniqueTopicRatio()).isEqualTo(0.0);
    }

    // ========================================
    // Immutability Tests
    // ========================================

    @Test(description = "getTransitionMatrix returns unmodifiable map")
    public void getTransitionMatrix_returnsImmutable() {
        // Given
        BehavioralProfile profile = createFreshProfile("user-immutable-matrix");
        InterestScorer.RawJourneyEvent event = new InterestScorer.RawJourneyEvent(
            "j1", "user-immutable-matrix", "s1", "c2", "video", "completed",
            1, 60, List.of("math"), "intermediate", "c1", 123456789L
        );
        analyzer.analyzeJourney(profile, event);

        // When
        Map<String, Map<String, Integer>> matrix = analyzer.getTransitionMatrix();

        // Then: trying to modify should throw UnsupportedOperationException
        assertThatThrownBy(() -> matrix.put("new", Map.of()))
            .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test(description = "getUserTopics returns unmodifiable set")
    public void getUserTopics_returnsImmutable() {
        // Given
        BehavioralProfile profile = createFreshProfile("user-immutable-topics");
        InterestScorer.RawJourneyEvent event = new InterestScorer.RawJourneyEvent(
            "j1", "user-immutable-topics", "s1", "c1", "video", "completed",
            1, 60, List.of("math"), "intermediate", null, 123456789L
        );
        analyzer.analyzeJourney(profile, event);

        // When
        var topics = analyzer.getUserTopics("user-immutable-topics");

        // Then: trying to modify should throw UnsupportedOperationException
        assertThatThrownBy(() -> topics.add("new-topic"))
            .isInstanceOf(UnsupportedOperationException.class);
    }

    // ========================================
    // Clear Tests
    // ========================================

    @Test(description = "clearTransitions clears all transition data")
    public void clearTransitions_clearsAll() {
        // Given
        BehavioralProfile profile = createFreshProfile("user-clear-trans");
        InterestScorer.RawJourneyEvent event = new InterestScorer.RawJourneyEvent(
            "j1", "user-clear-trans", "s1", "c2", "video", "completed",
            1, 60, List.of("math"), "intermediate", "c1", 123456789L
        );
        analyzer.analyzeJourney(profile, event);

        // When
        analyzer.clearTransitions();

        // Then: map is empty
        assertThat(analyzer.getTransitionMatrix()).isEmpty();
    }

    @Test(description = "clearUserTopics clears all user topic data")
    public void clearUserTopics_clearsAll() {
        // Given
        BehavioralProfile profile = createFreshProfile("user-clear-topics");
        InterestScorer.RawJourneyEvent event = new InterestScorer.RawJourneyEvent(
            "j1", "user-clear-topics", "s1", "c1", "video", "completed",
            1, 60, List.of("math"), "intermediate", null, 123456789L
        );
        analyzer.analyzeJourney(profile, event);

        // When
        analyzer.clearUserTopics();

        // Then: map is empty
        assertThat(analyzer.getUserTopics("user-clear-topics")).isEmpty();
    }

    // ========================================
    // Probability Tests
    // ========================================

    @Test(description = "transition probability is calculated correctly")
    public void transitionProbability_calculation() {
        // Given: A→B×3, A→C×7, total from A = 10
        BehavioralProfile profile = createFreshProfile("user-probability");
        for (int i = 0; i < 3; i++) {
            InterestScorer.RawJourneyEvent event = new InterestScorer.RawJourneyEvent(
                "j1", "user-probability", "s1", "B", "video", "completed",
                1, 60, List.of("math"), "intermediate", "A", 123456789L + i
            );
            analyzer.analyzeJourney(profile, event);
        }
        for (int i = 0; i < 7; i++) {
            InterestScorer.RawJourneyEvent event = new InterestScorer.RawJourneyEvent(
                "j2", "user-probability", "s1", "C", "video", "completed",
                2, 60, List.of("math"), "intermediate", "A", 123456800L + i
            );
            analyzer.analyzeJourney(profile, event);
        }

        // Then: P(B|A) = 3/10 = 0.3, P(C|A) = 7/10 = 0.7
        List<BehavioralProfile.ContentTransition> paths = profile.getCommonPaths();
        assertThat(paths).hasSize(2);

        var bTransition = paths.stream()
            .filter(p -> p.getToContent().equals("B"))
            .findFirst()
            .orElse(null);
        var cTransition = paths.stream()
            .filter(p -> p.getToContent().equals("C"))
            .findFirst()
            .orElse(null);

        assertThat(bTransition).isNotNull();
        assertThat(bTransition.getProbability()).isEqualTo(0.3);
        assertThat(cTransition).isNotNull();
        assertThat(cTransition.getProbability()).isEqualTo(0.7);
    }
}
