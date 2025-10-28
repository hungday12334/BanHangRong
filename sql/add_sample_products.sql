-- =============================================
-- ADD SAMPLE PRODUCTS: Thêm sản phẩm mẫu để test
-- =============================================

USE wap;

-- Ensure users.full_name exists (add if missing) to avoid INSERT error
SET @db := DATABASE();
SELECT COUNT(*) INTO @has_full_name
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'users' AND COLUMN_NAME = 'full_name';
SET @ddl := IF(@has_full_name = 0,
               'ALTER TABLE users ADD COLUMN full_name VARCHAR(100) NULL AFTER username;',
               'SELECT "users.full_name already exists" AS msg');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Thêm dữ liệu mẫu cho users (sellers)
INSERT IGNORE INTO users (user_id, username, full_name, email, password, user_type, is_email_verified, is_active, balance) VALUES
(1, 'seller1', 'Nguyễn Văn A', 'seller1@example.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi', 'SELLER', TRUE, TRUE, 1000000.00),
(2, 'seller2', 'Trần Thị B', 'seller2@example.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi', 'SELLER', TRUE, TRUE, 2000000.00),
(3, 'customer1', 'Lê Văn C', 'customer1@example.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi', 'CUSTOMER', TRUE, TRUE, 500000.00);

-- Thêm dữ liệu mẫu cho categories
INSERT IGNORE INTO categories (category_id, name, description) VALUES
(1, 'Phần mềm', 'Các phần mềm ứng dụng'),
(2, 'Game', 'Trò chơi điện tử'),
(3, 'Template', 'Mẫu thiết kế'),
(4, 'Ebook', 'Sách điện tử'),
(5, 'Khóa học', 'Khóa học online');

-- Thêm dữ liệu mẫu cho products
INSERT IGNORE INTO products (product_id, seller_id, name, description, price, sale_price, quantity, download_url, total_sales, average_rating, status) VALUES
(1, 1, 'Adobe Photoshop 2024', 'Phần mềm chỉnh sửa ảnh chuyên nghiệp với đầy đủ tính năng mới nhất', 2500000.00, 2000000.00, 50, 'https://example.com/download/photoshop2024.zip', 25, 4.8, 'Public'),
(2, 1, 'Microsoft Office 2024', 'Bộ ứng dụng văn phòng đầy đủ Word, Excel, PowerPoint', 1800000.00, NULL, 100, 'https://example.com/download/office2024.zip', 45, 4.6, 'Public'),
(3, 2, 'Game Minecraft Premium', 'Trò chơi xây dựng thế giới 3D sáng tạo', 800000.00, 600000.00, 200, 'https://example.com/download/minecraft.zip', 120, 4.9, 'Public'),
(4, 2, 'Template Website Responsive', 'Mẫu website responsive đẹp mắt cho doanh nghiệp', 500000.00, NULL, 30, 'https://example.com/download/template1.zip', 15, 4.5, 'Public'),
(5, 1, 'Ebook Lập trình Java', 'Sách điện tử hướng dẫn lập trình Java từ cơ bản đến nâng cao', 200000.00, 150000.00, 1000, 'https://example.com/download/java-ebook.pdf', 80, 4.7, 'Public'),
(6, 2, 'Khóa học React Native', 'Khóa học lập trình ứng dụng di động với React Native', 1200000.00, 900000.00, 20, 'https://example.com/download/react-course.zip', 12, 4.8, 'Public'),
(7, 1, 'Adobe Premiere Pro 2024', 'Phần mềm chỉnh sửa video chuyên nghiệp', 3000000.00, 2500000.00, 25, 'https://example.com/download/premiere2024.zip', 8, 4.9, 'Public'),
(8, 2, 'Game Among Us Premium', 'Trò chơi đa người chơi phổ biến', 300000.00, NULL, 500, 'https://example.com/download/amongus.zip', 200, 4.6, 'Public'),
(9, 1, 'Template Landing Page', 'Mẫu trang landing page chuyển đổi cao', 400000.00, 300000.00, 40, 'https://example.com/download/landing-template.zip', 25, 4.4, 'Public'),
(10, 2, 'Ebook Python cho người mới', 'Sách điện tử học Python từ đầu', 150000.00, NULL, 800, 'https://example.com/download/python-ebook.pdf', 60, 4.5, 'Public');

-- Thêm liên kết sản phẩm với danh mục
INSERT IGNORE INTO categories_products (category_id, product_id) VALUES
(1, 1), (1, 2), (1, 7),  -- Phần mềm
(2, 3), (2, 8),          -- Game  
(3, 4), (3, 9),          -- Template
(4, 5), (4, 10),         -- Ebook
(5, 6);                  -- Khóa học

-- Thêm hình ảnh sản phẩm
INSERT IGNORE INTO product_images (product_id, image_url, is_primary) VALUES
(1, '/img/products/photoshop.jpg', TRUE),
(2, '/img/products/office.jpg', TRUE),
(3, '/img/products/minecraft.jpg', TRUE),
(4, '/img/products/template1.jpg', TRUE),
(5, '/img/products/java-ebook.jpg', TRUE),
(6, '/img/products/react-course.jpg', TRUE),
(7, '/img/products/premiere.jpg', TRUE),
(8, '/img/products/amongus.jpg', TRUE),
(9, '/img/products/landing-template.jpg', TRUE),
(10, '/img/products/python-ebook.jpg', TRUE);

-- Thêm một số đánh giá mẫu
INSERT IGNORE INTO product_reviews (product_id, user_id, rating, comment) VALUES
(1, 3, 5, 'Phần mềm rất tốt, đầy đủ tính năng'),
(2, 3, 4, 'Office 2024 có nhiều cải tiến mới'),
(3, 3, 5, 'Game hay, đồ họa đẹp'),
(4, 3, 4, 'Template đẹp, dễ tùy chỉnh'),
(5, 3, 5, 'Ebook rất hữu ích cho người mới học');

-- Kiểm tra kết quả
SELECT 'Dữ liệu sản phẩm mẫu đã được thêm thành công!' AS message;

SELECT 
    p.product_id,
    p.name,
    p.price,
    p.sale_price,
    p.quantity,
    p.status,
    c.name as category_name
FROM products p
LEFT JOIN categories_products cp ON p.product_id = cp.product_id
LEFT JOIN categories c ON cp.category_id = c.category_id
WHERE p.status = 'Public'
ORDER BY p.product_id;
