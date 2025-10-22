# Test Cases - Quản lý Danh mục (Category Management)

## 📋 Tổng quan
Tài liệu này mô tả các test cases cho tính năng Quản lý Danh mục, bao gồm Happy Cases, Unhappy Cases, và Edge Cases.

---

## 1. TẠO DANH MỤC MỚI (CREATE CATEGORY)

### ✅ Happy Cases

#### TC-CREATE-001: Tạo danh mục với đầy đủ thông tin
**Điều kiện tiên quyết:**
- User đã đăng nhập với role SELLER
- Form thêm danh mục hiển thị

**Các bước thực hiện:**
1. Nhập tên danh mục: "Đồ họa & Thiết kế" (2-100 ký tự)
2. Nhập mô tả: "Các sản phẩm về đồ họa và thiết kế" (tùy chọn)
3. Click nút "Thêm"

**Kết quả mong đợi:**
- ✅ Danh mục được tạo thành công
- ✅ Hiển thị alert success: "✅ Đã tạo danh mục 'Đồ họa & Thiết kế' thành công"
- ✅ Danh mục mới xuất hiện trong bảng danh sách
- ✅ Form được reset về trạng thái rỗng
- ✅ Thống kê "Tổng danh mục" tăng lên 1
- ✅ Thống kê "Danh mục mới (7 ngày)" tăng lên 1

#### TC-CREATE-002: Tạo danh mục chỉ với tên (không có mô tả)
**Các bước thực hiện:**
1. Nhập tên danh mục: "Marketing"
2. Để trống mô tả
3. Click nút "Thêm"

**Kết quả mong đợi:**
- ✅ Danh mục được tạo thành công
- ✅ Cột mô tả hiển thị "---"
- ✅ Alert success hiển thị

### ❌ Unhappy Cases

#### TC-CREATE-003: Tạo danh mục với tên trống
**Các bước thực hiện:**
1. Để trống tên danh mục
2. Nhập mô tả (tùy chọn)
3. Click nút "Thêm"

**Kết quả mong đợi:**
- ❌ Browser validation hiển thị: "Please fill out this field"
- ❌ Form không được submit
- ❌ Không có request gửi đến server

#### TC-CREATE-004: Tạo danh mục với tên quá ngắn (< 2 ký tự)
**Các bước thực hiện:**
1. Nhập tên danh mục: "A" (1 ký tự)
2. Click nút "Thêm"

**Kết quả mong đợi:**
- ❌ Browser validation: "Please lengthen this text to 2 characters or more"
- ❌ Form không được submit

#### TC-CREATE-005: Tạo danh mục với tên quá dài (> 100 ký tự)
**Các bước thực hiện:**
1. Nhập tên danh mục với 101 ký tự
2. Click nút "Thêm"

**Kết quả mong đợi:**
- ❌ Input field không cho phép nhập quá 100 ký tự (maxlength)
- ❌ Hoặc browser validation hiển thị lỗi

#### TC-CREATE-006: Tạo danh mục với tên trùng lặp
**Điều kiện tiên quyết:**
- Đã có danh mục "Đồ họa" trong hệ thống

**Các bước thực hiện:**
1. Nhập tên danh mục: "Đồ họa" (trùng với tên đã tồn tại)
2. Click nút "Thêm"

**Kết quả mong đợi:**
- ❌ Alert error: "❌ Tên danh mục 'Đồ họa' đã tồn tại"
- ❌ Danh mục không được tạo
- ❌ Form vẫn giữ nguyên dữ liệu đã nhập

#### TC-CREATE-007: Tạo danh mục với mô tả quá dài (> 255 ký tự)
**Các bước thực hiện:**
1. Nhập tên danh mục hợp lệ
2. Nhập mô tả với 256 ký tự
3. Click nút "Thêm"

**Kết quả mong đợi:**
- ❌ Input field không cho phép nhập quá 255 ký tự (maxlength)

#### TC-CREATE-008: Tạo danh mục không có quyền truy cập
**Điều kiện tiên quyết:**
- User đăng nhập với role CUSTOMER hoặc không đăng nhập

