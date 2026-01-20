import { useEffect } from 'react';
import TelemetryService from '../services/TelemetryService';
import type { UseTelemetryResult } from '@gradepath/telemetry';

export const useTelemetry = (screenName: string): UseTelemetryResult => {
  useEffect(() => {
    TelemetryService.trackScreenView(screenName);

    return () => {
      TelemetryService.trackScreenEnd(screenName);
    };
  }, [screenName]);

  const trackEvent = async (
    eventType: string,
    metadata?: Record<string, any>,
  ) => {
    await TelemetryService.trackEvent(eventType, metadata, screenName);
  };

  const trackButtonPress = async (buttonId: string, metadata?: Record<string, any>) => {
    await trackEvent('button_press', { buttonId, ...metadata });
  };

  const trackInputChange = async (fieldId: string, value: any) => {
    await trackEvent('input_change', { fieldId, valueType: typeof value });
  };

  return {
    trackEvent,
    trackButtonPress,
    trackInputChange,
  };
};
