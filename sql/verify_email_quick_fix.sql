-- Script nhanh để verify email cho user (nếu bạn không muốn chờ email)
-- Thay 'your_username' bằng username của bạn

-- Cách 1: Verify email trực tiếp (không cần mã xác thực)
UPDATE users
SET is_email_verified = TRUE
WHERE username = 'your_username';  -- <-- ĐỔI username ở đây

-- Cách 2: Tạo và xem mã xác thực để nhập vào trang verify
-- Bước 1: Xóa token cũ nếu có
DELETE FROM email_verification_tokens
WHERE user_id = (SELECT user_id FROM users WHERE username = 'your_username')
AND is_used = FALSE;

-- Bước 2: Tạo token mới
INSERT INTO email_verification_tokens (user_id, token, expires_at, is_used, created_at)
SELECT
    user_id,
    '123456' as token,  -- Mã xác thực đơn giản
    DATE_ADD(NOW(), INTERVAL 30 DAY) as expires_at,
    FALSE as is_used,
    NOW() as created_at
FROM users
WHERE username = 'your_username';  -- <-- ĐỔI username ở đây

-- Bước 3: Xem mã xác thực vừa tạo
SELECT
    u.username,
    u.email,
    evt.token as verification_code,
    evt.expires_at,
    CONCAT('http://localhost:8080/customer/verify-email?token=', evt.token) as verification_link
FROM users u
INNER JOIN email_verification_tokens evt ON u.user_id = evt.user_id
WHERE u.username = 'your_username'  -- <-- ĐỔI username ở đây
AND evt.is_used = FALSE;

