import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import path from 'path'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: {
      // Exclude optional dependencies from web bundle
      'electron': path.resolve(__dirname, './src/empty-stub.ts'),
      'electron-store': path.resolve(__dirname, './src/empty-stub.ts'),
      '@react-native-async-storage/async-storage': path.resolve(__dirname, './src/empty-stub.ts'),
      'react-native': path.resolve(__dirname, './src/empty-stub.ts'),
    },
  },
  optimizeDeps: {
    exclude: [
      '@gradepath/telemetry',
      'electron',
      'electron-store',
      '@react-native-async-storage/async-storage',
      'react-native',
    ],
  },
})
