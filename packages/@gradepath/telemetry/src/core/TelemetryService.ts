import { v4 as uuidv4 } from 'uuid';
import {
  TelemetryEvent,
  ScreenViewEvent,
  PerformanceMetric,
  TelemetryConfig,
} from '../types/telemetry.types';
import { IStorageAdapter, IPlatformAdapter } from '../types/adapters';

const SESSION_ID_KEY = '@telemetry_session_id';
const USER_ID_KEY = '@telemetry_user_id';
const QUEUE_KEY = '@telemetry_queue';

/**
 * Core telemetry service for tracking user events, screen views, and performance metrics.
 * Uses adapter pattern to work across different platforms (React Native, Web, Electron).
 */
export class TelemetryService {
  private config: TelemetryConfig;
  private storageAdapter: IStorageAdapter;
  private platformAdapter: IPlatformAdapter;
  private userId: string | null = null;
  private sessionId: string | null = null;
  private eventQueue: TelemetryEvent[] = [];
  private flushTimer: ReturnType<typeof setInterval> | null = null;
  private screenViewStartTime: Record<string, number> = {};
  private initialized: boolean = false;

  constructor(
    storageAdapter: IStorageAdapter,
    platformAdapter: IPlatformAdapter,
    config: Partial<TelemetryConfig> = {},
  ) {
    this.storageAdapter = storageAdapter;
    this.platformAdapter = platformAdapter;
    this.config = {
      apiEndpoint: config.apiEndpoint || 'http://localhost:3000',
      batchSize: config.batchSize || 10,
      flushInterval: config.flushInterval || 30000, // 30 seconds
      enabled: config.enabled !== false,
    };
  }

  /**
   * Initialize the telemetry service
   * Loads or creates user/session IDs and starts the flush timer
   */
  async initialize(): Promise<void> {
    if (this.initialized) {
      return;
    }

    if (!this.config.enabled) {
      console.log('[Telemetry] Disabled');
      this.initialized = true;
      return;
    }

    // Get or create user ID
    this.userId = await this.storageAdapter.getItem(USER_ID_KEY);
    if (!this.userId) {
      this.userId = uuidv4();
      await this.storageAdapter.setItem(USER_ID_KEY, this.userId);
    }

    // Create new session ID
    this.sessionId = uuidv4();
    await this.storageAdapter.setItem(SESSION_ID_KEY, this.sessionId);

    // Load queued events
    const queueJson = await this.storageAdapter.getItem(QUEUE_KEY);
    if (queueJson) {
      try {
        this.eventQueue = JSON.parse(queueJson);
      } catch (e) {
        console.error('[Telemetry] Failed to parse queue:', e);
      }
    }

    // Start flush timer
    this.startFlushTimer();

    this.initialized = true;
    console.log('[Telemetry] Initialized', {
      userId: this.userId,
      sessionId: this.sessionId,
      platform: this.platformAdapter.getPlatform(),
      appVersion: this.platformAdapter.getAppVersion(),
    });
  }

  /**
   * Track a custom event
   * @param eventType - Type of the event
   * @param metadata - Additional event data
   * @param screenName - Optional screen name where event occurred
   */
  async trackEvent(
    eventType: string,
    metadata?: Record<string, any>,
    screenName?: string,
  ): Promise<void> {
    if (!this.config.enabled || !this.userId || !this.sessionId) {
      return;
    }

    const event: TelemetryEvent = {
      eventType,
      userId: this.userId,
      sessionId: this.sessionId,
      screenName,
      metadata,
      timestamp: Date.now(),
      platform: this.platformAdapter.getPlatform(),
      appVersion: this.platformAdapter.getAppVersion(),
    };

    this.eventQueue.push(event);
    await this.persistQueue();

    if (this.eventQueue.length >= this.config.batchSize) {
      await this.flush();
    }
  }

