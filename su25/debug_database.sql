-- ========================================
-- DEBUG DATABASE - KIỂM TRA DỮ LIỆU
-- ========================================

USE wap;

-- Kiểm tra tất cả users
SELECT user_id, username, email, user_type, is_email_verified, is_active, 
       SUBSTRING(password, 1, 20) as password_preview
FROM users 
ORDER BY user_id;

-- Kiểm tra cụ thể admin và seller
SELECT user_id, username, email, user_type, is_email_verified, is_active, password
FROM users 
WHERE username IN ('admin', 'seller', 'testadmin', 'testseller');

-- Đếm tổng số users
SELECT COUNT(*) as total_users FROM users;

-- Kiểm tra database connection
SELECT 'Database connection OK' as status, NOW() as current_time;
