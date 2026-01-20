# @gradepath/telemetry

Multi-platform telemetry package for React Native, Web, and Electron applications.

## Features

- **90%+ code shared** across all platforms
- **Automatic platform detection** - no platform-specific code needed in your app
- **Type-safe** with full TypeScript support
- **Persistent storage** - events survive app restarts
- **Batching** - efficient event delivery to backend
- **React hook** - simple `useTelemetry` hook for automatic screen tracking

## Installation

```bash
npm install @gradepath/telemetry uuid
```

### Platform-specific dependencies

The package includes optional dependencies that are installed automatically when available:

- **React Native**: `@react-native-async-storage/async-storage` (optional)
- **Electron**: `electron-store` (optional)
- **Web**: Uses browser localStorage (no additional dependencies)

## Quick Start

### React Native / Web / Electron

```typescript
// Create a telemetry service instance
import { createTelemetryService } from '@gradepath/telemetry';

const telemetryService = createTelemetryService({
  apiEndpoint: 'http://localhost:3000',
  batchSize: 10,
  flushInterval: 30000,
  enabled: true,
});

// Initialize on app startup
await telemetryService.initialize();
```

### Using the React Hook

```tsx
import { useTelemetry } from '@gradepath/telemetry';
import telemetryService from './telemetryService';

function HomeScreen() {
  const { trackButtonPress, trackInputChange } = useTelemetry(
    telemetryService,
    'HomeScreen'
  );

  return (
    <div>
      <input
        onChange={(e) => trackInputChange('email', e.target.value)}
      />
      <button onClick={() => trackButtonPress('submit')}>
        Submit
      </button>
    </div>
  );
}
```

## API

### `createTelemetryService(config?)`

Creates a telemetry service instance with auto-detected platform adapters.

```typescript
import { createTelemetryService } from '@gradepath/telemetry';

const service = createTelemetryService({
  apiEndpoint: string,      // Backend API endpoint (default: 'http://localhost:3000')
  batchSize: number,         // Events to batch before sending (default: 10)
  flushInterval: number,     // Auto-flush interval in ms (default: 30000)
  enabled: boolean,          // Enable/disable tracking (default: true)
});
```

### `useTelemetry(service, screenName)`

React hook for automatic screen tracking.

```typescript
const {
  trackEvent,           // Track custom events
  trackButtonPress,      // Track button presses
  trackInputChange,      // Track input changes
} = useTelemetry(service, 'ScreenName');
```

### TelemetryService Methods

- `initialize()` - Initialize the service (call on app startup)
- `trackEvent(eventType, metadata?, screenName?)` - Track a custom event
- `trackScreenView(screenName, properties?)` - Track screen view
- `trackScreenEnd(screenName)` - Track screen end (calculates duration)
- `trackPerformance(metricName, value, unit?, context?)` - Track performance metrics
- `flush()` - Manually flush queued events
- `destroy()` - Cleanup and flush remaining events
- `getSessionId()` - Get current session ID
- `getUserId()` - Get current user ID

## Event Schema

All events include:
- `eventType` - Type of event
- `userId` - Unique user identifier (auto-generated)
- `sessionId` - Unique session identifier (auto-generated)
- `timestamp` - Unix timestamp in milliseconds
- `platform` - Platform identifier (ios, android, web, windows, mac, linux)
- `appVersion` - Application version

## Platform Adapters

### Using Custom Adapters

For full control, you can provide your own adapters:

```typescript
import { createTelemetryServiceWithAdapters, LocalStorageAdapter, WebPlatformAdapter } from '@gradepath/telemetry';

const service = createTelemetryServiceWithAdapters(
  new LocalStorageAdapter(),
  new WebPlatformAdapter(),
  { apiEndpoint: 'https://api.example.com' }
);
```

## Backend Integration

The package sends events to these endpoints:

- `POST /telemetry/event` - General events
- `POST /telemetry/screen-view` - Screen view events
- `POST /telemetry/performance` - Performance metrics

See the [backend documentation](../../backend/README.md) for setting up the NestJS + Kafka backend.

## License

MIT
