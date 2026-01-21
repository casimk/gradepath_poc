-- behavioral_profiles table for storing NestJS behavioral profile data
-- Stores real-time behavioral profiles received from NestJS backend via Kafka

CREATE TABLE IF NOT EXISTS behavioral_profiles (
    id BIGSERIAL PRIMARY KEY,
    user_id VARCHAR NOT NULL,
    profile_data JSONB NOT NULL,
    timestamp TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for efficient queries
CREATE INDEX IF NOT EXISTS idx_behavioral_user_id ON behavioral_profiles(user_id);
CREATE INDEX IF NOT EXISTS idx_behavioral_timestamp ON behavioral_profiles(timestamp);

-- Comment for documentation
COMMENT ON TABLE behavioral_profiles IS 'Stores behavioral profiles from NestJS backend for TikTok-style recommendations';
COMMENT ON COLUMN behavioral_profiles.user_id IS 'User ID (String format from NestJS)';
COMMENT ON COLUMN behavioral_profiles.profile_data IS 'JSON serialized BehavioralProfile with interests, engagement patterns, peak windows';
COMMENT ON COLUMN behavioral_profiles.timestamp IS 'Timestamp from NestJS when profile was generated';
