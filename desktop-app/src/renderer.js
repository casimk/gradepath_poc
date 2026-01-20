const { ipcRenderer } = require('electron');

// Mock the telemetry package for the renderer process
// In a real app, you'd use webpack to bundle the package

let count = 0;
const countDisplay = document.getElementById('count');
const incrementButton = document.getElementById('increment');
const platformDisplay = document.getElementById('platform');
const sessionIdDisplay = document.getElementById('sessionId');

// Get platform info
const platform = process.platform === 'win32' ? 'windows' :
                 process.platform === 'darwin' ? 'mac' : 'linux';
platformDisplay.textContent = platform;

// Generate a session ID
const sessionId = 'session-' + Date.now() + '-' + Math.random().toString(36).substr(2, 9);
sessionIdDisplay.textContent = sessionId;

// Track events (simplified - in real app, use the actual package)
async function trackEvent(eventType, metadata = {}) {
  const event = {
    eventType,
    userId: 'user-' + platform, // simplified
    sessionId,
    timestamp: Date.now(),
    platform,
    appVersion: '1.0.0',
    metadata,
  };

  console.log('[Telemetry] Event:', event);

  // Send to backend
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

// Track initial page load
trackEvent('app_loaded', { platform });

// Button click handler
incrementButton.addEventListener('click', () => {
  count++;
  countDisplay.textContent = count;
  trackEvent('button_press', { buttonId: 'increment', newCount: count });
});

// Track app close
window.addEventListener('beforeunload', () => {
  trackEvent('app_closed', { duration: Date.now() - window.performance.timing.navigationStart });
});
