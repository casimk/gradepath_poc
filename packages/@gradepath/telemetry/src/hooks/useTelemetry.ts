import { useEffect, useCallback } from 'react';
import { TelemetryService } from '../core/TelemetryService';

/**
 * Result type for the useTelemetry hook
 */
export interface UseTelemetryResult {
  /** Track a custom event */
  trackEvent: (eventType: string, metadata?: Record<string, any>) => Promise<void>;
  /** Track a button press event */
  trackButtonPress: (buttonId: string, metadata?: Record<string, any>) => Promise<void>;
  /** Track an input change event */
  trackInputChange: (fieldId: string, value: any) => Promise<void>;
}

/**
 * React hook for automatic telemetry tracking
 *
 * Automatically tracks screen views when the component mounts/unmounts
 * and provides helper functions for tracking common user interactions.
 *
 * @param telemetryService - The TelemetryService instance
 * @param screenName - The name of the current screen/component
 *
 * @example
 * ```tsx
 * import { useTelemetry } from '@gradepath/telemetry';
 * import telemetryService from './telemetryService';
 *
 * function HomeScreen() {
 *   const { trackButtonPress } = useTelemetry(telemetryService, 'HomeScreen');
 *
 *   return (
 *     <button onClick={() => trackButtonPress('submit')}>
 *       Submit
 *     </button>
 *   );
 * }
 * ```
 */
export function useTelemetry(
  telemetryService: TelemetryService,
  screenName: string,
): UseTelemetryResult {
  useEffect(() => {
    telemetryService.trackScreenView(screenName);

    return () => {
      telemetryService.trackScreenEnd(screenName);
    };
  }, [telemetryService, screenName]);

  const trackEvent = useCallback(
    async (eventType: string, metadata?: Record<string, any>) => {
      await telemetryService.trackEvent(eventType, metadata, screenName);
    },
    [telemetryService, screenName],
  );

  const trackButtonPress = useCallback(
    async (buttonId: string, metadata?: Record<string, any>) => {
      await trackEvent('button_press', { buttonId, ...metadata });
    },
    [trackEvent],
  );

  const trackInputChange = useCallback(
    async (fieldId: string, value: any) => {
      await trackEvent('input_change', { fieldId, valueType: typeof value });
    },
    [trackEvent],
  );

  return {
    trackEvent,
    trackButtonPress,
    trackInputChange,
  };
}
