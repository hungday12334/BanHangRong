-- Script để tạo verification token cho các user đã đăng ký nhưng chưa có token
-- Chạy script này để tạo mã xác thực cho tất cả user chưa verify email

-- Xóa các token cũ chưa dùng (nếu có)
DELETE FROM email_verification_tokens
WHERE user_id IN (
    SELECT user_id FROM users WHERE is_email_verified = FALSE
) AND is_used = FALSE;

-- Tạo token mới cho tất cả user chưa verify email
-- Token là số ngẫu nhiên 6 chữ số
INSERT INTO email_verification_tokens (user_id, token, expires_at, is_used, created_at)
SELECT
    user_id,
    LPAD(CAST(FLOOR(RAND() * 1000000) AS CHAR), 6, '0') as token,
    DATE_ADD(NOW(), INTERVAL 1 DAY) as expires_at,
    FALSE as is_used,
    NOW() as created_at
FROM users
WHERE is_email_verified = FALSE
AND user_id NOT IN (
    SELECT user_id FROM email_verification_tokens WHERE is_used = FALSE
);

-- Hiển thị kết quả để admin có thể gửi email cho user
SELECT
    u.user_id,
    u.username,
    u.email,
    evt.token as verification_code,
    evt.expires_at
FROM users u
INNER JOIN email_verification_tokens evt ON u.user_id = evt.user_id
WHERE u.is_email_verified = FALSE
AND evt.is_used = FALSE
ORDER BY evt.created_at DESC;

