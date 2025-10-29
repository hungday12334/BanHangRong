# Test Cases cho Voucher Management

## 🧪 Hướng dẫn Test Tính năng Voucher

### Môi trường Test
- **Browser**: Chrome, Firefox, Safari
- **User Role**: SELLER
- **Database**: MySQL với dữ liệu mẫu

---

## 📋 Test Cases

### TC-001: Truy cập trang Voucher Management
**Mục đích**: Kiểm tra người dùng có thể truy cập trang quản lý voucher

**Điều kiện tiên quyết**:
- Đã đăng nhập với role SELLER
- Có ít nhất 1 sản phẩm trong database

**Các bước thực hiện**:
1. Đăng nhập với tài khoản seller
2. Truy cập `/seller/voucher`

**Kết quả mong đợi**:
- ✅ Trang hiển thị đầy đủ các thành phần
- ✅ Statistics cards hiển thị số 0
- ✅ Product selector hiển thị
- ✅ Empty state "Chọn sản phẩm để xem voucher"

---

### TC-002: Tìm kiếm và chọn sản phẩm
**Mục đích**: Kiểm tra chức năng tìm kiếm và chọn sản phẩm

**Các bước thực hiện**:
1. Nhập tên sản phẩm vào ô tìm kiếm
2. Xem danh sách gợi ý xuất hiện
3. Click chọn 1 sản phẩm

**Kết quả mong đợi**:
- ✅ Dropdown hiển thị kết quả phù hợp
- ✅ Hiển thị thông tin sản phẩm: tên, ID, giá
- ✅ Selected product display hiển thị
- ✅ Danh sách voucher được load

---

### TC-003: Tạo Voucher PERCENT
**Mục đích**: Kiểm tra tạo voucher giảm theo phần trăm

**Điều kiện tiên quyết**:
- Đã chọn sản phẩm

**Các bước thực hiện**:
1. Click nút "Tạo Voucher mới"
2. Nhập thông tin:
   - Mã: `TEST10`
   - Loại: `Phần trăm (%)`
   - Giá trị: `10`
   - Đơn tối thiểu: `100000`
   - Trạng thái: `Hoạt động`
3. Click "Lưu Voucher"

**Kết quả mong đợi**:
- ✅ Modal đóng lại
- ✅ Hiển thị thông báo "Lưu voucher thành công!"
- ✅ Voucher mới xuất hiện trong danh sách
- ✅ Statistics cập nhật (+1 Total, +1 Active)

**Test Data**:
```json
{
  "code": "TEST10",
  "discountType": "PERCENT",
  "discountValue": 10,
  "minOrder": 100000,
  "status": "active"
}
```

---

### TC-004: Tạo Voucher AMOUNT
**Mục đích**: Kiểm tra tạo voucher giảm số tiền cố định

**Các bước thực hiện**:
1. Click "Tạo Voucher mới"
2. Nhập thông tin:
   - Mã: `GIAM50K`
   - Loại: `Số tiền cố định (VNĐ)`
   - Giá trị: `50000`
   - Đơn tối thiểu: `200000`
   - Số lần dùng tối đa: `100`
   - Giới hạn/người: `1`
3. Click "Lưu Voucher"

**Kết quả mong đợi**:
- ✅ Voucher được tạo thành công
- ✅ Hiển thị đúng loại "Số tiền"
- ✅ Giá trị hiển thị dạng currency: "50.000 ₫"
- ✅ Số lần dùng: "0 / 100"

---

### TC-005: Tạo Voucher với thời hạn
**Mục đích**: Kiểm tra tạo voucher có ngày bắt đầu và kết thúc

**Các bước thực hiện**:
1. Click "Tạo Voucher mới"
2. Nhập thông tin cơ bản
3. Chọn:
   - Ngày bắt đầu: Hôm nay
   - Ngày kết thúc: 30 ngày sau
4. Click "Lưu Voucher"

**Kết quả mong đợi**:
- ✅ Voucher lưu thành công
- ✅ Hiển thị đúng thời hạn trong bảng
- ✅ Format ngày: dd/MM/yyyy

---

### TC-006: Validation - Mã voucher trùng
**Mục đích**: Kiểm tra không cho tạo mã voucher trùng

**Các bước thực hiện**:
1. Tạo voucher với mã `DUPLICATE`
2. Tạo voucher thứ 2 với cùng mã `DUPLICATE` cho cùng sản phẩm
3. Click "Lưu Voucher"

**Kết quả mong đợi**:
- ✅ Hiển thị lỗi "Voucher code already exists"
- ✅ Modal không đóng
- ✅ Voucher không được tạo

---

### TC-007: Validation - Giá trị discount không hợp lệ
**Mục đích**: Kiểm tra validation giá trị discount

**Test Cases con**:

**TC-007a**: Discount PERCENT > 100
- Input: `discountValue = 150`
- Expected: ❌ Validation error

**TC-007b**: Discount < 0
- Input: `discountValue = -10`
- Expected: ❌ Validation error

**TC-007c**: Discount = 0
- Input: `discountValue = 0`
- Expected: ⚠️ Warning nhưng cho phép lưu

