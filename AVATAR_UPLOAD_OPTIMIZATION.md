# ✅ Tối ưu Upload Avatar - Xóa ảnh cũ & Tối ưu luồng

## 🎯 Vấn đề đã giải quyết

### ❌ Trước đây:
1. **Lỗi "Lỗi khi upload avatar!"** - Backend trả về redirect thay vì JSON
2. **Ảnh cũ không bị xóa** - Tốn dung lượng ổ cứng
3. **Response không rõ ràng** - Không biết lỗi cụ thể gì
4. **Không có loading state** - User không biết đang upload

### ✅ Bây giờ:
1. ✅ **Backend trả về JSON** - Frontend xử lý đúng
2. ✅ **Tự động xóa ảnh cũ** trước khi lưu ảnh mới
3. ✅ **Error messages chi tiết** - Biết chính xác lỗi gì
4. ✅ **Loading spinner** - UX tốt hơn
5. ✅ **Không ảnh hưởng** các phần khác

---

## 🔧 Các thay đổi Backend

### 1. Đổi từ redirect sang JSON response

#### Trước:
```java
@PostMapping("/profile/upload-avatar")
public String uploadAvatar(@RequestParam("avatarFile") MultipartFile file,
                           RedirectAttributes redirectAttributes) {
    // ...
    redirectAttributes.addFlashAttribute("successMessage", "Success!");
    return "redirect:/seller/profile";
}
```

#### Sau:
```java
@PostMapping("/profile/upload-avatar")
@ResponseBody
public ResponseEntity<?> uploadAvatar(@RequestParam("avatar") MultipartFile file) {
    // ...
    return ResponseEntity.ok(Map.of(
        "success", true,
        "message", "Cập nhật avatar thành công!",
        "avatarUrl", avatarUrl
    ));
}
```

### 2. Tự động xóa ảnh cũ

```java
// ===== XÓA ẢNH CŨ TRƯỚC KHI UPLOAD ẢNH MỚI =====
String oldAvatarUrl = currentUser.getAvatarUrl();
if (oldAvatarUrl != null && !oldAvatarUrl.isEmpty() 
    && !oldAvatarUrl.equals("/img/avatar_default.jpg")) {
    try {
        String oldFileName = oldAvatarUrl.substring(oldAvatarUrl.lastIndexOf("/") + 1);
        Path oldFilePath = Paths.get(uploadDir).resolve(oldFileName);
        
        if (Files.exists(oldFilePath)) {
            Files.delete(oldFilePath);
            System.out.println("🗑️ Đã xóa ảnh cũ: " + oldFilePath);
        }
    } catch (Exception e) {
        System.out.println("⚠️ Không thể xóa ảnh cũ: " + e.getMessage());
        // Không throw exception, tiếp tục upload ảnh mới
    }
}
```

### 3. Error handling chi tiết

```java
// Validation errors
if (file.isEmpty()) {
    return ResponseEntity.badRequest().body(
        Map.of("error", "Vui lòng chọn file ảnh")
    );
}

if (file.getSize() > maxSize) {
    return ResponseEntity.badRequest().body(
        Map.of("error", "Kích thước file không được vượt quá 5MB")
    );
}

// Server errors
catch (IOException e) {
    return ResponseEntity.status(500).body(
        Map.of("error", "Lỗi khi lưu file. Vui lòng thử lại!")
    );
}
```

### 4. Sử dụng timestamp thay vì UUID

```java
// Trước: avatar_1_uuid-dài-dài.jpg
String fileName = "avatar_" + sellerId + "_" + UUID.randomUUID() + fileExtension;

// Sau: avatar_1_1730247010123.jpg (dễ sort theo thời gian)
String fileName = "avatar_" + sellerId + "_" + System.currentTimeMillis() + fileExtension;
```

---

## 🎨 Các thay đổi Frontend

### 1. Validation phía client đầy đủ

