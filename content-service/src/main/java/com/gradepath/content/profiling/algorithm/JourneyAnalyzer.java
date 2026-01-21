package com.gradepath.content.profiling.algorithm;

import com.gradepath.content.recommendation.profile.BehavioralProfile;
import com.gradepath.content.recommendation.profile.BehavioralProfile.ContentTransition;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Analyzes content journey patterns using Markov chains.
 * Ported from TypeScript JourneyAnalyzer.
 *
 * Features:
 * - Tracks content transitions (Markov chain)
 * - Calculates transition probabilities
 * - Identifies common paths
 * - Tracks topic diversity per user
 * - Predicts next likely content
 */
@Component
@Slf4j
public class JourneyAnalyzer {

    // Track transitions for Markov chain: fromContent -> (toContent -> frequency)
    private final Map<String, Map<String, Integer>> transitions = new ConcurrentHashMap<>();

    // Track unique topics per user for diversity calculation
    private final Map<String, Set<String>> userTopics = new ConcurrentHashMap<>();

    private static final int MIN_FREQUENCY_THRESHOLD = 2;
    private static final int MAX_COMMON_PATHS = 20;
    private static final int TOP_NEXT_PREDICTIONS = 3;

    /**
     * Analyze a journey event and update profile
     */
    public void analyzeJourney(BehavioralProfile profile, InterestScorer.RawJourneyEvent event) {
        String userId = event.userId();
        String contentId = event.contentId();
        String previousContentId = event.previousContentId();
        List<String> topicTags = event.topicTags();

        if (topicTags == null) {
            topicTags = List.of();
        }

        // Track topic diversity
        userTopics.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet())
                  .addAll(topicTags);

        // Update unique topic ratio in engagement
        Set<String> userTopicSet = userTopics.get(userId);
        if (profile.getTotalContentConsumed() > 0) {
            double uniqueTopicRatio = (double) userTopicSet.size() /
                                     Math.max(profile.getTotalContentConsumed(), 1);

            var engagement = profile.getEngagement();
            if (engagement != null) {
                engagement.setUniqueTopicRatio(uniqueTopicRatio);
            }
        }

        // Track content transitions for path analysis
        if (previousContentId != null && !previousContentId.isEmpty()) {
            trackTransition(previousContentId, contentId);
            updateCommonPaths(profile);
        }
    }

    /**
     * Track a content transition for Markov chain analysis
     */
    private void trackTransition(String from, String to) {
        transitions.computeIfAbsent(from, k -> new ConcurrentHashMap<>())
                  .merge(to, 1, Integer::sum);
    }

    /**
     * Update common paths in profile based on transition frequencies
     */
    private void updateCommonPaths(BehavioralProfile profile) {
        List<ContentTransition> result = new ArrayList<>();

        transitions.forEach((fromContent, toMap) -> {
            int totalFrom = toMap.values().stream().mapToInt(Integer::intValue).sum();

            toMap.forEach((toContent, frequency) -> {
                if (frequency >= MIN_FREQUENCY_THRESHOLD) {
                    result.add(ContentTransition.builder()
                        .fromContent(fromContent)
                        .toContent(toContent)
                        .frequency(frequency)
                        .probability((double) frequency / totalFrom)
                        .build());
                }
            });
        });

        // Sort by frequency and keep top paths
        List<ContentTransition> sorted = result.stream()
            .sorted((a, b) -> Integer.compare(b.getFrequency(), a.getFrequency()))
            .limit(MAX_COMMON_PATHS)
            .collect(Collectors.toList());

        profile.setCommonPaths(sorted);
    }

    /**
     * Predict next likely content items based on current content
     */
    public List<String> predictNext(String currentContent) {
        Map<String, Integer> fromMap = transitions.get(currentContent);
        if (fromMap == null || fromMap.isEmpty()) {
            return List.of();
        }

        return fromMap.entrySet().stream()
            .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
            .limit(TOP_NEXT_PREDICTIONS)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
    }

    /**
     * Get the transition matrix (for debugging/analysis)
     */
    public Map<String, Map<String, Integer>> getTransitionMatrix() {
        return Collections.unmodifiableMap(transitions);
    }

    /**
     * Get topics tracked for a user
     */
    public Set<String> getUserTopics(String userId) {
        return Collections.unmodifiableSet(userTopics.getOrDefault(userId, Set.of()));
    }

    /**
     * Clear transition data (useful for testing)
     */
    public void clearTransitions() {
        transitions.clear();
    }

    /**
     * Clear user topics (useful for testing)
     */
    public void clearUserTopics() {
        userTopics.clear();
    }
}
