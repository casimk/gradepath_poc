import { Injectable, Logger, OnModuleInit, OnModuleDestroy } from '@nestjs/common';
import { MessagePattern, EventPattern, Payload, ClientKafka } from '@nestjs/microservices';
import { Inject } from '@nestjs/common';
import { TelemetryEventDto, ScreenViewDto, PerformanceMetricDto } from './dto/telemetry-event.dto';

@Injectable()
export class TelemetryService implements OnModuleInit, OnModuleDestroy {
  private readonly logger = new Logger(TelemetryService.name);

  constructor(
    @Inject('TELEMETRY_KAFKA') private readonly kafkaClient: ClientKafka,
  ) {
    this.logger.log('TelemetryService initialized');
  }

  async onModuleInit() {
    // Connect the Kafka producer
    await this.kafkaClient.connect();
    this.logger.log('Kafka producer connected');
  }

  async onModuleDestroy() {
    await this.kafkaClient.close();
  }

  async trackEvent(event: TelemetryEventDto) {
    const eventWithTimestamp = {
      ...event,
      timestamp: event.timestamp || Date.now(),
    };

    this.logger.log(`Event tracked: ${event.eventType} for user ${event.userId}`);

    // Publish to Kafka topic 'user-events'
    this.kafkaClient.emit('user-events', eventWithTimestamp);

    return {
      success: true,
      message: 'Event tracked successfully',
      event: eventWithTimestamp,
    };
  }

  async trackScreenView(screenView: ScreenViewDto) {
    const eventWithTimestamp = {
      ...screenView,
      timestamp: screenView.timestamp || Date.now(),
    };

    this.logger.log(`Screen view tracked: ${screenView.screenName} for user ${screenView.userId}`);

    // Publish to Kafka topic 'screen-views'
    this.kafkaClient.emit('screen-views', eventWithTimestamp);

    return {
      success: true,
      message: 'Screen view tracked successfully',
      event: eventWithTimestamp,
    };
  }

  async trackPerformance(metric: PerformanceMetricDto) {
    const eventWithTimestamp = {
      ...metric,
      timestamp: metric.timestamp || Date.now(),
    };

    this.logger.log(
      `Performance metric tracked: ${metric.metricName} = ${metric.value}${metric.unit}`,
    );

    // Publish to Kafka topic 'performance'
    this.kafkaClient.emit('performance', eventWithTimestamp);

    return {
      success: true,
      message: 'Performance metric tracked successfully',
      event: eventWithTimestamp,
    };
  }

  async getHealth() {
    return {
      status: 'ok',
      service: 'telemetry',
      timestamp: new Date().toISOString(),
    };
  }

  @MessagePattern('user-events')
  async handleUserEvent(@Payload() data: any) {
    this.logger.log(`Received user event from Kafka: ${JSON.stringify(data)}`);
    // Process the event - could store in database, aggregate, etc.
  }

  @MessagePattern('screen-views')
  async handleScreenView(@Payload() data: any) {
    this.logger.log(`Received screen view from Kafka: ${JSON.stringify(data)}`);
    // Process the screen view event
  }

  @MessagePattern('performance')
  async handlePerformance(@Payload() data: any) {
    this.logger.log(`Received performance metric from Kafka: ${JSON.stringify(data)}`);
    // Process the performance metric
  }
}
