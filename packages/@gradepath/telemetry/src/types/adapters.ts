/**
 * Storage adapter interface for platform-specific persistence
 * Allows the telemetry service to work with AsyncStorage (React Native),
 * localStorage (Web), or electron-store (Electron)
 */
export interface IStorageAdapter {
  /**
   * Retrieve an item from storage
   * @param key - The storage key
   * @returns The stored value or null if not found
   */
  getItem(key: string): Promise<string | null>;

  /**
   * Store an item in storage
   * @param key - The storage key
   * @param value - The value to store
   */
  setItem(key: string, value: string): Promise<void>;

  /**
   * Remove an item from storage
   * @param key - The storage key to remove
   */
  removeItem(key: string): Promise<void>;
}

/**
 * Platform adapter interface for platform-specific information
 * Allows the telemetry service to detect the current platform and
 * retrieve platform-specific metadata
 */
export interface IPlatformAdapter {
  /**
   * Get the current platform identifier
   * @returns The platform type
   */
  getPlatform(): 'ios' | 'android' | 'web' | 'windows' | 'mac' | 'linux';

  /**
   * Get the current application version
   * @returns The application version string
   */
  getAppVersion(): string;
}
