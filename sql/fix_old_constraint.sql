-- Script để fix constraint cũ một cách an toàn
USE wap;

-- Bước 1: Tìm và xóa tất cả foreign key constraints liên quan
SELECT 
    CONSTRAINT_NAME,
    CONSTRAINT_TYPE
FROM information_schema.table_constraints 
WHERE table_schema = 'wap' 
AND table_name = 'product_reviews' 
AND constraint_type = 'FOREIGN KEY';

-- Bước 2: Xóa constraint cũ bằng cách tạm thời disable foreign key checks
SET FOREIGN_KEY_CHECKS = 0;

-- Xóa constraint cũ
ALTER TABLE product_reviews DROP CONSTRAINT IF EXISTS ux_reviews_user_product;

-- Bật lại foreign key checks
SET FOREIGN_KEY_CHECKS = 1;

-- Bước 3: Thêm constraint mới (chỉ nếu chưa tồn tại)
SET @new_constraint_exists = (
    SELECT COUNT(*) 
    FROM information_schema.table_constraints 
    WHERE table_schema = 'wap' 
    AND table_name = 'product_reviews' 
    AND constraint_name = 'ux_reviews_order_item'
);

SET @sql = IF(@new_constraint_exists = 0, 
    'ALTER TABLE product_reviews ADD CONSTRAINT ux_reviews_order_item UNIQUE (order_item_id)', 
    'SELECT "Constraint ux_reviews_order_item already exists" as message'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SELECT 'Old constraint fixed successfully' as result;