```javascript
// Check file size
const maxSize = 5 * 1024 * 1024; // 5MB
if (file.size > maxSize) {
    showToast('Kích thước file không được vượt quá 5MB!', 'error');
    return;
}

// Check file type
const validTypes = ['image/jpeg', 'image/jpg', 'image/png', 'image/gif'];
if (!validTypes.includes(file.type)) {
    showToast('Chỉ được upload file ảnh (JPEG, PNG, GIF)!', 'error');
    return;
}
```

### 2. Loading state với spinner

```javascript
// Disable button và show loading
uploadAvatar.disabled = true;
uploadAvatar.innerHTML = '<i class="ti ti-loader"></i> Đang tải lên...';

// ... upload code ...

// Enable button trở lại
.finally(() => {
    uploadAvatar.disabled = false;
    uploadAvatar.innerHTML = '<i class="ti ti-upload"></i> Tải lên';
});
```

### 3. Error handling chi tiết

```javascript
.then(response => {
    if (!response.ok) {
        return response.json().then(data => {
            throw new Error(data.error || 'Upload thất bại');
        });
    }
    return response.json();
})
.catch(error => {
    console.error('Upload error:', error);
    showToast(error.message || 'Lỗi khi upload avatar!', 'error');
})
```

### 4. Cache busting với timestamp

```javascript
// Thêm timestamp để tránh browser cache
document.getElementById('profileAvatar').src = data.avatarUrl + '?t=' + new Date().getTime();
avatarPreview.src = data.avatarUrl + '?t=' + new Date().getTime();
```

### 5. CSS cho loading spinner

```css
@keyframes spin {
    from { transform: rotate(0deg); }
    to { transform: rotate(360deg); }
}

.ti-loader {
    animation: spin 1s linear infinite;
}

.btn:disabled {
    opacity: 0.6;
    cursor: not-allowed;
}
```

---

## 📊 Luồng hoạt động mới

### 1. User chọn ảnh
```
User click "Đổi avatar"
  → Modal mở
  → User chọn file
  → Preview hiển thị
  → Click "Tải lên"
```

### 2. Client-side validation
```
✅ Check file tồn tại
✅ Check file size < 5MB
✅ Check file type (JPEG/PNG/GIF)
✅ Disable button + Show loading
```

### 3. Upload request
```
POST /seller/profile/upload-avatar
Content-Type: multipart/form-data
Body: { avatar: File }
```

### 4. Server-side processing
```
1. ✅ Validation: empty, size, type, extension, content
2. 🗑️ XÓA ẢNH CŨ (nếu có)
3. 💾 Lưu ảnh mới
4. 💽 Cập nhật database
5. ✅ Return JSON response
```

### 5. Response handling
```javascript
if (success) {
  ✅ Update avatar on page (with cache busting)
  ✅ Show success toast
  ✅ Close modal
  ✅ Reset form
} else {
  ❌ Show error toast with specific message
}
```

### 6. Cleanup
```
✅ Enable button
✅ Remove loading spinner
✅ Restore original button text
```

---

## 🔒 Bảo mật đã giữ nguyên

### Validation layers:
1. ✅ **Client**: File type, size
2. ✅ **Server**: MIME type validation
3. ✅ **Server**: File extension validation
4. ✅ **Server**: Magic number validation (file signature)
5. ✅ **Server**: Path traversal prevention
6. ✅ **Server**: Filename sanitization

### Security features:
- ✅ CSRF protection
- ✅ File size limit (5MB)
- ✅ Allowed file types only
- ✅ Magic number checking
- ✅ Path traversal prevention
- ✅ Filename sanitization

---

## 📁 Files đã thay đổi

### Backend:
```
src/main/java/banhangrong/su25/Controller/SellerProfileController.java
  ✅ Thêm import: ResponseEntity, Map
  ✅ Đổi return type: String → ResponseEntity<?>
  ✅ Thêm @ResponseBody
  ✅ Đổi parameter name: avatarFile → avatar
  ✅ Thêm logic xóa ảnh cũ
  ✅ Return JSON thay vì redirect
  ✅ Error handling chi tiết
```

