package com.gradepath.content.profile.controller;

import com.gradepath.content.profile.service.AuthenticationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * REST API controller for authentication requests from NestJS gateway.
 * Provides REST endpoints for user authentication operations.
 */
@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*", allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.OPTIONS})
public class AuthRestController {

    private static final Logger log = LoggerFactory.getLogger(AuthRestController.class);

    private final AuthenticationService authenticationService;

    @Autowired
    public AuthRestController(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    /**
     * User registration endpoint.
     * POST /api/auth/register
     * Body: {username, email, password, displayName?}
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, Object> payload) {
        log.info("Received registration request: {}", payload);

        try {
            String username = (String) payload.get("username");
            String email = (String) payload.get("email");
            String password = (String) payload.get("password");
            String displayName = (String) payload.get("displayName");

            AuthenticationService.AuthResult result = authenticationService.register(
                    username, email, password, displayName);

            Map<String, Object> response = Map.of(
                    "user", Map.of(
                            "id", result.userId(),
                            "email", result.email(),
                            "username", result.username(),
                            "displayName", result.displayName()
                    ),
                    "accessToken", result.accessToken(),
                    "refreshToken", result.refreshToken(),
                    "expiresIn", result.expiresIn()
            );

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            log.error("Registration failed", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", e.getMessage()));
        }
    }

    /**
     * User login endpoint.
     * POST /api/auth/login
     * Body: {username?, email?, password}
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, Object> payload) {
        log.info("Received login request: {}", payload);

        try {
            String identifier = (String) payload.getOrDefault("username", payload.get("email"));
            String password = (String) payload.get("password");

            if (identifier == null || password == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("message", "Username/email and password are required"));
            }

            AuthenticationService.AuthResult result = authenticationService.login(
                    identifier, password);

            Map<String, Object> response = Map.of(
                    "user", Map.of(
                            "id", result.userId(),
                            "email", result.email(),
                            "username", result.username(),
                            "displayName", result.displayName()
                    ),
                    "accessToken", result.accessToken(),
                    "refreshToken", result.refreshToken(),
                    "expiresIn", result.expiresIn()
            );

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Login failed", e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Invalid credentials"));
        }
    }

    /**
     * Token refresh endpoint.
     * POST /api/auth/refresh
     * Body: {refreshToken, userId}
     */
    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestBody Map<String, Object> payload) {
        log.info("Received token refresh request");

        try {
            String refreshToken = (String) payload.get("refreshToken");
            String userIdStr = (String) payload.get("userId");

            if (userIdStr == null || refreshToken == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("message", "userId and refreshToken are required"));
            }

            UUID userId = UUID.fromString(userIdStr);
            AuthenticationService.AuthResult result = authenticationService.refreshToken(
                    userId, refreshToken);

            Map<String, Object> response = Map.of(
                    "user", Map.of(
                            "id", result.userId(),
                            "email", result.email(),
                            "username", result.username(),
                            "displayName", result.displayName()
                    ),
                    "accessToken", result.accessToken(),
                    "refreshToken", result.refreshToken(),
                    "expiresIn", result.expiresIn()
            );

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Token refresh failed", e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Invalid refresh token"));
        }
    }

    /**
     * Logout endpoint.
     * POST /api/auth/logout
     * Body: {userId, refreshToken}
     */
    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestBody Map<String, Object> payload) {
        log.info("Received logout request");

        try {
            String userIdStr = (String) payload.get("userId");
            String refreshToken = (String) payload.get("refreshToken");

            if (userIdStr != null) {
                UUID userId = UUID.fromString(userIdStr);
                authenticationService.logout(userId, refreshToken);
            }

            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("Logout failed", e);
            return ResponseEntity.noContent().build(); // Always succeed for logout
        }
    }

    /**
     * User validation endpoint.
     * GET /api/auth/validate?userId={userId}
     */
    @GetMapping("/validate")
    public ResponseEntity<?> validate(@RequestParam String userId) {
        log.info("Received user validation request: {}", userId);

        try {
            UUID id = UUID.fromString(userId);
            // Return basic user data - in real implementation would query database
            return ResponseEntity.ok(Map.of("id", id.toString(), "valid", true));
        } catch (Exception e) {
            log.error("User validation failed", e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "User not found"));
        }
    }

    /**
     * OAuth login endpoint.
     * POST /api/auth/oauth
     * Body: {provider, providerUserId, email, displayName?, profileData?}
     */
    @PostMapping("/oauth")
    public ResponseEntity<?> oauth(@RequestBody Map<String, Object> payload) {
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

            Map<String, Object> response = Map.of(
                    "user", Map.of(
                            "id", result.userId(),
                            "email", result.email(),
                            "username", result.username(),
                            "displayName", result.displayName()
                    ),
                    "accessToken", result.accessToken(),
                    "refreshToken", result.refreshToken(),
                    "expiresIn", result.expiresIn()
            );

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("OAuth login failed", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", e.getMessage()));
        }
    }
}
