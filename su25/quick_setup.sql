-- ========================================
-- QUICK SETUP - CHẠY FILE NÀY ĐỂ SETUP NHANH
-- ========================================

USE wap;

-- Update password cho TẤT CẢ tài khoản test (password đều là: 123456)
UPDATE users SET password = '$2a$10$e0MYzXyjpJS7Pd0RVvHwHeFtVpVKxKBCl7Cn6FGa.Ll8xKKqqfLq2' WHERE username = 'seller';
UPDATE users SET password = '$2a$10$e0MYzXyjpJS7Pd0RVvHwHeFtVpVKxKBCl7Cn6FGa.Ll8xKKqqfLq2' WHERE username = 'alice';
UPDATE users SET password = '$2a$10$e0MYzXyjpJS7Pd0RVvHwHeFtVpVKxKBCl7Cn6FGa.Ll8xKKqqfLq2' WHERE username = 'bob';
UPDATE users SET password = '$2a$10$e0MYzXyjpJS7Pd0RVvHwHeFtVpVKxKBCl7Cn6FGa.Ll8xKKqqfLq2' WHERE username = 'charlie';
UPDATE users SET password = '$2a$10$e0MYzXyjpJS7Pd0RVvHwHeFtVpVKxKBCl7Cn6FGa.Ll8xKKqqfLq2' WHERE username = 'dave';

-- Verify tất cả tài khoản
UPDATE users SET is_email_verified = TRUE;

-- ========================================
-- TẤT CẢ TÀI KHOẢN BÂY GIỜ LOGIN VỚI:
-- ========================================
-- Username: seller    | Password: 123456 | Role: SELLER
-- Username: alice     | Password: 123456 | Role: CUSTOMER
-- Username: bob       | Password: 123456 | Role: CUSTOMER  
-- Username: charlie   | Password: 123456 | Role: CUSTOMER
-- Username: dave      | Password: 123456 | Role: CUSTOMER
-- ========================================

-- Kiểm tra
SELECT username, email, user_type, is_email_verified, is_active FROM users;

