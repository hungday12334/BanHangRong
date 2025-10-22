# Tính năng Quản lý Danh mục - Category Management

## 📋 Tổng quan
Đã nâng cấp trang quản lý danh mục của Seller với nhiều tính năng mới giúp quản lý danh mục hiệu quả hơn.

## ✨ Các tính năng đã thêm

### 1. **Thống kê Dashboard** 📊
- **Tổng danh mục**: Hiển thị tổng số danh mục đã tạo
- **Danh mục có sản phẩm**: Số lượng danh mục đang có sản phẩm
- **Tổng sản phẩm**: Tổng số sản phẩm trong tất cả danh mục
- **Danh mục mới**: Số lượng danh mục được tạo trong 7 ngày gần đây

Các thống kê được hiển thị dưới dạng cards với màu sắc gradient đẹp mắt.

### 2. **Tìm kiếm thông minh** 🔍
- Tìm kiếm real-time theo tên danh mục
- Tìm kiếm theo mô tả danh mục
- Cập nhật số lượng kết quả ngay lập tức
- Không cần reload trang

**Cách sử dụng**: Gõ từ khóa vào ô "Tìm kiếm danh mục..."

### 3. **Sắp xếp linh hoạt** 🔄
Sắp xếp danh sách danh mục theo 6 tiêu chí:
- **Tên (A-Z)**: Sắp xếp tên tăng dần
- **Tên (Z-A)**: Sắp xếp tên giảm dần
- **Mới nhất**: Danh mục mới tạo lên đầu
- **Cũ nhất**: Danh mục cũ nhất lên đầu
- **Nhiều sản phẩm nhất**: Danh mục có nhiều sản phẩm nhất
- **Ít sản phẩm nhất**: Danh mục có ít sản phẩm nhất

**Cách sử dụng**: Chọn tiêu chí từ dropdown "Sắp xếp"

### 4. **Hiển thị số sản phẩm** 📦
- Mỗi danh mục hiển thị số lượng sản phẩm
- Badge với số lượng sản phẩm nổi bật
- Nút "Xem" để preview nhanh các sản phẩm trong danh mục
- Hiển thị thông tin chi tiết sản phẩm trong modal

**Cách sử dụng**: Click vào icon mắt (👁) bên cạnh số lượng sản phẩm

### 5. **Bulk Actions - Xóa nhiều danh mục** ✅
- Chế độ chọn nhiều danh mục cùng lúc
- Checkbox "Select All" để chọn tất cả
- Hiển thị số lượng đã chọn
- Xóa nhiều danh mục trong 1 lần thao tác
- Xác nhận trước khi xóa để tránh nhầm lẫn

**Cách sử dụng**:
1. Click nút "Chọn nhiều"
2. Tick vào các checkbox bên cạnh danh mục cần xóa
3. Click "Xóa đã chọn"
4. Xác nhận trong dialog

### 6. **Export CSV** 📥
- Xuất danh sách danh mục ra file CSV
- Bao gồm: ID, Tên, Mô tả, Số sản phẩm, Ngày tạo
- Hỗ trợ tiếng Việt (UTF-8 BOM)
- Chỉ xuất các danh mục đang hiển thị (sau khi filter)
- Tên file tự động: `categories_[timestamp].csv`

**Cách sử dụng**: Click nút "Export CSV"

### 7. **Xem sản phẩm trong danh mục** 👁️
Modal popup hiển thị:
- Hình ảnh sản phẩm
- Tên sản phẩm
- SKU (mã sản phẩm)
- Giá bán
- Số lượng trong kho
- Số lượng đã bán

**Cách sử dụng**: Click icon mắt ở cột "Số sản phẩm"

### 8. **UI/UX cải tiến** 🎨
- Icons Tabler cho giao diện hiện đại
- Responsive design - tự động điều chỉnh trên mọi thiết bị
- Loading states rõ ràng
- Empty states với hướng dẫn
- Alert tự động đóng sau 5 giây
- Hover effects và transitions mượt mà
- Dark mode support (theo theme hệ thống)

## 🔧 Thay đổi kỹ thuật

### Frontend (HTML/JavaScript)
**File**: `/src/main/resources/templates/seller/category-management.html`

**Các function JavaScript mới**:
- `filterCategories()`: Tìm kiếm danh mục
- `sortCategories()`: Sắp xếp danh mục
- `toggleBulkMode()`: Bật/tắt chế độ chọn nhiều
- `toggleSelectAll()`: Chọn/bỏ chọn tất cả
- `updateSelectedCount()`: Cập nhật số lượng đã chọn
- `deleteSelected()`: Xóa các danh mục đã chọn
- `exportCategories()`: Xuất CSV
- `viewCategoryProducts()`: Xem sản phẩm trong danh mục

