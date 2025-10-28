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
    -- NOTE: Bỏ FK order_item_id ở phần CREATE TABLE để tránh trùng lặp, sẽ thêm có điều kiện ở dưới

    -- Indexes
    INDEX idx_reviews_product (product_id),
    INDEX idx_reviews_user (user_id),
    INDEX idx_reviews_order_item (order_item_id),
    INDEX idx_reviews_created (created_at),
    INDEX idx_reviews_product_created (product_id, created_at),
    INDEX idx_reviews_rating (rating),
    INDEX idx_reviews_service_rating (service_rating)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =====================================================
-- 2. XÓA CÁC CONSTRAINT CŨ (nếu có)
-- =====================================================

-- Xóa foreign key cụ thể fk_reviews_order_item nếu tồn tại
SET @db := DATABASE();

-- Drop FK named fk_reviews_order_item if exists
SELECT COUNT(*) INTO @fk_exists1
FROM information_schema.table_constraints
WHERE table_schema = @db
  AND table_name = 'product_reviews'
  AND constraint_name = 'fk_reviews_order_item'
  AND constraint_type = 'FOREIGN KEY';
SET @sql := IF(@fk_exists1 > 0,
    'ALTER TABLE product_reviews DROP FOREIGN KEY fk_reviews_order_item',
    'SELECT "fk_reviews_order_item not found" as message'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Drop FK named fk_product_reviews_order_item if exists
SELECT COUNT(*) INTO @fk_exists2
FROM information_schema.table_constraints
WHERE table_schema = @db
  AND table_name = 'product_reviews'
  AND constraint_name = 'fk_product_reviews_order_item'
  AND constraint_type = 'FOREIGN KEY';
SET @sql := IF(@fk_exists2 > 0,
    'ALTER TABLE product_reviews DROP FOREIGN KEY fk_product_reviews_order_item',
    'SELECT "fk_product_reviews_order_item not found" as message'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Xóa unique constraint cũ (MySQL: unique là index) 'ux_reviews_user_product' nếu tồn tại
SELECT COUNT(*) INTO @ux_exists
FROM information_schema.statistics
WHERE table_schema = @db
  AND table_name = 'product_reviews'
  AND index_name = 'ux_reviews_user_product';

SET @sql := IF(@ux_exists > 0,
    'ALTER TABLE product_reviews DROP INDEX ux_reviews_user_product',
    'SELECT "Index ux_reviews_user_product does not exist" as message'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- =====================================================
-- 3. THÊM CỘT ORDER_ITEM_ID (nếu chưa có)
-- =====================================================
SELECT COUNT(*) INTO @column_exists
FROM information_schema.columns
WHERE table_schema = @db
  AND table_name = 'product_reviews'
  AND column_name = 'order_item_id';

SET @sql := IF(@column_exists = 0,
    'ALTER TABLE product_reviews ADD COLUMN order_item_id BIGINT',
    'SELECT "Column order_item_id already exists" as message'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- =====================================================
-- 3.1 THÊM CÁC CỘT BỔ SUNG CHO PRODUCT_REVIEWS (nếu thiếu)
-- Đảm bảo có cột service_rating và media_urls trước khi tạo VIEW tham chiếu
SELECT COUNT(*) INTO @has_service_rating
FROM information_schema.columns
WHERE table_schema = @db AND table_name = 'product_reviews' AND column_name = 'service_rating';
SET @sql := IF(@has_service_rating = 0,
    'ALTER TABLE product_reviews ADD COLUMN service_rating INT NULL AFTER comment',
    'SELECT "Column service_rating already exists" as message');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SELECT COUNT(*) INTO @has_media_urls
FROM information_schema.columns
WHERE table_schema = @db AND table_name = 'product_reviews' AND column_name = 'media_urls';
SET @sql := IF(@has_media_urls = 0,
    'ALTER TABLE product_reviews ADD COLUMN media_urls TEXT NULL AFTER comment',
    'SELECT "Column media_urls already exists" as message');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Thêm index cho service_rating nếu thiếu
SELECT COUNT(*) INTO @has_idx_sr
FROM information_schema.statistics
WHERE table_schema = @db AND table_name = 'product_reviews' AND index_name = 'idx_reviews_service_rating';
SET @sql := IF(@has_idx_sr = 0,
    'CREATE INDEX idx_reviews_service_rating ON product_reviews(service_rating)',
    'SELECT "Index idx_reviews_service_rating already exists" as message');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- =====================================================
-- 4. THÊM CÁC CONSTRAINT MỚI
-- =====================================================

-- Constraint mới: mỗi order_item chỉ có thể có một review
SELECT COUNT(*) INTO @new_constraint_exists
FROM information_schema.statistics
WHERE table_schema = @db
  AND table_name = 'product_reviews'
  AND index_name = 'ux_reviews_order_item';

SET @sql := IF(@new_constraint_exists = 0,
    'ALTER TABLE product_reviews ADD CONSTRAINT ux_reviews_order_item UNIQUE (order_item_id)',
    'SELECT "Constraint/Index ux_reviews_order_item already exists" as message'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Foreign key constraint cho order_item_id
-- 4.a Làm sạch dữ liệu không hợp lệ để tránh lỗi 150
UPDATE product_reviews pr
LEFT JOIN order_items oi ON pr.order_item_id = oi.order_item_id
SET pr.order_item_id = NULL
WHERE pr.order_item_id IS NOT NULL AND oi.order_item_id IS NULL;

-- 4.a.1 Đảm bảo cả 2 bảng dùng InnoDB
SET @engine_pr := (SELECT ENGINE FROM information_schema.tables WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'product_reviews');
SET @fix_engine_pr := IF(@engine_pr <> 'InnoDB', 'ALTER TABLE product_reviews ENGINE=InnoDB', 'SELECT "product_reviews already InnoDB" AS msg');
PREPARE stmt FROM @fix_engine_pr; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @engine_oi := (SELECT ENGINE FROM information_schema.tables WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'order_items');
SET @fix_engine_oi := IF(@engine_oi <> 'InnoDB', 'ALTER TABLE order_items ENGINE=InnoDB', 'SELECT "order_items already InnoDB" AS msg');
PREPARE stmt FROM @fix_engine_oi; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 4.a.2 Đồng bộ kiểu dữ liệu giữa cột con và cha (match exactly COLUMN_TYPE)
SELECT COLUMN_TYPE INTO @parent_coltype
FROM information_schema.columns
WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'order_items' AND COLUMN_NAME = 'order_item_id';
SELECT COLUMN_TYPE INTO @child_coltype
FROM information_schema.columns
WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'product_reviews' AND COLUMN_NAME = 'order_item_id';

SET @needs_alter := (@parent_coltype <> @child_coltype);
SET @alter_child_exact := IF(@needs_alter,
    CONCAT('ALTER TABLE product_reviews MODIFY order_item_id ', @parent_coltype, ' NULL'),
    'SELECT "order_item_id type already matches parent" AS msg');
PREPARE stmt FROM @alter_child_exact; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 4.a.3 Đảm bảo không còn orphan sau cleanup
SELECT COUNT(*) INTO @orphans
FROM product_reviews pr
LEFT JOIN order_items oi ON pr.order_item_id = oi.order_item_id
WHERE pr.order_item_id IS NOT NULL AND oi.order_item_id IS NULL;

-- 4.b Xóa bất kỳ FK nào hiện tại trên order_item_id (dù tên gì)
SELECT CONSTRAINT_NAME INTO @fk_existing_name
FROM information_schema.KEY_COLUMN_USAGE
WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'product_reviews' AND COLUMN_NAME = 'order_item_id' AND REFERENCED_TABLE_NAME IS NOT NULL
LIMIT 1;
SET @drop_fk := IF(@fk_existing_name IS NOT NULL, CONCAT('ALTER TABLE product_reviews DROP FOREIGN KEY ', @fk_existing_name), 'SELECT "no existing FK on order_item_id" AS msg');
PREPARE stmt FROM @drop_fk; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 4.c Chỉ thêm FK nếu chưa có bất kỳ FK nào trên order_item_id tham chiếu order_items
SELECT COUNT(*) INTO @fk_new_exists
FROM information_schema.KEY_COLUMN_USAGE
WHERE TABLE_SCHEMA = @db
  AND TABLE_NAME = 'product_reviews'
  AND COLUMN_NAME = 'order_item_id'
  AND REFERENCED_TABLE_NAME = 'order_items';

-- Kiểm tra sự tồn tại của bảng/column parent
SELECT COUNT(*) INTO @has_order_items_table
FROM information_schema.tables
WHERE table_schema = @db AND table_name = 'order_items';
SELECT COUNT(*) INTO @has_parent_col
FROM information_schema.columns
WHERE table_schema = @db AND table_name = 'order_items' AND column_name = 'order_item_id';

-- Tạo tên constraint ngẫu nhiên để tránh trùng tên schema-wide
SET @fk_rand := LPAD(FLOOR(RAND()*1000000),6,'0');
SET @fk_name := CONCAT('fk_pr_oi_', DATE_FORMAT(NOW(), '%Y%m%d%H%i%S'), '_', @fk_rand);

-- Tạm tắt FK checks trong lúc ALTER (đã làm sạch dữ liệu ở bước trên)
SET @prev_fk_checks := @@FOREIGN_KEY_CHECKS;
SET FOREIGN_KEY_CHECKS = 0;

SET @should_add_fk := (@fk_new_exists = 0 AND @has_order_items_table = 1 AND @has_parent_col = 1 AND @orphans = 0);
SET @sql := IF(@should_add_fk,
    CONCAT('ALTER TABLE product_reviews ADD CONSTRAINT ', @fk_name, ' FOREIGN KEY (order_item_id) REFERENCES order_items(order_item_id) ON DELETE SET NULL ON UPDATE CASCADE'),
    'SELECT "Skip adding FK: either already exists, parent missing, or orphans present" as message'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Khôi phục lại FK checks
SET FOREIGN_KEY_CHECKS = @prev_fk_checks;

-- =====================================================
-- 5. THÊM CÁC INDEX TỐI ƯU
-- =====================================================

-- Index cho tìm kiếm review theo product
SELECT COUNT(*) INTO @index1_exists
FROM information_schema.statistics
WHERE table_schema = @db
  AND table_name = 'product_reviews'
  AND index_name = 'idx_reviews_product_rating';

SET @sql := IF(@index1_exists = 0,
    'CREATE INDEX idx_reviews_product_rating ON product_reviews(product_id, rating)',
    'SELECT "Index idx_reviews_product_rating already exists" as message'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Index cho tìm kiếm review theo user
SELECT COUNT(*) INTO @index2_exists
FROM information_schema.statistics
WHERE table_schema = @db
  AND table_name = 'product_reviews'
  AND index_name = 'idx_reviews_user_created';

SET @sql := IF(@index2_exists = 0,
    'CREATE INDEX idx_reviews_user_created ON product_reviews(user_id, created_at)',
    'SELECT "Index idx_reviews_user_created already exists" as message'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

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
-- 6.5. ĐẢM BẢO CỘT RATING TỒN TẠI TRƯỚC KHI TẠO PROC/TRIGGER
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
-- 7. TẠO STORED PROCEDURE CHO TÍNH TOÁN RATING
-- =====================================================

-- Xóa procedure cũ nếu có rồi tạo mới
DROP PROCEDURE IF EXISTS UpdateProductAverageRating;
DELIMITER $$
CREATE PROCEDURE UpdateProductAverageRating(IN product_id_param BIGINT)
BEGIN
    DECLARE v_avg_rating DECIMAL(3,2);
    DECLARE v_total_reviews INT;

    SELECT
        COALESCE(AVG(rating), 0),
        COUNT(*)
    INTO v_avg_rating, v_total_reviews
    FROM product_reviews
    WHERE product_id = product_id_param;

    -- Cập nhật trực tiếp (không dùng dynamic SQL) để dùng được trong trigger
    UPDATE products
    SET
        average_rating = v_avg_rating,
        total_reviews = v_total_reviews,
        updated_at = CURRENT_TIMESTAMP
    WHERE product_id = product_id_param;
END $$
DELIMITER ;

-- =====================================================
-- 8. TẠO TRIGGER TỰ ĐỘNG CẬP NHẬT RATING
-- =====================================================

-- Xóa và tạo lại trigger insert
DROP TRIGGER IF EXISTS tr_review_after_insert;
DELIMITER $$
CREATE TRIGGER tr_review_after_insert
AFTER INSERT ON product_reviews
FOR EACH ROW
BEGIN
    CALL UpdateProductAverageRating(NEW.product_id);
END $$
DELIMITER ;

-- Xóa và tạo lại trigger update
DROP TRIGGER IF EXISTS tr_review_after_update;
DELIMITER $$
CREATE TRIGGER tr_review_after_update
AFTER UPDATE ON product_reviews
FOR EACH ROW
BEGIN
    CALL UpdateProductAverageRating(NEW.product_id);
    IF OLD.product_id <> NEW.product_id THEN
        CALL UpdateProductAverageRating(OLD.product_id);
    END IF;
END $$
DELIMITER ;

-- Xóa và tạo lại trigger delete
DROP TRIGGER IF EXISTS tr_review_after_delete;
DELIMITER $$
CREATE TRIGGER tr_review_after_delete
AFTER DELETE ON product_reviews
FOR EACH ROW
BEGIN
    CALL UpdateProductAverageRating(OLD.product_id);
END $$
DELIMITER ;

-- =====================================================
-- 9. THÊM CỘT RATING VÀO BẢNG PRODUCTS (nếu chưa có)
-- =====================================================

-- (MOVED UP to section 6.5 to avoid IDE unresolved warnings before proc/trigger creation)
-- (removed duplicate: this section has been moved earlier)

-- =====================================================
-- 10. CẬP NHẬT RATING CHO TẤT CẢ SẢN PHẨM HIỆN TẠI
-- =====================================================

-- Cập nhật rating cho tất cả sản phẩm (dùng prepared statement để tránh lỗi phân tích tĩnh)
SET @upd_all := 'UPDATE products p\nSET \n    p.average_rating = COALESCE((\n        SELECT AVG(pr.rating) \n        FROM product_reviews pr \n        WHERE pr.product_id = p.product_id\n    ), 0.00),\n    p.total_reviews = COALESCE((\n        SELECT COUNT(*) \n        FROM product_reviews pr \n        WHERE pr.product_id = p.product_id\n    ), 0)';
PREPARE _stmt FROM @upd_all; EXECUTE _stmt; DEALLOCATE PREPARE _stmt;

-- =====================================================
-- 11. TẠO SAMPLE DATA (nếu cần)
-- =====================================================

-- Thêm một số review mẫu (chỉ khi chưa có data), tránh FK lỗi => order_item_id để NULL
INSERT IGNORE INTO product_reviews (
    product_id, 
    user_id, 
    rating, 
    comment, 
    service_rating, 
    order_item_id, 
    created_at
) VALUES 
(1, 1, 5, 'Sản phẩm rất tốt, giao hàng nhanh!', 5, NULL, NOW()),
(1, 2, 4, 'Chất lượng ổn, giá hợp lý', 4, NULL, NOW()),
(2, 1, 5, 'Tuyệt vời! Sẽ mua lại', 5, NULL, NOW()),
(2, 3, 3, 'Bình thường', 3, NULL, NOW());

-- =====================================================
-- 12. TẠO FUNCTION HELPER
-- =====================================================

-- Function tính rating trung bình
DROP FUNCTION IF EXISTS GetProductAverageRating;
DELIMITER $$
CREATE FUNCTION GetProductAverageRating(product_id_param BIGINT)
RETURNS DECIMAL(3,2)
READS SQL DATA
DETERMINISTIC
BEGIN
    DECLARE v_avg_rating DECIMAL(3,2) DEFAULT 0.00;
    SELECT COALESCE(AVG(rating), 0.00)
    INTO v_avg_rating
    FROM product_reviews
    WHERE product_id = product_id_param;
    RETURN v_avg_rating;
END $$
DELIMITER ;

-- Function đếm số review
DROP FUNCTION IF EXISTS GetProductReviewCount;
DELIMITER $$
CREATE FUNCTION GetProductReviewCount(product_id_param BIGINT)
RETURNS INT
READS SQL DATA
DETERMINISTIC
BEGIN
    DECLARE v_review_count INT DEFAULT 0;
    SELECT COUNT(*)
    INTO v_review_count
    FROM product_reviews
    WHERE product_id = product_id_param;
    RETURN v_review_count;
END $$
DELIMITER ;

-- =====================================================
-- HOÀN THÀNH
-- =====================================================

SELECT 'Review system database setup completed successfully!' as result;
SELECT 'All tables, constraints, indexes, triggers, and procedures have been created/updated' as status;
