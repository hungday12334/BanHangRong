-- ========================================
-- MIGRATION: Thêm cột STATUS vào bảng PRODUCTS
-- ========================================
-- Chạy script này nếu database đã tồn tại với cột is_active
-- ========================================

USE wap;

-- Thêm cột status mới
ALTER TABLE products ADD COLUMN status VARCHAR(20) DEFAULT 'DRAFT';

-- Migrate data từ is_active sang status
UPDATE products SET status = 'PUBLIC' WHERE is_active = TRUE;
UPDATE products SET status = 'DRAFT' WHERE is_active = FALSE OR is_active IS NULL;

-- Xóa cột is_active cũ
ALTER TABLE products DROP COLUMN is_active;

-- Kiểm tra kết quả
SELECT product_id, name, status FROM products LIMIT 10;

