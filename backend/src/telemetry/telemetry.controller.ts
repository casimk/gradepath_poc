import { Controller, Post, Body, Get } from '@nestjs/common';
import { ApiTags, ApiOperation, ApiResponse } from '@nestjs/swagger';
import { TelemetryEventDto, ScreenViewDto, PerformanceMetricDto } from './dto/telemetry-event.dto';
import { TelemetryService } from './telemetry.service';

@ApiTags('telemetry')
@Controller('telemetry')
export class TelemetryController {
  constructor(private readonly telemetryService: TelemetryService) {}

  @Post('event')
  @ApiOperation({ summary: 'Track a generic telemetry event' })
  @ApiResponse({ status: 201, description: 'Event tracked successfully' })
  async trackEvent(@Body() event: TelemetryEventDto) {
    return this.telemetryService.trackEvent(event);
  }

  @Post('screen-view')
  @ApiOperation({ summary: 'Track a screen view event' })
  @ApiResponse({ status: 201, description: 'Screen view tracked successfully' })
  async trackScreenView(@Body() screenView: ScreenViewDto) {
    return this.telemetryService.trackScreenView(screenView);
  }

  @Post('performance')
  @ApiOperation({ summary: 'Track a performance metric' })
  @ApiResponse({ status: 201, description: 'Performance metric tracked successfully' })
  async trackPerformance(@Body() metric: PerformanceMetricDto) {
    return this.telemetryService.trackPerformance(metric);
  }

  @Get('health')
  @ApiOperation({ summary: 'Check telemetry service health' })
  @ApiResponse({ status: 200, description: 'Service is healthy' })
  async getHealth() {
    return this.telemetryService.getHealth();
  }
}
