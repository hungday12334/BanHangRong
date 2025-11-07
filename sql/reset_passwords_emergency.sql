-- =============================================
-- EMERGENCY PASSWORD RESET SCRIPT
-- Reset password cho tất cả users về "123456"
-- BCrypt hash của "123456": $2a$10$OW3G2kQ03pPlhuEfQdKZZeUKV6FPZsGiQiV/Lh5JL3SqSJzwpG32m
-- =============================================

USE `wap`;

-- Reset password cho admin
UPDATE `users`
SET `password` = '$2a$10$OW3G2kQ03pPlhuEfQdKZZeUKV6FPZsGiQiV/Lh5JL3SqSJzwpG32m'
WHERE `username` = 'admin' OR `user_type` = 'ADMIN';

-- Reset password cho nguyenhung1401
UPDATE `users`
SET `password` = '$2a$10$OW3G2kQ03pPlhuEfQdKZZeUKV6FPZsGiQiV/Lh5JL3SqSJzwpG32m'
WHERE `username` = 'nguyenhung1401';

-- Reset password cho tất cả users (nếu cần)
-- UNCOMMENT DÒNG DƯỚI NẾU MUỐN RESET TẤT CẢ:
-- UPDATE `users`
-- SET `password` = '$2a$10$OW3G2kQ03pPlhuEfQdKZZeUKV6FPZsGiQiV/Lh5JL3SqSJzwpG32m'
-- WHERE `password` IS NULL OR `password` = '' OR `password` NOT LIKE '$2a$%';

-- Kiểm tra password đã được reset chưa
SELECT `username`, `user_type`, 
       CASE 
           WHEN `password` LIKE '$2a$%' THEN 'BCrypt format ✓'
           WHEN `password` IS NULL OR `password` = '' THEN 'NULL/EMPTY ✗'
           ELSE 'Invalid format ✗'
       END AS password_status,
       LENGTH(`password`) AS password_length
FROM `users`
WHERE `username` IN ('admin', 'nguyenhung1401') OR `user_type` = 'ADMIN';

