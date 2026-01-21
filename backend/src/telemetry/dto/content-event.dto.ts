import { IsString, IsNumber, IsObject, IsOptional, IsBoolean, IsEnum } from 'class-validator';

export class ContentEventDto {
  @IsString()
  eventType: 'content_viewed' | 'content_completed' | 'content_reaction' | 'assessment_completed';

  @IsString()
  userId: string;

  @IsString()
  sessionId: string;

  @IsString()
  contentId: string;

  @IsString()
  contentType: string;

  @IsNumber()
  @IsOptional()
  timeSpentSeconds?: number;

  @IsNumber()
  @IsOptional()
  score?: number;

  @IsBoolean()
  @IsOptional()
  passed?: boolean;

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

  // Behavioral tracking fields for journey analysis
  @IsString()
  @IsOptional()
  previousContentId?: string;

  @IsNumber()
  @IsOptional()
  sessionSequence?: number;
}