**Các bước thực hiện:**
1. Truy cập URL: `/seller/categories`

**Kết quả mong đợi:**
- ❌ Redirect về trang login hoặc 403 Forbidden
- ❌ Không được phép truy cập trang quản lý danh mục

### 🔶 Edge Cases

#### TC-CREATE-009: Tạo danh mục với ký tự đặc biệt
**Các bước thực hiện:**
1. Nhập tên: "Đồ họa & Thiết kế (2D/3D) - Premium ⭐"
2. Click nút "Thêm"

**Kết quả mong đợi:**
- ✅ Danh mục được tạo thành công với đầy đủ ký tự đặc biệt
- ✅ Hiển thị đúng trong bảng danh sách

#### TC-CREATE-010: Tạo danh mục với khoảng trắng đầu/cuối
**Các bước thực hiện:**
1. Nhập tên: "  Marketing  " (có spaces đầu và cuối)
2. Click nút "Thêm"

**Kết quả mong đợi:**
- ⚠️ Tùy thuộc vào backend có trim() không:
  - Nếu có trim: Tạo thành công với tên "Marketing"
  - Nếu không trim: Tạo với tên "  Marketing  "

#### TC-CREATE-011: Tạo nhiều danh mục liên tiếp
**Các bước thực hiện:**
1. Tạo danh mục "A"
2. Ngay sau đó tạo danh mục "B"
3. Ngay sau đó tạo danh mục "C"

**Kết quả mong đợi:**
- ✅ Cả 3 danh mục đều được tạo thành công
- ✅ Thống kê cập nhật đúng

#### TC-CREATE-012: Tạo danh mục khi database full/error
**Điều kiện:**
- Database connection bị lỗi hoặc storage full

**Kết quả mong đợi:**
- ❌ Alert error: "❌ Lỗi khi tạo category: [error message]"
- ❌ Danh mục không được tạo

---

## 2. CẬP NHẬT DANH MỤC (UPDATE CATEGORY)

### ✅ Happy Cases

#### TC-UPDATE-001: Cập nhật tên danh mục
**Các bước thực hiện:**
1. Click nút "Sửa" (icon bút) của danh mục
2. Modal hiển thị với dữ liệu hiện tại
3. Thay đổi tên từ "Marketing" → "Marketing & PR"
4. Click "Lưu thay đổi"

**Kết quả mong đợi:**
- ✅ Modal đóng lại
- ✅ Alert success: "✅ Đã cập nhật danh mục thành công"
- ✅ Tên danh mục trong bảng cập nhật thành "Marketing & PR"
- ✅ Timestamp "Ngày tạo" không thay đổi

#### TC-UPDATE-002: Cập nhật mô tả danh mục
**Các bước thực hiện:**
1. Click nút "Sửa"
2. Giữ nguyên tên, chỉ thay đổi mô tả
3. Click "Lưu thay đổi"

**Kết quả mong đợi:**
- ✅ Mô tả được cập nhật thành công
- ✅ Alert success hiển thị

#### TC-UPDATE-003: Cập nhật cả tên và mô tả
**Kết quả mong đợi:**
- ✅ Cả hai trường đều được cập nhật
- ✅ Alert success hiển thị

#### TC-UPDATE-004: Hủy cập nhật (Cancel)
**Các bước thực hiện:**
1. Click nút "Sửa"
2. Thay đổi dữ liệu trong form
3. Click nút "Hủy"

**Kết quả mong đợi:**
- ✅ Modal đóng lại
- ✅ Không có thay đổi nào được lưu
- ✅ Dữ liệu trong bảng giữ nguyên

### ❌ Unhappy Cases

#### TC-UPDATE-005: Cập nhật với tên trống
**Các bước thực hiện:**
1. Click nút "Sửa"
2. Xóa hết tên danh mục (để trống)
3. Click "Lưu thay đổi"

**Kết quả mong đợi:**
- ❌ Browser validation: "Please fill out this field"
- ❌ Modal không đóng
- ❌ Không có request gửi đến server

#### TC-UPDATE-006: Cập nhật tên trùng với danh mục khác
**Điều kiện:**
- Có danh mục A: "Marketing"
- Có danh mục B: "Design"

