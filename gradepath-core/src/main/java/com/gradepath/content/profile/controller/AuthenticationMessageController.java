package com.gradepath.content.profile.controller;

import com.gradepath.content.profile.service.AuthenticationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

import java.util.Map;

/**
 * Message controller for handling authentication requests from NestJS gateway.
 * Uses Spring Messaging for microservice communication via TCP.
 */
@Controller
@Profile("!test") // Disable in tests to avoid port conflicts
public class AuthenticationMessageController {

    private static final Logger log = LoggerFactory.getLogger(AuthenticationMessageController.class);

    private final AuthenticationService authenticationService;

    @Autowired
    public AuthenticationMessageController(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    /**
     * Handle user registration request.
     * Expected payload: {username, email, password, displayName?}
     */
    @MessageMapping("auth.register")
    public Map<String, Object> register(@Payload Map<String, Object> payload) {
        log.info("Received registration request: {}", payload);

        try {
            String username = (String) payload.get("username");
            String email = (String) payload.get("email");
            String password = (String) payload.get("password");
            String displayName = (String) payload.get("displayName");

            AuthenticationService.AuthResult result = authenticationService.register(
                    username, email, password, displayName);

            return Map.of(
                    "user", Map.of(
                            "id", result.userId(),
                            "email", result.email(),
                            "username", result.username(),
                            "displayName", result.displayName()
                    ),
                    "tokens", Map.of(
                            "accessToken", result.accessToken(),
                            "refreshToken", result.refreshToken(),
                            "expiresIn", result.expiresIn()
                    )
            );
        } catch (Exception e) {
            log.error("Registration failed", e);
            throw new RuntimeException(e.getMessage());
        }
    }

    /**
     * Handle user login request.
     * Expected payload: {username?, email?, password}
     */
    @MessageMapping("auth.login")
    public Map<String, Object> login(@Payload Map<String, Object> payload) {
        log.info("Received login request: {}", payload);

        try {
            String identifier = (String) payload.getOrDefault("username", payload.get("email"));
            String password = (String) payload.get("password");

            AuthenticationService.AuthResult result = authenticationService.login(
                    identifier, password);

            return Map.of(
                    "user", Map.of(
                            "id", result.userId(),
                            "email", result.email(),
                            "username", result.username(),
                            "displayName", result.displayName()
                    ),
                    "tokens", Map.of(
                            "accessToken", result.accessToken(),
                            "refreshToken", result.refreshToken(),
                            "expiresIn", result.expiresIn()
                    )
            );
        } catch (Exception e) {
            log.error("Login failed", e);
            throw new RuntimeException(e.getMessage());
        }
    }

    /**
     * Handle token refresh request.
     * Expected payload: {refreshToken}
     */
    @MessageMapping("auth.refresh")
    public Map<String, Object> refresh(@Payload Map<String, Object> payload) {
        log.info("Received token refresh request");

        try {
            String refreshToken = (String) payload.get("refreshToken");
            String userIdStr = (String) payload.get("userId");

            if (userIdStr == null) {
                throw new IllegalArgumentException("userId is required");
            }

            java.util.UUID userId = java.util.UUID.fromString(userIdStr);
            AuthenticationService.AuthResult result = authenticationService.refreshToken(
                    userId, refreshToken);

            return Map.of(
                    "user", Map.of(
                            "id", result.userId(),
                            "email", result.email(),
                            "username", result.username(),
                            "displayName", result.displayName()
                    ),
                    "tokens", Map.of(
                            "accessToken", result.accessToken(),
                            "refreshToken", result.refreshToken(),
                            "expiresIn", result.expiresIn()
                    )
            );
        } catch (Exception e) {
            log.error("Token refresh failed", e);
            throw new RuntimeException(e.getMessage());
        }
    }

    /**
     * Handle logout request.
     * Expected payload: {userId, refreshToken}
     */
    @MessageMapping("auth.logout")
    public void logout(@Payload Map<String, Object> payload) {
        log.info("Received logout request");

        try {
            String userIdStr = (String) payload.get("userId");
            String refreshToken = (String) payload.get("refreshToken");

            if (userIdStr == null) {
                throw new IllegalArgumentException("userId is required");
            }

            java.util.UUID userId = java.util.UUID.fromString(userIdStr);
            authenticationService.logout(userId, refreshToken);
        } catch (Exception e) {
            log.error("Logout failed", e);
            throw new RuntimeException(e.getMessage());
        }
    }

    /**
     * Handle user validation request.
     * Expected payload: {userId}
     */
    @MessageMapping("auth.validate")
    public Map<String, Object> validate(@Payload Map<String, Object> payload) {
        log.info("Received user validation request: {}", payload);

        try {
            String userIdStr = (String) payload.get("userId");

            if (userIdStr == null) {
                throw new IllegalArgumentException("userId is required");
            }

            // Return user data based on userId
            // This would typically query the user repository
            return Map.of(
                    "id", userIdStr,
                    "email", payload.get("email"),
                    "username", payload.get("username"),
                    "displayName", payload.get("displayName")
            );
        } catch (Exception e) {
            log.error("User validation failed", e);
            throw new RuntimeException(e.getMessage());
        }
    }

    /**
     * Handle OAuth login request.
     * Expected payload: {provider, providerUserId, email, displayName?, profileData?}
     */
    @MessageMapping("auth.oauth")
    public Map<String, Object> oauth(@Payload Map<String, Object> payload) {
        log.info("Received OAuth login request: {}", payload);

        try {
            String provider = (String) payload.get("provider");
            String providerUserId = (String) payload.get("providerUserId");
            String email = (String) payload.get("email");
            String displayName = (String) payload.getOrDefault("displayName", email);
            @SuppressWarnings("unchecked")
            Map<String, Object> profileData = (Map<String, Object>) payload.getOrDefault("profileData", Map.of());

            AuthenticationService.AuthResult result = authenticationService.handleOAuthLogin(
                    provider, providerUserId, email, displayName, profileData);

            return Map.of(
                    "user", Map.of(
                            "id", result.userId(),
                            "email", result.email(),
                            "username", result.username(),
                            "displayName", result.displayName()
                    ),
                    "tokens", Map.of(
                            "accessToken", result.accessToken(),
                            "refreshToken", result.refreshToken(),
                            "expiresIn", result.expiresIn()
                    )
            );
        } catch (Exception e) {
            log.error("OAuth login failed", e);
            throw new RuntimeException(e.getMessage());
        }
    }
}
