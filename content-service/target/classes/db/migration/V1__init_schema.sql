-- Content catalog table
CREATE TABLE IF NOT EXISTS content (
    id VARCHAR(255) PRIMARY KEY,
    type VARCHAR(50) NOT NULL CHECK (type IN ('LESSON', 'VIDEO', 'ARTICLE', 'EXERCISE', 'QUIZ')),
    title VARCHAR(500) NOT NULL,
    description TEXT,
    difficulty_level INTEGER CHECK (difficulty_level BETWEEN 1 AND 5),
    estimated_duration_minutes INTEGER,
    topics JSONB,
    tags JSONB,
    prerequisites JSONB,
    metadata JSONB,
    status VARCHAR(20) DEFAULT 'DRAFT' CHECK (status IN ('DRAFT', 'PUBLISHED', 'ARCHIVED')),
    published_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

-- Video chapters table
CREATE TABLE IF NOT EXISTS video_chapters (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    video_id VARCHAR(255) REFERENCES content(id) ON DELETE CASCADE,
    chapter_number INTEGER NOT NULL,
    title VARCHAR(500),
    start_time_seconds INTEGER NOT NULL,
    end_time_seconds INTEGER NOT NULL
);

-- Users table
CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    external_user_id VARCHAR(255) UNIQUE NOT NULL,
    email VARCHAR(255),
    display_name VARCHAR(200),
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

-- User profiles table
CREATE TABLE IF NOT EXISTS user_profiles (
    user_id UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    interests JSONB,
    learning_goals JSONB,
    preferred_learning_style VARCHAR(50),
    language VARCHAR(10) DEFAULT 'en'
);

-- User preferences table
CREATE TABLE IF NOT EXISTS user_preferences (
    user_id UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    difficulty_preference INTEGER CHECK (difficulty_preference BETWEEN 1 AND 5),
    content_type_preferences JSONB,
    topic_preferences JSONB,
    daily_time_target_minutes INTEGER DEFAULT 30
);

-- Skill levels table
CREATE TABLE IF NOT EXISTS skill_levels (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    topic VARCHAR(100) NOT NULL,
    level INTEGER NOT NULL CHECK (level BETWEEN 1 AND 100),
    confidence_score DECIMAL(5,2) CHECK (confidence_score BETWEEN 0 AND 1),
    last_assessed_at TIMESTAMPTZ,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id, topic)
);

-- Content interactions table
CREATE TABLE IF NOT EXISTS content_interactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    content_id VARCHAR(255) REFERENCES content(id) ON DELETE CASCADE,
    interaction_type VARCHAR(50) NOT NULL,
    timestamp TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    session_id VARCHAR(255),
    metadata JSONB
);

-- Learning history table
CREATE TABLE IF NOT EXISTS learning_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    content_id VARCHAR(255) REFERENCES content(id) ON DELETE CASCADE,
    started_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMPTZ,
    time_spent_seconds INTEGER,
    score INTEGER,
    passed BOOLEAN
);

-- Daily engagement metrics table
CREATE TABLE IF NOT EXISTS engagement_metrics (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    date DATE NOT NULL,
    total_time_minutes INTEGER DEFAULT 0,
    content_viewed INTEGER DEFAULT 0,
    content_completed INTEGER DEFAULT 0,
    UNIQUE(user_id, date)
);

-- Recommendations table
CREATE TABLE IF NOT EXISTS recommendations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    content_id VARCHAR(255) REFERENCES content(id) ON DELETE CASCADE,
    score DECIMAL(10,4) NOT NULL,
    algorithm VARCHAR(100),
    reason TEXT,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    shown_at TIMESTAMPTZ,
    clicked_at TIMESTAMPTZ
);

-- Indexes for content
CREATE INDEX IF NOT EXISTS idx_content_type_status ON content(type, status);
CREATE INDEX IF NOT EXISTS idx_content_difficulty ON content(difficulty_level);
CREATE INDEX IF NOT EXISTS idx_content_topics ON content USING GIN(topics);
CREATE INDEX IF NOT EXISTS idx_content_tags ON content USING GIN(tags);

-- Indexes for interactions
CREATE INDEX IF NOT EXISTS idx_interactions_user_time ON content_interactions(user_id, timestamp DESC);
CREATE INDEX IF NOT EXISTS idx_interactions_content ON content_interactions(content_id);

-- Indexes for learning history
CREATE INDEX IF NOT EXISTS idx_learning_history_user ON learning_history(user_id, completed_at DESC);

-- Indexes for recommendations
CREATE INDEX IF NOT EXISTS idx_recommendations_user_score ON recommendations(user_id, score DESC);

-- Indexes for skill levels
CREATE INDEX IF NOT EXISTS idx_skill_levels_user_topic ON skill_levels(user_id, topic);
