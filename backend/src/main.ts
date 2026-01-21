import { NestFactory } from '@nestjs/core';
import { ValidationPipe } from '@nestjs/common';
import { MicroserviceOptions, Transport } from '@nestjs/microservices';
import { AppModule } from './app.module';
import * as cookieParser from 'cookie-parser';

async function bootstrap() {
  const app = await NestFactory.create(AppModule);

  // Enable cookie parser for CSRF cookies
  app.use(cookieParser());

  // Enable validation
  app.useGlobalPipes(
    new ValidationPipe({
      whitelist: true,
      transform: true,
    }),
  );

  // Enable CORS for React Native
  app.enableCors({
    origin: ['http://localhost:5173', 'http://localhost:8083'],
    methods: 'GET,HEAD,PUT,PATCH,POST,DELETE,OPTIONS',
    credentials: true,
  });

  // Connect to Kafka as microservice (main telemetry consumer)
  const kafkaHost = process.env.KAFKA_BROKERS || 'localhost:9092';
  app.connectMicroservice<MicroserviceOptions>({
    transport: Transport.KAFKA,
    options: {
      client: {
        brokers: [kafkaHost],
      },
      consumer: {
        groupId: 'telemetry-consumer',
        allowAutoTopicCreation: true,
      },
      subscribe: {
        fromBeginning: false,
      },
      run: {
        autoCommit: true,
      },
    },
  });

  // Start both HTTP and microservice
  await app.startAllMicroservices();
  await app.listen(process.env.PORT || 3000);

  console.log(`Application is running on: ${await app.getUrl()}`);
  console.log(`Kafka broker: ${kafkaHost}`);
}
bootstrap();
