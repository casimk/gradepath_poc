package com.gradepath.content.profile.repository;

import com.gradepath.content.profile.model.OAuthConnection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OAuthConnectionRepository extends JpaRepository<OAuthConnection, UUID> {

    Optional<OAuthConnection> findByProviderAndProviderUserId(String provider, String providerUserId);

    List<OAuthConnection> findByUserId(UUID userId);

    List<OAuthConnection> findByProvider(String provider);

    boolean existsByProviderAndProviderUserId(String provider, String providerUserId);
}
