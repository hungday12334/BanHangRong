# 📧 Hướng Dẫn Cấu Hình Email Chi Tiết

## 🚨 Lỗi "Authentication failed" - Giải pháp

### **Nguyên nhân:**
- App Password Gmail đã hết hạn hoặc không đúng
- Chưa bật 2-Factor Authentication
- Cấu hình email sai

---

## 🛠️ **GIẢI PHÁP CHO GMAIL (Khuyến nghị)**

### **Bước 1: Bật 2-Factor Authentication**
1. Truy cập: https://myaccount.google.com/
2. Chọn **Security** (Bảo mật)
3. Tìm **2-Step Verification** → Click **Get Started**
4. Làm theo hướng dẫn để bật 2FA

### **Bước 2: Tạo App Password**
1. Sau khi bật 2FA, quay lại **Security**
2. Chọn **2-Step Verification**
3. Cuộn xuống tìm **App passwords**
4. Chọn **Mail** → **Other (Custom name)**
5. Nhập tên: `BanHangRong`
6. Click **Generate**
7. **Copy mật khẩu 16 ký tự** (ví dụ: `abcd efgh ijkl mnop`)

### **Bước 3: Cập nhật application.properties**
```properties
# Thay thông tin thực tế của bạn
spring.mail.username=your-email@gmail.com
spring.mail.password=abcdefghijklmnop
```

---

## 🔄 **GIẢI PHÁP THAY THẾ - OUTLOOK**

Nếu Gmail không hoạt động, dùng Outlook:

### **Cấu hình Outlook:**
```properties
spring.mail.host=smtp-mail.outlook.com
spring.mail.port=587
spring.mail.username=your-email@outlook.com
spring.mail.password=your-outlook-password
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
spring.mail.properties.mail.smtp.starttls.required=true
spring.mail.properties.mail.smtp.ssl.trust=smtp-mail.outlook.com
```

---

## 🧪 **TEST EMAIL**

Sau khi cấu hình, test bằng cách:

1. **Restart ứng dụng**
2. **Truy cập**: http://localhost:8080/forgot-password
3. **Nhập email** và nhấn gửi
4. **Kiểm tra console** xem có lỗi không

---

## 🔧 **TROUBLESHOOTING**

### **Lỗi thường gặp:**

1. **"Authentication failed"**
   - ✅ Kiểm tra App Password đúng chưa
   - ✅ Bật 2FA cho Gmail
   - ✅ Copy đúng 16 ký tự (không có dấu cách)

2. **"Connection timeout"**
   - ✅ Kiểm tra kết nối internet
   - ✅ Thử port 465 thay vì 587

3. **"Username/Password not accepted"**
   - ✅ Dùng App Password, không phải mật khẩu Gmail
   - ✅ Kiểm tra email đúng định dạng

---

## 📝 **BACKUP PLAN**

Nếu vẫn không gửi được email, hệ thống sẽ:
- ✅ Tạo reset link trong file `reset-password-link.txt`
- ✅ Hiển thị link trong console
- ✅ User có thể copy link để reset password

---

## 🎯 **KIỂM TRA THÀNH CÔNG**

Khi cấu hình đúng, bạn sẽ thấy:
```
✅ Email đặt lại mật khẩu đã được gửi đến: user@example.com
🔗 Reset URL: http://localhost:8080/reset-password?token=...
```

Thay vì:
```
❌ Lỗi khi gửi email: Authentication failed
```