**Các bước thực hiện:**
1. Sửa danh mục B, đổi tên thành "Marketing" (trùng A)
2. Click "Lưu thay đổi"

**Kết quả mong đợi:**
- ❌ Alert error: "❌ Tên danh mục 'Marketing' đã tồn tại"
- ❌ Danh mục không được cập nhật

#### TC-UPDATE-007: Cập nhật danh mục không tồn tại
**Các bước thực hiện:**
1. Mở modal sửa danh mục (ID = 999 không tồn tại)
2. Thay đổi thông tin
3. Submit form

**Kết quả mong đợi:**
- ❌ Alert error: "❌ Không tìm thấy danh mục với ID: 999"
- ❌ Hoặc "❌ Category not found"

### 🔶 Edge Cases

#### TC-UPDATE-008: Cập nhật danh mục đang có sản phẩm
**Điều kiện:**
- Danh mục có 10 sản phẩm

**Kết quả mong đợi:**
- ✅ Vẫn cập nhật được tên/mô tả
- ✅ Các sản phẩm vẫn giữ liên kết với danh mục

#### TC-UPDATE-009: Cập nhật nhiều lần liên tiếp
**Các bước thực hiện:**
1. Sửa danh mục A → Lưu
2. Ngay lập tức sửa lại danh mục A → Lưu
3. Lặp lại 3-4 lần

**Kết quả mong đợi:**
- ✅ Tất cả các lần cập nhật đều thành công
- ✅ Dữ liệu cuối cùng được lưu đúng

---

## 3. XÓA DANH MỤC (DELETE CATEGORY)

### ✅ Happy Cases

#### TC-DELETE-001: Xóa danh mục không có sản phẩm
**Các bước thực hiện:**
1. Click nút "Xóa" (icon thùng rác)
2. Confirm dialog hiển thị: "Bạn có chắc muốn xóa [tên danh mục]?"
3. Click "OK"

**Kết quả mong đợi:**
- ✅ Danh mục bị xóa khỏi database
- ✅ Alert success: "✅ Đã xóa danh mục '[tên]' thành công"
- ✅ Danh mục biến mất khỏi bảng danh sách
- ✅ Thống kê "Tổng danh mục" giảm 1

#### TC-DELETE-002: Hủy xóa danh mục (Cancel)
**Các bước thực hiện:**
1. Click nút "Xóa"
2. Confirm dialog hiển thị
3. Click "Cancel"

**Kết quả mong đợi:**
- ✅ Dialog đóng lại
- ✅ Danh mục không bị xóa
- ✅ Không có thay đổi trong database

### ❌ Unhappy Cases

#### TC-DELETE-003: Xóa danh mục đang có sản phẩm
**Điều kiện:**
- Danh mục có 5 sản phẩm đang active

**Các bước thực hiện:**
1. Click nút "Xóa" danh mục đang có sản phẩm
2. Confirm xóa

**Kết quả mong đợi:**
- ❌ Alert error: "❌ Không thể xóa danh mục đang có sản phẩm"
- ❌ Hoặc: "❌ Lỗi khi xóa category: [foreign key constraint]"
- ❌ Danh mục không bị xóa

#### TC-DELETE-004: Xóa danh mục không tồn tại
**Các bước thực hiện:**
1. Gửi request xóa danh mục với ID không tồn tại

**Kết quả mong đợi:**
- ❌ Alert error: "❌ Category not found"
- ❌ Hoặc "❌ Không tìm thấy danh mục"

#### TC-DELETE-005: Xóa danh mục không có quyền
**Điều kiện:**
- User A tạo danh mục
- User B (SELLER khác) cố gắng xóa

**Kết quả mong đợi:**
- ❌ Error 403 Forbidden
- ❌ Hoặc alert error: "❌ Bạn không có quyền xóa danh mục này"

### 🔶 Edge Cases

#### TC-DELETE-006: Xóa nhiều danh mục cùng lúc
**Các bước thực hiện:**
- Xem phần BULK DELETE bên dưới

---

## 4. TÌM KIẾM DANH MỤC (SEARCH)

