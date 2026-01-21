import { Module } from '@nestjs/common';
import { ConfigModule } from '@nestjs/config';
import { TelemetryModule } from './telemetry/telemetry.module';
import { BehavioralModule } from './behavioral/behavioral.module';

@Module({
  imports: [
    ConfigModule.forRoot({
      isGlobal: true,
    }),
    TelemetryModule,
    BehavioralModule,
  ],
  controllers: [],
  providers: [],
})
export class AppModule {}
