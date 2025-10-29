# HƯỚNG DẪN XÁC THỰC EMAIL SAU KHI ĐĂNG KÝ

## Vấn đề
Bạn đã đăng ký tài khoản nhưng không nhận được email xác thực (hoặc chỉ nhận email chào mừng mà không có mã xác thực).

## Nguyên nhân
Hệ thống trước đó không tự động gửi email xác thực khi đăng ký. **Vấn đề này đã được sửa** - từ bây giờ mọi đăng ký mới sẽ tự động nhận email có mã xác thực.

## Giải pháp cho tài khoản đã đăng ký

### Cách 1: Xác thực trực tiếp qua Database (NHANH NHẤT)

1. Mở database bằng công cụ quản lý (DBeaver, MySQL Workbench, hoặc H2 Console)

2. Chạy lệnh SQL sau (thay `your_username` bằng username của bạn):
```sql
UPDATE users 
SET is_email_verified = TRUE 
WHERE username = 'your_username';
```

3. Đăng nhập lại và profile của bạn sẽ hiển thị email đã được xác thực.

### Cách 2: Tạo mã xác thực và nhập vào hệ thống

1. Mở file `sql/verify_email_quick_fix.sql`

2. Tìm và thay tất cả `'your_username'` bằng username thực của bạn

3. Chạy toàn bộ script trong database

4. Bạn sẽ thấy mã xác thực (ví dụ: `123456`) và link xác thực

5. Có 2 cách sử dụng mã:
   - **Cách A**: Click vào link xác thực (ví dụ: http://localhost:8080/customer/verify-email?token=123456)
   - **Cách B**: 
     1. Đăng nhập vào tài khoản
     2. Vào trang Profile
     3. Click nút "Verify Email" hoặc vào `/customer/verify-code`
     4. Nhập mã `123456`

### Cách 3: Yêu cầu gửi lại email xác thực

1. Đăng nhập vào tài khoản của bạn

2. Vào trang Profile của bạn

3. Tìm nút "Send Verification Email" hoặc "Gửi lại mã xác thực"

4. Click vào nút đó

5. Kiểm tra email của bạn (có thể vào Spam/Junk folder)

6. Sử dụng mã 6 chữ số trong email để xác thực

## Từ bây giờ trở đi

Khi đăng ký tài khoản mới, bạn sẽ nhận được email có:
- Lời chào mừng
- Mã xác thực 6 chữ số
- Link xác thực trực tiếp
- Thời hạn: 24 giờ

## Kiểm tra trạng thái Email Verification

```sql
-- Kiểm tra xem email của bạn đã được verify chưa
SELECT username, email, is_email_verified 
FROM users 
WHERE username = 'your_username';

-- Xem tất cả token xác thực của bạn
SELECT evt.token, evt.expires_at, evt.is_used, evt.created_at
FROM email_verification_tokens evt
JOIN users u ON evt.user_id = u.user_id
WHERE u.username = 'your_username'
ORDER BY evt.created_at DESC;
```

## Cấu hình Email Server (cho Admin)

Nếu email không được gửi đi, kiểm tra file `application.properties`:

```properties
# Email configuration
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=your-email@gmail.com
spring.mail.password=your-app-password
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
```

Đảm bảo bạn đã bật "App Password" trong Gmail nếu dùng Gmail.

## Lưu ý

- Mã xác thực có hiệu lực 24 giờ
- Mỗi lần yêu cầu gửi lại mã mới phải đợi 60 giây (cooldown)
- Bạn vẫn có thể đăng nhập và sử dụng hệ thống ngay cả khi chưa verify email
- Một số tính năng có thể yêu cầu email đã được xác thực (như đăng ký làm seller)