### ✅ Happy Cases

#### TC-SEARCH-001: Tìm kiếm theo tên danh mục (exact match)
**Các bước thực hiện:**
1. Nhập vào ô tìm kiếm: "Marketing"
2. Kết quả hiển thị real-time

**Kết quả mong đợi:**
- ✅ Chỉ hiển thị các danh mục có tên chứa "Marketing"
- ✅ Các danh mục khác bị ẩn
- ✅ Counter cập nhật: "(2)" nếu có 2 kết quả

#### TC-SEARCH-002: Tìm kiếm theo mô tả
**Các bước thực hiện:**
1. Nhập từ khóa có trong mô tả nhưng không có trong tên

**Kết quả mong đợi:**
- ✅ Hiển thị các danh mục có mô tả chứa từ khóa

#### TC-SEARCH-003: Tìm kiếm không phân biệt hoa thường
**Các bước thực hiện:**
1. Nhập: "marketing" (viết thường)
2. Có danh mục tên: "MARKETING" (viết hoa)

**Kết quả mong đợi:**
- ✅ Vẫn tìm thấy danh mục "MARKETING"

#### TC-SEARCH-004: Xóa từ khóa tìm kiếm
**Các bước thực hiện:**
1. Nhập từ khóa → Kết quả lọc
2. Xóa hết từ khóa (ô tìm kiếm trống)

**Kết quả mong đợi:**
- ✅ Hiển thị lại tất cả danh mục
- ✅ Counter về tổng số danh mục ban đầu

### ❌ Unhappy Cases

#### TC-SEARCH-005: Tìm kiếm không có kết quả
**Các bước thực hiện:**
1. Nhập từ khóa: "xyz123abc" (không tồn tại)

**Kết quả mong đợi:**
- ✅ Hiển thị empty state
- ✅ Counter: "(0)"
- ✅ Tất cả các row bị ẩn

### 🔶 Edge Cases

#### TC-SEARCH-006: Tìm kiếm với ký tự đặc biệt
**Các bước thực hiện:**
1. Nhập: "Marketing & PR"

**Kết quả mong đợi:**
- ✅ Tìm được danh mục có tên chứa "Marketing & PR"

#### TC-SEARCH-007: Tìm kiếm với khoảng trắng
**Các bước thực hiện:**
1. Nhập: "   Marketing   " (nhiều spaces)

**Kết quả mong đợi:**
- ✅ Vẫn tìm được kết quả phù hợp

#### TC-SEARCH-008: Tìm kiếm khi đang ở bulk mode
**Kết quả mong đợi:**
- ✅ Tìm kiếm vẫn hoạt động bình thường
- ✅ Checkbox vẫn hiển thị ở các row tìm được

---

## 5. SẮP XẾP DANH MỤC (SORT)

### ✅ Happy Cases

#### TC-SORT-001: Sắp xếp theo tên A-Z
**Các bước thực hiện:**
1. Chọn dropdown: "Tên (A-Z)"

**Kết quả mong đợi:**
- ✅ Danh mục sắp xếp theo alphabet tăng dần
- ✅ "A Marketing" lên trước "Z Design"

#### TC-SORT-002: Sắp xếp theo tên Z-A
**Kết quả mong đợi:**
- ✅ Danh mục sắp xếp giảm dần

#### TC-SORT-003: Sắp xếp theo ngày tạo (Mới nhất)
**Kết quả mong đợi:**
- ✅ Danh mục mới nhất lên đầu

#### TC-SORT-004: Sắp xếp theo số sản phẩm (Nhiều nhất)
**Kết quả mong đợi:**
- ✅ Danh mục có 100 sản phẩm lên trước danh mục có 5 sản phẩm

### 🔶 Edge Cases

#### TC-SORT-005: Sắp xếp khi có tên trùng nhau
**Điều kiện:**
- 2 danh mục cùng tên "Marketing"

**Kết quả mong đợi:**
- ✅ Sắp xếp theo tiêu chí phụ (ID hoặc ngày tạo)

#### TC-SORT-006: Sắp xếp sau khi tìm kiếm
**Các bước thực hiện:**
1. Tìm kiếm → Có 5 kết quả
2. Chọn sắp xếp