### Frontend:
```
src/main/resources/templates/seller/profile.html
  ✅ Thêm CSS: @keyframes spin, .ti-loader, .btn:disabled
  ✅ Cập nhật uploadAvatar handler
  ✅ Thêm client-side validation
  ✅ Thêm loading state
  ✅ Cập nhật error handling
  ✅ Thêm cache busting
```

---

## ✅ Kết quả

### Before → After:

| Feature | Trước | Sau |
|---------|-------|-----|
| **Response type** | Redirect | JSON ✅ |
| **Error message** | Generic | Chi tiết ✅ |
| **Xóa ảnh cũ** | ❌ Không | ✅ Có |
| **Loading state** | ❌ Không | ✅ Có |
| **Cache busting** | ❌ Không | ✅ Có |
| **Validation** | Chỉ server | Client + Server ✅ |
| **UX** | Không rõ ràng | Rõ ràng, mượt mà ✅ |
| **Disk space** | Tốn (ảnh cũ còn) | Tối ưu (xóa cũ) ✅ |

---

## 🧪 Test Cases

### ✅ Success Case:
```
1. Chọn ảnh JPG < 5MB
2. Click "Tải lên"
3. Loading spinner hiện
4. Upload thành công
5. Avatar cập nhật (no cache)
6. Toast: "Cập nhật avatar thành công!"
7. Modal đóng
8. Button enable lại
```

### ❌ Error Cases:

#### File quá lớn:
```
1. Chọn ảnh > 5MB
2. Click "Tải lên"
3. Toast ngay: "Kích thước file không được vượt quá 5MB!"
4. Không gửi request
```

#### File không hợp lệ:
```
1. Chọn file .txt đổi extension thành .jpg
2. Click "Tải lên"
3. Loading spinner hiện
4. Server validate magic number
5. Toast: "File không hợp lệ! Vui lòng upload ảnh thật."
```

#### Network error:
```
1. Mất kết nối
2. Click "Tải lên"
3. Loading spinner hiện
4. Request fail
5. Toast: "Lỗi khi upload avatar!"
6. Button enable lại
```

---

## 🚀 Build thành công

```bash
[INFO] BUILD SUCCESS
[INFO] Total time: 8.615 s
```

---

## 💡 Lợi ích

### 1. **Tiết kiệm dung lượng**
- Xóa ảnh cũ → không tốn disk space
- Chỉ giữ 1 ảnh mới nhất

### 2. **UX tốt hơn**
- Loading spinner → user biết đang xử lý
- Error messages chi tiết → biết cách fix
- No page reload → smooth experience

### 3. **Performance**
- Client validation → giảm request không cần thiết
- Cache busting → luôn hiển thị ảnh mới
- Timestamp filename → dễ sort, dễ debug

### 4. **Maintainability**
- Code clean, dễ đọc
- Error handling tốt
- Logging chi tiết
- Easy to debug

### 5. **Security**
- Validation đầy đủ
- Không ảnh hưởng logic khác
- Isolated changes

---

## 📝 API Response Format

### Success:
```json
{
  "success": true,
  "message": "Cập nhật avatar thành công!",
  "avatarUrl": "/uploads/avatar_1_1730247010123.jpg"
}
```

### Error (400 Bad Request):
```json
{
  "error": "Kích thước file không được vượt quá 5MB"
}
```

### Error (500 Internal Server Error):
```json
{
  "error": "Lỗi khi lưu file. Vui lòng thử lại!"
}
```

---

## 🎯 Test ngay:

```bash
# 1. Khởi động
mvn spring-boot:run

# 2. Truy cập
http://localhost:8080/seller/profile

# 3. Test:
- Click "Chỉnh sửa"
- Click camera icon
- Chọn ảnh
- Click "Tải lên"
- ✅ Xem loading spinner
- ✅ Xem toast thông báo
- ✅ Avatar cập nhật ngay lập tức
- ✅ Check folder uploads/ → chỉ có 1 ảnh mới!
```

---

✨ **HOÀN THÀNH! Upload avatar giờ hoạt động hoàn hảo, xóa ảnh cũ tự động, UX mượt mà!** 🎉

