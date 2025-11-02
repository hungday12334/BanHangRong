# License Management Feature - Category Management

## Tổng quan
Tính năng này cho phép seller quản lý licenses (giấy phép) cho từng danh mục sản phẩm. Mỗi seller có thể:
- Tạo licenses mới và lưu vào database
- Gán licenses vào danh mục để dễ quản lý
- Xóa license khỏi danh mục (không xóa khỏi database)
- Xem số lượng sản phẩm và licenses trong mỗi danh mục

## Cấu trúc Database

### Bảng `category_licenses`
Bảng này lưu thông tin về việc gán licenses vào từng danh mục:

```sql
CREATE TABLE category_licenses (
    category_license_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    category_id BIGINT NOT NULL,
    shop_license_id BIGINT NOT NULL,
    seller_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

## Các file đã tạo/sửa đổi

### Backend

1. **Entity**
   - `CategoryLicenses.java` - Entity mới để tracking license assignments
   
2. **Repository**
   - `CategoryLicensesRepository.java` - Repository cho category_licenses

3. **DTO**
   - `LicenseDTO.java` - Data Transfer Object cho licenses

4. **Service**
   - `LicenseManagementService.java` - Service xử lý logic quản lý licenses

5. **Controller**
   - `SellerCategoryController.java` - Thêm các API endpoints:
     - `GET /seller/categories/api/licenses` - Lấy tất cả licenses
     - `GET /seller/categories/api/licenses/assigned` - Lấy licenses đã gán
     - `POST /seller/categories/api/licenses` - Tạo license mới
     - `POST /seller/categories/api/licenses/assign` - Gán license vào danh mục
     - `DELETE /seller/categories/api/licenses/remove` - Xóa license khỏi danh mục

### Frontend

1. **HTML Template**
   - `category-management.html` - Đã có sẵn modal và form
   - Cập nhật JavaScript functions để tích hợp với API mới

2. **CSS**
   - `category-management.css` - Thêm styles cho:
     - Assigned licenses section
     - License badges
     - Loading states
     - Status indicators

### SQL Migration

1. **create_category_licenses_table.sql** - Script tạo bảng mới

## Cách sử dụng

### 1. Cài đặt Database

Chạy script SQL để tạo bảng:

```bash
mysql -u [username] -p [database_name] < sql/create_category_licenses_table.sql
```

### 2. Khởi động ứng dụng

```bash
./mvnw spring-boot:run
```

### 3. Truy cập tính năng

1. Đăng nhập với tài khoản seller
2. Vào "Quản lý Danh mục" (`/seller/categories`)
3. Click vào nút "Quản lý Licenses" ở header hoặc nút license icon bên cạnh mỗi danh mục

### 4. Quản lý Licenses

#### Thêm License mới:
1. Nhập tên license (VD: "Thực phẩm chức năng")
2. Nhập mã license (VD: "FOOD_SUPPLEMENT")
3. Nhập mô tả (tùy chọn)
4. Click "Thêm License"

#### Gán License vào danh mục:
1. Mở License Manager cho danh mục cụ thể
2. Tìm license trong danh sách "Danh sách Licenses hiện có"
3. Click nút "Thêm" bên cạnh license
4. License sẽ xuất hiện ở phần "Licenses đã gán vào danh mục"

#### Xóa License khỏi danh mục:
1. Trong phần "Licenses đã gán vào danh mục"
2. Click nút X trên badge của license
3. Hoặc click nút "Xóa" trong danh sách licenses

## Flow hoạt động

```
┌─────────────────┐
│ Seller Dashboard│
└────────┬────────┘
         │
         ▼
┌─────────────────────────────┐
│ Category Management Page    │
│ - Hiển thị stats            │
│ - Số lượng sản phẩm         │
│ - Số lượng licenses         │
└────────┬────────────────────┘
         │
         │ Click "Quản lý Licenses"
         ▼