**Kết quả mong đợi:**
- ✅ Chỉ sắp xếp 5 kết quả đang hiển thị

---

## 6. XÓA NHIỀU DANH MỤC (BULK DELETE)

### ✅ Happy Cases

#### TC-BULK-001: Bật chế độ chọn nhiều
**Các bước thực hiện:**
1. Click nút "Chọn nhiều"

**Kết quả mong đợi:**
- ✅ Cột checkbox xuất hiện
- ✅ Nút "Xóa đã chọn" hiển thị (disabled nếu chưa chọn)
- ✅ Nút "Chọn nhiều" đổi thành "Hủy chọn nhiều"

#### TC-BULK-002: Chọn nhiều danh mục và xóa
**Các bước thực hiện:**
1. Bật bulk mode
2. Tick 3 checkbox
3. Counter hiển thị: "Xóa đã chọn (3)"
4. Click "Xóa đã chọn"
5. Confirm

**Kết quả mong đợi:**
- ✅ Cả 3 danh mục bị xóa
- ✅ Alert: "✅ Đã xóa 3 danh mục thành công"
- ✅ Thống kê cập nhật

#### TC-BULK-003: Chọn tất cả (Select All)
**Các bước thực hiện:**
1. Click checkbox "Select All"

**Kết quả mong đợi:**
- ✅ Tất cả checkbox trong danh sách được tick
- ✅ Counter hiển thị số lượng đúng

#### TC-BULK-004: Bỏ chọn tất cả
**Các bước thực hiện:**
1. Click "Select All" lần 1 → Tất cả được chọn
2. Click "Select All" lần 2

**Kết quả mong đợi:**
- ✅ Tất cả checkbox bị bỏ tick
- ✅ Counter: "(0)"

#### TC-BULK-005: Tắt bulk mode
**Các bước thực hiện:**
1. Bật bulk mode, chọn vài item
2. Click "Hủy chọn nhiều"

**Kết quả mong đợi:**
- ✅ Cột checkbox ẩn đi
- ✅ Tất cả checkbox bị uncheck
- ✅ Nút "Xóa đã chọn" ẩn đi

### ❌ Unhappy Cases

#### TC-BULK-006: Click "Xóa đã chọn" khi chưa chọn gì
**Các bước thực hiện:**
1. Bật bulk mode
2. Không tick checkbox nào
3. Click "Xóa đã chọn"

**Kết quả mong đợi:**
- ❌ Alert: "Vui lòng chọn ít nhất một danh mục để xóa."
- ❌ Không có danh mục nào bị xóa

#### TC-BULK-007: Hủy confirm dialog
**Các bước thực hiện:**
1. Chọn nhiều danh mục
2. Click "Xóa đã chọn"
3. Click "Cancel" trong confirm dialog

**Kết quả mong đợi:**
- ✅ Dialog đóng
- ✅ Không có danh mục nào bị xóa
- ✅ Các checkbox vẫn giữ trạng thái đã chọn

#### TC-BULK-008: Xóa nhiều, một số có sản phẩm
**Điều kiện:**
- Chọn 5 danh mục
- 2 danh mục có sản phẩm
- 3 danh mục không có sản phẩm

**Kết quả mong đợi:**
- ✅ 3 danh mục không có sản phẩm bị xóa thành công
- ❌ 2 danh mục có sản phẩm không xóa được
- ⚠️ Alert: "✅ Đã xóa 3 danh mục thành công (có 2 lỗi)"
- ⚠️ Alert error: "❌ Một số danh mục không thể xóa: ID 5: [error]; ID 8: [error]"

### 🔶 Edge Cases

#### TC-BULK-009: Select All khi đang tìm kiếm
**Các bước thực hiện:**
1. Tìm kiếm → Còn 3 kết quả
2. Click "Select All"

**Kết quả mong đợi:**
- ✅ Chỉ 3 kết quả đang hiển thị được chọn
- ✅ Các row bị ẩn không được chọn

#### TC-BULK-010: Xóa bulk với số lượng lớn
**Các bước thực hiện:**
1. Chọn 50 danh mục
2. Xóa bulk

