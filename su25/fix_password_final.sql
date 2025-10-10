-- ========================================
-- FIX PASSWORD - SỬ DỤNG HASH MỚI
-- ========================================

USE wap;

-- Xóa tài khoản cũ nếu có
DELETE FROM users WHERE username IN ('admin', 'seller');

-- Tạo tài khoản admin mới với password đơn giản
INSERT INTO users (username, email, password, user_type, phone_number, gender, balance, is_email_verified, is_active, created_at)
VALUES 
    ('admin', 'admin@example.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi', 'ADMIN', '0900000001', 'male', 10000.00, TRUE, TRUE, NOW()),
    ('seller', 'seller@example.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi', 'SELLER', '0900000002', 'male', 5000.00, TRUE, TRUE, NOW());

-- Kiểm tra kết quả
SELECT user_id, username, email, user_type, is_email_verified, is_active, 
       SUBSTRING(password, 1, 30) as password_preview
FROM users 
WHERE username IN ('admin', 'seller');

-- ========================================
-- THÔNG TIN LOGIN:
-- ========================================
-- Username: admin    | Password: 123456 | Role: ADMIN
-- Username: seller   | Password: 123456 | Role: SELLER
-- ========================================
