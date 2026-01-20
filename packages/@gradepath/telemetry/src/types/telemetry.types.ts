/**
 * Standard telemetry event sent to the backend
 */
export interface TelemetryEvent {
  /** Type of the event (e.g., 'button_press', 'screen_view') */
  eventType: string;
  /** Unique user identifier */
  userId: string;
  /** Unique session identifier */
  sessionId: string;
  /** Optional screen name where the event occurred */
  screenName?: string;
  /** Additional event metadata */
  metadata?: Record<string, any>;
  /** Unix timestamp in milliseconds */
  timestamp?: number;
  /** Platform identifier (ios, android, web, windows, mac, linux) */
  platform: string;
  /** Application version */
  appVersion: string;
}

/**
 * Screen view event for tracking navigation
 */
export interface ScreenViewEvent {
  /** Name of the screen viewed */
  screenName: string;
  /** Unique user identifier */
  userId: string;
  /** Unique session identifier */
  sessionId: string;
  /** Duration in milliseconds (for screen_view_end events) */
  duration?: number;
  /** Additional screen properties */
  properties?: Record<string, any>;
  /** Unix timestamp in milliseconds */
  timestamp?: number;
}

/**
 * Performance metric event for tracking app performance
 */
export interface PerformanceMetric {
  /** Name of the metric (e.g., 'app_start_time', 'api_response_time') */
  metricName: string;
  /** Unique user identifier */
  userId: string;
  /** Unique session identifier */
  sessionId: string;
  /** Numeric value of the metric */
  value: number;
  /** Unit of measurement (e.g., 'ms', 'bytes') */
  unit: string;
  /** Additional context for the metric */
  context?: Record<string, any>;
  /** Unix timestamp in milliseconds */
  timestamp?: number;
}

/**
 * Configuration options for the telemetry service
 */
export interface TelemetryConfig {
  /** Backend API endpoint URL */
  apiEndpoint: string;
  /** Number of events to batch before sending */
  batchSize: number;
  /** Interval in milliseconds to auto-flush events */
  flushInterval: number;
  /** Whether telemetry is enabled */
  enabled: boolean;
}