---

### TC-008: Chỉnh sửa Voucher
**Mục đích**: Kiểm tra chức năng cập nhật voucher

**Điều kiện tiên quyết**:
- Có voucher đã tạo

**Các bước thực hiện**:
1. Click icon "Edit" (✏️) trên 1 voucher
2. Thay đổi thông tin:
   - Giá trị discount: `15` (thay vì 10)
   - Trạng thái: `Tạm dừng`
3. Click "Lưu Voucher"

**Kết quả mong đợi**:
- ✅ Voucher được cập nhật
- ✅ Giá trị mới hiển thị đúng
- ✅ Trạng thái đổi sang "Tạm dừng"
- ✅ Statistics cập nhật (-1 Active, +1 Inactive)

---

### TC-009: Xóa Voucher
**Mục đích**: Kiểm tra chức năng xóa voucher

**Các bước thực hiện**:
1. Click icon "Delete" (🗑️) trên 1 voucher
2. Xác nhận xóa trong dialog

**Kết quả mong đợi**:
- ✅ Hiển thị confirm dialog
- ✅ Voucher bị xóa khỏi danh sách
- ✅ Hiển thị thông báo "Xóa voucher thành công!"
- ✅ Statistics giảm đi 1

---

### TC-010: Lọc voucher theo trạng thái
**Mục đích**: Kiểm tra bộ lọc trạng thái

**Điều kiện tiên quyết**:
- Có voucher với các trạng thái khác nhau

**Các bước thực hiện**:
1. Click filter "Hoạt động"
2. Quan sát danh sách
3. Click filter "Tạm dừng"
4. Click filter "Hết hạn"
5. Click filter "Tất cả"

**Kết quả mong đợi**:
- ✅ Mỗi lần click, chỉ hiển thị voucher đúng trạng thái
- ✅ Filter button active có highlight
- ✅ "Tất cả" hiển thị tất cả voucher

---

### TC-011: Phân trang
**Mục đích**: Kiểm tra chức năng phân trang

**Điều kiện tiên quyết**:
- Có > 10 vouchers

**Các bước thực hiện**:
1. Quan sát danh sách (trang 1)
2. Click nút "Sau"
3. Click số trang cụ thể (VD: trang 3)
4. Click nút "Trước"

**Kết quả mong đợi**:
- ✅ Mỗi trang hiển thị tối đa 10 voucher
- ✅ Nút "Trước" disabled ở trang đầu
- ✅ Nút "Sau" disabled ở trang cuối
- ✅ Active page number được highlight

---

### TC-012: Xem lịch sử sử dụng Voucher
**Mục đích**: Kiểm tra hiển thị redemption history

**Điều kiện tiên quyết**:
- Có voucher đã được sử dụng (có records trong `voucher_redemptions`)

**Các bước thực hiện**:
1. Click icon "History" (🕒) trên voucher
2. Xem modal hiển thị

**Kết quả mong đợi**:
- ✅ Modal mở với danh sách redemptions
- ✅ Hiển thị: Redeem ID, Order ID, User ID, Discount amount, Date
- ✅ Nếu chưa có lượt dùng: hiển thị empty state

---

### TC-013: Click vào Statistics Card
**Mục đích**: Kiểm tra filter từ statistics cards

**Các bước thực hiện**:
1. Click vào card "Đang hoạt động" (màu xanh)
2. Click vào card "Tạm dừng" (màu vàng)
3. Click vào card "Hết hạn" (màu đỏ)
4. Click vào card "Tổng Voucher" (màu xanh dương)

**Kết quả mong đợi**:
- ✅ Mỗi lần click, danh sách được lọc theo trạng thái tương ứng
- ✅ Filter button phía dưới cũng được cập nhật
- ✅ Card có hiệu ứng hover và click

---

### TC-014: Bỏ chọn sản phẩm
**Mục đích**: Kiểm tra clear product selection

**Các bước thực hiện**:
1. Chọn 1 sản phẩm (có vouchers)
2. Click nút "Bỏ chọn"

**Kết quả mong đợi**:
- ✅ Selected product display ẩn đi
- ✅ Search input được clear
- ✅ Danh sách voucher trở về empty state
- ✅ Statistics reset về 0

---

### TC-015: Responsive Design
**Mục đích**: Kiểm tra giao diện trên mobile

**Các bước thực hiện**:
1. Mở DevTools (F12)
2. Toggle device toolbar (Ctrl+Shift+M)
3. Chọn iPhone 12 Pro
4. Test các chức năng

**Kết quả mong đợi**:
- ✅ Layout điều chỉnh theo màn hình nhỏ
- ✅ Statistics cards xếp dọc
- ✅ Bảng có scroll ngang
- ✅ Modal responsive
- ✅ Buttons đủ lớn để tap

---

### TC-016: Performance Test
**Mục đích**: Kiểm tra hiệu năng với nhiều voucher

**Điều kiện tiên quyết**:
- Tạo 100+ vouchers cho 1 sản phẩm

**Các bước thực hiện**:
1. Chọn sản phẩm có nhiều voucher
2. Đo thời gian load
3. Test phân trang
4. Test filter

