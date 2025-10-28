-- =============================================
-- FIX USERS TABLE: Add missing full_name column if not exists
-- =============================================

USE wap;

-- Conditionally add the column only if it's missing
SET @db := DATABASE();
SELECT COUNT(*) INTO @col_exists
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = @db
  AND TABLE_NAME = 'users'
  AND COLUMN_NAME = 'full_name';

SET @ddl := IF(@col_exists = 0,
               'ALTER TABLE users ADD COLUMN full_name VARCHAR(100) NULL AFTER username;',
               'SELECT "Column full_name already exists" AS message;');

PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Verify
SELECT COLUMN_NAME
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'users'
ORDER BY ORDINAL_POSITION;
