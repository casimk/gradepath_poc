import { Test, TestingModule } from '@nestjs/testing';
import { TelemetryService } from './telemetry.service';
import { ClientKafka } from '@nestjs/microservices';

describe('TelemetryService', () => {
  let service: TelemetryService;
  let kafkaClient: ClientKafka;

  beforeEach(async () => {
    const module: TestingModule = await Test.createTestingModule({
      providers: [
        TelemetryService,
        {
          provide: 'TELEMETRY_KAFKA',
          useValue: {
            connect: jest.fn().mockResolvedValue(undefined),
            close: jest.fn().mockResolvedValue(undefined),
            emit: jest.fn().mockReturnValue({
              subscribe: jest.fn(),
            }),
          },
        },
      ],
    }).compile();

    service = module.get<TelemetryService>(TelemetryService);
    kafkaClient = module.get<ClientKafka>('TELEMETRY_KAFKA');
  });

  it('should be defined', () => {
    expect(service).toBeDefined();
  });

  it('should track event and emit to Kafka', async () => {
    const event = {
      userId: 'user-123',
      eventType: 'button_click',
      sessionId: 'session-456',
    };

    const result = await service.trackEvent(event as any);

    expect(result.success).toBe(true);
    expect(kafkaClient.emit).toHaveBeenCalledWith('user-events', expect.objectContaining(event));
  });

  it('should track screen view and emit to Kafka', async () => {
    const screenView = {
      userId: 'user-123',
      screenName: 'HomeScreen',
      sessionId: 'session-456',
    };

    const result = await service.trackScreenView(screenView as any);

    expect(result.success).toBe(true);
    expect(kafkaClient.emit).toHaveBeenCalledWith('screen-views', expect.objectContaining(screenView));
  });
});
