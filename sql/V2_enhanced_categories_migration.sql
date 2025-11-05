-- =====================================================
-- ENHANCED CATEGORY MANAGEMENT - DATABASE MIGRATION
-- =====================================================
-- Description: Add new fields to categories table for enhanced functionality
-- Author: GitHub Copilot
-- Date: November 5, 2025
-- Version: 2.0
-- =====================================================

-- Add new columns to categories table
ALTER TABLE categories
ADD COLUMN slug VARCHAR(100) COMMENT 'URL-friendly category name',
ADD COLUMN parent_id BIGINT COMMENT 'Parent category ID for subcategories (NULL = root)',
ADD COLUMN icon VARCHAR(50) COMMENT 'Icon class name (e.g., ti-device-laptop)',
ADD COLUMN image_url VARCHAR(255) COMMENT 'Category image URL',
ADD COLUMN sort_order INT DEFAULT 0 COMMENT 'Display order (lower numbers first)',
ADD COLUMN status VARCHAR(20) DEFAULT 'ACTIVE' COMMENT 'Category status: ACTIVE, HIDDEN, DRAFT',
ADD COLUMN featured BOOLEAN DEFAULT FALSE COMMENT 'Featured on homepage';

-- Add indexes for better performance
CREATE INDEX idx_categories_slug ON categories(slug);
CREATE INDEX idx_categories_parent ON categories(parent_id);
CREATE INDEX idx_categories_status ON categories(status);
CREATE INDEX idx_categories_featured ON categories(featured);
CREATE INDEX idx_categories_sort_order ON categories(sort_order);

-- Add foreign key constraint for parent_id (optional, for data integrity)
-- Uncomment if you want strict referential integrity
-- ALTER TABLE categories
-- ADD CONSTRAINT fk_categories_parent
-- FOREIGN KEY (parent_id) REFERENCES categories(category_id)
-- ON DELETE SET NULL;

-- Update existing categories with default values
UPDATE categories
SET
    slug = LOWER(REPLACE(REPLACE(REPLACE(name, ' ', '-'), '--', '-'), '---', '-')),
    status = 'ACTIVE',
    sort_order = 0,
    featured = FALSE
WHERE slug IS NULL;

-- =====================================================
-- VALIDATION QUERIES (Run these to verify migration)
-- =====================================================

-- Check if new columns exist
SELECT
    COLUMN_NAME,
    DATA_TYPE,
    IS_NULLABLE,
    COLUMN_DEFAULT,
    COLUMN_COMMENT
FROM
    INFORMATION_SCHEMA.COLUMNS
WHERE
    TABLE_NAME = 'categories'
    AND COLUMN_NAME IN ('slug', 'parent_id', 'icon', 'image_url', 'sort_order', 'status', 'featured')
ORDER BY
    ORDINAL_POSITION;

-- Check if indexes were created
SHOW INDEX FROM categories
WHERE Key_name LIKE 'idx_categories_%';

-- Count categories by status
SELECT status, COUNT(*) as count
FROM categories
GROUP BY status;

-- Show sample data with new fields
SELECT
    category_id,
    name,
    slug,
    parent_id,
    icon,
    status,
    featured,
    sort_order,
    created_at
FROM categories
LIMIT 10;

-- =====================================================
-- ROLLBACK SCRIPT (Use in case of issues)
-- =====================================================

-- WARNING: This will remove all new columns and their data!
-- Only run this if you need to rollback the migration

/*
-- Drop indexes first
DROP INDEX idx_categories_slug ON categories;
DROP INDEX idx_categories_parent ON categories;
DROP INDEX idx_categories_status ON categories;
DROP INDEX idx_categories_featured ON categories;
DROP INDEX idx_categories_sort_order ON categories;

-- Drop foreign key if created
-- ALTER TABLE categories DROP FOREIGN KEY fk_categories_parent;

-- Drop columns
ALTER TABLE categories
DROP COLUMN slug,
DROP COLUMN parent_id,
DROP COLUMN icon,
DROP COLUMN image_url,
DROP COLUMN sort_order,
DROP COLUMN status,
DROP COLUMN featured;
*/

-- =====================================================
-- END OF MIGRATION SCRIPT
-- =====================================================

-- Success message
SELECT 'Migration completed successfully! ✅' AS message;
SELECT 'New fields added: slug, parent_id, icon, image_url, sort_order, status, featured' AS fields_added;
SELECT 'Indexes created: 5 indexes for performance optimization' AS indexes_info;
SELECT 'Run validation queries above to verify the migration.' AS next_step;

