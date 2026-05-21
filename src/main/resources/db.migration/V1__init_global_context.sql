CREATE TABLE IF NOT EXISTS project_context (
    project_id VARCHAR(64) PRIMARY KEY,
    project_name VARCHAR(200) NOT NULL,
    base_path VARCHAR(500),
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS context_entry (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id VARCHAR(64) NOT NULL REFERENCES project_context(project_id) ON DELETE CASCADE,
    category VARCHAR(50) NOT NULL, -- 'architecture', 'glossary', 'endpoints', 'test_data', 'openapi', 'team_conventions'
    context_key VARCHAR(128) NOT NULL,
    content TEXT NOT NULL,
    content_hash VARCHAR(64) NOT NULL, -- для детекта изменений
    version INTEGER DEFAULT 1,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,

    UNIQUE(project_id, category, context_key)
);

CREATE INDEX IF NOT EXISTS idx_ctx_project_cat ON context_entry(project_id, category);
CREATE INDEX IF NOT EXISTS idx_ctx_content_hash ON context_entry(content_hash);

-- Функция авто-обновления updated_at
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_project_context_updated_at
    BEFORE UPDATE ON project_context
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_context_entry_updated_at
    BEFORE UPDATE ON context_entry
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();