import { IPlatformAdapter } from '../../types/adapters';

/**
 * Platform adapter implementation for Electron
 */
export class ElectronPlatformAdapter implements IPlatformAdapter {
  getPlatform(): 'windows' | 'mac' | 'linux' {
    if (typeof process === 'undefined' || !process.platform) {
      return 'linux'; // fallback
    }

    const platform = process.platform;
    if (platform === 'win32') return 'windows';
    if (platform === 'darwin') return 'mac';
    return 'linux';
  }

  getAppVersion(): string {
    // Try to get version from electron app
    if (typeof window !== 'undefined' && (window as any).require) {
      try {
        const { app } = (window as any).require('electron').remote || (window as any).require('@electron/remote');
        return app.getVersion();
      } catch (e) {
        // Fall through to default
      }
    }

    // Default version
    return '1.0.0';
  }
}
