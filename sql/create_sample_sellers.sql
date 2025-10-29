-- =============================================
-- TẠO TÀI KHOẢN SELLER MẪU ĐỂ TEST
-- Password: 123456 (đã mã hóa BCrypt)
-- =============================================

USE wap;

-- Cập nhật hoặc tạo mới tài khoản Seller 1
INSERT INTO users (username, full_name, email, password, user_type, is_email_verified, is_active, balance, created_at, updated_at, phone_number, gender)
VALUES 
('seller1', 'Nguyễn Văn A - Seller 1', 'seller1@banhangrong.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi', 'SELLER', TRUE, TRUE, 1000000.00, NOW(), NOW(), '0901234567', 'male')
ON DUPLICATE KEY UPDATE 
    password = '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi',
    user_type = 'SELLER',
    is_email_verified = TRUE,
    is_active = TRUE,
    updated_at = NOW();

-- Cập nhật hoặc tạo mới tài khoản Seller 2  
INSERT INTO users (username, full_name, email, password, user_type, is_email_verified, is_active, balance, created_at, updated_at, phone_number, gender)
VALUES 
('seller2', 'Trần Thị B - Seller 2', 'seller2@banhangrong.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi', 'SELLER', TRUE, TRUE, 2000000.00, NOW(), NOW(), '0909876543', 'female')
ON DUPLICATE KEY UPDATE 
    password = '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi',
    user_type = 'SELLER',
    is_email_verified = TRUE,
    is_active = TRUE,
    updated_at = NOW();

-- Cập nhật hoặc tạo mới tài khoản Customer để test
INSERT INTO users (username, full_name, email, password, user_type, is_email_verified, is_active, balance, created_at, updated_at, phone_number, gender)
VALUES 
('customer1', 'Lê Văn C - Customer Test', 'customer1@banhangrong.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi', 'CUSTOMER', TRUE, TRUE, 500000.00, NOW(), NOW(), '0912345678', 'male')
ON DUPLICATE KEY UPDATE 
    password = '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi',
    user_type = 'CUSTOMER',
    is_email_verified = TRUE,
    is_active = TRUE,
    updated_at = NOW();

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

