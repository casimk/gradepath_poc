export interface TelemetryEvent {
  eventType: string;
  userId: string;
  sessionId: string;
  screenName?: string;
  metadata?: Record<string, any>;
  timestamp?: number;
  platform: string;
  appVersion: string;
}

export interface ScreenViewEvent {
  screenName: string;
  userId: string;
  sessionId: string;
  duration?: number;
  properties?: Record<string, any>;
  timestamp?: number;
}

export interface PerformanceMetric {
  metricName: string;
  userId: string;
  sessionId: string;
  value: number;
  unit: string;
  context?: Record<string, any>;
  timestamp?: number;
}

export interface TelemetryConfig {
  apiEndpoint: string;
  batchSize: number;
  flushInterval: number;
  enabled: boolean;
}
