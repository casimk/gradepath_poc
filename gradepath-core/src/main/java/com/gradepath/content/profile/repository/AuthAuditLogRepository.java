package com.gradepath.content.profile.repository;

import com.gradepath.content.profile.model.AuthAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface AuthAuditLogRepository extends JpaRepository<AuthAuditLog, UUID> {

    List<AuthAuditLog> findByUserIdOrderByOccurredAtDesc(UUID userId);

    List<AuthAuditLog> findByEventTypeOrderByOccurredAtDesc(AuthAuditLog.EventType eventType);

    List<AuthAuditLog> findBySuccessFalseAndOccurredAtAfter(Instant after);

    List<AuthAuditLog> findTop20ByUserIdOrderByOccurredAtDesc(UUID userId);
}
