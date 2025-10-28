-- =============================================
-- FIX DATABASE: Thêm column status vào products (an toàn, idempotent)
-- =============================================

USE wap;

-- Đảm bảo column status tồn tại
SET @db := DATABASE();
SELECT COUNT(*) INTO @has_status
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'products' AND COLUMN_NAME = 'status';

SET @ddl := IF(@has_status = 0,
               'ALTER TABLE products ADD COLUMN status VARCHAR(15) NULL AFTER average_rating;',
               'SELECT "products.status already exists" AS msg');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Nếu có cột is_active thì map sang status, ngược lại set mặc định
SELECT COUNT(*) INTO @has_is_active
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'products' AND COLUMN_NAME = 'is_active';

SET @upd := IF(@has_is_active = 1,
  'UPDATE products \nSET status = CASE \n    WHEN is_active = TRUE THEN ''Public''\n    WHEN is_active = FALSE THEN ''Hidden''\n    ELSE ''Pending''\nEND\nWHERE status IS NULL OR status = '''';',
  'UPDATE products SET status = COALESCE(NULLIF(status, ''''), ''Public'') WHERE status IS NULL OR status = '''';'
);
PREPARE stmt2 FROM @upd;
EXECUTE stmt2;
DEALLOCATE PREPARE stmt2;

-- Kiểm tra kết quả
SELECT product_id, name, status
FROM products
LIMIT 10;

SELECT 'Database đã được cập nhật thành công!' AS message;
