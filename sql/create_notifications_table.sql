-- =============================================
-- TẠO BẢNG NOTIFICATIONS
-- =============================================

USE smiledev_wap;

-- Tạo bảng notifications
CREATE TABLE IF NOT EXISTS notifications (
    notification_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    type VARCHAR(50),
    reference_id BIGINT,
    is_read BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    read_at TIMESTAMP NULL,
    INDEX idx_user_id (user_id),
    INDEX idx_is_read (is_read),
    INDEX idx_created_at (created_at),
    INDEX idx_type (type),
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Tạo một số notification mẫu để test
INSERT INTO notifications (user_id, title, message, type, reference_id, is_read, created_at)
SELECT 
    u.user_id,
    'Chào mừng bạn đến với BanHangRong!',
    'Cảm ơn bạn đã đăng ký tài khoản. Hãy khám phá các sản phẩm tuyệt vời của chúng tôi!',
    'SYSTEM',
    NULL,
    FALSE,
    NOW()
FROM users u
WHERE u.user_type = 'CUSTOMER'
AND NOT EXISTS (
    SELECT 1 FROM notifications n WHERE n.user_id = u.user_id AND n.title = 'Chào mừng bạn đến với BanHangRong!'
)
LIMIT 10;

-- Kiểm tra kết quả
SELECT 
    n.notification_id,
    n.user_id,
    u.username,
    n.title,
    n.type,
    n.is_read,
    n.created_at
FROM notifications n
JOIN users u ON n.user_id = u.user_id
ORDER BY n.created_at DESC
LIMIT 10;

SELECT 'BẢNG NOTIFICATIONS ĐÃ ĐƯỢC TẠO THÀNH CÔNG!' AS message;

