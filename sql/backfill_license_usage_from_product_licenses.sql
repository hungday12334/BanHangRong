-- Backfill activation_date and last_used_date from product_licenses into license_usage_logs
-- This will insert 'activated' and 'used' events if they do not already exist.

-- Insert activation events
INSERT INTO license_usage_logs (license_id, event_time, action, user_id, ip, device, meta, created_at)
SELECT pl.license_id, pl.activation_date, 'activated', pl.user_id, NULL, pl.device_identifier, NULL, NOW()
FROM product_licenses pl
WHERE pl.activation_date IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM license_usage_logs l
    WHERE l.license_id = pl.license_id AND l.event_time = pl.activation_date AND l.action = 'activated'
  );

-- Insert last-used events
INSERT INTO license_usage_logs (license_id, event_time, action, user_id, ip, device, meta, created_at)
SELECT pl.license_id, pl.last_used_date, 'used', pl.user_id, NULL, pl.device_identifier, NULL, NOW()
FROM product_licenses pl
WHERE pl.last_used_date IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM license_usage_logs l
    WHERE l.license_id = pl.license_id AND l.event_time = pl.last_used_date AND l.action = 'used'
  );

-- Add index if not exists (safe to run multiple times in many DBs)
CREATE INDEX IF NOT EXISTS idx_lul_license_time ON license_usage_logs(license_id, event_time);
