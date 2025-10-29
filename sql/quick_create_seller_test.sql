-- =============================================
-- SCRIPT NHANH TẠO TÀI KHOẢN SELLER TEST
-- Password: 123456
-- =============================================

USE wap;

-- XÓA VÀ TẠO LẠI TÀI KHOẢN TEST (không liên quan products)
-- Nếu bị lỗi foreign key, bỏ qua lệnh DELETE và chỉ chạy INSERT

-- Tạo seller test mới (user_id sẽ tự động tăng)
INSERT INTO users (username, full_name, email, password, user_type, phone_number, gender, is_email_verified, is_active, balance, created_at, updated_at)
VALUES 
('sellertest', 'Seller Test Account', 'sellertest@test.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi', 'SELLER', '0900000000', 'male', TRUE, TRUE, 0.00, NOW(), NOW())
ON DUPLICATE KEY UPDATE 
    password = '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi',
    user_type = 'SELLER',
    is_email_verified = TRUE,
    is_active = TRUE;

-- Kiểm tra xem đã tạo thành công chưa
SELECT 
    user_id, 
    username, 
    email, 
    user_type, 
    is_active,
    is_email_verified,
    'Password là: 123456' as password_info
FROM users 
WHERE username = 'sellertest';

-- Nếu username seller1, seller2 đã tồn tại, cập nhật password
UPDATE users 
SET password = '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi',
    is_active = TRUE,
    is_email_verified = TRUE,
    user_type = 'SELLER'
WHERE username IN ('seller1', 'seller2');

-- Kiểm tra seller1 và seller2
SELECT 
    username, 
    email, 
    user_type,
    is_active,
    is_email_verified,
    'Password: 123456' as info
FROM users 
WHERE username IN ('seller1', 'seller2', 'sellertest');

