import { useState } from 'react';
import reactLogo from './assets/react.svg';
import viteLogo from '/vite.svg';
import './App.css';

// Simple web-only telemetry implementation for testing
const simpleTelemetryService = {
  sessionId: 'session-' + Date.now(),
  userId: 'user-' + Math.random().toString(36).substr(2, 9),
  initialized: false,

  async initialize() {
    this.initialized = true;
    console.log('[Telemetry] Initialized', { userId: this.userId, sessionId: this.sessionId });
  },

  async trackEvent(eventType, metadata = {}) {
    if (!this.initialized) return;

    const event = {
      eventType,
      userId: this.userId,
      sessionId: this.sessionId,
      timestamp: Date.now(),
      platform: 'web',
      appVersion: '1.0.0',
      metadata,
    };

    console.log('[Telemetry] Event:', event);

    try {
      const response = await fetch('http://localhost:3000/telemetry/event', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(event),
      });

      if (response.ok) {
        console.log('[Telemetry] Event sent successfully');
      }
    } catch (error) {
      console.error('[Telemetry] Failed to send event:', error);
    }
  }
};

// Initialize telemetry
simpleTelemetryService.initialize();

function App() {
  const [count, setCount] = useState(0);

  const handleCountIncrement = async () => {
    setCount((c) => c + 1);
    await simpleTelemetryService.trackEvent('button_press', { buttonId: 'increment_counter', newCount: count + 1 });
  };

  return (
    <>
      <div>
        <a href="https://vite.dev" target="_blank" rel="noreferrer">
          <img src={viteLogo} className="logo" alt="Vite logo" />
        </a>
        <a href="https://react.dev" target="_blank" rel="noreferrer">
          <img src={reactLogo} className="logo react" alt="React logo" />
        </a>
      </div>
      <h1>Vite + React + Telemetry</h1>
      <div className="card">
        <button onClick={handleCountIncrement}>
          count is {count}
        </button>
        <p>
          This web app uses telemetry tracking.
          Click the button to see events in the console and Kafka UI!
        </p>
        <p style={{ fontSize: '0.9em', color: '#666' }}>
          Check the console for [Telemetry] logs and Kafka UI at{' '}
          <a href="http://localhost:8080" target="_blank" rel="noreferrer">
            localhost:8080
          </a>
        </p>
      </div>
      <p className="read-the-docs">
        Click on the Vite and React logos to learn more
      </p>
    </>
  );
}

export default App;
