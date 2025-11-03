-- Add was_public flag to mark if a product has ever been public
ALTER TABLE products ADD COLUMN IF NOT EXISTS was_public TINYINT(1) NOT NULL DEFAULT 0;
-- Backfill for currently public products
UPDATE products SET was_public = 1 WHERE LOWER(status) = 'public';
