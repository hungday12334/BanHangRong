-- =============================================
-- FIX DATABASE: Thêm column status vào products
-- =============================================

USE wap;

-- Thêm column status (MySQL 5.7+ hỗ trợ IF NOT EXISTS)
ALTER TABLE products ADD COLUMN status VARCHAR(15) AFTER average_rating;

-- Cập nhật giá trị mặc định cho các sản phẩm đã có
-- Nếu is_active = TRUE thì set status = 'Public'
-- Nếu is_active = FALSE thì set status = 'Hidden'
UPDATE products 
SET status = CASE 
    WHEN is_active = TRUE THEN 'Public'
    WHEN is_active = FALSE THEN 'Hidden'
    ELSE 'Pending'
END
WHERE status IS NULL;

-- Kiểm tra kết quả
SELECT product_id, name, status, is_active 
FROM products 
LIMIT 10;

SELECT 'Database đã được cập nhật thành công!' AS message;

