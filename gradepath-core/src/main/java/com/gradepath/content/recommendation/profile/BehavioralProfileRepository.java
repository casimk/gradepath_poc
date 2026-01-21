package com.gradepath.content.recommendation.profile;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * JPA Repository for behavioral profiles
 * Note: Since userId comes as String from NestJS, we store it as String
 */
@Repository
public interface BehavioralProfileRepository extends JpaRepository<BehavioralProfileEntity, Long> {

    Optional<BehavioralProfileEntity> findFirstByUserIdOrderByTimestampDesc(String userId);

    void deleteByUserId(String userId);
}
