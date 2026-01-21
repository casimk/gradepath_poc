import { Injectable, OnModuleInit, OnModuleDestroy, Logger } from '@nestjs/common';
import Redis from 'ioredis';

interface SessionState {
  userId: string;
  startTime: number;
  contentCount: number;
  lastActivity: number;
}

interface JourneyEntry {
  contentId: string;
  timestamp: number;
  action: string;
}

@Injectable()
export class SessionCacheService implements OnModuleInit, OnModuleDestroy {
  private readonly logger = new Logger(SessionCacheService.name);
  private redis: Redis;
  private readonly SESSION_TTL = 86400; // 24 hours in seconds

  constructor() {
    const redisHost = process.env.REDIS_HOST || 'localhost';
    const redisPort = parseInt(process.env.REDIS_PORT || '6379');

    this.redis = new Redis({
      host: redisHost,
      port: redisPort,
      retryStrategy: (times) => {
        const delay = Math.min(times * 50, 2000);
        return delay;
      },
    });

    this.redis.on('connect', () => {
      this.logger.log(`Connected to Redis at ${redisHost}:${redisPort}`);
    });

    this.redis.on('error', (err) => {
      this.logger.error('Redis connection error:', err);
    });
  }

  async onModuleInit() {
    // Wait for Redis connection
    await this.redis.ping();
    this.logger.log('SessionCacheService initialized');
  }

  async onModuleDestroy() {
    await this.redis.quit();
  }

  // Session State Operations
  async setSessionState(sessionId: string, state: SessionState): Promise<void> {
    const key = `session:${sessionId}:state`;
    await this.redis.set(key, JSON.stringify(state), 'EX', this.SESSION_TTL);
    this.logger.debug(`Session state set for ${sessionId}`);
  }

  async getSessionState(sessionId: string): Promise<SessionState | null> {
    const key = `session:${sessionId}:state`;
    const data = await this.redis.get(key);
    if (!data) return null;
    return JSON.parse(data) as SessionState;
  }

  async deleteSessionState(sessionId: string): Promise<void> {
    const key = `session:${sessionId}:state`;
    await this.redis.del(key);
    this.logger.debug(`Session state deleted for ${sessionId}`);
  }

  // Journey Operations
  async addJourneyEntry(sessionId: string, entry: JourneyEntry): Promise<void> {
    const key = `session:${sessionId}:journey`;
    await this.redis.rpush(key, JSON.stringify(entry));
    await this.redis.expire(key, this.SESSION_TTL);
    this.logger.debug(`Journey entry added for ${sessionId}`);
  }

  async getJourney(sessionId: string): Promise<JourneyEntry[]> {
    const key = `session:${sessionId}:journey`;
    const length = await this.redis.llen(key);
    if (length === 0) return [];

    const entries = await this.redis.lrange(key, 0, -1);
    return entries.map((e) => JSON.parse(e) as JourneyEntry);
  }

  async getJourneyLength(sessionId: string): Promise<number> {
    const key = `session:${sessionId}:journey`;
    return await this.redis.llen(key);
  }

  // User Sessions Tracking
  async addUserSession(userId: string, sessionId: number, timestamp: number): Promise<void> {
    const key = `user:${userId}:sessions`;
    await this.redis.zadd(key, timestamp, sessionId);
    await this.redis.expire(key, this.SESSION_TTL * 30); // 30 days
    this.logger.debug(`Session ${sessionId} added for user ${userId}`);
  }

  async getUserSessions(
    userId: string,
    limit = 30,
  ): Promise<Array<{ sessionId: number; timestamp: number }>> {
    const key = `user:${userId}:sessions`;
    const results = await this.redis.zrevrange(key, 0, limit - 1, 'WITHSCORES');

    const sessions: Array<{ sessionId: number; timestamp: number }> = [];
    for (let i = 0; i < results.length; i += 2) {
      sessions.push({
        sessionId: parseInt(results[i]),
        timestamp: parseFloat(results[i + 1]),
      });
    }
    return sessions;
  }

  async getUserSessionCount(userId: string): Promise<number> {
    const key = `user:${userId}:sessions`;
    return await this.redis.zcard(key);
  }

  // Health check
  async healthCheck(): Promise<boolean> {
    try {
      await this.redis.ping();
      return true;
    } catch {
      return false;
    }
  }
}
