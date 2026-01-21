package com.gradepath.content.profile.repository;

import com.gradepath.content.profile.model.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findByToken(String token);

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    List<RefreshToken> findByUserId(UUID userId);

    @Query("SELECT rt FROM RefreshToken rt WHERE rt.userId = :userId AND rt.revokedAt IS NULL AND rt.expiresAt > :now")
    List<RefreshToken> findValidTokensByUserId(@Param("userId") UUID userId, @Param("now") Instant now);

    @Query("SELECT rt FROM RefreshToken rt WHERE rt.revokedAt IS NULL AND rt.expiresAt < :now")
    List<RefreshToken> findExpiredTokens(@Param("now") Instant now);

    void deleteByExpiresAtBefore(Instant expirationDate);
}
