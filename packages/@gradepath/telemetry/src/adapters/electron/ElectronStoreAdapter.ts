import Store from 'electron-store';
import { IStorageAdapter } from '../../types/adapters';

/**
 * Storage adapter implementation for Electron using electron-store
 * Provides persistent storage across app restarts
 */
export class ElectronStoreAdapter implements IStorageAdapter {
  private store: Store;

  constructor() {
    this.store = new Store({ name: 'telemetry' });
  }

  async getItem(key: string): Promise<string | null> {
    const value = this.store.get(key);
    return value !== undefined ? String(value) : null;
  }

  async setItem(key: string, value: string): Promise<void> {
    this.store.set(key, value);
  }

  async removeItem(key: string): Promise<void> {
    this.store.delete(key);
  }
}
