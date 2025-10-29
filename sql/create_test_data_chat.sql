-- =============================================
-- SCRIPT TẠO DỮ LIỆU TEST CHO HỆ THỐNG CHAT
-- =============================================
-- Chạy script này để tạo tài khoản test và dữ liệu mẫu

-- 1. TẠO TÀI KHOẢN TEST
-- =============================================

-- Xóa dữ liệu cũ nếu có (optional - cẩn thận khi chạy!)
-- DELETE FROM chat_messages;
-- DELETE FROM chat_conversations;
-- DELETE FROM users WHERE username LIKE 'test_%';

-- Tạo 1 Seller account
INSERT INTO users (username, email, password, full_name, user_type, phone_number, is_active, is_email_verified, balance, created_at)
VALUES (
    'test_seller1',
    'seller1@test.com',
    '$2a$10$rV3VLGzXxZmj6h0Cbn3K1.VqT3.0qE9YC5YVz9qY8Kp0U/IqZXCyW', -- Password: 123456
    'Shop Điện Thoại ABC',
    'SELLER',
    '0901234567',
    true,
    true,
    0,
    NOW()
) ON DUPLICATE KEY UPDATE user_type='SELLER';

-- Tạo 3 Customer accounts
INSERT INTO users (username, email, password, full_name, user_type, phone_number, is_active, is_email_verified, balance, created_at)
VALUES
    (
        'test_customer1',
        'customer1@test.com',
        '$2a$10$rV3VLGzXxZmj6h0Cbn3K1.VqT3.0qE9YC5YVz9qY8Kp0U/IqZXCyW', -- Password: 123456
        'Nguyễn Văn An',
        'CUSTOMER',
        '0912345678',
        true,
        true,
        100000,
        NOW()
    ),
    (
        'test_customer2',
        'customer2@test.com',
        '$2a$10$rV3VLGzXxZmj6h0Cbn3K1.VqT3.0qE9YC5YVz9qY8Kp0U/IqZXCyW', -- Password: 123456
        'Trần Thị Bình',
        'CUSTOMER',
        '0923456789',
        true,
        true,
        200000,
        NOW()
    ),
    (
        'test_customer3',
        'customer3@test.com',
        '$2a$10$rV3VLGzXxZmj6h0Cbn3K1.VqT3.0qE9YC5YVz9qY8Kp0U/IqZXCyW', -- Password: 123456
        'Lê Minh Châu',
        'CUSTOMER',
        '0934567890',
        true,
        true,
        150000,
        NOW()
    )
ON DUPLICATE KEY UPDATE user_type='CUSTOMER';

-- 2. HIỂN THỊ THÔNG TIN TÀI KHOẢN
-- =============================================
SELECT
    '=== TÀI KHOẢN TEST ĐÃ TẠO ===' as info,
    NULL as user_id,
    NULL as username,
    NULL as password,
    NULL as full_name,
    NULL as user_type
UNION ALL
SELECT
    '---' as info,
    user_id,
    username,
    '123456' as password,
    full_name,
    user_type
FROM users
WHERE username LIKE 'test_%'
ORDER BY user_type DESC, username;

-- 3. TẠO DỮ LIỆU CONVERSATION MẪU (Optional)
-- =============================================
-- Lưu ý: Conversations sẽ được tạo tự động khi customer gửi tin nhắn đầu tiên
-- Script này để tạo sẵn một số conversation mẫu cho test

-- Lấy user_id của seller và customers
SET @seller_id = (SELECT user_id FROM users WHERE username = 'test_seller1' LIMIT 1);
SET @customer1_id = (SELECT user_id FROM users WHERE username = 'test_customer1' LIMIT 1);
SET @customer2_id = (SELECT user_id FROM users WHERE username = 'test_customer2' LIMIT 1);
SET @customer3_id = (SELECT user_id FROM users WHERE username = 'test_customer3' LIMIT 1);

-- Tạo conversations (nếu muốn có sẵn)
INSERT INTO chat_conversations (id, customer_id, customer_name, seller_id, seller_name, last_message, last_message_time, created_at)
VALUES
    (
        CONCAT('conv_', @customer1_id, '_', @seller_id),
        @customer1_id,
        'Nguyễn Văn An',
        @seller_id,
        'Shop Điện Thoại ABC',
        'Xin chào shop!',
        NOW() - INTERVAL 5 MINUTE,
        NOW() - INTERVAL 10 MINUTE
    ),
    (
        CONCAT('conv_', @customer2_id, '_', @seller_id),
        @customer2_id,
        'Trần Thị Bình',
        @seller_id,
        'Shop Điện Thoại ABC',
        'Shop ơi, cho em hỏi giá iPhone 15',
        NOW() - INTERVAL 2 MINUTE,
        NOW() - INTERVAL 5 MINUTE
    )
ON DUPLICATE KEY UPDATE last_message_time = VALUES(last_message_time);

