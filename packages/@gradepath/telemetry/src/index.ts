// Core exports
export { TelemetryService } from './core/TelemetryService';

// Factory functions
export {
  createTelemetryService,
  createTelemetryServiceWithAdapters,
  createWebTelemetryService,
  getPlatformName,
} from './factory';

// Hooks
export { useTelemetry } from './hooks/useTelemetry';
export type { UseTelemetryResult } from './hooks/useTelemetry';

// Types
export type {
  TelemetryEvent,
  ScreenViewEvent,
  PerformanceMetric,
  TelemetryConfig,
} from './types/telemetry.types';

export type {
  IStorageAdapter,
  IPlatformAdapter,
} from './types/adapters';

// Adapters (for explicit usage)
export { AsyncStorageAdapter } from './adapters/react-native/AsyncStorageAdapter';
export { ReactNativePlatformAdapter } from './adapters/react-native/ReactNativePlatformAdapter';
export { LocalStorageAdapter } from './adapters/web/LocalStorageAdapter';
export { WebPlatformAdapter } from './adapters/web/WebPlatformAdapter';
export { ElectronStoreAdapter } from './adapters/electron/ElectronStoreAdapter';
export { ElectronPlatformAdapter } from './adapters/electron/ElectronPlatformAdapter';

// Note: Default export creates a web telemetry service synchronously
// This is safe for all environments since it only uses web APIs
import { createWebTelemetryService } from './factory';

export default createWebTelemetryService();