  /**
   * Track when a user views a screen
   * @param screenName - Name of the screen being viewed
   * @param properties - Additional screen properties
   */
  async trackScreenView(screenName: string, properties?: Record<string, any>): Promise<void> {
    if (!this.config.enabled || !this.userId || !this.sessionId) {
      return;
    }

    // Record the start time for duration tracking
    this.screenViewStartTime[screenName] = Date.now();

    await this.trackEvent('screen_view', { screenName, ...properties }, screenName);
  }

  /**
   * Track when a user leaves a screen (calculates duration)
   * @param screenName - Name of the screen being left
   */
  async trackScreenEnd(screenName: string): Promise<void> {
    const startTime = this.screenViewStartTime[screenName];
    if (startTime) {
      const duration = Date.now() - startTime;
      delete this.screenViewStartTime[screenName];

      await this.trackEvent(
        'screen_view_end',
        { screenName, duration },
        screenName,
      );
    }
  }

  /**
   * Track a performance metric
   * @param metricName - Name of the metric
   * @param value - Numeric value of the metric
   * @param unit - Unit of measurement (default: 'ms')
   * @param context - Additional context for the metric
   */
  async trackPerformance(
    metricName: string,
    value: number,
    unit: string = 'ms',
    context?: Record<string, any>,
  ): Promise<void> {
    if (!this.config.enabled) {
      return;
    }

    const metric: PerformanceMetric = {
      metricName,
      userId: this.userId || 'unknown',
      sessionId: this.sessionId || 'unknown',
      value,
      unit,
      context,
      timestamp: Date.now(),
    };

    try {
      const response = await fetch(`${this.config.apiEndpoint}/telemetry/performance`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(metric),
      });

      if (!response.ok) {
        throw new Error(`HTTP ${response.status}`);
      }

      console.log('[Telemetry] Performance metric sent:', metricName);
    } catch (error) {
      console.error('[Telemetry] Failed to send performance metric:', error);
    }
  }

  /**
   * Persist the event queue to storage
   */
  private async persistQueue(): Promise<void> {
    try {
      await this.storageAdapter.setItem(QUEUE_KEY, JSON.stringify(this.eventQueue));
    } catch (error) {
      console.error('[Telemetry] Failed to persist queue:', error);
    }
  }

  /**
   * Start the automatic flush timer
   */
  private startFlushTimer(): void {
    if (this.flushTimer) {
      clearInterval(this.flushTimer);
    }

    this.flushTimer = setInterval(() => {
      this.flush();
    }, this.config.flushInterval);
  }

  /**
   * Flush all queued events to the backend
   */
  async flush(): Promise<void> {
    if (this.eventQueue.length === 0) {
      return;
    }

    const eventsToSend = [...this.eventQueue];
    this.eventQueue = [];
    await this.persistQueue();

    // Send each event
    for (const event of eventsToSend) {
      try {
        const endpoint = event.eventType.startsWith('screen_view')
          ? '/telemetry/screen-view'
          : '/telemetry/event';

        const response = await fetch(`${this.config.apiEndpoint}${endpoint}`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify(event),
        });

        if (!response.ok) {
          throw new Error(`HTTP ${response.status}`);
        }

        console.log('[Telemetry] Event sent:', event.eventType);
      } catch (error) {
        console.error('[Telemetry] Failed to send event:', error);
        // Re-queue failed events
        this.eventQueue.push(event);
      }
    }

    await this.persistQueue();
  }

  /**
   * Cleanup and flush any remaining events
   */
  async destroy(): Promise<void> {
    if (this.flushTimer) {
      clearInterval(this.flushTimer);
      this.flushTimer = null;
    }
    await this.flush();
    this.initialized = false;
  }

  /**
   * Get the current session ID
   */
  getSessionId(): string | null {
    return this.sessionId;
  }

  /**
   * Get the current user ID
   */
  getUserId(): string | null {
    return this.userId;
  }

  /**
   * Check if the service is initialized
   */
  isInitialized(): boolean {
    return this.initialized;
  }
}