**Kết quả mong đợi:**
- ⚠️ Có thể mất vài giây
- ✅ Tất cả được xóa thành công (hoặc báo lỗi từng item)
- ✅ Alert hiển thị kết quả

---

## 7. XEM SẢN PHẨM TRONG DANH MỤC (VIEW PRODUCTS)

### ✅ Happy Cases

#### TC-VIEW-001: Xem sản phẩm của danh mục có sản phẩm
**Các bước thực hiện:**
1. Click icon mắt (👁️) ở cột "Số sản phẩm"

**Kết quả mong đợi:**
- ✅ Modal mở ra với title: "Sản phẩm trong danh mục: [tên]"
- ✅ Hiển thị loading state ban đầu
- ✅ Sau vài giây, hiển thị danh sách sản phẩm
- ✅ Mỗi sản phẩm hiển thị:
  - Hình ảnh
  - Tên sản phẩm
  - SKU
  - Giá
  - Kho (quantity)
  - Đã bán

#### TC-VIEW-002: Đóng modal
**Các bước thực hiện:**
1. Mở modal xem sản phẩm
2. Click nút "Đóng" hoặc "X"

**Kết quả mong đợi:**
- ✅ Modal đóng lại
- ✅ Không có thay đổi gì

### ❌ Unhappy Cases

#### TC-VIEW-003: Xem sản phẩm của danh mục trống
**Điều kiện:**
- Danh mục có 0 sản phẩm (nhưng vẫn hiển thị nút xem)

**Kết quả mong đợi:**
- ✅ Modal mở ra
- ✅ Hiển thị empty state: "Không có sản phẩm nào"
- ✅ Icon inbox

#### TC-VIEW-004: API error khi load sản phẩm
**Điều kiện:**
- API `/seller/categories/{id}/products` trả về 500 error

**Kết quả mong đợi:**
- ❌ Modal hiển thị error state
- ❌ Icon alert với text: "Lỗi khi tải sản phẩm"
- ❌ Console log error

### 🔶 Edge Cases

#### TC-VIEW-005: Xem sản phẩm của danh mục có nhiều sản phẩm (100+)
**Kết quả mong đợi:**
- ✅ Modal có scrollbar
- ✅ Tất cả sản phẩm được load và hiển thị
- ⚠️ Có thể mất vài giây

#### TC-VIEW-006: Sản phẩm không có hình ảnh
**Kết quả mong đợi:**
- ✅ Hiển thị placeholder: `/img/white.png`

---

## 8. XUẤT CSV (EXPORT CSV)

### ✅ Happy Cases

#### TC-EXPORT-001: Xuất tất cả danh mục
**Các bước thực hiện:**
1. Click nút "Export CSV"

**Kết quả mong đợi:**
- ✅ File CSV được tải xuống
- ✅ Tên file: `categories_[timestamp].csv`
- ✅ File chứa header: "ID,Tên Danh mục,Mô tả,Số sản phẩm,Ngày tạo"
- ✅ File chứa tất cả danh mục đang hiển thị
- ✅ Hỗ trợ tiếng Việt (UTF-8 BOM)
- ✅ Có thể mở bằng Excel

#### TC-EXPORT-002: Xuất sau khi tìm kiếm
**Các bước thực hiện:**
1. Tìm kiếm → Còn 3 kết quả
2. Click "Export CSV"

**Kết quả mong đợi:**
- ✅ File CSV chỉ chứa 3 danh mục đang hiển thị
- ✅ Không xuất các row bị ẩn

### ❌ Unhappy Cases

#### TC-EXPORT-003: Xuất khi không có danh mục nào
**Điều kiện:**
- Danh sách trống hoặc tìm kiếm không có kết quả

**Kết quả mong đợi:**
- ✅ File CSV vẫn được tải xuống
- ✅ Chỉ có header, không có data rows

### 🔶 Edge Cases

#### TC-EXPORT-004: Xuất với ký tự đặc biệt trong tên
**Điều kiện:**
- Danh mục có tên: "Marketing & PR (2024)"
- Mô tả có dấu phẩy, quotes

