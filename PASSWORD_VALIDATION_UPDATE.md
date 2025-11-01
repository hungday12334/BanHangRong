# Cập Nhật Validation Mật Khẩu - Seller Profile

## Ngày cập nhật: 02/11/2025

## Tóm tắt thay đổi

Đã thêm validation **không cho phép mật khẩu chứa dấu cách** trong chức năng đổi mật khẩu của seller.

## Chi tiết thay đổi

### 1. **Backend - SellerProfileController.java**

#### Thêm Validation 5: Kiểm tra mật khẩu không chứa dấu cách
```java
// ===== VALIDATION 5: Check password does not contain spaces =====
if (newPassword.contains(" ")) {
    return ResponseEntity.badRequest().body(Map.of("error", "Mật khẩu không được chứa dấu cách"));
}
```

#### Thay đổi cấu trúc response
- **Trước đây**: Method `changePassword` trả về `String` (redirect) với `RedirectAttributes`
- **Bây giờ**: Method `changePassword` trả về `ResponseEntity<?>` với JSON response
  - ✅ Thành công: `{"success": true, "message": "Đổi mật khẩu thành công!"}`
  - ❌ Lỗi: `{"error": "Thông báo lỗi cụ thể"}`

#### Các validation hiện tại (theo thứ tự):
1. **VALIDATION 1**: Kiểm tra các trường không được để trống
2. **VALIDATION 2**: Xác minh mật khẩu hiện tại đúng
3. **VALIDATION 3**: Mật khẩu mới tối thiểu 6 ký tự
4. **VALIDATION 4**: Mật khẩu mới tối đa 100 ký tự
5. **VALIDATION 5**: ⭐ **Mật khẩu không được chứa dấu cách** (MỚI)
6. **VALIDATION 6**: Mật khẩu xác nhận phải khớp
7. **VALIDATION 7**: Mật khẩu mới phải khác mật khẩu hiện tại

### 2. **Frontend - seller/profile.html**

#### Cập nhật JavaScript xử lý form đổi mật khẩu

**Client-side validation (kiểm tra ngay tại browser):**
```javascript
// Kiểm tra dấu cách
if (newPassword.includes(' ')) {
    showToast('Mật khẩu không được chứa dấu cách!', 'error');
    return;
}

// Kiểm tra độ dài tối thiểu (6 ký tự)
if (newPassword.length < 6) {
    showToast('Mật khẩu phải có ít nhất 6 ký tự!', 'error');
    return;
}

// Kiểm tra khớp mật khẩu
if (newPassword !== confirmPassword) {
    showToast('Mật khẩu xác nhận không khớp!', 'error');
    return;
}
```

**Server response handling:**
```javascript
.then(response => response.json())
.then(data => {
    if (data.success) {
        showToast(data.message || 'Đổi mật khẩu thành công!', 'success');
        passwordModal.style.display = 'none';
        passwordForm.reset();
    } else if (data.error) {
        showToast(data.error, 'error'); // Hiển thị lỗi từ server
    }
})
```

## Kết quả

### ✅ Trước khi cập nhật
- Nhập mật khẩu có dấu cách → Hiển thị "Đổi mật khẩu thành công" nhưng thực tế không đổi
- Không có thông báo lỗi rõ ràng

### ✅ Sau khi cập nhật
- **Client-side**: Ngay lập tức hiển thị lỗi khi nhập dấu cách
- **Server-side**: Nếu bypass client validation, server vẫn reject với thông báo lỗi cụ thể
- **Thông báo rõ ràng**: "Mật khẩu không được chứa dấu cách"
- **Không cho phép đổi mật khẩu** cho đến khi nhập mật khẩu hợp lệ

## Testing

### Test Case 1: Nhập mật khẩu có dấu cách
```
Mật khẩu hiện tại: oldpassword123
Mật khẩu mới: new pass 123
Xác nhận: new pass 123

Kết quả: ❌ "Mật khẩu không được chứa dấu cách"
```

### Test Case 2: Nhập mật khẩu hợp lệ
```
Mật khẩu hiện tại: oldpassword123
Mật khẩu mới: newpassword123
Xác nhận: newpassword123

Kết quả: ✅ "Đổi mật khẩu thành công!"
```

### Test Case 3: Mật khẩu quá ngắn
```
Mật khẩu hiện tại: oldpassword123
Mật khẩu mới: 12345
Xác nhận: 12345

Kết quả: ❌ "Mật khẩu phải có ít nhất 6 ký tự!"
```

### Test Case 4: Mật khẩu xác nhận không khớp
```
Mật khẩu hiện tại: oldpassword123
Mật khẩu mới: newpassword123
Xác nhận: newpassword456

Kết quả: ❌ "Mật khẩu xác nhận không khớp!"
```

## Tệp đã thay đổi

1. `/src/main/java/banhangrong/su25/Controller/SellerProfileController.java`
   - Thêm validation kiểm tra dấu cách
   - Chuyển từ redirect sang JSON response

2. `/src/main/resources/templates/seller/profile.html`
   - Thêm client-side validation kiểm tra dấu cách
   - Cập nhật xử lý JSON response từ server

## Build Status

✅ **BUILD SUCCESS** - Đã compile và package thành công
```
Total time:  14.739 s
```

## Ghi chú

- Validation hoạt động cả ở **client-side** (JavaScript) và **server-side** (Java)
- Thông báo lỗi hiển thị bằng **toast notification** rõ ràng và user-friendly
- Tất cả validation messages đều bằng **tiếng Việt**
- Mật khẩu cũ vẫn được giữ nguyên nếu có bất kỳ validation nào fail

