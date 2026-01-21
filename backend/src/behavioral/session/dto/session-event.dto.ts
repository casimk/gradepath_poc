import { IsString, IsNumber, IsOptional, IsEnum } from 'class-validator';

export class SessionEventDto {
  @IsEnum(['session_start', 'session_end', 'session_resume'])
  eventType: 'session_start' | 'session_end' | 'session_resume';

  @IsString()
  userId: string;

  @IsString()
  sessionId: string;

  @IsNumber()
  startTime: number;

  @IsNumber()
  @IsOptional()
  endTime?: number;

  @IsNumber()
  @IsOptional()
  durationSeconds?: number;

  @IsNumber()
  @IsOptional()
  contentCount?: number;

  @IsString()
  @IsOptional()
  deviceType?: string;

  @IsNumber()
  @IsOptional()
  timezoneOffset?: number;

  @IsString()
  @IsOptional()
  previousSessionId?: string;

  @IsNumber()
  @IsOptional()
  timestamp?: number;
}
