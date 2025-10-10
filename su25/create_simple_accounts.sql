-- ========================================
-- TẠO TÀI KHOẢN ĐƠN GIẢN - CHẠY FILE NÀY
-- ========================================

USE wap;

-- Xóa tài khoản cũ nếu có
DELETE FROM users WHERE username IN ('admin', 'seller', 'testadmin', 'testseller');

-- Tạo tài khoản mới với password đơn giản (không hash)
INSERT INTO users (username, email, password, user_type, phone_number, gender, balance, is_email_verified, is_active, created_at)
VALUES 
    ('testadmin', 'testadmin@example.com', '123456', 'ADMIN', '0900000001', 'male', 10000.00, TRUE, TRUE, NOW()),
    ('testseller', 'testseller@example.com', '123456', 'SELLER', '0900000002', 'male', 5000.00, TRUE, TRUE, NOW());

-- Kiểm tra kết quả
SELECT user_id, username, email, user_type, is_email_verified, is_active, password
FROM users 
WHERE username IN ('testadmin', 'testseller');

-- ========================================
-- THÔNG TIN LOGIN:
-- ========================================
-- Username: testadmin  | Password: 123456 | Role: ADMIN
-- Username: testseller | Password: 123456 | Role: SELLER
-- ========================================
