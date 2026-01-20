# GradePath User Telemetry PoC

A proof of concept for real-time user telemetry collection using React Native, NestJS, Apache Kafka, and Docker.

## Architecture

```
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│  React Native   │────▶│     NestJS      │────▶│   Apache Kafka  │
│    Frontend     │     │      API        │     │                 │
└─────────────────┘     └─────────────────┘     └─────────────────┘
                               │
                               ▼
                        ┌─────────────────┐
                        │  Consumer/      │
                        │  Processor      │
                        └─────────────────┘
```

## Components

- **Frontend**: React Native mobile application with telemetry tracking
- **Backend**: NestJS API for receiving and processing telemetry events
- **Message Broker**: Apache Kafka for event streaming
- **Containerization**: Docker Compose for local development

## Getting Started

### Prerequisites

- Node.js 18+
- Docker & Docker Compose
- React Native CLI (for iOS/Android development)

### Quick Start

```bash
# Start infrastructure (Kafka, Zookeeper, Kafka UI)
npm run docker:up

# Start backend API
npm run backend

# Start React Native app (in new terminal)
npm run frontend
```

### Access Points

- **Backend API**: http://localhost:3000
- **Kafka UI**: http://localhost:8080
- **Kafka Broker**: localhost:9092

## Telemetry Events

Events are streamed to Kafka topics:

- `user-events` - General user interaction events
- `screen-views` - Screen navigation and view time
- `performance` - App performance metrics

## Development

See individual directories for more details:
- [Backend](./backend/README.md)
- [Frontend](./frontend/README.md)
