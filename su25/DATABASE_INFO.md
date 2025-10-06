# Thông tin Database - BanHangRong

## Cấu hình Database

### Thay đổi từ In-Memory sang File-based Database

**Trước đây (In-Memory):**
- Database chỉ tồn tại trong RAM
- Dữ liệu bị mất khi tắt ứng dụng
- Cấu hình: `jdbc:h2:mem:testdb` với `ddl-auto=create-drop`

**Bây giờ (File-based):**
- Database được lưu trong file: `./data/banhangrong_db.mv.db`
- Dữ liệu được lưu vĩnh viễn
- Cấu hình: `jdbc:h2:file:./data/banhangrong_db` với `ddl-auto=update`

## Tài khoản Test có sẵn

Khi khởi động ứng dụng, hệ thống sẽ tự động tạo các tài khoản test sau:

1. **nguyenhung1401** / **123456** (USER)
2. **admin** / **admin123** (ADMIN) 
3. **testuser** / **test123** (USER)

## Cách sử dụng

### 1. Khởi động ứng dụng
```bash
mvn spring-boot:run
```

### 2. Đăng nhập
- Truy cập: `http://localhost:8080/login`
- Sử dụng một trong các tài khoản test ở trên

### 3. Đăng ký tài khoản mới
- Truy cập: `http://localhost:8080/register`
- Đăng ký tài khoản mới
- Tài khoản sẽ được lưu vĩnh viễn

### 4. Truy cập H2 Console (để xem database)
- URL: `http://localhost:8080/h2-console`
- JDBC URL: `jdbc:h2:file:./data/banhangrong_db`
- Username: `sa`
- Password: (để trống)

## Lưu ý quan trọng

✅ **Dữ liệu được lưu vĩnh viễn**: Tài khoản đăng ký sẽ không bị mất khi tắt ứng dụng

✅ **Chỉ Admin có thể xóa**: Tài khoản admin có quyền quản lý người dùng

✅ **Database file**: Được lưu tại `./data/banhangrong_db.mv.db`

## Troubleshooting

### Nếu không thể đăng nhập sau khi tắt/bật lại ứng dụng:

1. Kiểm tra file database có tồn tại không:
   ```
   ./data/banhangrong_db.mv.db
   ```

2. Kiểm tra log khi khởi động ứng dụng:
   ```
   === ĐANG TẠO TÀI KHOẢN TEST ===
   ✓ User nguyenhung1401 đã tồn tại
   ✓ User admin đã tồn tại
   ✓ User testuser đã tồn tại
   === HOÀN THÀNH TẠO TÀI KHOẢN TEST ===
   ```

3. Nếu vẫn không được, xóa file database và khởi động lại:
   ```bash
   rm ./data/banhangrong_db.mv.db
   mvn spring-boot:run
   ```
