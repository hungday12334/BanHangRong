-- =============================================
-- TẠO TÀI KHOẢN CUSTOMER DEMO
-- Password: demo123
-- =============================================

USE smiledev_wap;

-- Tạo tài khoản Customer Demo
INSERT INTO users (username, full_name, email, password, user_type, is_email_verified, is_active, balance, created_at, updated_at, phone_number, gender)
VALUES 
('customer_demo', 'Nguyễn Văn Demo', 'demo@customer.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi', 'CUSTOMER', TRUE, TRUE, 1000000.00, NOW(), NOW(), '0987654321', 'male')
ON DUPLICATE KEY UPDATE 
    password = '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi',
    full_name = 'Nguyễn Văn Demo',
    email = 'demo@customer.com',
    user_type = 'CUSTOMER',
    is_email_verified = TRUE,
    is_active = TRUE,
    balance = 1000000.00,
    phone_number = '0987654321',
    gender = 'male',
    updated_at = NOW();

-- Kiểm tra kết quả
SELECT user_id, username, full_name, email, user_type, balance, is_active, phone_number
FROM users 
WHERE username = 'customer_demo';

-- Hiển thị thông tin đăng nhập
SELECT 
    'TÀI KHOẢN CUSTOMER DEMO ĐÃ ĐƯỢC TẠO!' AS message,
    'Username: customer_demo' AS username,
    'Password: demo123' AS password,
    'Email: demo@customer.com' AS email,
    'Balance: 1,000,000 VNĐ' AS balance_info;

