import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: {
      // Exclude optional dependencies from web bundle
      'electron': '/Users/qasim/Documents/Dev/GradePath_UserTelemetryPoC/web-app/src/empty-stub.ts',
      'electron-store': '/Users/qasim/Documents/Dev/GradePath_UserTelemetryPoC/web-app/src/empty-stub.ts',
      '@react-native-async-storage/async-storage': '/Users/qasim/Documents/Dev/GradePath_UserTelemetryPoC/web-app/src/empty-stub.ts',
      'react-native': '/Users/qasim/Documents/Dev/GradePath_UserTelemetryPoC/web-app/src/empty-stub.ts',
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
