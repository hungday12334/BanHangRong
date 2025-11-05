# Category Management - Advanced Filters Feature

## 📋 Tổng Quan

Đã thêm chức năng **Advanced Filters** vào trang Category Management, cho phép lọc categories theo nhiều tiêu chí khác nhau, tương tự như tính năng trong Review & Response Management.

## ✨ Tính Năng Mới

### 1. Advanced Filters UI
Thêm một section filter nâng cao với các trường sau:
- **Category ID**: Lọc theo ID category cụ thể
- **Category Name**: Tìm kiếm theo tên category (không phân biệt hoa thường)
- **Product Count From**: Số sản phẩm tối thiểu
- **Product Count To**: Số sản phẩm tối đa
- **From Date**: Lọc category được tạo từ ngày nào
- **To Date**: Lọc category được tạo đến ngày nào

### 2. Filter Status Display
- Hiển thị các filter đang active dưới dạng badges
- Button "Clear filters" để xóa tất cả filters
- Auto-expand filter form nếu có filter đang active

### 3. Toggle Show/Hide
- Button để ẩn/hiện filter form
- Icon thay đổi (chevron-down/chevron-up)
- Text thay đổi (Show/Hide)
- Smooth animation khi toggle

## 🔧 Thay Đổi Kỹ Thuật

### Frontend (HTML + JavaScript)

#### File: `category-management.html`

**1. Thêm Advanced Filters Section (sau Statistics Cards):**
```html
<!-- Advanced Filters -->
<div class="card" style="margin: 24px 0;">
    <div class="card-body">
        <form id="filterForm" th:action="@{/seller/categories}" method="get">
            <!-- Category ID -->
            <input type="number" name="categoryId" placeholder="Enter Category ID">
            
            <!-- Category Name -->
            <input type="text" name="categoryName" placeholder="Search by name">
            
            <!-- Product Count Range -->
            <input type="number" name="productCountFrom" placeholder="Min products">
            <input type="number" name="productCountTo" placeholder="Max products">
            
            <!-- Date Range -->
            <input type="date" name="fromDate">
            <input type="date" name="toDate">
        </form>
    </div>
</div>
```

**2. Thêm Filter Status Display:**
```html
<!-- Filter Status Display -->
<div th:if="${filterCategoryId != null || ...}" class="card">
    <div class="card-body">
        <!-- Active filter badges -->
        <span class="badge">ID: ${filterCategoryId}</span>
        <span class="badge">Name: ${filterCategoryName}</span>
        <!-- ... -->
    </div>
</div>
```

**3. JavaScript Functions:**
```javascript
// Toggle advanced filters
function toggleAdvancedFilters() {
    const filterForm = document.getElementById('filterForm');
    const toggleBtn = document.getElementById('toggleFilters');
    // Toggle display and update icon/text
}

// Clear all filters
function clearCategoryFilters() {
    // Clear all filter inputs
    // Redirect to /seller/categories
    window.location.href = '/seller/categories';
}

// Auto-expand if filters are active
document.addEventListener('DOMContentLoaded', function() {
    const hasActiveFilters = document.querySelector('.card[style*="color-mix"]');
    if (hasActiveFilters) {
        // Auto expand filter form
    }
});
```

### Backend (Java Controller)

#### File: `SellerCategoryController.java`

**1. Thêm Request Parameters:**
```java
@GetMapping
public String categoryManagementPage(
    @RequestParam(required = false) Long categoryId,
    @RequestParam(required = false) String categoryName,
    @RequestParam(required = false) Long productCountFrom,
    @RequestParam(required = false) Long productCountTo,
    @RequestParam(required = false) String fromDate,
    @RequestParam(required = false) String toDate,
    Model model, 
    HttpSession session) {
    // ...
}
```

**2. Apply Filters Logic:**
```java
// Filter by Category ID
if (categoryId != null) {
    filteredCategories = filteredCategories.stream()
        .filter(c -> c.getCategoryId().equals(categoryId))
        .collect(Collectors.toList());
}

// Filter by Category Name (case-insensitive)
if (categoryName != null && !categoryName.trim().isEmpty()) {
    String searchTerm = categoryName.toLowerCase().trim();
    filteredCategories = filteredCategories.stream()
        .filter(c -> c.getName().toLowerCase().contains(searchTerm))
        .collect(Collectors.toList());
}

// Filter by Product Count Range
if (productCountFrom != null) {
    filteredCategories = filteredCategories.stream()
        .filter(c -> {
            Long count = productCountByCategory.getOrDefault(c.getCategoryId(), 0L);
            return count >= productCountFrom;
        })
        .collect(Collectors.toList());
}

// Filter by Date Range
if (fromDate != null && !fromDate.isEmpty()) {
    LocalDateTime fromDateTime = LocalDateTime.parse(fromDate + "T00:00:00");
    filteredCategories = filteredCategories.stream()
        .filter(c -> c.getCreatedAt().isAfter(fromDateTime))
        .collect(Collectors.toList());
}
```

