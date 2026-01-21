package com.gradepath.content.recommendation.controller;

import com.gradepath.content.recommendation.service.RecommendationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/recommendations")
@Tag(name = "recommendations", description = "Content recommendation API")
public class RecommendationController {

    private final RecommendationService recommendationService;

    public RecommendationController(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    @GetMapping("/next")
    @Operation(summary = "Get next recommended content for a user")
    public ResponseEntity<RecommendationService.ContentResponse> getNextContent(
            @Parameter(description = "User ID") @RequestParam UUID userId) {
        try {
            RecommendationService.ContentResponse content = recommendationService.getNextContent(userId);
            return ResponseEntity.ok(content);
        } catch (RecommendationService.NoContentAvailableException e) {
            return ResponseEntity.noContent().build();
        }
    }

    @GetMapping
    @Operation(summary = "Get list of recommendations for a user")
    public ResponseEntity<?> getRecommendations(
            @Parameter(description = "User ID") @RequestParam UUID userId,
            @Parameter(description = "Maximum number of recommendations") @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(recommendationService.getRecommendations(userId, limit));
    }

    @PostMapping("/{contentId}/feedback")
    @Operation(summary = "Record feedback on a recommendation")
    public ResponseEntity<Void> recordFeedback(
            @Parameter(description = "Content ID") @PathVariable String contentId,
            @RequestBody @Valid FeedbackRequest request) {
        recommendationService.recordFeedback(request.userId(), contentId, request.feedback());
        return ResponseEntity.accepted().build();
    }

    public record FeedbackRequest(
        UUID userId,
        RecommendationService.FeedbackType feedback
    ) {}
}
