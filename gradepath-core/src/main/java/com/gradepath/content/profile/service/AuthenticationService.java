package com.gradepath.content.profile.service;

import at.favre.lib.crypto.bcrypt.BCrypt;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gradepath.content.profile.model.*;
import com.gradepath.content.profile.repository.*;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

/**
 * Service for handling authentication operations including registration,
 * login, OAuth, token management, and audit logging.
 */
@Service
@Transactional
public class AuthenticationService {

    private static final Logger log = LoggerFactory.getLogger(AuthenticationService.class);
    private static final int BCRYPT_COST = 12;
    private static final Duration ACCESS_TOKEN_EXPIRATION = Duration.ofMinutes(15);
    private static final Duration REFRESH_TOKEN_EXPIRATION = Duration.ofDays(7);

    private final UserRepository userRepository;
    private final UserCredentialsRepository userCredentialsRepository;
    private final OAuthConnectionRepository oauthConnectionRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final AuthAuditLogRepository authAuditLogRepository;
    private final ObjectMapper objectMapper;

    @Value("${jwt.secret:gradepath-jwt-secret-key-change-in-production}")
    private String jwtSecret;

    private final SecretKey signingKey;

    public AuthenticationService(
            UserRepository userRepository,
            UserCredentialsRepository userCredentialsRepository,
            OAuthConnectionRepository oauthConnectionRepository,
            RefreshTokenRepository refreshTokenRepository,
            AuthAuditLogRepository authAuditLogRepository,
            ObjectMapper objectMapper) {
        this.userRepository = userRepository;
        this.userCredentialsRepository = userCredentialsRepository;
        this.oauthConnectionRepository = oauthConnectionRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.authAuditLogRepository = authAuditLogRepository;
        this.objectMapper = objectMapper;
        this.signingKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    // ==================== Registration ====================

    public AuthResult register(String username, String email, String password, String displayName) {
        log.info("Registering new user: {}", username);

        // Check if username or email already exists
        if (userCredentialsRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Username already exists");
        }
        if (userCredentialsRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already exists");
        }

        // Create user
        User user = User.builder()
                .externalUserId(UUID.randomUUID().toString())
                .email(email)
                .displayName(displayName != null ? displayName : username)
                .build();
        user = userRepository.save(user);

        // Hash password
        String passwordHash = BCrypt.withDefaults().hashToString(BCRYPT_COST, password.toCharArray());

        // Create user credentials
        UserCredentials credentials = UserCredentials.builder()
                .userId(user.getId())
                .username(username)
                .email(email)
                .passwordHash(passwordHash)
                .emailVerified(false)
                .build();
        userCredentialsRepository.save(credentials);

        // Log audit event
        logAuditEvent(user.getId(), AuthAuditLog.EventType.REGISTER, AuthAuditLog.AuthMethod.PASSWORD, true, null);

        // Generate tokens
        return generateTokens(user);
    }

    // ==================== Login ====================

    public AuthResult login(String identifier, String password) {
        log.info("Login attempt for: {}", identifier);

        // Find user by username or email
        Optional<UserCredentials> credentialsOpt = userCredentialsRepository.findByUsername(identifier);
        if (credentialsOpt.isEmpty()) {
            credentialsOpt = userCredentialsRepository.findByEmail(identifier);
        }

        if (credentialsOpt.isEmpty()) {
            logAuditEvent(null, AuthAuditLog.EventType.LOGIN, AuthAuditLog.AuthMethod.PASSWORD,
                    false, "Invalid credentials");
            throw new IllegalArgumentException("Invalid credentials");
        }

        UserCredentials credentials = credentialsOpt.get();

        // Verify password
        BCrypt.Result result = BCrypt.verifyer().verify(password.toCharArray(), credentials.getPasswordHash());
        if (!result.verified) {
            logAuditEvent(credentials.getUserId(), AuthAuditLog.EventType.LOGIN,
                    AuthAuditLog.AuthMethod.PASSWORD, false, "Invalid password");
            throw new IllegalArgumentException("Invalid credentials");
        }

        // Get user
        Optional<User> userOpt = userRepository.findById(credentials.getUserId());
        if (userOpt.isEmpty()) {
            throw new IllegalStateException("User not found");
        }

        User user = userOpt.get();

        // Log audit event
        logAuditEvent(user.getId(), AuthAuditLog.EventType.LOGIN, AuthAuditLog.AuthMethod.PASSWORD, true, null);

        return generateTokens(user);
    }

    // ==================== OAuth ====================

    public AuthResult handleOAuthLogin(String provider, String providerUserId, String email,
            String displayName, Map<String, Object> profileData) {
        log.info("OAuth login attempt: {} - {}", provider, email);

        // Check for existing OAuth connection
        Optional<OAuthConnection> connectionOpt = oauthConnectionRepository
                .findByProviderAndProviderUserId(provider, providerUserId);

        User user;
        if (connectionOpt.isPresent()) {
            // Existing user - update last used
            OAuthConnection connection = connectionOpt.get();
            connection.setLastUsedAt(Instant.now());
            oauthConnectionRepository.save(connection);

            Optional<User> userOpt = userRepository.findById(connection.getUserId());
            if (userOpt.isEmpty()) {
                throw new IllegalStateException("User not found for OAuth connection");
            }
            user = userOpt.get();
        } else {
            // New user - check if email already exists
            Optional<UserCredentials> existingCredentials = userCredentialsRepository.findByEmail(email);

            if (existingCredentials.isPresent()) {
                // Link OAuth to existing account
                user = userRepository.findById(existingCredentials.get().getUserId())
                        .orElseThrow(() -> new IllegalStateException("User not found"));
            } else {
                // Create new user
                user = User.builder()
                        .externalUserId(UUID.randomUUID().toString())
                        .email(email)
                        .displayName(displayName)
                        .build();
                user = userRepository.save(user);
            }

            // Create OAuth connection
            try {
                String profileJson = objectMapper.writeValueAsString(profileData);
                OAuthConnection connection = OAuthConnection.builder()
                        .userId(user.getId())
                        .provider(provider)
                        .providerUserId(providerUserId)
                        .providerEmail(email)
                        .profileData(profileJson)
                        .linkedAt(Instant.now())
                        .lastUsedAt(Instant.now())
                        .build();
                oauthConnectionRepository.save(connection);
            } catch (Exception e) {
                log.error("Failed to serialize OAuth profile data", e);
            }
        }

        // Log audit event
        logAuditEvent(user.getId(), AuthAuditLog.EventType.OAUTH_LOGIN,
                AuthAuditLog.AuthMethod.valueOf(provider.toUpperCase()), true, null);

        return generateTokens(user);
    }

    // ==================== Token Refresh ====================

    public AuthResult refreshToken(UUID userId, String oldRefreshToken) {
        log.info("Refreshing token for user: {}", userId);

        // Find and validate refresh token
        Optional<RefreshToken> tokenOpt = refreshTokenRepository.findByToken(oldRefreshToken);
        if (tokenOpt.isEmpty()) {
            throw new IllegalArgumentException("Invalid refresh token");
        }

        RefreshToken refreshToken = tokenOpt.get();
        if (!refreshToken.isValid()) {
            throw new IllegalArgumentException("Refresh token is expired or revoked");
        }

        // Get user
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            throw new IllegalStateException("User not found");
        }

        User user = userOpt.get();

        // Revoke old refresh token
        refreshToken.setRevokedAt(Instant.now());
        refreshToken.setRevokedReason("token_refresh");
        refreshTokenRepository.save(refreshToken);

        // Log audit event
        logAuditEvent(user.getId(), AuthAuditLog.EventType.TOKEN_REFRESH,
                AuthAuditLog.AuthMethod.TOKEN_REFRESH, true, null);

        return generateTokens(user);
    }

