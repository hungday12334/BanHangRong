# 🔐 HƯỚNG DẪN SETUP TÀI KHOẢN SELLER

## Bước 1: Chạy SQL này trong MySQL

Mở MySQL Workbench hoặc command line, chạy:

```sql
USE wap;

-- Cập nhật password cho tài khoản seller
UPDATE users 
SET password = '$2a$10$e0MYzXyjpJS7Pd0RVvHwHeFtVpVKxKBCl7Cn6FGa.Ll8xKKqqfLq2'
WHERE username = 'seller';

-- Kiểm tra kết quả
SELECT username, email, user_type, password 
FROM users 
WHERE username = 'seller';
```

## Bước 2: Login

Vào trang login: http://localhost:8080/login

```
Username: seller
Password: 123456
```

## ✅ Nếu vẫn không được

### Giải pháp 1: Dùng password generator

1. Chạy lệnh này trong terminal:
```bash
cd su25
mvnw spring-boot:run -Dspring-boot.run.arguments="--generate-password=123456"
```

2. Copy hash BCrypt từ console
3. Update vào database

### Giải pháp 2: Tạo tài khoản mới qua API

Chạy app, sau đó POST request:

```bash
curl -X POST http://localhost:8080/api/auth/register ^
  -H "Content-Type: application/json" ^
  -d "{\"username\":\"myseller\",\"email\":\"myseller@test.com\",\"password\":\"123456\",\"confirmPassword\":\"123456\",\"phoneNumber\":\"0900111222\",\"gender\":\"male\",\"termsAccepted\":true,\"captchaResponse\":\"test\"}"
```

Sau đó update user_type thành SELLER:
```sql
UPDATE users SET user_type = 'SELLER' WHERE username = 'myseller';
```

### Giải pháp 3: Dùng online BCrypt generator

1. Vào: https://bcrypt-generator.com/
2. Nhập password: `123456`
3. Rounds: `10`
4. Copy hash và chạy SQL:

```sql
UPDATE users 
SET password = 'HASH_VỪA_GENERATE'
WHERE username = 'seller';
```

## 📋 Tài khoản có sẵn trong database

| Username | Password (sau khi chạy SQL) | Role | Email |
|----------|-----------|------|-------|
| seller | 123456 | SELLER | seller@example.com |
| alice | (chưa setup) | CUSTOMER | alice@example.com |
| bob | (chưa setup) | CUSTOMER | bob@example.com |

## ❗ Nếu vẫn lỗi

Kiểm tra:
1. ✅ Database `wap` tồn tại
2. ✅ Bảng `users` có data
3. ✅ Password đã được update (không còn là `$2a$10$hashseller`)
4. ✅ App đã restart sau khi update database
5. ✅ Username/password nhập ĐÚNG (không có space thừa)

