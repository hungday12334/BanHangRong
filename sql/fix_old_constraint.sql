-- Script để fix constraint cũ một cách an toàn (MySQL-compatible)
USE wap;

-- Bước 1: Tham khảo constraint hiện có (thông tin)
SELECT
    CONSTRAINT_NAME,
    CONSTRAINT_TYPE
FROM information_schema.table_constraints 
WHERE table_schema = 'wap' 
AND table_name = 'product_reviews';

-- Bước 2: Xóa UNIQUE INDEX cũ 'ux_reviews_user_product' nếu tồn tại (đúng cú pháp MySQL)
SET @db := DATABASE();
SELECT COUNT(*) INTO @old_idx_exists
FROM INFORMATION_SCHEMA.STATISTICS
WHERE TABLE_SCHEMA = @db
  AND TABLE_NAME = 'product_reviews'
  AND INDEX_NAME = 'ux_reviews_user_product';

SET @drop_sql := IF(@old_idx_exists > 0,
    'ALTER TABLE product_reviews DROP INDEX ux_reviews_user_product',
    'SELECT "Index ux_reviews_user_product not found" AS msg'
);
PREPARE stmt FROM @drop_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Bước 3: Đảm bảo cột order_item_id tồn tại trước khi thêm UNIQUE
SELECT COUNT(*) INTO @has_order_item_id
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = @db
  AND TABLE_NAME = 'product_reviews'
  AND COLUMN_NAME = 'order_item_id';

SET @add_col := IF(@has_order_item_id = 0,
    'ALTER TABLE product_reviews ADD COLUMN order_item_id BIGINT NULL',
    'SELECT "Column order_item_id already exists" AS msg'
);
PREPARE stmt2 FROM @add_col;
EXECUTE stmt2;
DEALLOCATE PREPARE stmt2;

-- Bước 4: Thêm UNIQUE INDEX mới cho order_item_id nếu chưa tồn tại
SELECT COUNT(*) INTO @new_idx_exists
FROM INFORMATION_SCHEMA.STATISTICS
WHERE TABLE_SCHEMA = @db
  AND TABLE_NAME = 'product_reviews'
  AND INDEX_NAME = 'ux_reviews_order_item';

SET @add_unique := IF(@new_idx_exists = 0,
    'ALTER TABLE product_reviews ADD CONSTRAINT ux_reviews_order_item UNIQUE (order_item_id)',
    'SELECT "Constraint/Index ux_reviews_order_item already exists" AS message'
);
PREPARE stmt3 FROM @add_unique;
EXECUTE stmt3;
DEALLOCATE PREPARE stmt3;

SELECT 'Old constraint fixed successfully' AS result;
