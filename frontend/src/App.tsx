import React, { useEffect, useState } from 'react';
import { StatusBar } from 'react-native';
import AppNavigator from './navigation/AppNavigator';
import TelemetryService from './services/TelemetryService';

const App: React.FC = () => {
  const [isReady, setIsReady] = useState(false);

  useEffect(() => {
    const initializeTelemetry = async () => {
      try {
        await TelemetryService.initialize();
        setIsReady(true);

        // Track app launch
        await TelemetryService.trackEvent('app_launch', {
          timestamp: Date.now(),
        });

        // Track app launch performance
        await TelemetryService.trackPerformance('app_startup_time', 0, 'ms', {
          context: 'app_initialization',
        });
      } catch (error) {
        console.error('Failed to initialize telemetry:', error);
        setIsReady(true);
      }
    };

    initializeTelemetry();

    return () => {
      TelemetryService.destroy();
    };
  }, []);

  if (!isReady) {
    return null;
  }

  return (
    <>
      <StatusBar barStyle="light-content" />
      <AppNavigator />
    </>
  );
};

export default App;