### Backend (Java/Spring Boot)
**File**: `/src/main/java/banhangrong/su25/Controller/SellerCategoryController.java`

**Các endpoint mới**:

1. **GET `/seller/categories`** - Cải tiến
   - Thêm thống kê: totalCategories, categoriesWithProducts, totalProducts, recentCategories
   - Thêm productCountByCategory Map

2. **POST `/seller/categories/actions/bulk-delete`** - MỚI
   - Xóa nhiều danh mục cùng lúc
   - Parameter: `List<Long> categoryIds`
   - Xử lý lỗi từng danh mục riêng biệt

3. **GET `/seller/categories/{categoryId}/products`** - MỚI
   - API trả về JSON
   - Danh sách sản phẩm trong danh mục
   - Bao gồm primary image của mỗi sản phẩm

**File**: `/src/main/java/banhangrong/su25/Repository/ProductsRepository.java`

**Các method mới**:
- `countByCategoryId(Long categoryId)`: Đếm sản phẩm theo danh mục
- `findByCategoryId(Long categoryId)`: Tìm sản phẩm theo danh mục

## 📊 Data Flow

```
User Action → JavaScript Function → API Call → Controller → Service → Repository → Database
                                                      ↓
                                                   Response
                                                      ↓
                                            Update UI (no reload)
```

## 🎯 Lợi ích

1. **Tiết kiệm thời gian**: Bulk actions, tìm kiếm nhanh
2. **Dễ quản lý**: Thống kê rõ ràng, sắp xếp linh hoạt
3. **Trực quan**: Biết ngay danh mục nào có/không có sản phẩm
4. **Chuyên nghiệp**: Export CSV cho báo cáo, phân tích
5. **User-friendly**: Không cần reload trang, phản hồi nhanh

## 🚀 Hướng dẫn sử dụng

### Thêm danh mục mới
1. Nhập tên danh mục (bắt buộc)
2. Nhập mô tả (tùy chọn)
3. Click "Thêm"

### Sửa danh mục
1. Click nút "Sửa" (icon bút)
2. Chỉnh sửa thông tin trong modal
3. Click "Lưu thay đổi"

### Xóa một danh mục
1. Click nút "Xóa" (icon thùng rác)
2. Xác nhận trong dialog

### Xóa nhiều danh mục
1. Click "Chọn nhiều"
2. Tick checkbox các danh mục cần xóa
3. Click "Xóa đã chọn"
4. Xác nhận

### Tìm kiếm
- Gõ từ khóa vào ô tìm kiếm
- Kết quả hiển thị ngay lập tức

### Xem sản phẩm
- Click icon mắt ở cột "Số sản phẩm"
- Modal hiển thị danh sách sản phẩm

### Xuất CSV
- Click "Export CSV"
- File tự động tải về

## 🔮 Tính năng có thể mở rộng thêm

1. **Drag & Drop**: Kéo thả để sắp xếp thứ tự danh mục
2. **Category Images**: Thêm hình ảnh cho danh mục
3. **Sub-categories**: Danh mục con/danh mục cha
4. **Category Status**: Active/Inactive toggle
5. **Import CSV**: Nhập danh mục từ file CSV
6. **Category Analytics**: Biểu đồ doanh thu theo danh mục
7. **Bulk Edit**: Sửa nhiều danh mục cùng lúc
8. **Category Tags**: Thêm tags cho danh mục
9. **Advanced Filters**: Lọc theo nhiều tiêu chí
10. **Pagination**: Phân trang khi có quá nhiều danh mục
11. **Category Templates**: Mẫu danh mục có sẵn
12. **SEO Settings**: Cài đặt SEO cho mỗi danh mục

## 🐛 Lưu ý

- Thống kê được tính real-time mỗi lần load trang
- Bulk delete sẽ bỏ qua các danh mục có lỗi và tiếp tục xóa các danh mục khác
- Export CSV chỉ xuất các danh mục đang hiển thị sau khi filter/search
- Modal xem sản phẩm load dữ liệu qua API, có thể mất vài giây nếu có nhiều sản phẩm

## 📝 Version History

**Version 2.0** - 23/10/2025
- ✅ Thêm thống kê dashboard
- ✅ Tìm kiếm real-time
- ✅ Sắp xếp 6 tiêu chí
- ✅ Hiển thị số sản phẩm
- ✅ Bulk delete
- ✅ Export CSV
- ✅ Xem sản phẩm trong modal
- ✅ UI/UX cải tiến

**Version 1.0** - Trước đây
- CRUD cơ bản cho danh mục

