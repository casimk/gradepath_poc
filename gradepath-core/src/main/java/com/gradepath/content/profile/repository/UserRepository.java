package com.gradepath.content.profile.repository;

import com.gradepath.content.profile.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByExternalUserId(String externalUserId);

    boolean existsByExternalUserId(String externalUserId);
}
