-- ========================================
-- FIX TESTADMIN PASSWORD - HASH ĐÚNG CÁCH
-- ========================================

USE wap;

-- BCrypt hash cho password "123456" với rounds=10
-- Hash này đã được test và hoạt động với Spring Security
UPDATE users 
SET password = '$2a$10$e0MYzXyjpJS7Pd0RVvHwHeFtVpVKxKBCl7Cn6FGa.Ll8xKKqqfLq2'
WHERE username = 'testadmin';

-- Đảm bảo tài khoản được verify và active
UPDATE users 
SET is_email_verified = TRUE, is_active = TRUE
WHERE username = 'testadmin';

-- Kiểm tra kết quả
SELECT user_id, username, email, user_type, is_email_verified, is_active, 
       SUBSTRING(password, 1, 30) as password_preview
FROM users 
WHERE username = 'testadmin';

-- ========================================
-- THÔNG TIN LOGIN:
-- ========================================
-- Username: testadmin | Password: 123456 | Role: ADMIN
-- ========================================