**Kết quả mong đợi:**
- ✅ Ký tự đặc biệt được escape đúng
- ✅ CSV format hợp lệ
- ✅ Mở bằng Excel không bị lỗi

---

## 9. THỐNG KÊ (STATISTICS)

### ✅ Happy Cases

#### TC-STATS-001: Hiển thị thống kê khi load trang
**Kết quả mong đợi:**
- ✅ Card "Tổng danh mục" hiển thị số đúng
- ✅ Card "Danh mục có sản phẩm" đếm đúng
- ✅ Card "Tổng sản phẩm" tính tổng đúng
- ✅ Card "Danh mục mới (7 ngày)" đếm đúng

#### TC-STATS-002: Thống kê cập nhật sau khi thao tác
**Các bước thực hiện:**
1. Ghi nhớ số "Tổng danh mục": 10
2. Tạo danh mục mới
3. Trang reload

**Kết quả mong đợi:**
- ✅ "Tổng danh mục" = 11
- ✅ "Danh mục mới (7 ngày)" tăng lên 1

### 🔶 Edge Cases

#### TC-STATS-003: Thống kê khi không có danh mục
**Kết quả mong đợi:**
- ✅ Tất cả cards hiển thị "0"

#### TC-STATS-004: Thống kê với số lớn (1000+ danh mục)
**Kết quả mong đợi:**
- ✅ Số lượng hiển thị đúng
- ⚠️ Có thể cần format: "1,234"

---

## 10. PHÂN QUYỀN & BẢO MẬT (AUTHORIZATION & SECURITY)

### ❌ Security Test Cases

#### TC-SEC-001: Truy cập khi chưa đăng nhập
**Các bước thực hiện:**
1. Logout
2. Truy cập: `/seller/categories`

**Kết quả mong đợi:**
- ❌ Redirect về `/login`
- ❌ Không được phép xem trang

#### TC-SEC-002: Truy cập với role CUSTOMER
**Kết quả mong đợi:**
- ❌ 403 Forbidden
- ❌ Hoặc redirect về customer dashboard

#### TC-SEC-003: CSRF token validation
**Các bước thực hiện:**
1. Submit form không có CSRF token
2. Hoặc CSRF token sai

**Kết quả mong đợi:**
- ❌ 403 Forbidden
- ❌ Form không được submit

#### TC-SEC-004: SQL Injection attempt
**Các bước thực hiện:**
1. Nhập tên danh mục: `'; DROP TABLE categories; --`
2. Submit form

**Kết quả mong đợi:**
- ✅ Tên được lưu như string bình thường
- ✅ Không có SQL injection
- ✅ Database an toàn

#### TC-SEC-005: XSS attempt
**Các bước thực hiện:**
1. Nhập tên: `<script>alert('XSS')</script>`
2. Submit

**Kết quả mong đợi:**
- ✅ Tên được escape/sanitize
- ✅ Script không chạy khi hiển thị
- ✅ Hiển thị dạng text thuần

---

## 11. HIỆU NĂNG (PERFORMANCE)

### ⚡ Performance Test Cases

#### TC-PERF-001: Load trang với 100 danh mục
**Kết quả mong đợi:**
- ✅ Trang load < 2 giây
- ✅ Không có lag khi scroll

#### TC-PERF-002: Tìm kiếm real-time
**Kết quả mong đợi:**
- ✅ Kết quả hiển thị ngay lập tức (< 100ms)
- ✅ Không có delay đáng kể

#### TC-PERF-003: Sắp xếp với nhiều item
**Kết quả mong đợi:**
- ✅ Sắp xếp hoàn thành < 500ms

#### TC-PERF-004: Export CSV với 1000 danh mục
**Kết quả mong đợi:**
- ✅ File được tạo và download thành công
- ⚠️ Có thể mất 2-3 giây

---

## 12. TRẢI NGHIỆM NGƯỜI DÙNG (UX)

### ✅ UX Test Cases

#### TC-UX-001: Alert tự động đóng
**Kết quả mong đợi:**
- ✅ Alert success/error đóng sau 5 giây
- ✅ Không cần click "X"