**3. Pass Filter Params to View:**
```java
model.addAttribute("filterCategoryId", categoryId);
model.addAttribute("filterCategoryName", categoryName);
model.addAttribute("filterProductCountFrom", productCountFrom);
model.addAttribute("filterProductCountTo", productCountTo);
model.addAttribute("filterFromDate", fromDate);
model.addAttribute("filterToDate", toDate);
```

### Styling (CSS)

#### File: `category-management.css`

```css
/* Advanced Filters Section */
.card {
    background: var(--bg-elev);
    border: 1px solid var(--border);
    border-radius: 16px;
}

.card-body {
    padding: 20px 24px;
}

/* Filter Badges */
.badge {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    padding: 6px 12px;
    border-radius: 20px;
    font-size: 0.875rem;
    background: color-mix(in oklab, var(--accent) 15%, var(--bg-soft));
    color: var(--accent);
}

/* Filter Form Animation */
#filterForm {
    animation: slideDown 0.3s ease-out;
}

@keyframes slideDown {
    from {
        opacity: 0;
        max-height: 0;
        transform: translateY(-10px);
    }
    to {
        opacity: 1;
        max-height: 1000px;
        transform: translateY(0);
    }
}
```

## 🎯 Cách Sử Dụng

### 1. Mở Advanced Filters
- Click button "Show" bên cạnh "Advanced Filters"
- Form sẽ mở ra với smooth animation

### 2. Nhập Filter Criteria
- **Lọc theo ID**: Nhập category ID cụ thể
- **Lọc theo tên**: Nhập từ khóa tìm kiếm (tìm kiếm một phần)
- **Lọc theo số sản phẩm**: Nhập min/max product count
- **Lọc theo ngày**: Chọn from date và/hoặc to date

### 3. Apply Filters
- Click button "Apply Filters" để thực hiện lọc
- Trang sẽ reload với URL parameters
- Filter badges sẽ hiển thị bên dưới

### 4. Clear Filters
- Click button "Clear Filters" trong filter form
- Hoặc click button "Clear filters" ở filter status display
- Tất cả filters sẽ bị xóa và về trang gốc

## 📊 Ví Dụ URL với Filters

```
/seller/categories?categoryId=5
/seller/categories?categoryName=electronics
/seller/categories?productCountFrom=5&productCountTo=20
/seller/categories?fromDate=2025-01-01&toDate=2025-12-31
/seller/categories?categoryName=electronics&productCountFrom=10
```

## 🎨 UI/UX Features

1. **Responsive Design**: Filters tự động chuyển sang 1 column trên mobile
2. **Visual Feedback**: Active filters hiển thị với màu accent
3. **Smooth Animations**: Slide down animation khi mở/đóng
4. **Icon Rotation**: Chevron icon xoay khi toggle
5. **Badge Display**: Filters đang active hiển thị rõ ràng
6. **Auto-expand**: Form tự động mở nếu có filter đang active

## ⚙️ Technical Details

### Filter Logic Flow
1. User nhập filter criteria và submit form
2. Browser gửi GET request với query parameters
3. Controller nhận parameters và validate
4. Áp dụng filters tuần tự theo thứ tự:
   - Category ID (exact match)
   - Category Name (contains, case-insensitive)
   - Product Count From (>=)
   - Product Count To (<=)
   - From Date (after)
   - To Date (before)
5. Trả về filtered categories và filter params
6. View hiển thị kết quả và active filter badges

### Date Filtering
- Input format: `YYYY-MM-DD` (HTML5 date input)
- Backend parsing: Convert to `LocalDateTime`
- From Date: `T00:00:00` (start of day)
- To Date: `T23:59:59` (end of day)
- Comparison: Use `isAfter()` and `isBefore()`

### Product Count Filtering
- Lấy product count từ `productCountByCategory` Map
- Default value: 0 nếu không có products
- Support range filtering: from-to, only from, only to

## 🐛 Error Handling

1. **Invalid Date Format**: Try-catch khi parse date, log error
2. **Null Values**: Sử dụng `required = false` và null checks
3. **Empty Strings**: Trim và check `!isEmpty()`
4. **Invalid Numbers**: HTML input type="number" ngăn invalid input

## 🔄 Tương Thích

- ✅ Works với existing search/sort functionality
- ✅ Works với pagination (nếu có)
- ✅ Works với bulk actions
- ✅ Tương thích với tất cả browsers hiện đại
- ✅ Responsive trên mobile/tablet

## 📝 Notes

- Filters được áp dụng TRƯỚC khi hiển thị, không làm ảnh hưởng statistics
- Statistics cards vẫn hiển thị tổng số (không filtered)
- Client-side search (searchInput) vẫn hoạt động độc lập
- Filters có thể combine với nhau
- URL có thể bookmark hoặc share

## 🎉 Kết Quả

Tính năng Advanced Filters giúp:
- ✅ Tìm kiếm categories nhanh hơn và chính xác hơn
- ✅ Lọc theo nhiều criteria khác nhau
- ✅ Trải nghiệm người dùng tốt hơn
- ✅ Tương tự như Review Management (consistency)
- ✅ Dễ dàng quản lý khi có nhiều categories

---

**Implemented Date**: November 5, 2025  
**Developer**: GitHub Copilot  
**Version**: 1.0

