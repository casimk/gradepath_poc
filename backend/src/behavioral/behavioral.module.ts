import { Module } from '@nestjs/common';
import { ClientsModule, Transport } from '@nestjs/microservices';
import { SessionController } from './session/session.controller';
import { SessionService } from './session/session.service';
import { SessionCacheService } from './redis/session-cache.service';

/**
 * Behavioral Module - I/O Layer Only
 *
 * This module handles ONLY I/O operations:
 * - HTTP endpoints for session and journey tracking
 * - Redis session state management
 * - Kafka emission of raw events (to be processed by Java Spring Modulith)
 *
 * All CPU-intensive profiling logic has been moved to:
 * content-service/src/main/java/com/gradepath/content/profiling/
 */
@Module({
  imports: [
    ClientsModule.register([
      {
        name: 'BEHAVIORAL_KAFKA',
        transport: Transport.KAFKA,
        options: {
          client: {
            clientId: 'behavioral-service',
            brokers: [process.env.KAFKA_BROKERS || 'localhost:9092'],
          },
          consumer: {
            groupId: 'behavioral-consumer',
          },
        },
      },
    ]),
  ],
  controllers: [SessionController],
  providers: [
    SessionService,
    SessionCacheService,
  ],
  exports: [SessionService, SessionCacheService],
})
export class BehavioralModule {}
