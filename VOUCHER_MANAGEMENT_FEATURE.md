# Voucher Management Feature - Documentation

## 📋 Tổng quan

Tính năng quản lý Voucher cho phép người bán tạo, chỉnh sửa, và quản lý các mã giảm giá cho sản phẩm của họ. Hệ thống hỗ trợ hai loại voucher: giảm theo phần trăm và giảm theo số tiền cố định.

## 🎯 Các tính năng chính

### 1. Chọn sản phẩm
- Tìm kiếm sản phẩm theo tên hoặc Product ID
- Chọn sản phẩm để quản lý voucher
- Hiển thị thông tin sản phẩm đã chọn

### 2. Thống kê Voucher
- **Tổng Voucher**: Tổng số voucher của sản phẩm
- **Đang hoạt động**: Số voucher đang active
- **Tạm dừng**: Số voucher bị tạm dừng
- **Hết hạn**: Số voucher đã hết hạn

### 3. Quản lý Voucher

#### Tạo Voucher mới
Các trường thông tin:
- **Mã Voucher** (bắt buộc): Mã duy nhất cho voucher (tối đa 64 ký tự)
- **Loại giảm giá** (bắt buộc):
  - Phần trăm (%): Giảm theo tỷ lệ phần trăm
  - Số tiền cố định (VNĐ): Giảm một số tiền cụ thể
- **Giá trị giảm** (bắt buộc): Giá trị giảm giá
- **Đơn hàng tối thiểu**: Giá trị đơn hàng tối thiểu để áp dụng voucher
- **Ngày bắt đầu**: Thời điểm bắt đầu có hiệu lực
- **Ngày kết thúc**: Thời điểm hết hiệu lực
- **Số lần sử dụng tối đa**: Giới hạn tổng số lần sử dụng
- **Giới hạn/người dùng**: Số lần một người dùng có thể sử dụng
- **Trạng thái**: Active (Hoạt động) hoặc Inactive (Tạm dừng)

#### Chỉnh sửa Voucher
- Cập nhật thông tin voucher hiện có
- Thay đổi trạng thái (Active/Inactive)
- Điều chỉnh số lần sử dụng và thời hạn

#### Xóa Voucher
- Xóa voucher không còn sử dụng
- Xác nhận trước khi xóa

#### Xem lịch sử sử dụng
- Danh sách các lần voucher được sử dụng
- Thông tin đơn hàng và người dùng
- Số tiền giảm giá cho mỗi lần sử dụng

### 4. Bộ lọc và Tìm kiếm
- Lọc theo trạng thái: Tất cả, Hoạt động, Tạm dừng, Hết hạn
- Phân trang kết quả
- Thống kê theo từng trạng thái

## 🔧 Cấu trúc kỹ thuật

### Backend API Endpoints

#### 1. Lấy danh sách voucher
```
GET /api/seller/vouchers
Parameters:
  - productId: Long (required)
  - status: String (optional) - "active", "inactive", "expired"
  - page: int (default: 0)
  - size: int (default: 10)
Response: Page<VoucherDto>
```

#### 2. Lấy chi tiết voucher
```
GET /api/seller/vouchers/{voucherId}
Response: VoucherDto
```

#### 3. Tạo/Cập nhật voucher
```
POST /api/seller/vouchers
Body: VoucherDto
Response: VoucherDto
```

#### 4. Xóa voucher
```
DELETE /api/seller/vouchers/{voucherId}
Response: 204 No Content
```

#### 5. Xem lịch sử sử dụng
```
GET /api/seller/vouchers/{voucherId}/redemptions
Response: List<VoucherRedemptions>
```

### Database Schema

#### Table: vouchers
```sql
CREATE TABLE vouchers (
  voucher_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  seller_id BIGINT NOT NULL,
  product_id BIGINT NOT NULL,
  code VARCHAR(64) NOT NULL,
  discount_type VARCHAR(16) NOT NULL, -- PERCENT | AMOUNT
  discount_value DECIMAL(12,2) NOT NULL DEFAULT 0,
  min_order DECIMAL(12,2) DEFAULT NULL,
  start_at DATETIME DEFAULT NULL,
  end_at DATETIME DEFAULT NULL,
  max_uses INT DEFAULT NULL,
  max_uses_per_user INT DEFAULT NULL,
  used_count INT NOT NULL DEFAULT 0,
  status VARCHAR(16) NOT NULL DEFAULT 'active',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT ux_vouchers_seller_product_code UNIQUE (seller_id, product_id, code)
);
```

