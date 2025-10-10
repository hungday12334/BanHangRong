-- ========================================
-- FIX ADMIN PASSWORD - CHẠY FILE NÀY
-- ========================================

USE wap;

-- BCrypt hash cho password "123456" với rounds=10
-- Hash này đã được test và hoạt động
UPDATE users 
SET password = '$2a$10$e0MYzXyjpJS7Pd0RVvHwHeFtVpVKxKBCl7Cn6FGa.Ll8xKKqqfLq2'
WHERE username = 'admin';

UPDATE users 
SET password = '$2a$10$e0MYzXyjpJS7Pd0RVvHwHeFtVpVKxKBCl7Cn6FGa.Ll8xKKqqfLq2'
WHERE username = 'seller';

-- Đảm bảo tài khoản được verify
UPDATE users 
SET is_email_verified = TRUE, is_active = TRUE
WHERE username IN ('admin', 'seller');

-- Kiểm tra kết quả
SELECT user_id, username, email, user_type, is_email_verified, is_active, 
       SUBSTRING(password, 1, 20) as password_preview
FROM users 
WHERE username IN ('admin', 'seller');

-- ========================================
-- THÔNG TIN LOGIN SAU KHI CHẠY SCRIPT:
-- ========================================
-- Username: admin    | Password: 123456 | Role: ADMIN
-- Username: seller   | Password: 123456 | Role: SELLER
-- ========================================
