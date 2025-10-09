-- ========================================
-- XÓA SẢN PHẨM VÀ DỮ LIỆU LIÊN QUAN
-- ========================================
-- Cách dùng: Chỉ cần thay đổi biến @product_ids bên dưới
-- Ví dụ: 
--   - Xóa 1 sản phẩm: SET @product_ids = '1';
--   - Xóa nhiều sản phẩm: SET @product_ids = '1,2,3,5,10';
-- ========================================

-- THAY ĐỔI BIẾN NÀY (danh sách ID cách nhau bởi dấu phẩy)
SET @product_ids = '1,2,3,4,5,6,7,8,9,10';

-- ========================================
-- KHÔNG CẦN SỬA GÌ Ở DƯỚI ĐÂY
-- ========================================

-- Tạo bảng tạm để lưu danh sách product_id
DROP TEMPORARY TABLE IF EXISTS temp_delete_products;
CREATE TEMPORARY TABLE temp_delete_products (product_id BIGINT);

-- Chèn các ID từ biến vào bảng tạm
SET @sql = CONCAT('INSERT INTO temp_delete_products (product_id) VALUES (', REPLACE(@product_ids, ',', '),('), ')');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Bắt đầu transaction
START TRANSACTION;

-- Xóa product_licenses (liên quan order_items)
DELETE FROM product_licenses 
WHERE order_item_id IN (
    SELECT order_item_id FROM order_items 
    WHERE product_id IN (SELECT product_id FROM temp_delete_products)
);

-- Xóa product_images
DELETE FROM product_images 
WHERE product_id IN (SELECT product_id FROM temp_delete_products);

-- Xóa product_reviews
DELETE FROM product_reviews 
WHERE product_id IN (SELECT product_id FROM temp_delete_products);

-- Xóa categories_products
DELETE FROM categories_products 
WHERE product_id IN (SELECT product_id FROM temp_delete_products);

-- Xóa shopping_cart
DELETE FROM shopping_cart 
WHERE product_id IN (SELECT product_id FROM temp_delete_products);

-- Xóa order_items
DELETE FROM order_items 
WHERE product_id IN (SELECT product_id FROM temp_delete_products);

-- Xóa sản phẩm chính
DELETE FROM products 
WHERE product_id IN (SELECT product_id FROM temp_delete_products);

-- Commit nếu thành công (hoặc ROLLBACK; nếu cần hủy)
COMMIT;

-- Dọn dẹp bảng tạm
DROP TEMPORARY TABLE IF EXISTS temp_delete_products;

SELECT 'Xóa sản phẩm thành công!' AS status;

