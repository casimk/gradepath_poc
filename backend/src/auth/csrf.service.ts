import { Injectable, UnauthorizedException, Logger } from '@nestjs/common';
import { randomBytes, createHash } from 'crypto';

@Injectable()
export class CsrfService {
  private readonly logger = new Logger(CsrfService.name);
  private readonly tokens = new Map<string, { token: string; expiresAt: number }>();
  private readonly TOKEN_EXPIRY = 24 * 60 * 60 * 1000; // 24 hours
  private readonly TOKEN_LENGTH = 32;

  /**
   * Generate a CSRF token for a session
   */
  generateToken(sessionId: string): string {
    // Generate random token
    const token = randomBytes(this.TOKEN_LENGTH).toString('hex');

    // Store token with expiration
    this.tokens.set(sessionId, {
      token,
      expiresAt: Date.now() + this.TOKEN_EXPIRY,
    });

    this.logger.debug(`Generated CSRF token for session: ${sessionId}`);

    return token;
  }

  /**
   * Validate a CSRF token
   */
  validateToken(sessionId: string, providedToken: string): boolean {
    const storedData = this.tokens.get(sessionId);

    // No token stored or expired
    if (!storedData) {
      return false;
    }

    // Check expiration
    if (Date.now() > storedData.expiresAt) {
      this.tokens.delete(sessionId);
      return false;
    }

    // Validate token using constant-time comparison
    const isValid = this.constantTimeCompare(storedData.token, providedToken);

    if (!isValid) {
      this.logger.warn(`Invalid CSRF token for session: ${sessionId}`);
      return false;
    }

    this.logger.debug(`Validated CSRF token for session: ${sessionId}`);
    return true;
  }

  /**
   * Revoke a CSRF token (after logout or use)
   */
  revokeToken(sessionId: string): void {
    this.tokens.delete(sessionId);
    this.logger.debug(`Revoked CSRF token for session: ${sessionId}`);
  }

  /**
   * Clean up expired tokens (call periodically)
   */
  cleanupExpiredTokens(): void {
    const now = Date.now();
    let cleaned = 0;

    for (const [sessionId, data] of this.tokens.entries()) {
      if (now > data.expiresAt) {
        this.tokens.delete(sessionId);
        cleaned++;
      }
    }

    if (cleaned > 0) {
      this.logger.debug(`Cleaned up ${cleaned} expired CSRF tokens`);
    }
  }

  /**
   * Constant-time string comparison to prevent timing attacks
   */
  private constantTimeCompare(a: string, b: string): boolean {
    if (a.length !== b.length) {
      return false;
    }

    const aBuf = Buffer.from(a, 'utf8');
    const bBuf = Buffer.from(b, 'utf8');
    const result = aBuf.length ^ bBuf.length;

    // Return true if result is 0 (strings are equal)
    return result === 0;
  }

  /**
   * Generate a session ID from user ID
   */
  generateSessionId(userId: string): string {
    // Create a session ID that incorporates user ID and random data
    const data = `${userId}-${Date.now()}-${randomBytes(16).toString('hex')}`;
    return createHash('sha256')
      .update(data)
      .digest('hex')
      .substring(0, 32);
  }
}
