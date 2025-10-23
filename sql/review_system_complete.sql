-- =====================================================
-- COMPLETE REVIEW SYSTEM DATABASE SCRIPT
-- Tất cả các phần database liên quan đến review
-- =====================================================

USE wap;

-- =====================================================
-- 1. TẠO BẢNG PRODUCT_REVIEWS (nếu chưa có)
-- =====================================================
CREATE TABLE IF NOT EXISTS product_reviews (
    review_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    rating INT NOT NULL CHECK (rating >= 1 AND rating <= 5),
    comment TEXT,
    media_urls TEXT,
    service_rating INT CHECK (service_rating >= 1 AND service_rating <= 5),
    order_item_id BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    -- Foreign keys
    FOREIGN KEY (product_id) REFERENCES products(product_id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (order_item_id) REFERENCES order_items(order_item_id) ON DELETE CASCADE,
    
    -- Indexes
    INDEX idx_reviews_product (product_id),
    INDEX idx_reviews_user (user_id),
    INDEX idx_reviews_order_item (order_item_id),
    INDEX idx_reviews_created (created_at),
    INDEX idx_reviews_product_created (product_id, created_at),
    INDEX idx_reviews_rating (rating),
    INDEX idx_reviews_service_rating (service_rating)
);

-- =====================================================
-- 2. XÓA CÁC CONSTRAINT CŨ (nếu có)
-- =====================================================

-- Xóa foreign key constraints cũ
SET @fk_exists = (
    SELECT COUNT(*) 
    FROM information_schema.table_constraints 
    WHERE table_schema = 'wap' 
    AND table_name = 'product_reviews' 
    AND constraint_name LIKE '%reviews%'
    AND constraint_type = 'FOREIGN KEY'
);

SET @sql = IF(@fk_exists > 0, 
    'ALTER TABLE product_reviews DROP FOREIGN KEY fk_reviews_order_item', 
    'SELECT "No foreign key constraints to drop" as message'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Xóa unique constraint cũ
SET @constraint_exists = (
    SELECT COUNT(*) 
    FROM information_schema.table_constraints 
    WHERE table_schema = 'wap' 
    AND table_name = 'product_reviews' 
    AND constraint_name = 'ux_reviews_user_product'
);

SET @sql = IF(@constraint_exists > 0, 
    'ALTER TABLE product_reviews DROP CONSTRAINT ux_reviews_user_product', 
    'SELECT "Old constraint ux_reviews_user_product does not exist" as message'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- =====================================================
-- 3. THÊM CỘT ORDER_ITEM_ID (nếu chưa có)
-- =====================================================
SET @column_exists = (
    SELECT COUNT(*) 
    FROM information_schema.columns 
    WHERE table_schema = 'wap' 
    AND table_name = 'product_reviews' 
    AND column_name = 'order_item_id'
);

SET @sql = IF(@column_exists = 0, 
    'ALTER TABLE product_reviews ADD COLUMN order_item_id BIGINT', 
    'SELECT "Column order_item_id already exists" as message'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- =====================================================
-- 4. THÊM CÁC CONSTRAINT MỚI
-- =====================================================

-- Constraint mới: mỗi order_item chỉ có thể có một review
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

-- Foreign key constraint cho order_item_id
SET @fk_new_exists = (
    SELECT COUNT(*) 
    FROM information_schema.table_constraints 
    WHERE table_schema = 'wap' 
    AND table_name = 'product_reviews' 
    AND constraint_name = 'fk_reviews_order_item'
);

SET @sql = IF(@fk_new_exists = 0, 
    'ALTER TABLE product_reviews ADD CONSTRAINT fk_reviews_order_item FOREIGN KEY (order_item_id) REFERENCES order_items(order_item_id)', 
    'SELECT "Foreign key fk_reviews_order_item already exists" as message'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- =====================================================
-- 5. THÊM CÁC INDEX TỐI ƯU
-- =====================================================

-- Index cho tìm kiếm review theo product
SET @index1_exists = (
    SELECT COUNT(*) 
    FROM information_schema.statistics 
    WHERE table_schema = 'wap' 
    AND table_name = 'product_reviews' 
    AND index_name = 'idx_reviews_product_rating'
);

SET @sql = IF(@index1_exists = 0, 
    'CREATE INDEX idx_reviews_product_rating ON product_reviews(product_id, rating)', 
    'SELECT "Index idx_reviews_product_rating already exists" as message'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Index cho tìm kiếm review theo user
SET @index2_exists = (
    SELECT COUNT(*) 
    FROM information_schema.statistics 
    WHERE table_schema = 'wap' 
    AND table_name = 'product_reviews' 
    AND index_name = 'idx_reviews_user_created'
);

SET @sql = IF(@index2_exists = 0, 
    'CREATE INDEX idx_reviews_user_created ON product_reviews(user_id, created_at)', 
    'SELECT "Index idx_reviews_user_created already exists" as message'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- =====================================================
-- 6. TẠO VIEW CHO THỐNG KÊ REVIEW
-- =====================================================

-- View tổng hợp rating của sản phẩm
CREATE OR REPLACE VIEW product_rating_summary AS
SELECT 
    p.product_id,
    p.name as product_name,
    COUNT(pr.review_id) as total_reviews,
    AVG(pr.rating) as average_rating,
    AVG(pr.service_rating) as average_service_rating,
    COUNT(CASE WHEN pr.rating = 5 THEN 1 END) as five_star_count,
    COUNT(CASE WHEN pr.rating = 4 THEN 1 END) as four_star_count,
    COUNT(CASE WHEN pr.rating = 3 THEN 1 END) as three_star_count,
    COUNT(CASE WHEN pr.rating = 2 THEN 1 END) as two_star_count,
    COUNT(CASE WHEN pr.rating = 1 THEN 1 END) as one_star_count
FROM products p
LEFT JOIN product_reviews pr ON p.product_id = pr.product_id
GROUP BY p.product_id, p.name;

-- =====================================================
-- 7. TẠO STORED PROCEDURE CHO TÍNH TOÁN RATING
-- =====================================================

DELIMITER //

CREATE OR REPLACE PROCEDURE UpdateProductAverageRating(IN product_id_param BIGINT)
BEGIN
    DECLARE avg_rating DECIMAL(3,2);
    DECLARE total_reviews INT;
    
    -- Tính toán rating trung bình
    SELECT 
        COALESCE(AVG(rating), 0),
        COUNT(*)
    INTO avg_rating, total_reviews
    FROM product_reviews 
    WHERE product_id = product_id_param;
    
    -- Cập nhật bảng products
    UPDATE products 
    SET 
        average_rating = avg_rating,
        total_reviews = total_reviews,
        updated_at = CURRENT_TIMESTAMP
    WHERE product_id = product_id_param;
    
END //

DELIMITER ;

-- =====================================================
-- 8. TẠO TRIGGER TỰ ĐỘNG CẬP NHẬT RATING
-- =====================================================

DELIMITER //

CREATE OR REPLACE TRIGGER tr_review_after_insert
AFTER INSERT ON product_reviews
FOR EACH ROW
BEGIN
    CALL UpdateProductAverageRating(NEW.product_id);
END //

CREATE OR REPLACE TRIGGER tr_review_after_update
AFTER UPDATE ON product_reviews
FOR EACH ROW
BEGIN
    CALL UpdateProductAverageRating(NEW.product_id);
    -- Nếu product_id thay đổi, cập nhật cả product cũ
    IF OLD.product_id != NEW.product_id THEN
        CALL UpdateProductAverageRating(OLD.product_id);
    END IF;
END //

CREATE OR REPLACE TRIGGER tr_review_after_delete
AFTER DELETE ON product_reviews
FOR EACH ROW
BEGIN
    CALL UpdateProductAverageRating(OLD.product_id);
END //

DELIMITER ;

-- =====================================================
-- 9. THÊM CỘT RATING VÀO BẢNG PRODUCTS (nếu chưa có)
-- =====================================================

SET @rating_column_exists = (
    SELECT COUNT(*) 
    FROM information_schema.columns 
    WHERE table_schema = 'wap' 
    AND table_name = 'products' 
    AND column_name = 'average_rating'
);

SET @sql = IF(@rating_column_exists = 0, 
    'ALTER TABLE products ADD COLUMN average_rating DECIMAL(3,2) DEFAULT 0.00', 
    'SELECT "Column average_rating already exists" as message'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @total_reviews_column_exists = (
    SELECT COUNT(*) 
    FROM information_schema.columns 
    WHERE table_schema = 'wap' 
    AND table_name = 'products' 
    AND column_name = 'total_reviews'
);

SET @sql = IF(@total_reviews_column_exists = 0, 
    'ALTER TABLE products ADD COLUMN total_reviews INT DEFAULT 0', 
    'SELECT "Column total_reviews already exists" as message'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- =====================================================
-- 10. CẬP NHẬT RATING CHO TẤT CẢ SẢN PHẨM HIỆN TẠI
-- =====================================================

-- Cập nhật rating cho tất cả sản phẩm
UPDATE products p
SET 
    average_rating = COALESCE((
        SELECT AVG(pr.rating) 
        FROM product_reviews pr 
        WHERE pr.product_id = p.product_id
    ), 0.00),
    total_reviews = COALESCE((
        SELECT COUNT(*) 
        FROM product_reviews pr 
        WHERE pr.product_id = p.product_id
    ), 0);

-- =====================================================
-- 11. TẠO SAMPLE DATA (nếu cần)
-- =====================================================

-- Thêm một số review mẫu (chỉ khi chưa có data)
INSERT IGNORE INTO product_reviews (
    product_id, 
    user_id, 
    rating, 
    comment, 
    service_rating, 
    order_item_id, 
    created_at
) VALUES 
(1, 1, 5, 'Sản phẩm rất tốt, giao hàng nhanh!', 5, 1, NOW()),
(1, 2, 4, 'Chất lượng ổn, giá hợp lý', 4, 2, NOW()),
(2, 1, 5, 'Tuyệt vời! Sẽ mua lại', 5, 3, NOW()),
(2, 3, 3, 'Bình thường', 3, 4, NOW());

-- =====================================================
-- 12. TẠO FUNCTION HELPER
-- =====================================================

DELIMITER //

-- Function tính rating trung bình
CREATE OR REPLACE FUNCTION GetProductAverageRating(product_id_param BIGINT)
RETURNS DECIMAL(3,2)
READS SQL DATA
DETERMINISTIC
BEGIN
    DECLARE avg_rating DECIMAL(3,2) DEFAULT 0.00;
    
    SELECT COALESCE(AVG(rating), 0.00)
    INTO avg_rating
    FROM product_reviews 
    WHERE product_id = product_id_param;
    
    RETURN avg_rating;
END //

-- Function đếm số review
CREATE OR REPLACE FUNCTION GetProductReviewCount(product_id_param BIGINT)
RETURNS INT
READS SQL DATA
DETERMINISTIC
BEGIN
    DECLARE review_count INT DEFAULT 0;
    
    SELECT COUNT(*)
    INTO review_count
    FROM product_reviews 
    WHERE product_id = product_id_param;
    
    RETURN review_count;
END //

DELIMITER ;

-- =====================================================
-- HOÀN THÀNH
-- =====================================================

SELECT 'Review system database setup completed successfully!' as result;
SELECT 'All tables, constraints, indexes, triggers, and procedures have been created/updated' as status;
