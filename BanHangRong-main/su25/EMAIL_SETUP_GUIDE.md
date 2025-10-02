# 📧 Hướng dẫn cấu hình Email cho BanHangRong

## Vấn đề hiện tại
Email của bạn chưa hoạt động vì cấu hình chưa đúng. Dưới đây là hướng dẫn chi tiết để thiết lập email.

## 🔧 Cấu hình Email

### Bước 1: Chọn nhà cung cấp email

Bạn có thể chọn một trong các tùy chọn sau:

#### Option 1: Gmail (Khuyến nghị)
1. Mở file `src/main/resources/application.properties`
2. Đảm bảo các dòng sau được bật (không có dấu #):
```properties
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=nguyenhung14012k5@gmail.com
spring.mail.password=eqpmnlwwgkwiizgbz
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
spring.mail.properties.mail.smtp.starttls.required=true
spring.mail.properties.mail.smtp.ssl.trust=smtp.gmail.com
```

#### Option 2: Outlook/Hotmail
1. Comment các dòng Gmail (thêm #)
2. Bỏ comment các dòng Outlook:
```properties
# spring.mail.host=smtp.gmail.com
# spring.mail.port=587
# spring.mail.username=nguyenhung14012k5@gmail.com
# spring.mail.password=eqpmnlwwgkwiizgbz

spring.mail.host=smtp-mail.outlook.com
spring.mail.port=587
spring.mail.username=YOUR_EMAIL@outlook.com
spring.mail.password=YOUR_PASSWORD
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
spring.mail.properties.mail.smtp.starttls.required=true
spring.mail.properties.mail.smtp.ssl.trust=smtp-mail.outlook.com
```

### Bước 2: Thiết lập App Password (cho Gmail)

1. Đăng nhập vào Gmail
2. Vào **Settings** > **Security**
3. Bật **2-Step Verification** nếu chưa bật
4. Tạo **App Password**:
   - Vào **Security** > **App passwords**
   - Chọn **Mail** và **Other (Custom name)**
   - Nhập tên: "BanHangRong"
   - Copy mật khẩu 16 ký tự được tạo
5. Thay thế `eqpmnlwwgkwiizgbz` bằng App Password mới

### Bước 3: Test Email

1. Khởi động ứng dụng: `mvn spring-boot:run`
2. Mở trình duyệt: `http://localhost:8080/test-email`
3. Nhập email của bạn và click "Gửi Email Test"
4. Kiểm tra hộp thư email

## 🚀 Luồng Quên Mật Khẩu

Sau khi cấu hình email xong, luồng sẽ hoạt động như sau:

1. **Người dùng vào trang quên mật khẩu**: `/forgot-password`
2. **Nhập email**: Người dùng nhập email của họ
3. **Gửi email**: Hệ thống gửi email chứa link reset
4. **Click link**: Người dùng click vào link trong email
5. **Đặt mật khẩu mới**: Chuyển đến trang `/reset-password?token=...`
6. **Hoàn thành**: Đặt mật khẩu mới thành công

## 🔍 Troubleshooting

### Lỗi "Email chưa được cấu hình"
- Kiểm tra `spring.mail.username` không chứa "your-email" hoặc "YOUR_EMAIL"
- Đảm bảo `spring.mail.password` là App Password thực tế

### Lỗi "Authentication failed"
- Kiểm tra App Password có đúng không
- Đảm bảo 2-Step Verification đã bật
- Thử tạo App Password mới

### Lỗi "Connection timeout"
- Kiểm tra firewall có chặn port 587 không
- Thử đổi sang port 465 với SSL

## 📝 Ghi chú quan trọng

- **App Password** chỉ hiển thị 1 lần, hãy lưu lại cẩn thận
- Không sử dụng mật khẩu Gmail thông thường
- Test email trước khi deploy production
- Link reset password có thời hạn 24 giờ

## 🎯 Test ngay

Sau khi cấu hình xong, hãy test:
1. Vào `http://localhost:8080/test-email`
2. Test gửi email
3. Test tính năng quên mật khẩu
4. Kiểm tra email có nhận được không

---
**Chúc bạn thành công! 🎉**
