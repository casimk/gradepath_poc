package com.gradepath.content.analytics.repository;

import com.gradepath.content.analytics.model.ContentInteraction;
import com.gradepath.content.analytics.model.InteractionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface ContentInteractionRepository extends JpaRepository<ContentInteraction, UUID> {

    List<ContentInteraction> findByUserIdOrderByTimestampDesc(UUID userId);

    List<ContentInteraction> findByContentIdOrderByTimestampDesc(String contentId);

    List<ContentInteraction> findByUserIdAndInteractionTypeOrderByTimestampDesc(
        UUID userId,
        InteractionType interactionType
    );

    @Query("""
        SELECT COUNT(ci) FROM ContentInteraction ci
        WHERE ci.userId = :userId
        AND ci.timestamp >= :since
    """)
    long countInteractionsSince(@Param("userId") UUID userId, @Param("since") Instant since);

    @Query("""
        SELECT COUNT(ci) FROM ContentInteraction ci
        WHERE ci.userId = :userId
        AND ci.interactionType = :type
        AND ci.timestamp >= :since
    """)
    long countByTypeSince(
        @Param("userId") UUID userId,
        @Param("type") InteractionType type,
        @Param("since") Instant since
    );

    @Query("""
        SELECT DISTINCT ci.contentId FROM ContentInteraction ci
        WHERE ci.userId = :userId
        AND ci.interactionType IN :types
    """)
    List<String> findViewedContentIds(@Param("userId") UUID userId, @Param("types") List<InteractionType> types);
}
