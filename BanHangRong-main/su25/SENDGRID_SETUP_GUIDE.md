# 📧 Hướng Dẫn Cấu Hình SendGrid API

## 🚀 **Tại sao chọn SendGrid?**
- ✅ **Miễn phí 100 email/ngày** (đủ cho development)
- ✅ **Độ tin cậy cao** (99.9% uptime)
- ✅ **Dễ setup** (chỉ cần API key)
- ✅ **Email đẹp** (hỗ trợ HTML template)
- ✅ **Tracking** (theo dõi email đã gửi)

---

## 🛠️ **Hướng Dẫn Setup SendGrid**

### **Bước 1: Tạo tài khoản SendGrid**
1. Truy cập: https://sendgrid.com/
2. Click **"Start for Free"**
3. Điền thông tin đăng ký
4. Xác thực email

### **Bước 2: Tạo API Key**
1. Đăng nhập SendGrid Dashboard
2. Vào **Settings** → **API Keys**
3. Click **"Create API Key"**
4. Chọn **"Restricted Access"**
5. Tên API Key: `BanHangRong`
6. Permissions:
   - **Mail Send**: Full Access
   - **Mail Settings**: Read Access
7. Click **"Create & View"**
8. **Copy API Key** (dạng: `SG.xxxxxx...`)

### **Bước 3: Cấu hình Domain (Tùy chọn)**
1. Vào **Settings** → **Sender Authentication**
2. **Domain Authentication** → **Authenticate Your Domain**
3. Nhập domain của bạn (hoặc dùng subdomain)
4. Làm theo hướng dẫn DNS

### **Bước 4: Cập nhật application.properties**
```properties
# Thay YOUR_SENDGRID_API_KEY bằng API key thực tế
sendgrid.api.key=SG.xxxxxxxxxxxxxxxxxxxxxx
sendgrid.from.email=noreply@yourdomain.com
sendgrid.from.name=BanHangRong
```

---

## 🧪 **Test SendGrid**

### **Test ngay không cần domain:**
```properties
# Dùng email đã verify trong SendGrid
sendgrid.from.email=your-verified-email@gmail.com
```

### **Restart ứng dụng và test:**
1. Chạy ứng dụng
2. Truy cập: http://localhost:8080/forgot-password
3. Nhập email và gửi
4. Kiểm tra email nhận được

---

## 🔄 **Fallback System**

Hệ thống được thiết kế với fallback:
1. **Ưu tiên**: SendGrid API
2. **Fallback**: SMTP Gmail (nếu SendGrid lỗi)
3. **Emergency**: Tạo file reset link

---

## 📊 **Monitoring**

### **SendGrid Dashboard:**
- Theo dõi email đã gửi
- Tỷ lệ delivery
- Bounce rate
- Click tracking

### **Console Logs:**
```
✅ Email đặt lại mật khẩu đã được gửi đến: user@example.com
🔗 Reset URL: http://localhost:8080/reset-password?token=...
```

---

## 🚨 **Troubleshooting**

### **Lỗi thường gặp:**

1. **"API key chưa được cấu hình"**
   - Kiểm tra `sendgrid.api.key` trong application.properties
   - Đảm bảo API key đúng format `SG.xxx`

2. **"403 Forbidden"**
   - API key không có quyền Mail Send
   - Tạo lại API key với Full Access

3. **"From email not verified"**
   - Verify email trong SendGrid Dashboard
   - Hoặc setup domain authentication

4. **"Daily limit exceeded"**
   - Free plan: 100 emails/ngày
   - Upgrade plan hoặc đợi ngày mới

---

## 💡 **Tips**

### **Development:**
- Dùng email cá nhân đã verify
- Test với ít email trước

### **Production:**
- Setup domain authentication
- Dùng email domain riêng
- Monitor delivery rate

---

## 🎯 **Kết quả mong đợi**

Sau khi setup thành công:
- ✅ Email gửi qua SendGrid API
- ✅ Template HTML đẹp mắt
- ✅ Tracking và monitoring
- ✅ Fallback SMTP khi cần
- ✅ Reset link backup trong file

**Chúc bạn setup thành công! 🚀**
