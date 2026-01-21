package com.gradepath.content.recommendation.repository;

import com.gradepath.content.recommendation.model.Recommendation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RecommendationRepository extends JpaRepository<Recommendation, UUID> {

    List<Recommendation> findByUserIdOrderByScoreDesc(UUID userId);

    @Query("""
        SELECT r FROM Recommendation r
        WHERE r.user.id = :userId
        AND r.content.id NOT IN (
            SELECT ci.contentId FROM ContentInteraction ci
            WHERE ci.userId = :userId
            AND ci.interactionType IN ('VIEWED', 'COMPLETED', 'SKIPPED')
        )
        ORDER BY r.score DESC
    """)
    List<Recommendation> findPendingRecommendations(@Param("userId") UUID userId);

    @Query("""
        SELECT r FROM Recommendation r
        WHERE r.user.id = :userId
        ORDER BY r.score DESC
        LIMIT 1
    """)
    Optional<Recommendation> findTopByUserIdOrderByScoreDesc(@Param("userId") UUID userId);

    @Query("""
        SELECT COUNT(r) > 0 FROM Recommendation r
        WHERE r.user.id = :userId
        AND r.content.id = :contentId
        AND r.createdAt > :since
    """)
    boolean existsRecentRecommendation(@Param("userId") UUID userId,
                                       @Param("contentId") String contentId,
                                       @Param("since") java.time.Instant since);

    void deleteByUserId(UUID userId);
}
