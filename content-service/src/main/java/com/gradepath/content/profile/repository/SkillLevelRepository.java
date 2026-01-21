package com.gradepath.content.profile.repository;

import com.gradepath.content.profile.model.SkillLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SkillLevelRepository extends JpaRepository<SkillLevel, UUID> {

    List<SkillLevel> findByUserId(UUID userId);

    Optional<SkillLevel> findByUserIdAndTopic(UUID userId, String topic);

    boolean existsByUserIdAndTopic(UUID userId, String topic);
}
