package com.gradepath.content.recommendation.algorithm;

import com.gradepath.content.content.model.Content;
import com.gradepath.content.recommendation.profile.BehavioralProfile;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Multi-Armed Bandit strategy for Explore vs Exploit
 * TikTok-style: balances personalization with discovery
 */
@Service
@Slf4j
public class BanditStrategyService {

    private static final double DEFAULT_EPSILON = 0.2; // 20% exploration
    private final Random random = new Random();

    /**
     * Apply epsilon-greedy strategy to content ranking
     * With probability epsilon, return random content (explore)
     * Otherwise, return top scored content (exploit)
     */
    public List<Content> applyEpsilonGreedy(
            List<Content> candidates,
            Map<String, Double> scores,
            double epsilon) {

        // Separate into explored and unexplored
        List<ScoredContent> scored = candidates.stream()
            .map(c -> new ScoredContent(c, scores.getOrDefault(c.getId(), 0.5)))
            .sorted((a, b) -> Double.compare(b.score(), a.score()))
            .toList();

        // Epsilon-greedy: explore with probability epsilon
        if (random.nextDouble() < epsilon) {
            return explore(scored);
        } else {
            return exploit(scored);
        }
    }

    /**
     * Apply bandit strategy with behavioral context
     * Adjusts epsilon based on user engagement patterns
     */
    public List<Content> applyWithContext(
            List<Content> candidates,
            Map<String, Double> scores,
            Optional<BehavioralProfile> profile) {

        double epsilon = calculateEpsilon(profile);
        log.debug("Using epsilon: {} for recommendation", epsilon);

        return applyEpsilonGreedy(candidates, scores, epsilon);
    }

    /**
     * Calculate epsilon based on user profile
     * Explorers get higher epsilon, specialists get lower
     */
    private double calculateEpsilon(Optional<BehavioralProfile> profile) {
        if (profile.isEmpty()) {
            return DEFAULT_EPSILON;
        }

        BehavioralProfile p = profile.get();
        if (p.getEngagement() == null) {
            return DEFAULT_EPSILON;
        }

        return switch (p.getEngagement().getClassification()) {
            case "explorer" -> 0.4;  // More exploration
            case "specialist" -> 0.1; // Less exploration
            case "binge_consumer" -> 0.3; // Moderate exploration
            case "casual_browser" -> 0.35; // More exploration
            case "deep_learner" -> 0.15; // Less exploration
            default -> DEFAULT_EPSILON;
        };
    }

    /**
     * Explore: return content with lower scores or random
     * Introduces variety and discovery
     */
    private List<Content> explore(List<ScoredContent> scored) {
        // Sample from lower half of scored content
        int midPoint = scored.size() / 2;
        if (midPoint == 0) {
            return scored.stream().map(ScoredContent::content).toList();
        }

        List<ScoredContent> lowerHalf = scored.subList(midPoint, scored.size());
        Collections.shuffle(lowerHalf);

        // Return 70% from lower half, 30% from top
        List<Content> result = new ArrayList<>();
        int exploreCount = Math.min(scored.size(), lowerHalf.size());
        result.addAll(lowerHalf.stream().limit(exploreCount).map(ScoredContent::content).toList());
        result.addAll(scored.stream().limit(scored.size() / 3).map(ScoredContent::content).toList());

        return result.stream().distinct().toList();
    }

    /**
     * Exploit: return top scored content
     * Maximizes immediate user satisfaction
     */
    private List<Content> exploit(List<ScoredContent> scored) {
        return scored.stream()
            .map(ScoredContent::content)
            .toList();
    }

    /**
     * Upper Confidence Bound (UCB) algorithm for more sophisticated bandit
     * Balances estimated score with uncertainty (exploration bonus)
     */
    public Map<String, Double> applyUCB(
            Map<String, Double> estimatedScores,
            Map<String, Integer> selectionCounts,
            int totalCount,
            double c) { // Exploration parameter

        Map<String, Double> ucbScores = new HashMap<>();

        for (Map.Entry<String, Double> entry : estimatedScores.entrySet()) {
            String contentId = entry.getKey();
            double score = entry.getValue();
            int count = selectionCounts.getOrDefault(contentId, 1);

            // UCB = score + c * sqrt(ln(total) / count)
            double explorationBonus = c * Math.sqrt(Math.log(totalCount + 1) / count);
            ucbScores.put(contentId, score + explorationBonus);
        }

        return ucbScores;
    }

    /**
     * Thompson Sampling for Bayesian bandit
     * Uses Beta distribution approximation for exploration
     */
    public String sampleThompsonompson(Map<String, BetaParams> betaParams) {
        String bestContent = null;
        double bestSample = Double.NEGATIVE_INFINITY;

        for (Map.Entry<String, BetaParams> entry : betaParams.entrySet()) {
            BetaParams params = entry.getValue();
            // Approximation of Beta distribution using Gamma
            // In production, use a proper statistical library
            double alpha = params.alpha();
            double beta = params.beta();
            double sample = gammaSample(alpha) / (gammaSample(alpha) + gammaSample(beta));

            if (sample > bestSample) {
                bestSample = sample;
                bestContent = entry.getKey();
            }
        }

        return bestContent;
    }

    /**
     * Approximation of Gamma distribution for Thompson sampling
     * Uses Marsaglia and Tsang's method
     */
    private double gammaSample(double alpha) {
        if (alpha < 1) {
            return gammaSample(alpha + 1) * Math.pow(random.nextDouble(), 1.0 / alpha);
        }

        // Marsaglia and Tsang's method for alpha >= 1
        double d = alpha - 0.333333333;
        double c = 1.0 / (3.0 * Math.sqrt(d));

        while (true) {
            double x = random.nextGaussian();
            double v = (1.0 + c * x) * (1.0 + c * x) * (1.0 + c * x);
            if (v <= 0) continue;

            double u = random.nextDouble();
            if (u < 1.0 - 0.0331 * (x * x) * (x * x)) {
                return d * v * Math.pow(u, 1.0 / alpha);
            }
            if (Math.log(u) < 0.5 * x * x + d * (1.0 - v + Math.log(v))) {
                return d * v * Math.pow(u, 1.0 / alpha);
            }
        }
    }

    /**
     * Get exploration reason for explanation
     */
    public String getExplorationReason() {
        return "Exploring new content to discover your interests";
    }

    /**
     * Get exploitation reason for explanation
     */
    public String getExploitationReason() {
        return "Personalized based on your learning patterns";
    }

    private record ScoredContent(Content content, double score) {}

    public record BetaParams(double alpha, double beta) {}
}
