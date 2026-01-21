import { Controller, Post, Get, Body, Param, HttpCode, HttpStatus } from '@nestjs/common';
import { ApiTags, ApiOperation, ApiResponse } from '@nestjs/swagger';
import { SessionService } from './session.service';
import { SessionEventDto, ContentJourneyDto } from './dto';

@ApiTags('behavioral')
@Controller('behavioral')
export class SessionController {
  constructor(private readonly sessionService: SessionService) {}

  @Post('session/start')
  @ApiOperation({ summary: 'Start a new behavioral tracking session' })
  @ApiResponse({ status: 201, description: 'Session started successfully' })
  @HttpCode(HttpStatus.CREATED)
  async startSession(@Body() event: SessionEventDto) {
    return this.sessionService.startSession(event);
  }

  @Post('session/end')
  @ApiOperation({ summary: 'End a behavioral tracking session' })
  @ApiResponse({ status: 200, description: 'Session ended successfully' })
  async endSession(@Body() event: SessionEventDto) {
    return this.sessionService.endSession(event);
  }

  @Post('session/resume')
  @ApiOperation({ summary: 'Resume an existing session' })
  @ApiResponse({ status: 200, description: 'Session resumed successfully' })
  async resumeSession(@Body() event: SessionEventDto) {
    return this.sessionService.resumeSession(event);
  }

  @Post('journey/track')
  @ApiOperation({ summary: 'Track content consumption journey' })
  @ApiResponse({ status: 201, description: 'Journey tracked successfully' })
  @HttpCode(HttpStatus.CREATED)
  async trackJourney(@Body() journey: ContentJourneyDto) {
    return this.sessionService.trackContentJourney(journey);
  }

  @Get('session/:id')
  @ApiOperation({ summary: 'Get session details' })
  @ApiResponse({ status: 200, description: 'Session details retrieved' })
  async getSession(@Param('id') sessionId: string) {
    return this.sessionService.getSession(sessionId);
  }

  @Get('sessions/:userId')
  @ApiOperation({ summary: "Get user's session history" })
  @ApiResponse({ status: 200, description: 'Session history retrieved' })
  async getUserSessions(@Param('userId') userId: string) {
    return this.sessionService.getUserSessions(userId);
  }
}
