-- Script để sửa đổi hệ thống đánh giá
-- Cho phép mỗi lần mua hàng tạo ra một đánh giá riêng biệt

-- Chọn database trước khi thực thi
USE wap;

-- Bước 1: Xóa constraint UNIQUE hiện tại (nếu tồn tại)
-- Kiểm tra và xóa constraint cũ nếu có
SET @constraint_exists = (
    SELECT COUNT(*) 
    FROM information_schema.table_constraints 
    WHERE table_schema = 'wap' 
    AND table_name = 'product_reviews' 
    AND constraint_name = 'ux_reviews_user_product'
);

SET @sql = IF(@constraint_exists > 0, 
    'ALTER TABLE product_reviews DROP CONSTRAINT ux_reviews_user_product', 
    'SELECT "Constraint ux_reviews_user_product does not exist" as message'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Bước 2: Thêm cột order_item_id để liên kết với từng lần mua hàng cụ thể (nếu chưa có)
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

-- Bước 3: Thêm foreign key constraint cho order_item_id (nếu chưa có)
SET @fk_exists = (
    SELECT COUNT(*) 
    FROM information_schema.table_constraints 
    WHERE table_schema = 'wap' 
    AND table_name = 'product_reviews' 
    AND constraint_name = 'fk_reviews_order_item'
);

SET @sql = IF(@fk_exists = 0, 
    'ALTER TABLE product_reviews ADD CONSTRAINT fk_reviews_order_item FOREIGN KEY (order_item_id) REFERENCES order_items(order_item_id)', 
    'SELECT "Foreign key fk_reviews_order_item already exists" as message'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Bước 4: Thêm index để tối ưu hóa truy vấn (nếu chưa có)
SET @index1_exists = (
    SELECT COUNT(*) 
    FROM information_schema.statistics 
    WHERE table_schema = 'wap' 
    AND table_name = 'product_reviews' 
    AND index_name = 'idx_reviews_order_item'
);

SET @sql = IF(@index1_exists = 0, 
    'CREATE INDEX idx_reviews_order_item ON product_reviews(order_item_id)', 
    'SELECT "Index idx_reviews_order_item already exists" as message'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @index2_exists = (
    SELECT COUNT(*) 
    FROM information_schema.statistics 
    WHERE table_schema = 'wap' 
    AND table_name = 'product_reviews' 
    AND index_name = 'idx_reviews_product_created'
);

SET @sql = IF(@index2_exists = 0, 
    'CREATE INDEX idx_reviews_product_created ON product_reviews(product_id, created_at)', 
    'SELECT "Index idx_reviews_product_created already exists" as message'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Bước 5: Thêm cột review_id làm primary key (nếu chưa có)
-- (Cột này đã có trong schema hiện tại)

-- Bước 6: Cập nhật constraint để cho phép nhiều review từ cùng user cho cùng product
-- nhưng mỗi review phải liên kết với một order_item_id khác nhau (nếu chưa có)
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

-- Lưu ý: Constraint mới này đảm bảo mỗi order_item chỉ có thể có một review
-- Nhưng cùng user có thể có nhiều review cho cùng product nếu mua nhiều lần