**Kết quả mong đợi**:
- ✅ Load danh sách < 2 giây
- ✅ Phân trang không lag
- ✅ Filter mượt mà
- ✅ Không có memory leak

---

### TC-017: Error Handling
**Mục đích**: Kiểm tra xử lý lỗi

**Test Cases con**:

**TC-017a**: Network Error
- Tắt backend server
- Thử load vouchers
- Expected: ✅ Hiển thị error message "Không thể tải danh sách voucher"

**TC-017b**: Invalid Product ID
- Chọn product không tồn tại
- Expected: ✅ Hiển thị error

**TC-017c**: Unauthorized Access
- Logout user
- Truy cập `/seller/voucher`
- Expected: ✅ Redirect to login

---

### TC-018: Browser Compatibility
**Mục đích**: Kiểm tra tương thích trình duyệt

**Các trình duyệt test**:
- ✅ Chrome (latest)
- ✅ Firefox (latest)
- ✅ Safari (latest)
- ✅ Edge (latest)

**Các tính năng test**:
- Load trang
- Tạo/sửa/xóa voucher
- Tìm kiếm sản phẩm
- Modal interactions
- CSS styling

---

### TC-019: Security Test
**Mục đích**: Kiểm tra bảo mật

**Test Cases con**:

**TC-019a**: SQL Injection
- Input: `' OR '1'='1` trong voucher code
- Expected: ✅ Không thực thi SQL injection

**TC-019b**: XSS Attack
- Input: `<script>alert('xss')</script>` trong voucher code
- Expected: ✅ Code được escape, không thực thi

**TC-019c**: Access Control
- Seller A tạo voucher
- Seller B thử edit voucher của A
- Expected: ✅ 403 Forbidden

---

### TC-020: Load Test - Concurrent Users
**Mục đích**: Kiểm tra với nhiều người dùng

**Công cụ**: JMeter hoặc k6

**Scenario**:
- 50 users đồng thời
- Mỗi user:
  - Chọn sản phẩm
  - Load vouchers
  - Tạo 1 voucher
  - Cập nhật 1 voucher

**Kết quả mong đợi**:
- ✅ Response time < 3s (p95)
- ✅ Error rate < 1%
- ✅ Không có data corruption

---

## 🎯 Checklist Tổng hợp

### Functional Testing
- [ ] TC-001: Truy cập trang
- [ ] TC-002: Tìm kiếm sản phẩm
- [ ] TC-003: Tạo Voucher PERCENT
- [ ] TC-004: Tạo Voucher AMOUNT
- [ ] TC-005: Tạo với thời hạn
- [ ] TC-006: Validation mã trùng
- [ ] TC-007: Validation giá trị
- [ ] TC-008: Chỉnh sửa voucher
- [ ] TC-009: Xóa voucher
- [ ] TC-010: Lọc theo trạng thái
- [ ] TC-011: Phân trang
- [ ] TC-012: Xem lịch sử
- [ ] TC-013: Click statistics
- [ ] TC-014: Bỏ chọn sản phẩm

### UI/UX Testing
- [ ] TC-015: Responsive design
- [ ] TC-018: Browser compatibility
- [ ] Kiểm tra màu sắc, fonts
- [ ] Kiểm tra icons hiển thị
- [ ] Kiểm tra animations

### Performance Testing
- [ ] TC-016: Performance với nhiều data
- [ ] TC-020: Load test concurrent users

### Security Testing
- [ ] TC-017: Error handling
- [ ] TC-019: Security vulnerabilities
- [ ] Authentication/Authorization

---

## 📊 Test Report Template

```markdown
### Test Report - Voucher Management

**Date**: [Date]
**Tester**: [Name]
**Environment**: [Dev/Staging/Prod]

#### Summary
- Total Test Cases: 20
- Passed: [X]
- Failed: [Y]
- Skipped: [Z]

#### Failed Test Cases
1. TC-XXX: [Description]
   - Issue: [Description]
   - Severity: [Critical/High/Medium/Low]
   - Screenshot: [Link]

#### Notes
[Any additional observations]
```

---

## 🐛 Known Issues

### Issue #1: Modal không đóng khi ESC
- **Severity**: Low
- **Workaround**: Click nút X hoặc click outside

### Issue #2: Search có delay
- **Severity**: Low
- **Fix**: Implement debounce

---

## 📝 Test Data

### Sample Products
```sql
INSERT INTO products (name, price, seller_id) VALUES
('Laptop Dell XPS 15', 30000000, 1),
('iPhone 14 Pro', 25000000, 1),
('Samsung Galaxy S23', 20000000, 1);
```

### Sample Vouchers
```sql
INSERT INTO vouchers (seller_id, product_id, code, discount_type, discount_value, status) VALUES
(1, 1, 'LAPTOP10', 'PERCENT', 10, 'active'),
(1, 1, 'GIAM500K', 'AMOUNT', 500000, 'active'),
(1, 2, 'IPHONE20', 'PERCENT', 20, 'inactive');
```

---

**Last Updated**: October 29, 2025
**Version**: 1.0.0

