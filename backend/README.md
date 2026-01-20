# Telemetry Backend

NestJS API for receiving and processing user telemetry events.

## Development

```bash
# Install dependencies
npm install

# Start development server (with hot reload)
npm run start:dev

# Run tests
npm run test
npm run test:cov

# Lint code
npm run lint
```

## API Endpoints

### Track Event
```bash
POST /telemetry/event
Content-Type: application/json

{
  "eventType": "button_click",
  "userId": "user-123",
  "sessionId": "session-abc",
  "screenName": "HomeScreen",
  "metadata": {
    "buttonId": "submit-button"
  }
}
```

### Track Screen View
```bash
POST /telemetry/screen-view
Content-Type: application/json

{
  "screenName": "HomeScreen",
  "userId": "user-123",
  "sessionId": "session-abc",
  "duration": 5000,
  "properties": {
    "referrer": "LoginScreen"
  }
}
```

### Track Performance Metric
```bash
POST /telemetry/performance
Content-Type: application/json

{
  "metricName": "app_start_time",
  "userId": "user-123",
  "sessionId": "session-abc",
  "value": 1200,
  "unit": "ms"
}
```

## Environment Variables

- `PORT` - Server port (default: 3000)
- `KAFKA_BROKERS` - Kafka broker addresses (default: localhost:9092)
- `NODE_ENV` - Environment (development/production)
