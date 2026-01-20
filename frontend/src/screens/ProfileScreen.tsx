import React, { useState, useEffect } from 'react';
import {
  View,
  Text,
  StyleSheet,
  TouchableOpacity,
  ScrollView,
  ActivityIndicator,
} from 'react-native';
import { useTelemetry } from '../hooks/useTelemetry';

interface HealthStatus {
  status: string;
  service: string;
  timestamp: string;
}

const ProfileScreen: React.FC = () => {
  const { trackButtonPress } = useTelemetry('ProfileScreen');
  const [healthStatus, setHealthStatus] = useState<HealthStatus | null>(null);
  const [loading, setLoading] = useState(false);

  const checkBackendHealth = async () => {
    trackButtonPress('health_check');
    setLoading(true);

    try {
      const response = await fetch('http://localhost:3000/telemetry/health');
      const data = await response.json();
      setHealthStatus(data);
    } catch (error) {
      console.error('Health check failed:', error);
      setHealthStatus({
        status: 'error',
        service: 'telemetry',
        timestamp: new Date().toISOString(),
      });
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    checkBackendHealth();
  }, []);

  return (
    <ScrollView style={styles.container}>
      <View style={styles.content}>
        <Text style={styles.title}>Profile & Debug</Text>

        <View style={styles.section}>
          <Text style={styles.sectionTitle}>Telemetry Status</Text>

          <View style={styles.statusCard}>
            <Text style={styles.statusLabel}>Backend Service:</Text>
            <Text style={[
              styles.statusValue,
              { color: healthStatus?.status === 'ok' ? '#34C759' : '#FF3B30' }
            ]}>
              {healthStatus?.status || 'Checking...'}
            </Text>

            {healthStatus && (
              <Text style={styles.timestamp}>
                Last checked: {new Date(healthStatus.timestamp).toLocaleTimeString()}
              </Text>
            )}
          </View>

          <TouchableOpacity
            style={styles.refreshButton}
            onPress={checkBackendHealth}
            disabled={loading}
          >
            {loading ? (
              <ActivityIndicator color="#fff" />
            ) : (
              <Text style={styles.buttonText}>Refresh Status</Text>
            )}
          </TouchableOpacity>
        </View>

        <View style={styles.section}>
          <Text style={styles.sectionTitle}>Session Information</Text>

          <View style={styles.infoCard}>
            <Text style={styles.infoLabel}>Session ID:</Text>
            <Text style={styles.infoValue}>
              {TelemetryService.getSessionId() || 'Not initialized'}
            </Text>
          </View>

          <View style={styles.infoCard}>
            <Text style={styles.infoLabel}>User ID:</Text>
            <Text style={styles.infoValue}>
              {TelemetryService.getUserId() || 'Not initialized'}
            </Text>
          </View>
        </View>

        <View style={styles.section}>
          <Text style={styles.sectionTitle}>Event Tracking Test</Text>

          <TouchableOpacity
            style={styles.testButton}
            onPress={() => trackButtonPress('test_event_1')}
          >
            <Text style={styles.buttonText}>Send Test Event 1</Text>
          </TouchableOpacity>

          <TouchableOpacity
            style={styles.testButton}
            onPress={() => trackButtonPress('test_event_2', { customData: 'test' })}
          >
            <Text style={styles.buttonText}>Send Test Event 2 (with data)</Text>
          </TouchableOpacity>

          <TouchableOpacity
            style={styles.testButton}
            onPress={() => trackButtonPress('test_event_3', {
              timestamp: Date.now(),
              nested: { value: 123, label: 'test' }
            })}
          >
            <Text style={styles.buttonText}>Send Test Event 3 (nested data)</Text>
          </TouchableOpacity>
        </View>
      </View>
    </ScrollView>
  );
};

import TelemetryService from '../services/TelemetryService';

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#f5f5f5',
  },
  content: {
    padding: 20,
  },
  title: {
    fontSize: 28,
    fontWeight: 'bold',
    color: '#333',
    marginBottom: 24,
  },
  section: {
    marginBottom: 24,
  },
  sectionTitle: {
    fontSize: 18,
    fontWeight: '600',
    color: '#444',
    marginBottom: 12,
  },
  statusCard: {
    backgroundColor: '#fff',
    padding: 16,
    borderRadius: 8,
    marginBottom: 12,
    borderWidth: 1,
    borderColor: '#ddd',
  },
  statusLabel: {
    fontSize: 14,
    color: '#666',
    marginBottom: 4,
  },
  statusValue: {
    fontSize: 20,
    fontWeight: 'bold',
    marginBottom: 8,
  },
  timestamp: {
    fontSize: 12,
    color: '#888',
  },
  refreshButton: {
    backgroundColor: '#007AFF',
    paddingVertical: 14,
    borderRadius: 8,
    alignItems: 'center',
  },
  testButton: {
    backgroundColor: '#5856D6',
    paddingVertical: 12,
    borderRadius: 8,
    alignItems: 'center',
    marginBottom: 8,
  },
  buttonText: {
    color: '#fff',
    fontSize: 16,
    fontWeight: '600',
  },
  infoCard: {
    backgroundColor: '#fff',
    padding: 16,
    borderRadius: 8,
    marginBottom: 8,
    borderWidth: 1,
    borderColor: '#ddd',
  },
  infoLabel: {
    fontSize: 14,
    color: '#666',
    marginBottom: 4,
  },
  infoValue: {
    fontSize: 14,
    color: '#333',
    fontFamily: 'monospace',
  },
});

export default ProfileScreen;
