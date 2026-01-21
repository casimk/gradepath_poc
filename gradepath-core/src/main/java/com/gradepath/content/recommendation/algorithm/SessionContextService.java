package com.gradepath.content.recommendation.algorithm;

import com.gradepath.content.content.model.Content;
import com.gradepath.content.recommendation.profile.BehavioralProfile;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

/**
 * Session context scoring based on time-of-day and user patterns
 * TikTok-style: adapts recommendations based on current session context
 */
@Service
@Slf4j
public class SessionContextService {

    /**
     * Calculate session context score for content
     * Returns 0.0 to 1.0 based on how well content fits current session
     */
    public double calculateSessionScore(Content content, Optional<BehavioralProfile> profile) {
        LocalDateTime now = LocalDateTime.now();
        int currentHour = now.getHour();
        DayOfWeek currentDay = now.getDayOfWeek();

        double timeScore = calculateTimeScore(currentHour, currentDay, profile);
        double energyScore = calculateEnergyScore(currentHour, content.getEstimatedDurationMinutes());
        double patternScore = calculatePatternScore(currentHour, currentDay, profile);

        // Weighted combination
        return (timeScore * 0.4) + (energyScore * 0.3) + (patternScore * 0.3);
    }

    /**
     * Time-of-day scoring - matches content to optimal viewing times
     */
    private double calculateTimeScore(int hour, DayOfWeek day, Optional<BehavioralProfile> profile) {
        // Default time preferences if no profile
        if (profile.isEmpty()) {
            return getDefaultTimeScore(hour);
        }

        BehavioralProfile p = profile.get();
        return p.getPeakWindows().stream()
            .filter(w -> w.getDay().equalsIgnoreCase(day.name()) || w.getDay().equals("all"))
            .filter(w -> Math.abs(w.getHour() - hour) <= 1) // Within 1 hour
            .findFirst()
            .map(PeakWindow -> PeakWindow.getScore())
            .orElse(getDefaultTimeScore(hour));
    }

    /**
     * Default time score when no behavioral profile exists
     * Morning (6-12): 0.7, Afternoon (12-18): 0.9, Evening (18-22): 1.0, Night (22-6): 0.5
     */
    private double getDefaultTimeScore(int hour) {
        if (hour >= 6 && hour < 12) return 0.7;
        if (hour >= 12 && hour < 18) return 0.9;
        if (hour >= 18 && hour < 22) return 1.0;
        return 0.5;
    }

    /**
     * Energy scoring - shorter content when energy is low (late night)
     * Longer content when energy is high (morning/afternoon)
     */
    private double calculateEnergyScore(int hour, Integer contentDurationMinutes) {
        if (contentDurationMinutes == null) return 0.8;

        // Energy level based on time of day
        double energyLevel = getEnergyLevel(hour);

        // Match content length to energy
        // Short content (<= 5 min) is good for low energy
        // Long content (> 20 min) is good for high energy
        double optimalDurationMinutes = energyLevel * 30; // 0-30 minutes based on energy

        double durationDifference = Math.abs(contentDurationMinutes - optimalDurationMinutes);
        return Math.max(0.2, 1.0 - (durationDifference / 30.0));
    }

    /**
     * Get user energy level (0.0 to 1.0) based on time of day
     */
    private double getEnergyLevel(int hour) {
        if (hour >= 6 && hour < 12) return 0.9; // Morning - high energy
        if (hour >= 12 && hour < 18) return 0.8; // Afternoon - good energy
        if (hour >= 18 && hour < 22) return 0.6; // Evening - moderate energy
        return 0.3; // Night - low energy
    }

    /**
     * Pattern scoring - matches user's historical session patterns
     */
    private double calculatePatternScore(int hour, DayOfWeek day, Optional<BehavioralProfile> profile) {
        if (profile.isEmpty()) return 0.5;

        BehavioralProfile p = profile.get();
        if (p.getEngagement() == null) return 0.5;

        // Check if user typically engages at this time
        boolean isPeakTime = p.getPeakWindows().stream()
            .anyMatch(w -> w.getDay().equalsIgnoreCase(day.name()) && Math.abs(w.getHour() - hour) <= 1);

        // Bonus if this is the user's peak time
        return isPeakTime ? 1.0 : 0.6;
    }

    /**
     * Get session context reason for explanation
     */
    public String getSessionReason(double score, LocalDateTime now) {
        int hour = now.getHour();
        if (hour >= 6 && hour < 12) return "Good for morning learning";
        if (hour >= 12 && hour < 18) return "Great for afternoon sessions";
        if (hour >= 18 && hour < 22) return "Perfect for evening learning";
        return "Quick content for late night";
    }
}
