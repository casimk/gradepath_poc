import AsyncStorage from '@react-native-async-storage/async-storage';
import { IStorageAdapter } from '../../types/adapters';

/**
 * Storage adapter implementation for React Native using AsyncStorage
 */
export class AsyncStorageAdapter implements IStorageAdapter {
  async getItem(key: string): Promise<string | null> {
    return AsyncStorage.getItem(key);
  }

  async setItem(key: string, value: string): Promise<void> {
    return AsyncStorage.setItem(key, value);
  }

  async removeItem(key: string): Promise<void> {
    return AsyncStorage.removeItem(key);
  }
}