┌─────────────────────────────┐
│ License Manager Modal       │
│                             │
│ ┌─────────────────────────┐ │
│ │ Thêm License Mới        │ │
│ │ - Tên, Mã, Mô tả        │ │
│ │ - Lưu vào DB            │ │
│ └─────────────────────────┘ │
│                             │
│ ┌─────────────────────────┐ │
│ │ Licenses đã gán         │ │
│ │ (Nếu mở từ danh mục)    │ │
│ │ - Có thể xóa            │ │
│ └─────────────────────────┘ │
│                             │
│ ┌─────────────────────────┐ │
│ │ Danh sách Licenses      │ │
│ │ - Tất cả licenses       │ │
│ │ - Nút Thêm/Xóa          │ │
│ └─────────────────────────┘ │
└─────────────────────────────┘
```

## API Endpoints

### 1. GET /seller/categories/api/licenses
Lấy tất cả licenses của seller, có thể filter theo category

**Parameters:**
- `categoryId` (optional) - Filter licenses cho category cụ thể

**Response:**
```json
{
  "success": true,
  "licenses": [
    {
      "shopLicenseId": 1,
      "licenseName": "Thực phẩm chức năng",
      "licenseType": "FOOD_SUPPLEMENT",
      "licenseNumber": "FOOD_SUPPLEMENT",
      "description": "Giấy phép cho thực phẩm chức năng",
      "status": "ACTIVE",
      "isActive": true,
      "isAssignedToCategory": false
    }
  ]
}
```

### 2. GET /seller/categories/api/licenses/assigned
Lấy licenses đã được gán vào category

**Parameters:**
- `categoryId` (required)

**Response:**
```json
{
  "success": true,
  "licenses": [...]
}
```

### 3. POST /seller/categories/api/licenses
Tạo license mới

**Request Body:**
```json
{
  "licenseName": "Thực phẩm chức năng",
  "licenseType": "FOOD_SUPPLEMENT",
  "licenseNumber": "FOOD_SUPPLEMENT",
  "description": "Giấy phép cho thực phẩm chức năng",
  "status": "ACTIVE",
  "isActive": true
}
```

**Response:**
```json
{
  "success": true,
  "message": "Đã tạo license thành công",
  "license": {...}
}
```

### 4. POST /seller/categories/api/licenses/assign
Gán license vào category

**Parameters:**
- `categoryId` (required)
- `licenseId` (required)

**Response:**
```json
{
  "success": true,
  "message": "Đã thêm license vào danh mục"
}
```

### 5. DELETE /seller/categories/api/licenses/remove
Xóa license khỏi category

**Parameters:**
- `categoryId` (required)
- `licenseId` (required)

**Response:**
```json
{
  "success": true,
  "message": "Đã xóa license khỏi danh mục"
}
```

## Lưu ý quan trọng

1. **Không xóa license khỏi database**: Khi xóa license khỏi danh mục, chỉ xóa liên kết trong bảng `category_licenses`, license vẫn tồn tại trong `shop_licenses`

2. **Mỗi seller có licenses riêng**: Tất cả queries đều filter theo `sellerId` để đảm bảo data isolation

3. **Unique constraint**: Không thể gán cùng một license vào một category nhiều lần

4. **Cascade delete**: Khi xóa category, license, hoặc seller, các liên kết trong `category_licenses` sẽ tự động bị xóa

## Troubleshooting

### License không hiển thị
- Kiểm tra đã chạy SQL migration chưa
- Kiểm tra seller đã đăng nhập đúng chưa
- Xem console log để kiểm tra API errors

### Không thể thêm license
- Kiểm tra tên và mã license đã điền đủ chưa
- Kiểm tra database connection
- Xem network tab trong browser DevTools

### License đã gán vẫn hiển thị trong danh sách available
- Refresh trang hoặc đóng/mở lại modal
- Kiểm tra `isAssignedToCategory` flag trong response

## Tương lai mở rộng

- [ ] Thêm tính năng edit license
- [ ] Thêm tính năng deactivate/activate license
- [ ] Thêm tính năng search/filter licenses
- [ ] Thêm export licenses ra CSV
- [ ] Thêm license templates
- [ ] Thêm license expiry notification
- [ ] Thêm bulk assign/remove licenses

