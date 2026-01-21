package com.gradepath.content.recommendation.profile;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Consumes behavioral profile updates from NestJS backend
 * Listens on 'profile-updates' Kafka topic
 */
@Component
@Slf4j
public class BehavioralProfileConsumer {

    private final ObjectMapper objectMapper;
    private final BehavioralProfileService profileService;

    @Autowired
    public BehavioralProfileConsumer(
            ObjectMapper objectMapper,
            BehavioralProfileService profileService) {
        this.objectMapper = objectMapper;
        this.profileService = profileService;
    }

    @KafkaListener(
        topics = "profile-updates",
        groupId = "behavioral-profile-consumer",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeProfileUpdate(String message) {
        try {
            log.debug("Received profile update from Kafka: {}", message);

            JsonNode json = objectMapper.readTree(message);
            JsonNode profileNode = json.get("profile");

            if (profileNode == null) {
                log.warn("No profile data in message: {}", message);
                return;
            }

            BehavioralProfile profile = objectMapper.treeToValue(profileNode, BehavioralProfile.class);

            // Set userId from the message if not in profile
            if (profile.getUserId() == null && json.has("userId")) {
                profile.setUserId(json.get("userId").asText());
            }

            // Set timestamp from message if not in profile
            if (profile.getTimestamp() == null && json.has("timestamp")) {
                profile.setTimestamp(objectMapper.treeToValue(json.get("timestamp"), Instant.class));
            }

            profileService.saveProfile(profile);
            log.info("Processed behavioral profile update for user: {}", profile.getUserId());

        } catch (Exception e) {
            log.error("Error processing profile update from Kafka: {}", message, e);
        }
    }
}
