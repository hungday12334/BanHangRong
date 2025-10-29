# API Resend Email Verification

## Endpoint mới để gửi lại email xác thực

### POST `/api/auth/resend-verification`

**Mô tả**: Gửi lại email xác thực cho user đã đăng ký nhưng chưa verify email

**Parameters**:
- `username` (query param, required): Username của user cần gửi lại email

**Response Success (200)**:
```json
{
  "message": "Verification email has been sent to your email address"
}
```

**Response Error (400)**:
```json
{
  "error": "User not found"
}
```
hoặc
```json
{
  "error": "Email is already verified"
}
```

---

## Cách sử dụng

### 1. Dùng cURL:
```bash
curl -X POST "http://localhost:8080/api/auth/resend-verification?username=your_username"
```

### 2. Dùng Postman:
- Method: POST
- URL: `http://localhost:8080/api/auth/resend-verification`
- Params:
  - Key: `username`
  - Value: `your_username`

### 3. Dùng JavaScript (fetch):
```javascript
fetch('http://localhost:8080/api/auth/resend-verification?username=your_username', {
  method: 'POST'
})
.then(response => response.json())
.then(data => console.log(data))
.catch(error => console.error('Error:', error));
```

### 4. Dùng jQuery (ajax):
```javascript
$.ajax({
  url: '/api/auth/resend-verification',
  method: 'POST',
  data: { username: 'your_username' },
  success: function(response) {
    alert(response.message);
  },
  error: function(xhr) {
    alert(xhr.responseJSON.error);
  }
});
```

---

## Test nhanh

Sau khi đăng ký user mới và muốn gửi lại email:

```bash
# Thay 'testuser' bằng username thật
curl -X POST "http://localhost:8080/api/auth/resend-verification?username=testuser"
```

Kết quả:
- Email mới sẽ được gửi với mã xác thực 6 chữ số
- Token cũ (nếu có) sẽ bị xóa
- Token mới có hiệu lực 24 giờ

---

## Flow hoàn chỉnh

1. User đăng ký → Nhận email với mã xác thực
2. Nếu không nhận được email → Call API `/api/auth/resend-verification`
3. Nhận email mới với mã mới
4. Verify bằng 1 trong 2 cách:
   - Click link trong email: `http://localhost:8080/customer/verify-email?token=123456`
   - Vào `/customer/verify-code` và nhập mã

---

## Notes

- API này không yêu cầu authentication
- Có thể gọi nhiều lần nhưng mỗi lần gọi sẽ tạo token mới và xóa token cũ
- Nếu user đã verify email rồi, API sẽ trả về lỗi
- Email chỉ được gửi nếu user tồn tại trong database

