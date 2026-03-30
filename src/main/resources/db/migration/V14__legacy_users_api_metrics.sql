-- Flyway V14: metrics table for deprecated /api/users usage tracking

CREATE TABLE IF NOT EXISTS legacy_users_api_metrics (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    hit_date DATE NOT NULL,
    endpoint_path VARCHAR(120) NOT NULL,
    http_method VARCHAR(10) NOT NULL,
    app_version VARCHAR(40) NOT NULL,
    hit_count BIGINT NOT NULL DEFAULT 0,
    first_hit_at DATETIME NOT NULL,
    last_hit_at DATETIME NOT NULL,
    CONSTRAINT uk_legacy_users_metric UNIQUE (hit_date, endpoint_path, http_method, app_version)
);

CREATE INDEX idx_legacy_users_metric_date ON legacy_users_api_metrics(hit_date);
CREATE INDEX idx_legacy_users_metric_version ON legacy_users_api_metrics(app_version);
