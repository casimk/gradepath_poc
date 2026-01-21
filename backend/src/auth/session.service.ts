import { Injectable, Logger } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { createClient, RedisClientType } from 'redis';

interface SessionData {
  userId: string;
  email: string;
  createdAt: number;
  lastActivity: number;
}

@Injectable()
export class SessionService {
  private readonly logger = new Logger(SessionService.name);
  private readonly redisClient: RedisClientType;
  private readonly sessionExpiry = 7 * 24 * 60 * 60; // 7 days in seconds

  constructor(private readonly configService: ConfigService) {
    const redisUrl = this.configService.get('REDIS_URL') || 'redis://localhost:6379';
    this.redisClient = createClient({ url: redisUrl });

    this.redisClient
      .connect()
      .then(() => this.logger.log('Connected to Redis for session management'))
      .catch((err) => this.logger.error(`Failed to connect to Redis: ${err.message}`));
  }

  async createSession(sessionId: string, sessionData: SessionData): Promise<void> {
    try {
      await this.redisClient.setEx(
        `session:${sessionId}`,
        this.sessionExpiry,
        JSON.stringify(sessionData),
      );
      this.logger.log(`Created session: ${sessionId}`);
    } catch (error) {
      this.logger.error(`Failed to create session: ${error.message}`);
    }
  }

  async getSession(sessionId: string): Promise<SessionData | null> {
    try {
      const data = await this.redisClient.get(`session:${sessionId}`);
      if (data && typeof data === 'string') {
        return JSON.parse(data);
      }
      return null;
    } catch (error) {
      this.logger.error(`Failed to get session: ${error.message}`);
      return null;
    }
  }

  async updateSession(sessionId: string, sessionData: Partial<SessionData>): Promise<void> {
    try {
      const existing = await this.getSession(sessionId);
      if (existing) {
        await this.redisClient.setEx(
          `session:${sessionId}`,
          this.sessionExpiry,
          JSON.stringify({ ...existing, ...sessionData }),
        );
      }
    } catch (error) {
      this.logger.error(`Failed to update session: ${error.message}`);
    }
  }

  async deleteSession(sessionId: string): Promise<void> {
    try {
      await this.redisClient.del(`session:${sessionId}`);
      this.logger.log(`Deleted session: ${sessionId}`);
    } catch (error) {
      this.logger.error(`Failed to delete session: ${error.message}`);
    }
  }

  async deleteAllUserSessions(userId: string): Promise<void> {
    try {
      const keys = await this.redisClient.keys('session:*');
      for (const key of keys) {
        const data = await this.redisClient.get(key);
        if (data && typeof data === 'string') {
          const session: SessionData = JSON.parse(data);
          if (session.userId === userId) {
            await this.redisClient.del(key);
          }
        }
      }
      this.logger.log(`Deleted all sessions for user: ${userId}`);
    } catch (error) {
      this.logger.error(`Failed to delete user sessions: ${error.message}`);
    }
  }

  async trackRateLimit(identifier: string): Promise<boolean> {
    try {
      const key = `rate_limit:${identifier}`;
      const count = await this.redisClient.incr(key);

      if (count === 1) {
        await this.redisClient.expire(key, 60); // 1 minute window
      }

      return count <= 10; // Allow 10 requests per minute
    } catch (error) {
      this.logger.error(`Failed to track rate limit: ${error.message}`);
      return true; // Fail open - allow request if tracking fails
    }
  }
}