-- 4. TẠO TIN NHẮN MẪU (Optional)
-- =============================================
-- Conversation 1: Customer1 với Seller
INSERT INTO chat_messages (conversation_id, sender_id, sender_name, sender_role, receiver_id, content, message_type, is_read, created_at)
VALUES
    -- Customer gửi
    (CONCAT('conv_', @customer1_id, '_', @seller_id), @customer1_id, 'Nguyễn Văn An', 'CUSTOMER', @seller_id,
     'Xin chào shop!', 'TEXT', true, NOW() - INTERVAL 10 MINUTE),

    -- Seller trả lời
    (CONCAT('conv_', @customer1_id, '_', @seller_id), @seller_id, 'Shop Điện Thoại ABC', 'SELLER', @customer1_id,
     'Chào bạn, shop có thể giúp gì cho bạn?', 'TEXT', true, NOW() - INTERVAL 9 MINUTE),

    -- Customer hỏi
    (CONCAT('conv_', @customer1_id, '_', @seller_id), @customer1_id, 'Nguyễn Văn An', 'CUSTOMER', @seller_id,
     'Em muốn mua iPhone 15 Pro Max ạ', 'TEXT', true, NOW() - INTERVAL 8 MINUTE),

    -- Seller trả lời
    (CONCAT('conv_', @customer1_id, '_', @seller_id), @seller_id, 'Shop Điện Thoại ABC', 'SELLER', @customer1_id,
     'Dạ shop có đủ các màu và dung lượng. Bạn muốn màu gì và dung lượng bao nhiêu?', 'TEXT', true, NOW() - INTERVAL 7 MINUTE),

    -- Customer trả lời
    (CONCAT('conv_', @customer1_id, '_', @seller_id), @customer1_id, 'Nguyễn Văn An', 'CUSTOMER', @seller_id,
     'Em muốn màu xanh, 256GB. Giá bao nhiêu ạ?', 'TEXT', false, NOW() - INTERVAL 5 MINUTE);

-- Conversation 2: Customer2 với Seller
INSERT INTO chat_messages (conversation_id, sender_id, sender_name, sender_role, receiver_id, content, message_type, is_read, created_at)
VALUES
    -- Customer gửi
    (CONCAT('conv_', @customer2_id, '_', @seller_id), @customer2_id, 'Trần Thị Bình', 'CUSTOMER', @seller_id,
     'Shop ơi, cho em hỏi giá iPhone 15', 'TEXT', false, NOW() - INTERVAL 2 MINUTE);

-- 5. KIỂM TRA DỮ LIỆU ĐÃ TẠO
-- =============================================

-- Kiểm tra users
SELECT
    '=== USERS ===' as type,
    user_id,
    username,
    full_name,
    user_type,
    is_active
FROM users
WHERE username LIKE 'test_%';

-- Kiểm tra conversations
SELECT
    '=== CONVERSATIONS ===' as type,
    id as conversation_id,
    customer_name,
    seller_name,
    last_message,
    DATE_FORMAT(last_message_time, '%H:%i:%s') as time
FROM chat_conversations
WHERE customer_id = @customer1_id
   OR customer_id = @customer2_id
   OR customer_id = @customer3_id;

-- Kiểm tra messages
SELECT
    '=== MESSAGES ===' as type,
    message_id,
    sender_name,
    LEFT(content, 50) as content_preview,
    is_read,
    DATE_FORMAT(created_at, '%H:%i:%s') as time
FROM chat_messages
WHERE conversation_id LIKE CONCAT('conv_', @customer1_id, '%')
   OR conversation_id LIKE CONCAT('conv_', @customer2_id, '%')
ORDER BY created_at;

-- 6. THỐNG KÊ
-- =============================================
SELECT
    'Total Conversations' as metric,
    COUNT(*) as count
FROM chat_conversations
WHERE seller_id = @seller_id

UNION ALL

SELECT
    'Total Messages' as metric,
    COUNT(*) as count
FROM chat_messages
WHERE conversation_id IN (
    SELECT id FROM chat_conversations WHERE seller_id = @seller_id
)

UNION ALL

SELECT
    'Unread Messages' as metric,
    COUNT(*) as count
FROM chat_messages
WHERE is_read = false
  AND receiver_id = @seller_id;

-- =============================================
-- SCRIPT HOÀN TẤT
-- =============================================

SELECT '
╔════════════════════════════════════════════════════════╗
║         DỮ LIỆU TEST ĐÃ ĐƯỢC TẠO THÀNH CÔNG!         ║
╠════════════════════════════════════════════════════════╣
║  TÀI KHOẢN TEST:                                       ║
║  ------------------------------------------------      ║
║  Seller:                                               ║
║    Username: test_seller1                              ║
║    Password: 123456                                    ║
║    Name: Shop Điện Thoại ABC                          ║
║                                                        ║
║  Customer 1:                                           ║
║    Username: test_customer1                            ║
║    Password: 123456                                    ║
║    Name: Nguyễn Văn An                                ║
║                                                        ║
║  Customer 2:                                           ║
║    Username: test_customer2                            ║
║    Password: 123456                                    ║
║    Name: Trần Thị Bình                                ║
║                                                        ║
║  Customer 3:                                           ║
║    Username: test_customer3                            ║
║    Password: 123456                                    ║
║    Name: Lê Minh Châu                                 ║
║                                                        ║
╠════════════════════════════════════════════════════════╣
║  CÁCH TEST:                                            ║
║  1. Đăng nhập test_customer1 tại /login               ║
║  2. Truy cập /customer/chat                           ║
║  3. Gửi tin nhắn cho Shop Điện Thoại ABC             ║
║  4. Đăng nhập test_seller1 (trình duyệt khác)        ║
║  5. Truy cập /seller/chat                             ║
║  6. Xem và trả lời tin nhắn                           ║
╚════════════════════════════════════════════════════════╝
' as SUCCESS_MESSAGE;

