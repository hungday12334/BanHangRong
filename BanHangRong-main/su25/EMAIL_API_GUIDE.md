# Hướng dẫn API Email - BanHangRong

## 📧 Các API Email có sẵn

### 1. **API Gửi Email Quên Mật Khẩu**
```http
POST /api/auth/forgot-password
Content-Type: application/x-www-form-urlencoded

email=user@example.com
```

**Response thành công:**
```json
{
  "message": "Email đặt lại mật khẩu đã được gửi đến user@example.com"
}
```

**Response lỗi:**
```json
{
  "error": "Email không tồn tại trong hệ thống"
}
```

### 2. **API Test Email**
```http
POST /api/auth/test-email?email=user@example.com
```

**Response thành công:**
```json
{
  "message": "Email test đã được gửi thành công!"
}
```

### 3. **API Reset Password**
```http
POST /api/auth/reset-password
Content-Type: application/json

{
  "token": "reset-token-from-email",
  "newPassword": "newpassword123",
  "confirmPassword": "newpassword123"
}
```

## 🧪 Trang Test Email

Truy cập: `http://localhost:8080/test-email`

**Tính năng:**
- Test gửi email với token mẫu
- Test chức năng quên mật khẩu
- Hiển thị kết quả real-time
- Hướng dẫn sử dụng

## 📋 Các dịch vụ Email API phổ biến

### 1. **Gmail SMTP** (Đang sử dụng)
```properties
# application.properties
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=your-email@gmail.com
spring.mail.password=your-app-password
```

**Ưu điểm:**
- ✅ Miễn phí
- ✅ Dễ cấu hình
- ✅ Deliverability tốt

**Nhược điểm:**
- ❌ Cần App Password
- ❌ Giới hạn 500 email/ngày
- ❌ Có thể bị spam

### 2. **SendGrid**
```properties
spring.mail.host=smtp.sendgrid.net
spring.mail.port=587
spring.mail.username=apikey
spring.mail.password=your-sendgrid-api-key
```

**Ưu điểm:**
- ✅ Chuyên nghiệp
- ✅ Deliverability cao
- ✅ Analytics chi tiết
- ✅ 100 email/ngày miễn phí

**Nhược điểm:**
- ❌ Có phí sau quota miễn phí
- ❌ Cần đăng ký account

### 3. **Mailgun**
```properties
spring.mail.host=smtp.mailgun.org
spring.mail.port=587
spring.mail.username=your-mailgun-username
spring.mail.password=your-mailgun-password
```

**Ưu điểm:**
- ✅ API mạnh mẽ
- ✅ Analytics tốt
- ✅ 5,000 email/tháng miễn phí

**Nhược điểm:**
- ❌ Có phí sau quota miễn phí
- ❌ Cần verify domain

### 4. **Amazon SES**
```properties
spring.mail.host=email-smtp.us-east-1.amazonaws.com
spring.mail.port=587
spring.mail.username=your-ses-access-key
spring.mail.password=your-ses-secret-key
```

**Ưu điểm:**
- ✅ Rất rẻ ($0.10/1000 email)
- ✅ Scalable
- ✅ Tích hợp AWS

**Nhược điểm:**
- ❌ Cần AWS account
- ❌ Cần verify email/domain

## 🔧 Cách thay đổi Email Provider

### Bước 1: Cập nhật application.properties
```properties
# Thay đổi cấu hình SMTP
spring.mail.host=new-smtp-host
spring.mail.port=587
spring.mail.username=new-username
spring.mail.password=new-password
```

### Bước 2: Test cấu hình
1. Truy cập: `http://localhost:8080/test-email`
2. Nhập email của bạn
3. Click "Gửi Email Test"
4. Kiểm tra hộp thư

### Bước 3: Kiểm tra log
```bash
# Xem log khi gửi email
tail -f logs/application.log
```

## 🚀 Tính năng Email hiện có

### ✅ **Đã hoàn thành:**
- Gửi email quên mật khẩu
- Gửi email chào mừng
- Test email functionality
- Fallback mode (hiển thị trong console)
- Tạo file reset link

### 🔄 **Có thể cải thiện:**
- Email template HTML đẹp hơn
- Email verification khi đăng ký
- Email notification cho các hoạt động
- Email marketing
- Email analytics

## 📞 Troubleshooting

### Lỗi "Authentication failed"
```
❌ Lỗi: Authentication failed
```
**Giải pháp:**
1. Kiểm tra username/password
2. Với Gmail: sử dụng App Password
3. Kiểm tra 2FA đã bật

### Lỗi "Connection timeout"
```
❌ Lỗi: Connection timeout
```
**Giải pháp:**
1. Kiểm tra firewall
2. Kiểm tra port (587/465)
3. Thử port khác

### Email không đến
**Giải pháp:**
1. Kiểm tra spam folder
2. Kiểm tra email address
3. Test với email khác
4. Kiểm tra log server

## 💡 Tips

1. **Sử dụng App Password cho Gmail** thay vì password thường
2. **Test với nhiều email provider** khác nhau
3. **Monitor email deliverability** để tránh spam
4. **Sử dụng email template** chuyên nghiệp
5. **Backup email configuration** để dễ restore
