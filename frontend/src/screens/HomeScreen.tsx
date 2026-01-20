import React, { useEffect } from 'react';
import {
  View,
  Text,
  StyleSheet,
  TouchableOpacity,
  TextInput,
  ScrollView,
} from 'react-native';
import { useTelemetry } from '../hooks/useTelemetry';

const HomeScreen: React.FC = () => {
  const { trackButtonPress, trackInputChange } = useTelemetry('HomeScreen');

  useEffect(() => {
    const startTime = Date.now();

    return () => {
      const loadTime = Date.now() - startTime;
      TelemetryService.trackPerformance('home_screen_load_time', loadTime, 'ms');
    };
  }, []);

  const handleButtonClick = (buttonId: string) => {
    trackButtonPress(buttonId, { timestamp: Date.now() });
  };

  const handleTextChange = (fieldId: string, value: string) => {
    trackInputChange(fieldId, value);
  };

  return (
    <ScrollView style={styles.container}>
      <View style={styles.content}>
        <Text style={styles.title}>Welcome to GradePath</Text>
        <Text style={styles.subtitle}>
          User Telemetry Proof of Concept
        </Text>

        <View style={styles.section}>
          <Text style={styles.sectionTitle}>Test Telemetry Events</Text>

          <TouchableOpacity
            style={styles.button}
            onPress={() => handleButtonClick('primary_action')}
          >
            <Text style={styles.buttonText}>Primary Action</Text>
          </TouchableOpacity>

          <TouchableOpacity
            style={[styles.button, styles.secondaryButton]}
            onPress={() => handleButtonClick('secondary_action')}
          >
            <Text style={styles.buttonText}>Secondary Action</Text>
          </TouchableOpacity>

          <TouchableOpacity
            style={[styles.button, styles.tertiaryButton]}
            onPress={() => handleButtonClick('navigation_trigger')}
          >
            <Text style={styles.buttonText}>Navigate to Profile</Text>
          </TouchableOpacity>
        </View>

        <View style={styles.section}>
          <Text style={styles.sectionTitle}>Test Input Tracking</Text>

          <TextInput
            style={styles.input}
            placeholder="Enter your name"
            placeholderTextColor="#888"
            onChangeText={(value) => handleTextChange('name_field', value)}
          />

          <TextInput
            style={[styles.input, styles.textArea]}
            placeholder="Enter a message"
            placeholderTextColor="#888"
            multiline
            numberOfLines={4}
            onChangeText={(value) => handleTextChange('message_field', value)}
          />
        </View>

        <View style={styles.info}>
          <Text style={styles.infoText}>
            All interactions are tracked and sent to the backend API
          </Text>
          <Text style={styles.infoText}>
            Session ID: {TelemetryService.getSessionId()}
          </Text>
          <Text style={styles.infoText}>
            User ID: {TelemetryService.getUserId()}
          </Text>
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
    marginBottom: 8,
  },
  subtitle: {
    fontSize: 16,
    color: '#666',
    marginBottom: 30,
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
  button: {
    backgroundColor: '#007AFF',
    paddingVertical: 14,
    paddingHorizontal: 20,
    borderRadius: 8,
    marginBottom: 12,
    alignItems: 'center',
  },
  secondaryButton: {
    backgroundColor: '#5856D6',
  },
  tertiaryButton: {
    backgroundColor: '#34C759',
  },
  buttonText: {
    color: '#fff',
    fontSize: 16,
    fontWeight: '600',
  },
  input: {
    backgroundColor: '#fff',
    borderWidth: 1,
    borderColor: '#ddd',
    borderRadius: 8,
    padding: 12,
    fontSize: 16,
    marginBottom: 12,
    color: '#333',
  },
  textArea: {
    height: 100,
    textAlignVertical: 'top',
  },
  info: {
    marginTop: 20,
    padding: 16,
    backgroundColor: '#e8f4f8',
    borderRadius: 8,
    borderLeftWidth: 4,
    borderLeftColor: '#007AFF',
  },
  infoText: {
    fontSize: 12,
    color: '#555',
    marginBottom: 4,
  },
});

export default HomeScreen;
