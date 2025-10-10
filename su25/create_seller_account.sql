-- ========================================
-- TÀI KHOẢN SELLER ĐỂ TEST
-- ========================================

USE wap;

-- ========================================
-- CÁCH 1: SỬ DỤNG TÀI KHOẢN CÓ SẴN
-- ========================================
-- Cập nhật password cho tài khoản 'seller' có sẵn
-- Password: 123456 (đơn giản để test)

UPDATE users 
SET password = '$2a$10$e0MYzXyjpJS7Pd0RVvHwHeFtVpVKxKBCl7Cn6FGa.Ll8xKKqqfLq2'
WHERE username = 'admin';

-- ========================================
-- THÔNG TIN LOGIN
-- ========================================
-- Username: seller
-- Password: 123456
-- Role: SELLER
-- ========================================

-- Kiểm tra tài khoản
SELECT user_id, username, email, user_type, is_email_verified, is_active 
FROM users 
WHERE username = 'admin';

-- ========================================
-- CÁCH 2: TẠO TÀI KHOẢN MỚI (NẾU CẦN)
-- ========================================
/*
INSERT INTO users (username, email, password, user_type, phone_number, gender, balance, is_email_verified, is_active)
VALUES 
    ('testseller', 'testseller@test.com', '$2a$10$e0MYzXyjpJS7Pd0RVvHwHeFtVpVKxKBCl7Cn6FGa.Ll8xKKqqfLq2', 'SELLER', '0900111222', 'male', 5000.00, TRUE, TRUE)
ON DUPLICATE KEY UPDATE username=username;

-- Login với:
-- Username: testseller
-- Password: 123456
*/

