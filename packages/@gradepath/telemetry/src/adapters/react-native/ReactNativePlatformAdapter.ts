import { Platform } from 'react-native';
import { IPlatformAdapter } from '../../types/adapters';

/**
 * Platform adapter implementation for React Native
 */
export class ReactNativePlatformAdapter implements IPlatformAdapter {
  getPlatform(): 'ios' | 'android' {
    return Platform.OS as 'ios' | 'android';
  }

  getAppVersion(): string {
    // In a real app, this would come from app.json or native code
    // For now, return a default version
    const defaultVersion = '1.0.0';

    // Try to get version from native modules if available
    try {
      if (typeof (global as any).nativeAppVersion !== 'undefined') {
        return (global as any).nativeAppVersion;
      }
      // eslint-disable-next-line no-empty
    } catch (e) {}

    return defaultVersion;
  }
}
