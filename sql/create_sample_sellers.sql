-- =============================================
-- TẠO TÀI KHOẢN SELLER MẪU ĐỂ TEST
-- Password: 123456 (đã mã hóa BCrypt)
-- =============================================

USE wap;

-- Xóa tài khoản cũ nếu có (để tránh duplicate)
DELETE FROM users WHERE username IN ('seller1', 'seller2', 'customer1');

-- Tạo tài khoản Seller 1
INSERT INTO users (username, full_name, email, password, user_type, is_email_verified, is_active, balance, created_at, updated_at)
VALUES 
('seller1', 'Nguyễn Văn A - Seller 1', 'seller1@banhangrong.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi', 'SELLER', TRUE, TRUE, 1000000.00, NOW(), NOW());

-- Tạo tài khoản Seller 2  
INSERT INTO users (username, full_name, email, password, user_type, is_email_verified, is_active, balance, created_at, updated_at)
VALUES 
('seller2', 'Trần Thị B - Seller 2', 'seller2@banhangrong.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi', 'SELLER', TRUE, TRUE, 2000000.00, NOW(), NOW());

-- Tạo tài khoản Customer để test
INSERT INTO users (username, full_name, email, password, user_type, is_email_verified, is_active, balance, created_at, updated_at)
VALUES 
('customer1', 'Lê Văn C - Customer Test', 'customer1@banhangrong.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi', 'CUSTOMER', TRUE, TRUE, 500000.00, NOW(), NOW());

-- Kiểm tra kết quả
SELECT user_id, username, full_name, email, user_type, balance, is_active 
FROM users 
WHERE username IN ('seller1', 'seller2', 'customer1')
ORDER BY user_type, username;

-- Hiển thị thông báo
SELECT 
    'TÀI KHOẢN ĐÃ ĐƯỢC TẠO THÀNH CÔNG!' AS message,
    'Username: seller1, seller2, customer1' AS usernames,
    'Password: 123456 (cho tất cả tài khoản)' AS password_info;