#### Table: voucher_redemptions
```sql
CREATE TABLE voucher_redemptions (
  redeem_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  voucher_id BIGINT NOT NULL,
  order_id BIGINT DEFAULT NULL,
  user_id BIGINT DEFAULT NULL,
  discount_amount DECIMAL(12,2) DEFAULT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### Entity Classes

#### Vouchers.java
- Entity class ánh xạ với bảng `vouchers`
- Các phương thức lifecycle: `@PrePersist`, `@PreUpdate`
- Getters/Setters cho tất cả các trường

#### VoucherRedemptions.java
- Entity class ánh xạ với bảng `voucher_redemptions`
- Lưu trữ lịch sử sử dụng voucher

### Repository Interfaces

#### VouchersRepository.java
```java
public interface VouchersRepository extends JpaRepository<Vouchers, Long> {
    Page<Vouchers> findBySellerIdAndProductId(Long sellerId, Long productId, Pageable pageable);
    Page<Vouchers> findBySellerIdAndProductIdAndStatus(Long sellerId, Long productId, String status, Pageable pageable);
    boolean existsBySellerIdAndProductIdAndCodeIgnoreCase(Long sellerId, Long productId, String code);
}
```

#### VoucherRedemptionsRepository.java
```java
public interface VoucherRedemptionsRepository extends JpaRepository<VoucherRedemptions, Long> {
    List<VoucherRedemptions> findByVoucherId(Long voucherId);
}
```

### Controller

#### VouchersApiController.java
- Xử lý tất cả các API requests cho voucher
- Validate dữ liệu đầu vào
- Kiểm tra quyền sở hữu (seller chỉ quản lý voucher của mình)
- Xử lý các trường hợp lỗi (duplicate code, không tìm thấy, v.v.)

## 🎨 Giao diện người dùng

### Components

#### 1. Header Section
- Tiêu đề trang
- Nút quay lại Dashboard

#### 2. Statistics Cards
- 4 thẻ thống kê có thể click để lọc
- Hiển thị số lượng voucher theo từng trạng thái

#### 3. Product Selector
- Input tìm kiếm với autocomplete
- Hiển thị kết quả tìm kiếm dạng dropdown
- Thông tin sản phẩm đã chọn

#### 4. Voucher List
- Bảng danh sách voucher
- Các cột: Mã, Loại, Giá trị, Đã dùng, Thời hạn, Trạng thái, Thao tác
- Phân trang
- Empty state khi chưa có voucher

#### 5. Voucher Modal
- Form tạo/sửa voucher
- Validation frontend
- Responsive design

#### 6. Redemptions Modal
- Hiển thị lịch sử sử dụng voucher
- Thông tin chi tiết từng lần sử dụng

### Styling
- Sử dụng CSS variables cho theme consistency
- Dark mode compatible
- Responsive design
- Animation và transition mượt mà
- Icons từ Tabler Icons

## 🔐 Security

### Authorization
- Seller chỉ được quản lý voucher của sản phẩm thuộc về họ
- Kiểm tra `sellerId` trong mọi API request
- Validate ownership trong backend

### Validation
- Frontend: HTML5 validation, JavaScript validation
- Backend: Business logic validation
- Kiểm tra duplicate voucher code
- Validate giá trị (discount value, dates, v.v.)

## 📊 Business Logic

### Voucher Types
1. **PERCENT**: Giảm theo phần trăm
   - Giá trị từ 0-100%
   - Tính: `discount = orderAmount * (discountValue / 100)`

2. **AMOUNT**: Giảm số tiền cố định
   - Giá trị tính bằng VNĐ
   - Tính: `discount = discountValue`

### Voucher Status
1. **active**: Đang hoạt động, có thể sử dụng
2. **inactive**: Tạm dừng, không thể sử dụng
3. **expired**: Hết hạn (tự động hoặc thủ công)

### Usage Limits
- **Global limit** (`max_uses`): Tổng số lần voucher có thể được sử dụng
- **Per-user limit** (`max_uses_per_user`): Số lần một người dùng có thể sử dụng
- **Minimum order** (`min_order`): Đơn hàng phải đạt giá trị tối thiểu

## 🧪 Testing

### Test Cases

#### 1. Tạo Voucher
- ✅ Tạo voucher PERCENT thành công
- ✅ Tạo voucher AMOUNT thành công
- ✅ Lỗi khi mã voucher trùng
- ✅ Validation giá trị discount
- ✅ Validation thời hạn (start < end)

#### 2. Cập nhật Voucher
- ✅ Cập nhật thông tin thành công
- ✅ Thay đổi trạng thái
- ✅ Không được trùng mã với voucher khác

#### 3. Xóa Voucher
- ✅ Xóa voucher thành công
- ✅ Không được xóa voucher của seller khác

#### 4. Lọc và Phân trang
- ✅ Lọc theo status
- ✅ Phân trang hoạt động đúng
- ✅ Tìm kiếm sản phẩm

#### 5. Redemptions
- ✅ Xem lịch sử sử dụng
- ✅ Hiển thị đầy đủ thông tin

## 🚀 Deployment

### Prerequisites
- Java 17+
- Spring Boot 3.x
- MySQL 8+
- Maven

### Setup Steps

1. **Database Setup**
```bash
mysql -u root -p < sql/add_vouchers.sql
```

2. **Build Application**
```bash
mvn clean install
```

3. **Run Application**
```bash
mvn spring-boot:run
```

4. **Access Voucher Management**
```
URL: http://localhost:8080/seller/voucher
```

## 📝 Usage Guide

### Tạo Voucher mới

1. Đăng nhập với tài khoản Seller
2. Vào trang "Quản lý Voucher" (`/seller/voucher`)
3. Tìm và chọn sản phẩm từ Product Selector
4. Click nút "Tạo Voucher mới"
5. Điền thông tin voucher:
   - Mã voucher (bắt buộc)
   - Loại giảm giá (PERCENT hoặc AMOUNT)
   - Giá trị giảm
   - Các điều kiện (tùy chọn)
6. Click "Lưu Voucher"

### Chỉnh sửa Voucher

1. Trong danh sách voucher, click icon "Edit" (✏️)
2. Cập nhật thông tin trong modal
3. Click "Lưu Voucher"

### Xóa Voucher

1. Click icon "Delete" (🗑️) 
2. Xác nhận xóa trong dialog
3. Voucher sẽ bị xóa khỏi hệ thống

### Xem lịch sử sử dụng

1. Click icon "History" (🕒)
2. Xem danh sách các lần voucher được sử dụng
3. Chi tiết: Order ID, User ID, số tiền giảm, ngày sử dụng

## 🐛 Troubleshooting

### Lỗi thường gặp

#### 1. "Voucher code already exists"
**Nguyên nhân**: Mã voucher đã tồn tại cho sản phẩm này
**Giải pháp**: Sử dụng mã khác hoặc chỉnh sửa voucher cũ

#### 2. "Product ID is required"
**Nguyên nhân**: Chưa chọn sản phẩm
**Giải pháp**: Chọn sản phẩm từ Product Selector trước

#### 3. API không trả về dữ liệu
**Nguyên nhân**: Có thể do authentication hoặc authorization
**Giải pháp**: Kiểm tra đăng nhập, kiểm tra logs backend

#### 4. Modal không đóng
**Nguyên nhân**: JavaScript error
**Giải pháp**: Check browser console, refresh trang

## 🔄 Future Enhancements

### Planned Features

1. **Bulk Operations**
   - Tạo nhiều voucher cùng lúc
   - Xóa nhiều voucher
   - Export/Import voucher

2. **Advanced Analytics**
   - Biểu đồ sử dụng voucher theo thời gian
   - Top voucher được sử dụng nhiều nhất
   - Tỷ lệ conversion

3. **Voucher Templates**
   - Lưu template voucher để tái sử dụng
   - Copy voucher sang sản phẩm khác

4. **Notifications**
   - Thông báo khi voucher sắp hết hạn
   - Thông báo khi voucher được sử dụng
   - Alert khi voucher sắp hết lượt dùng

5. **Customer View**
   - Trang hiển thị voucher available cho khách
   - Áp dụng voucher khi checkout
   - Tự động áp dụng voucher tốt nhất

## 📞 Support

Nếu gặp vấn đề, vui lòng:
1. Kiểm tra logs: `app.log`
2. Kiểm tra database connectivity
3. Xem lại documentation này
4. Liên hệ team support

## 📄 License

Copyright © 2025 Bán Hàng Rong Project

---

**Last Updated**: October 29, 2025
**Version**: 1.0.0
**Author**: Development Team

