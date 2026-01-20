# Telemetry Frontend

React Native application with built-in user telemetry tracking.

## Features

- Automatic session tracking
- Screen view tracking with duration
- Event batching and queuing
- Performance metrics
- Background event flushing

## Development Setup

```bash
# Install dependencies
npm install

# For iOS
cd ios && pod install && cd ..

# Start Metro bundler
npm start

# Run on iOS
npm run ios

# Run on Android
npm run android
```

## Telemetry Usage

### Automatic Tracking

Screen views are automatically tracked when using the `useTelemetry` hook:

```typescript
import { useTelemetry } from '../hooks/useTelemetry';

const MyScreen = () => {
  const { trackButtonPress, trackInputChange } = useTelemetry('MyScreen');

  return (
    <TouchableOpacity onPress={() => trackButtonPress('my_button')}>
      <Text>Click me</Text>
    </TouchableOpacity>
  );
};
```

### Manual Tracking

```typescript
import TelemetryService from '../services/TelemetryService';

// Track custom event
await TelemetryService.trackEvent('custom_event', { key: 'value' }, 'ScreenName');

// Track performance metric
await TelemetryService.trackPerformance('operation_time', 250, 'ms');
```

## Configuration

Configure the telemetry service by modifying the initialization in `TelemetryService.ts`:

```typescript
new TelemetryService({
  apiEndpoint: 'http://localhost:3000',
  batchSize: 10,        // Events per batch
  flushInterval: 30000, // ms between flushes
  enabled: true,
});
```

## Environment Variables

- `API_ENDPOINT` - Backend API URL (default: http://localhost:3000)
