import { IsString, IsNumber, IsOptional, IsEnum, IsArray } from 'class-validator';

export class ContentJourneyDto {
  @IsString()
  journeyId: string;

  @IsString()
  userId: string;

  @IsString()
  sessionId: string;

  @IsString()
  contentId: string;

  @IsString()
  contentType: string;

  @IsEnum(['started', 'completed', 'abandoned', 'revisited'])
  action: 'started' | 'completed' | 'abandoned' | 'revisited';

  @IsNumber()
  sequencePosition: number;

  @IsNumber()
  timeInContentSeconds: number;

  @IsString()
  @IsOptional()
  previousContentId?: string;

  @IsString()
  @IsOptional()
  nextContentId?: string;

  @IsArray()
  @IsString({ each: true })
  @IsOptional()
  topicTags?: string[];

  @IsString()
  @IsOptional()
  difficultyLevel?: string;

  @IsNumber()
  @IsOptional()
  timestamp?: number;
}

export class JourneyTrackResponseDto {
  success: boolean;
  tracked: boolean;
  journeyId: string;
  nextContent?: {
    contentId: string;
    contentType: string;
    confidence: number;
  };
}
