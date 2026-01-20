# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a **User Telemetry Proof of Concept** that demonstrates real-time event collection from a React Native mobile app, streamed through a NestJS API to Apache Kafka. The system uses Docker Compose for containerized local development.

### Architecture

```
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│  React Native   │────▶│     NestJS      │────▶│   Apache Kafka  │
│    Frontend     │     │      API        │     │                 │
└─────────────────┘     └─────────────────┘     └─────────────────┘
                               │
                               ▼
                        ┌─────────────────┐
                        │  Kafka Consumer │
                        │  (in Service)   │
                        └─────────────────┘
```

## Development Commands

### Root (Monorepo)

```bash
# Start all infrastructure (Kafka, Zookeeper, Kafka UI)
npm run docker:up

# Stop all infrastructure
npm run docker:down

# View Docker logs
npm run docker:logs

# Start backend API
npm run backend

# Start frontend (from project root, uses workspace)
npm run frontend
```

### Backend (NestJS)

Located in `./backend/`

```bash
cd backend

# Install dependencies
npm install

# Development server with hot reload
npm run start:dev

# Run tests
npm run test
npm run test:cov

# Lint code
npm run lint

# Build for production
npm run build
```

**Backend runs on port 3000 by default.**

### Frontend (React Native)

Located in `./frontend/`

```bash
cd frontend

# Install dependencies
npm install

# iOS only: Install CocoaPods dependencies
cd ios && pod install && cd ..

# Start Metro bundler
npm start

# Run on iOS simulator
npm run ios

# Run on Android emulator
npm run android

# Type check
npm run typecheck

# Run tests
npm run test
```

### Docker Services

- **Kafka UI**: http://localhost:8080 - Visual interface for Kafka topics/consumers
- **Kafka Broker**: localhost:9092 - For direct Kafka connections
- **Backend API**: http://localhost:3000 - Also accessible via Docker network

## Code Architecture

### Backend Structure (`backend/src/`)

- **`main.ts`** - Application entry point, initializes both HTTP server and Kafka microservice
- **`app.module.ts`** - Root module importing ConfigModule and TelemetryModule
- **`telemetry/`** - Core telemetry functionality
  - **`telemetry.controller.ts`** - REST API endpoints (`POST /telemetry/event`, `/screen-view`, `/performance`)
  - **`telemetry.service.ts`** - Business logic with `@EventPattern` decorators for Kafka consumption
  - **`dto/telemetry-event.dto.ts`** - Request validation schemas (TelemetryEventDto, ScreenViewDto, PerformanceMetricDto)

**Key Pattern**: The backend operates as a hybrid HTTP/Microservice - REST endpoints receive events and can publish to Kafka, while the service also consumes from Kafka topics (`user-events`, `screen-views`, `performance`).

### Frontend Structure (`frontend/src/`)

- **`App.tsx`** - Entry point, initializes TelemetryService on mount
- **`services/TelemetryService.ts`** - Singleton service managing event queue, batching, and persistence
- **`hooks/useTelemetry.ts`** - React hook for automatic screen view tracking
- **`navigation/AppNavigator.tsx`** - React Navigation stack with HomeScreen and ProfileScreen
- **`screens/`** - Screen components using the `useTelemetry` hook

**Key Pattern**: TelemetryService batches events in AsyncStorage and flushes them periodically or when batch size is reached. Screen views are tracked automatically on mount/unmount using the `useTelemetry` hook.

## Telemetry Event Flow

1. **Frontend**: User interacts with UI → `useTelemetry` hook → `TelemetryService.trackEvent()`
2. **Queue**: Event added to in-memory queue + persisted to AsyncStorage
3. **Flush**: On batch size or interval, events sent via HTTP POST to backend
4. **Backend**: REST endpoint receives event → can publish to Kafka topic
5. **Kafka**: Events stored in topics (`user-events`, `screen-views`, `performance`)
6. **Consumer**: Backend service consumes from Kafka via `@EventPattern` decorators

## Environment Variables

### Backend
- `PORT` - Server port (default: 3000)
- `KAFKA_BROKERS` - Kafka addresses (default: localhost:9092)
- `NODE_ENV` - development/production

### Frontend
- `API_ENDPOINT` - Backend API URL (default: http://localhost:3000)

## Testing

Run tests from respective directories:

```bash
# Backend tests
cd backend && npm run test

# Frontend tests
cd frontend && npm run test
```

## Important Notes

- The backend connects to Kafka as a **microservice** (using `@nestjs/microservices`) in addition to the REST API
- Frontend uses **AsyncStorage** to persist events between app launches
- Kafka UI is available for debugging topics at http://localhost:8080
- All React Native screens should use the `useTelemetry` hook for automatic tracking
- TelemetryService is a singleton - import directly from `@services/TelemetryService`
