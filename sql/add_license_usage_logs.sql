-- Migration: create license usage logs table
-- Run this on your database to store detailed license usage events
CREATE TABLE IF NOT EXISTS license_usage_logs (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  license_id BIGINT NOT NULL,
  event_time DATETIME NOT NULL,
  action VARCHAR(64) NOT NULL,
  user_id BIGINT DEFAULT NULL,
  ip VARCHAR(64) DEFAULT NULL,
  device VARCHAR(255) DEFAULT NULL,
  meta TEXT DEFAULT NULL,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_lul_license FOREIGN KEY (license_id) REFERENCES product_licenses(license_id) ON DELETE CASCADE
);

-- Recommended index to quickly look up logs per license
CREATE INDEX idx_lul_license_time ON license_usage_logs(license_id, event_time);
