import { Injectable, Logger, Inject } from '@nestjs/common';
import { ClientKafka } from '@nestjs/microservices';
import { SessionCacheService } from '../redis/session-cache.service';
import { SessionEventDto, ContentJourneyDto, JourneyTrackResponseDto } from './dto';

/**
 * Session Service - I/O Layer Only
 *
 * This service handles ONLY I/O operations:
 * - Redis session state management
 * - Kafka emission of raw events
 *
 * All CPU-intensive profiling logic (interest scoring, engagement classification,
 * journey analysis) has been moved to the Spring Modulith core (Java).
 */
@Injectable()
export class SessionService {
  private readonly logger = new Logger(SessionService.name);

  constructor(
    private readonly sessionCache: SessionCacheService,
    @Inject('BEHAVIORAL_KAFKA') private readonly kafkaClient: ClientKafka,
  ) {}

  async startSession(event: SessionEventDto) {
    const eventWithTimestamp = {
      ...event,
      timestamp: event.timestamp || Date.now(),
    };

    this.logger.log(`Session started: ${event.sessionId} for user ${event.userId}`);

    // Store session state in Redis
    await this.sessionCache.setSessionState(event.sessionId, {
      userId: event.userId,
      startTime: event.startTime,
      contentCount: 0,
      lastActivity: event.startTime,
    });

    // Emit raw event to Kafka for Java profiling
    this.kafkaClient.emit('raw-behavioral-events', {
      ...eventWithTimestamp,
      topic: 'session_lifecycle',
      eventType: 'session_start',
    });

    return {
      success: true,
      sessionId: event.sessionId,
      startTime: event.startTime,
    };
  }

  async endSession(event: SessionEventDto) {
    const durationSeconds = event.durationSeconds || Math.floor((Date.now() - event.startTime) / 1000);
    const eventWithTimestamp = {
      ...event,
      endTime: event.endTime || Date.now(),
      durationSeconds,
      timestamp: event.timestamp || Date.now(),
    };

    this.logger.log(
      `Session ended: ${event.sessionId} for user ${event.userId}, duration: ${durationSeconds}s`,
    );

    // Get final session state
    const sessionState = await this.sessionCache.getSessionState(event.sessionId);
    const contentCount = sessionState?.contentCount || 0;

    // Update session state with final data
    await this.sessionCache.setSessionState(event.sessionId, {
      userId: event.userId,
      startTime: event.startTime,
      contentCount,
      lastActivity: eventWithTimestamp.endTime,
    });

    // Add to user's session history
    await this.sessionCache.addUserSession(event.userId, parseInt(event.sessionId), event.startTime);

    // Emit raw event to Kafka for Java profiling
    this.kafkaClient.emit('raw-behavioral-events', {
      ...eventWithTimestamp,
      topic: 'session_lifecycle',
      eventType: 'session_end',
      contentCount,
    });

    return {
      success: true,
      sessionId: event.sessionId,
      durationSeconds,
      contentCount,
    };
  }

  async resumeSession(event: SessionEventDto) {
    const eventWithTimestamp = {
      ...event,
      timestamp: event.timestamp || Date.now(),
    };

    this.logger.log(`Session resumed: ${event.sessionId} for user ${event.userId}`);

    // Update last activity
    const existingState = await this.sessionCache.getSessionState(event.sessionId);
    if (existingState) {
      await this.sessionCache.setSessionState(event.sessionId, {
        ...existingState,
        lastActivity: Date.now(),
      });
    }

    // Emit raw event to Kafka
    this.kafkaClient.emit('raw-behavioral-events', {
      ...eventWithTimestamp,
      topic: 'session_lifecycle',
      eventType: 'session_resume',
    });

    return {
      success: true,
      sessionId: event.sessionId,
    };
  }

  async trackContentJourney(journey: ContentJourneyDto): Promise<JourneyTrackResponseDto> {
    const journeyWithTimestamp = {
      ...journey,
      timestamp: journey.timestamp || Date.now(),
    };

    this.logger.log(
      `Content journey tracked: ${journey.contentId} (${journey.action}) for session ${journey.sessionId}`,
    );

    // Add journey entry to Redis
    await this.sessionCache.addJourneyEntry(journey.sessionId, {
      contentId: journey.contentId,
      timestamp: journeyWithTimestamp.timestamp,
      action: journey.action,
    });

    // Update session state content count
    const sessionState = await this.sessionCache.getSessionState(journey.sessionId);
    if (sessionState) {
      await this.sessionCache.setSessionState(journey.sessionId, {
        ...sessionState,
        contentCount: (sessionState.contentCount || 0) + 1,
        lastActivity: Date.now(),
      });
    }

    // Emit raw event to Kafka for Java profiling
    // Note: Setting previousContentId to empty string for now - could be enhanced
    // to track the previous content from Redis session state
    this.kafkaClient.emit('raw-behavioral-events', {
      ...journeyWithTimestamp,
      topic: 'content_journey',
      previousContentId: '', // Could be enhanced to track from Redis
    });

    const response: JourneyTrackResponseDto = {
      success: true,
      tracked: true,
      journeyId: journey.journeyId,
    };

    return response;
  }

  async getSession(sessionId: string) {
    const sessionState = await this.sessionCache.getSessionState(sessionId);
    const journey = await this.sessionCache.getJourney(sessionId);

    return {
      sessionId,
      state: sessionState,
      journey,
      journeyLength: await this.sessionCache.getJourneyLength(sessionId),
    };
  }

  async getUserSessions(userId: string, limit = 30) {
    return await this.sessionCache.getUserSessions(userId, limit);
  }

  async onModuleInit() {
    // This will be called by the module lifecycle
  }
}
