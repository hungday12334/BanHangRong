-- =============================================
-- FIX CHARSET CHO BẢNG NOTIFICATIONS
-- =============================================

USE smiledev_wap;

-- Kiểm tra charset hiện tại
SHOW CREATE TABLE notifications;

-- Sửa charset của toàn bộ bảng và các cột
ALTER TABLE notifications 
    CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Đảm bảo các cột text đều dùng utf8mb4
ALTER TABLE notifications 
    MODIFY COLUMN title VARCHAR(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
    MODIFY COLUMN message TEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
    MODIFY COLUMN type VARCHAR(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Kiểm tra lại
SHOW CREATE TABLE notifications;

SELECT 'CHARSET ĐÃ ĐƯỢC SỬA THÀNH UTF8MB4!' AS message;

