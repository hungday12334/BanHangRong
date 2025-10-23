# Shop License Management Feature - Documentation

## Tổng quan
Tính năng quản lý Licenses/Giấy phép cho phép người bán quản lý các giấy phép kinh doanh, chứng nhận của cửa hàng một cách dễ dàng và hiệu quả.

## Các tính năng chính

### 1. **Nút License trong Category Management**
- Được đặt giữa nút **Edit** và nút **More** (dấu 3 chấm)
- Icon: 🏷️ (ti-license)
- Màu sắc: Cam/Vàng (#f59e0b)
- Chức năng: Mở modal quản lý licenses

### 2. **Modal Quản lý Licenses**
Modal chứa 2 phần chính:

#### A. Thêm License mới
- **Tên License** (bắt buộc): Tên giấy phép/chứng nhận
- **Mã (Code)** (bắt buộc): Mã định danh
- **Mô tả**: Thông tin chi tiết về license
- **Icon**: Biểu tượng hiển thị (mặc định: ti-license)
- **Màu sắc**: Màu định danh cho license

#### B. Danh sách Licenses hiện có
Mỗi license hiển thị:
- Tên và trạng thái (Hoạt động/Tạm dừng)
- Loại license
- Số hiệu
- Mô tả
- Ngày cấp và ngày hết hạn
- 3 nút thao tác:
  - **Toggle**: Bật/Tắt trạng thái
  - **Edit**: Chỉnh sửa thông tin
  - **Delete**: Xóa license

### 3. **API Endpoints**

```
GET    /seller/licenses/list          - Lấy danh sách licenses
POST   /seller/licenses/add           - Thêm license mới
PUT    /seller/licenses/update/{id}   - Cập nhật license
PUT    /seller/licenses/toggle-status/{id} - Bật/Tắt trạng thái
DELETE /seller/licenses/delete/{id}   - Xóa license
GET    /seller/licenses/stats         - Thống kê licenses
```

## Cài đặt

### Bước 1: Tạo bảng Database
Chạy file SQL:
```bash
mysql -u [username] -p [database_name] < create_shop_licenses_table.sql
```

Hoặc chạy trực tiếp trong MySQL:
```sql
source create_shop_licenses_table.sql;
```

### Bước 2: Khởi động ứng dụng
```bash
mvn clean install
mvn spring-boot:run
```

### Bước 3: Truy cập trang quản lý
```
http://localhost:8080/seller/categories
```

## Cấu trúc Database

### Bảng: shop_licenses
```sql
shop_license_id BIGINT PRIMARY KEY AUTO_INCREMENT
seller_id       BIGINT NOT NULL (FK -> users.user_id)
license_name    VARCHAR(100) NOT NULL
license_type    VARCHAR(50)
license_number  VARCHAR(100)
description     TEXT
issue_date      DATETIME
expiry_date     DATETIME
is_active       BOOLEAN DEFAULT TRUE
status          VARCHAR(20) DEFAULT 'ACTIVE'
document_url    VARCHAR(500)
created_at      DATETIME
updated_at      DATETIME
```

## Cấu trúc Code

### 1. Entity
- `ShopLicenses.java` - JPA Entity cho bảng shop_licenses

### 2. Repository
- `ShopLicensesRepository.java` - JPA Repository với các query methods

### 3. Controller
- `SellerLicenseController.java` - REST Controller xử lý API requests

### 4. Frontend
- **HTML**: `category-management.html` (đã được cập nhật)
- **CSS**: `category-management.css` (đã thêm license styles)
- **JavaScript**: Các functions quản lý licenses được thêm vào `category-management.html`

## Sử dụng

### Thêm License mới
1. Click vào nút "Quản lý Licenses" ở header hoặc nút License (🏷️) ở cột thao tác
2. Điền thông tin vào form "Thêm License mới"
3. Click "Thêm License"
4. License mới sẽ xuất hiện trong danh sách bên dưới

### Chỉnh sửa License
1. Click nút "Edit" (✏️) bên cạnh license cần sửa
2. Nhập thông tin mới vào các dialog boxes
3. Thông tin được cập nhật tự động

### Bật/Tắt License
1. Click nút "Toggle" (🔄) để thay đổi trạng thái
2. License sẽ chuyển giữa trạng thái "Hoạt động" ↔ "Tạm dừng"

### Xóa License
1. Click nút "Delete" (🗑️)
2. Xác nhận xóa trong dialog
3. License bị xóa khỏi hệ thống

## Tính năng nâng cao có thể mở rộng

### 1. Upload tài liệu
- Thêm field upload file PDF/Image cho giấy phép
- Lưu vào `document_url` field

### 2. Thông báo hết hạn
- Gửi email/notification khi license sắp hết hạn
- Check `expiry_date` và gửi cảnh báo trước 30/15/7 ngày

### 3. Phân loại theo Category
- Gắn licenses với categories cụ thể
- Yêu cầu license cho từng loại sản phẩm

### 4. Xác thực Admin
- Admin có thể duyệt/từ chối licenses
- Thêm field `verification_status` và `verified_by`

### 5. Lịch sử thay đổi
- Log tất cả thay đổi về licenses
- Audit trail cho compliance

## Troubleshooting

### Lỗi: Cannot resolve table 'shop_licenses'
**Giải pháp**: Chạy file SQL để tạo bảng:
```bash
mysql -u root -p banhangrong_db < create_shop_licenses_table.sql
```

### Lỗi: 401 Unauthorized
**Giải pháp**: Đảm bảo đã đăng nhập với tài khoản Seller

### Nút License không hiển thị
**Giải pháp**: 
1. Clear cache trình duyệt (Ctrl+Shift+R)
2. Kiểm tra file CSS đã được cập nhật
3. Xem Console để kiểm tra lỗi JavaScript

### Modal không mở
**Giải pháp**:
1. Kiểm tra JavaScript console (F12)
2. Đảm bảo các functions đã được thêm vào file HTML
3. Kiểm tra modal có id="licenseManagerModal"

## Testing

### Test thủ công
1. ✅ Thêm license mới với đầy đủ thông tin
2. ✅ Thêm license chỉ với thông tin bắt buộc
3. ✅ Chỉnh sửa license đã tồn tại
4. ✅ Bật/Tắt trạng thái license
5. ✅ Xóa license
6. ✅ Kiểm tra danh sách rỗng
7. ✅ Kiểm tra hiển thị responsive trên mobile

### Test API với cURL
```bash
# Get licenses
curl -X GET http://localhost:8080/seller/licenses/list

# Add license
curl -X POST http://localhost:8080/seller/licenses/add \
  -H "Content-Type: application/json" \
  -d '{
    "licenseName": "Test License",
    "licenseType": "GENERAL",
    "licenseNumber": "TEST-001",
    "description": "Test description"
  }'

# Toggle status
curl -X PUT http://localhost:8080/seller/licenses/toggle-status/1

# Delete license
curl -X DELETE http://localhost:8080/seller/licenses/delete/1
```

## Best Practices

1. **Validation**: Luôn validate input trước khi gửi lên server
2. **Error Handling**: Hiển thị thông báo lỗi rõ ràng cho người dùng
3. **Loading States**: Hiển thị trạng thái loading khi gọi API
4. **Confirmation**: Yêu cầu xác nhận trước khi xóa
5. **Security**: Kiểm tra quyền truy cập ở cả frontend và backend

## Support & Contact

Nếu có vấn đề hoặc câu hỏi, vui lòng:
1. Kiểm tra phần Troubleshooting ở trên
2. Xem logs trong Console (F12)
3. Kiểm tra logs server trong file `app.log`

---

**Version**: 1.0.0  
**Last Updated**: October 23, 2025  
**Author**: Development Team