    // ==================== Logout ====================

    public void logout(UUID userId, String refreshToken) {
        log.info("Logging out user: {}", userId);

        // Revoke refresh token
        Optional<RefreshToken> tokenOpt = refreshTokenRepository.findByToken(refreshToken);
        if (tokenOpt.isPresent()) {
            RefreshToken token = tokenOpt.get();
            token.setRevokedAt(Instant.now());
            token.setRevokedReason("logout");
            refreshTokenRepository.save(token);
        }

        // Log audit event
        logAuditEvent(userId, AuthAuditLog.EventType.LOGOUT, null, true, null);
    }

    // ==================== Token Generation ====================

    private AuthResult generateTokens(User user) {
        Instant now = Instant.now();
        Instant accessExpiry = now.plus(ACCESS_TOKEN_EXPIRATION);
        Instant refreshExpiry = now.plus(REFRESH_TOKEN_EXPIRATION);

        // Generate access token (JWT)
        String accessToken = Jwts.builder()
                .subject(user.getId().toString())
                .claim("email", user.getEmail())
                .claim("username", getUsernameForUser(user.getId()))
                .claim("displayName", user.getDisplayName())
                .issuedAt(Date.from(now))
                .expiration(Date.from(accessExpiry))
                .signWith(signingKey)
                .compact();

        // Generate refresh token
        String refreshTokenValue = UUID.randomUUID().toString();
        String tokenHash = hashToken(refreshTokenValue);

        // Revoke old refresh tokens for user
        refreshTokenRepository.findValidTokensByUserId(user.getId(), Instant.now())
                .forEach(oldToken -> {
                    oldToken.setRevokedAt(Instant.now());
                    oldToken.setRevokedReason("token_rotation");
                    refreshTokenRepository.save(oldToken);
                });

        // Save new refresh token
        RefreshToken refreshToken = RefreshToken.builder()
                .userId(user.getId())
                .token(refreshTokenValue)
                .tokenHash(tokenHash)
                .expiresAt(refreshExpiry)
                .build();
        refreshTokenRepository.save(refreshToken);

        return new AuthResult(
                user.getId(),
                user.getEmail(),
                getUsernameForUser(user.getId()),
                user.getDisplayName(),
                accessToken,
                refreshTokenValue,
                (int) ACCESS_TOKEN_EXPIRATION.getSeconds()
        );
    }

    // ==================== Helper Methods ====================

    private String hashToken(String token) {
        return BCrypt.withDefaults().hashToString(BCRYPT_COST, token.toCharArray());
    }

    private String getUsernameForUser(UUID userId) {
        return userCredentialsRepository.findByUserId(userId)
                .map(UserCredentials::getUsername)
                .orElse(null);
    }

    private void logAuditEvent(UUID userId, AuthAuditLog.EventType eventType,
            AuthAuditLog.AuthMethod authMethod, boolean success, String failureReason) {
        try {
            AuthAuditLog auditLog = AuthAuditLog.builder()
                    .userId(userId)
                    .eventType(eventType)
                    .authMethod(authMethod)
                    .success(success)
                    .failureReason(failureReason)
                    .occurredAt(Instant.now())
                    .build();
            authAuditLogRepository.save(auditLog);
        } catch (Exception e) {
            log.error("Failed to log audit event", e);
        }
    }

    // ==================== DTOs ====================

    public record AuthResult(
            UUID userId,
            String email,
            String username,
            String displayName,
            String accessToken,
            String refreshToken,
            int expiresIn
    ) {}
}
