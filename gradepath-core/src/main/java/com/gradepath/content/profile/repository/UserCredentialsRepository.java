package com.gradepath.content.profile.repository;

import com.gradepath.content.profile.model.UserCredentials;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserCredentialsRepository extends JpaRepository<UserCredentials, UUID> {

    Optional<UserCredentials> findByUsername(String username);

    Optional<UserCredentials> findByEmail(String email);

    Optional<UserCredentials> findByUserId(UUID userId);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);
}
