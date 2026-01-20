import { IPlatformAdapter } from '../../types/adapters';

/**
 * Platform adapter implementation for Web
 */
export class WebPlatformAdapter implements IPlatformAdapter {
  getPlatform(): 'web' {
    return 'web';
  }

  getAppVersion(): string {
    // Try to get version from global window object (set by build process)
    if (typeof window !== 'undefined' && (window as any).__APP_VERSION__) {
      return (window as any).__APP_VERSION__;
    }
    // Default version
    return '1.0.0';
  }
}
