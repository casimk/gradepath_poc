/**
 * TelemetryService - Re-exports from @gradepath/telemetry package
 *
 * This file provides a default export for backward compatibility.
 * The actual implementation is now in the shared @gradepath/telemetry package.
 */

import { TelemetryService, createTelemetryServiceWithAdapters, AsyncStorageAdapter, ReactNativePlatformAdapter } from '@gradepath/telemetry';

/**
 * Default telemetry service instance for React Native
 * Uses explicit adapters for React Native platform
 */
const telemetryService = createTelemetryServiceWithAdapters(
  new AsyncStorageAdapter(),
  new ReactNativePlatformAdapter(),
  {
    apiEndpoint: process.env.API_ENDPOINT || 'http://localhost:3000',
    batchSize: 10,
    flushInterval: 30000,
    enabled: true,
  }
);

export default telemetryService;

// Also export the class and types for advanced usage
export * from '@gradepath/telemetry';