#### TC-UX-002: Form reset sau khi thêm
**Kết quả mong đợi:**
- ✅ Input fields trống sau khi submit thành công
- ✅ Sẵn sàng thêm danh mục tiếp theo

#### TC-UX-003: Loading states
**Kết quả mong đợi:**
- ✅ Modal xem sản phẩm hiển thị loading spinner
- ✅ User biết hệ thống đang xử lý

#### TC-UX-004: Empty states
**Kết quả mong đợi:**
- ✅ Khi không có danh mục: Hiển thị icon + text hướng dẫn
- ✅ Khi tìm kiếm không có kết quả: Hiển thị friendly message

#### TC-UX-005: Responsive design
**Các bước thực hiện:**
1. Resize browser xuống mobile size (375px)

**Kết quả mong đợi:**
- ✅ Table có scrollbar ngang
- ✅ Buttons vẫn click được
- ✅ Form vẫn sử dụng được

#### TC-UX-006: Keyboard navigation
**Kết quả mong đợi:**
- ✅ Tab qua các input fields
- ✅ Enter để submit form
- ✅ Esc để đóng modal

---

## 📊 TÓM TẮT TEST COVERAGE

| Module | Happy Cases | Unhappy Cases | Edge Cases | Total |
|--------|-------------|---------------|------------|-------|
| Create | 2 | 7 | 4 | 13 |
| Update | 4 | 3 | 2 | 9 |
| Delete | 2 | 3 | 1 | 6 |
| Search | 4 | 1 | 3 | 8 |
| Sort | 4 | 0 | 2 | 6 |
| Bulk Delete | 5 | 3 | 2 | 10 |
| View Products | 2 | 2 | 2 | 6 |
| Export CSV | 2 | 1 | 1 | 4 |
| Statistics | 2 | 0 | 2 | 4 |
| Security | 0 | 5 | 0 | 5 |
| Performance | 0 | 0 | 4 | 4 |
| UX | 6 | 0 | 0 | 6 |
| **TOTAL** | **33** | **25** | **23** | **81** |

---

## 🧪 HƯỚNG DẪN TEST

### Manual Testing Checklist
1. ✅ Đi qua tất cả Happy Cases trước
2. ⚠️ Test các Unhappy Cases quan trọng
3. 🔍 Test một số Edge Cases thú vị
4. 🔐 Đảm bảo Security test cases pass
5. 📱 Test responsive trên mobile/tablet

### Automated Testing (Khuyến nghị)
- **Unit Tests**: Service layer, Repository methods
- **Integration Tests**: Controller endpoints
- **E2E Tests**: Selenium/Cypress cho các flows quan trọng
- **Load Tests**: JMeter cho performance testing

### Priority Level
- **P0 (Critical)**: Security, Create, Delete, Authorization
- **P1 (High)**: Update, Search, Bulk Delete
- **P2 (Medium)**: Sort, View Products, Export
- **P3 (Low)**: Statistics, UX enhancements

---

## 🐛 BUG REPORT TEMPLATE

Khi phát hiện bug, báo cáo theo format:

```
**BUG ID**: CAT-BUG-001
**Severity**: Critical / High / Medium / Low
**Test Case**: TC-CREATE-006
**Summary**: Không validate tên danh mục trùng

**Steps to Reproduce**:
1. Tạo danh mục "Marketing"
2. Tạo danh mục "Marketing" lần nữa
3. Observe

**Expected**: Alert error "Tên đã tồn tại"
**Actual**: Danh mục được tạo thành công (duplicate)

**Environment**: Chrome 120, Windows 11
**Screenshot**: [attach]
```

---

## ✅ ACCEPTANCE CRITERIA

Tính năng được coi là PASS khi:
- ✅ Tất cả P0 test cases PASS
- ✅ 95% P1 test cases PASS
- ✅ 80% P2 test cases PASS
- ✅ Không có critical bugs
- ✅ Performance đạt yêu cầu
- ✅ Security tests PASS 100%

---

**Người tạo**: AI Assistant  
**Ngày tạo**: 23/10/2025  
**Version**: 1.0  
**Tổng số test cases**: 81

