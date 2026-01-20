import { IsString, IsNumber, IsObject, IsDateString, IsOptional } from 'class-validator';

export class TelemetryEventDto {
  @IsString()
  eventType: string;

  @IsString()
  userId: string;

  @IsString()
  sessionId: string;

  @IsString()
  @IsOptional()
  screenName?: string;

  @IsObject()
  @IsOptional()
  metadata?: Record<string, any>;

  @IsNumber()
  @IsOptional()
  timestamp?: number;

  @IsString()
  @IsOptional()
  platform?: string;

  @IsString()
  @IsOptional()
  appVersion?: string;
}

export class ScreenViewDto {
  @IsString()
  screenName: string;

  @IsString()
  userId: string;

  @IsString()
  sessionId: string;

  @IsNumber()
  duration?: number;

  @IsObject()
  @IsOptional()
  properties?: Record<string, any>;

  @IsNumber()
  @IsOptional()
  timestamp?: number;
}

export class PerformanceMetricDto {
  @IsString()
  metricName: string;

  @IsString()
  userId: string;

  @IsString()
  sessionId: string;

  @IsNumber()
  value: number;

  @IsString()
  unit: string;

  @IsObject()
  @IsOptional()
  context?: Record<string, any>;

  @IsNumber()
  @IsOptional()
  timestamp?: number;
}
