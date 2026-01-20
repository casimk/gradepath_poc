import { TelemetryService } from './core/TelemetryService';
import { TelemetryConfig } from './types/telemetry.types';
import { IStorageAdapter, IPlatformAdapter } from './types/adapters';

// Web adapters - only these are safe to import at module level
import { LocalStorageAdapter } from './adapters/web/LocalStorageAdapter';
import { WebPlatformAdapter } from './adapters/web/WebPlatformAdapter';

/**
 * Platform detection result
 */
interface PlatformDetection {
  platform: 'react-native' | 'electron' | 'web';
  storageAdapter: IStorageAdapter;
  platformAdapter: IPlatformAdapter;
}

/**
 * Dynamically import React Native adapters (only when actually needed)
 */
async function loadReactNativeAdapters(): Promise<{ storage: IStorageAdapter; platform: IPlatformAdapter }> {
  const { AsyncStorageAdapter } = await import('./adapters/react-native/AsyncStorageAdapter');
  const { ReactNativePlatformAdapter } = await import('./adapters/react-native/ReactNativePlatformAdapter');
  return {
    storage: new AsyncStorageAdapter(),
    platform: new ReactNativePlatformAdapter(),
  };
}

/**
 * Dynamically import Electron adapters (only when actually needed)
 */
async function loadElectronAdapters(): Promise<{ storage: IStorageAdapter; platform: IPlatformAdapter }> {
  const { ElectronStoreAdapter } = await import('./adapters/electron/ElectronStoreAdapter');
  const { ElectronPlatformAdapter } = await import('./adapters/electron/ElectronPlatformAdapter');
  return {
    storage: new ElectronStoreAdapter(),
    platform: new ElectronPlatformAdapter(),
  };
}

/**
 * Detect the current platform and return appropriate adapters
 */
async function detectPlatform(): Promise<PlatformDetection> {
  // Check for React Native
  const isReactNative = typeof navigator !== 'undefined' &&
    (navigator as any).product === 'ReactNative';

  // Check for Electron
  const isElectron = typeof process !== 'undefined' &&
    process !== null &&
    process.versions !== undefined &&
    process.versions.electron !== undefined;

  // Check for Web (must be last, as both RN and Electron run in browser-like environments)
  const isWeb = typeof window !== 'undefined' && typeof document !== 'undefined';

  if (isReactNative) {
    const adapters = await loadReactNativeAdapters();
    return {
      platform: 'react-native',
      storageAdapter: adapters.storage,
      platformAdapter: adapters.platform,
    };
  }

  if (isElectron) {
    const adapters = await loadElectronAdapters();
    return {
      platform: 'electron',
      storageAdapter: adapters.storage,
      platformAdapter: adapters.platform,
    };
  }

  // Web (or fallback)
  return {
    platform: 'web',
    storageAdapter: new LocalStorageAdapter(),
    platformAdapter: new WebPlatformAdapter(),
  };
}

/**
 * Synchronous platform detection for simple platform name queries
 * Does not instantiate adapters
 */
export function getPlatformName(): 'react-native' | 'electron' | 'web' {
  const isReactNative = typeof navigator !== 'undefined' &&
    (navigator as any).product === 'ReactNative';

  const isElectron = typeof process !== 'undefined' &&
    process !== null &&
    process.versions !== undefined &&
    process.versions.electron !== undefined;

  const isWeb = typeof window !== 'undefined' && typeof document !== 'undefined';

  if (isReactNative) return 'react-native';
  if (isElectron) return 'electron';
  return 'web';
}

/**
 * Factory function to create a telemetry service with auto-detected platform adapters
 * @param config - Partial configuration options
 * @returns Promise that resolves to a configured TelemetryService instance
 */
export async function createTelemetryService(config?: Partial<TelemetryConfig>): Promise<TelemetryService> {
  const { storageAdapter, platformAdapter } = await detectPlatform();
  return new TelemetryService(storageAdapter, platformAdapter, config);
}

/**
 * Create a telemetry service with explicit adapters (synchronous)
 * Use this when you want full control over adapter selection
 */
export function createTelemetryServiceWithAdapters(
  storageAdapter: IStorageAdapter,
  platformAdapter: IPlatformAdapter,
  config?: Partial<TelemetryConfig>,
): TelemetryService {
  return new TelemetryService(storageAdapter, platformAdapter, config);
}

/**
 * Create a web telemetry service (synchronous convenience function)
 * Use this for web applications when you know you're running in a browser
 */
export function createWebTelemetryService(config?: Partial<TelemetryConfig>): TelemetryService {
  return new TelemetryService(
    new LocalStorageAdapter(),
    new WebPlatformAdapter(),
    config,
  );
}
