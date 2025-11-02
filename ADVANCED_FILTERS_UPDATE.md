# ✅ Advanced Filters Update - Reviews Management

## 🎯 YÊU CẦU ĐÃ THỰC HIỆN

### 1. **User ID → Customer Name** ✅
- **TRƯỚC:** Filter theo User ID (nhập số)
- **SAU:** Filter theo Customer Name (nhập tên, tìm kiếm trong full_name và username)

### 2. **Single Rating → Rating Range** ✅
- **TRƯỚC:** Chọn 1 giá trị rating cụ thể (1, 2, 3, 4, hoặc 5 sao)
- **SAU:** Chọn khoảng từ X sao đến Y sao (Rating From - Rating To)

---

## 🔧 THAY ĐỔI CHI TIẾT

### 1. Frontend (reviews.html)

#### Advanced Filters Form:

**TRƯỚC:**
```html
<!-- Rating - Single Select -->
<select name="rating">
    <option value="">All ratings</option>
    <option value="5">⭐⭐⭐⭐⭐ (5 stars)</option>
    <option value="4">⭐⭐⭐⭐ (4 stars)</option>
    ...
</select>

<!-- User ID - Number Input -->
<input type="number" name="userId" placeholder="Enter User ID">
```

**SAU:**
```html
<!-- Rating From -->
<select name="ratingFrom">
    <option value="">Any</option>
    <option value="1">⭐ 1 star</option>
    <option value="2">⭐⭐ 2 stars</option>
    ...
</select>

<!-- Rating To -->
<select name="ratingTo">
    <option value="">Any</option>
    <option value="1">⭐ 1 star</option>
    ...
</select>

<!-- Customer Name - Text Input -->
<input type="text" name="customerName" placeholder="Enter customer name">
```

#### Filter Status Display:

**TRƯỚC:**
```html
<span th:if="${filterRating != null}">Rating: 5⭐</span>
<span th:if="${filterUserId != null}">User ID: 123</span>
```

**SAU:**
```html
<span th:if="${filterRatingFrom != null || filterRatingTo != null}">
    Rating: 1⭐ - 5⭐
</span>
<span th:if="${filterCustomerName != null}">Customer: John Doe</span>
```

#### JavaScript Validation:

**Thêm validation mới:**
```javascript
// Validate rating range
if (ratingFrom > ratingTo) {
    alert('Rating From must be less than or equal to Rating To');
    return;
}
```

---

### 2. Backend Changes

#### Controller (SellerReviewController.java):

**TRƯỚC:**
```java
@RequestParam(required = false) Integer rating,
@RequestParam(required = false) Long userId,

model.addAttribute("filterRating", rating);
model.addAttribute("filterUserId", userId);
```

**SAU:**
```java
@RequestParam(required = false) Integer ratingFrom,
@RequestParam(required = false) Integer ratingTo,
@RequestParam(required = false) String customerName,

model.addAttribute("filterRatingFrom", ratingFrom);
model.addAttribute("filterRatingTo", ratingTo);
model.addAttribute("filterCustomerName", customerName);
```

#### Service (ProductReviewService.java):

**TRƯỚC:**
```java
public Page<ProductReviews> getFilteredReviews(
    Long sellerId, String status, Integer rating,
    String fromDate, String toDate, Long productId, Long userId, 
    Pageable pageable)
```

**SAU:**
```java
public Page<ProductReviews> getFilteredReviews(
    Long sellerId, String status, Integer ratingFrom, Integer ratingTo,
    String fromDate, String toDate, Long productId, String customerName,
    Pageable pageable)
```

#### Repository (ProductReviewsRepository.java):

**TRƯỚC:**
```sql
WHERE p.sellerId = :sellerId
AND (:rating IS NULL OR pr.rating = :rating)
AND (:userId IS NULL OR pr.userId = :userId)
```

**SAU:**
```sql
WHERE p.sellerId = :sellerId
AND (:ratingFrom IS NULL OR pr.rating >= :ratingFrom)
AND (:ratingTo IS NULL OR pr.rating <= :ratingTo)
AND (:customerName IS NULL OR 
     LOWER(u.fullName) LIKE LOWER(CONCAT('%', :customerName, '%')) OR 
     LOWER(u.username) LIKE LOWER(CONCAT('%', :customerName, '%')))
```

**Lưu ý:** Thêm `LEFT JOIN Users u ON pr.userId = u.userId` để tìm kiếm theo tên

---

## 📊 TÍNH NĂNG MỚI

### Rating Range Filter

#### Ví dụ sử dụng:

| Rating From | Rating To | Kết Quả |
|-------------|-----------|---------|
| (Any) | (Any) | Tất cả reviews |
| 1 | 3 | Reviews từ 1⭐ đến 3⭐ |
| 4 | 5 | Reviews từ 4⭐ đến 5⭐ (tốt) |
| 1 | 2 | Reviews từ 1⭐ đến 2⭐ (xấu) |
| 3 | (Any) | Reviews từ 3⭐ trở lên |
| (Any) | 2 | Reviews 2⭐ trở xuống |

#### Validation:
- ✅ Rating From phải ≤ Rating To
- ✅ Nếu vi phạm → Hiện toast/alert error
- ✅ Focus vào field sai để sửa

### Customer Name Search

#### Cách hoạt động:
- Tìm kiếm **không phân biệt hoa thường**
- Tìm trong **2 fields**:
  1. `full_name` (tên đầy đủ của customer)
  2. `username` (username của customer)
- Hỗ trợ **partial search** (tìm một phần tên)

#### Ví dụ:

| Input | Tìm thấy |
|-------|----------|
| "John" | John Doe, Johnny Smith, John123 |
| "nguy" | Nguyễn Văn A, nguyen123 |
| "smith" | John Smith, mary_smith |

---

## 🧪 CÁCH TEST

### Test 1: Rating Range

1. **Login as seller**
2. **Go to Reviews & Feedback**
3. **Click "Show" advanced filters**
4. **Set Rating From = 1, Rating To = 3**
5. **Click "Apply filters"**
6. ✅ **Kết quả:** Chỉ thấy reviews từ 1⭐ đến 3⭐

### Test 2: Customer Name Search

1. **Login as seller**
2. **Go to Reviews & Feedback**
3. **Click "Show" advanced filters**
4. **Type "john" in Customer Name**
5. **Click "Apply filters"**
6. ✅ **Kết quả:** Chỉ thấy reviews của customers có tên chứa "john"

### Test 3: Combine Filters

1. **Set Rating From = 4, Rating To = 5**
2. **Type "nguyen" in Customer Name**
3. **Set From Date = 01/10/2025**
4. **Click "Apply filters"**
5. ✅ **Kết quả:** Reviews 4-5⭐ từ customers tên "nguyen" sau 01/10

### Test 4: Validation

1. **Set Rating From = 5**
2. **Set Rating To = 1**
3. **Click "Apply filters"**
4. ✅ **Kết quả:** Hiện error "Rating From must be less than or equal to Rating To"

---

## 📁 FILES MODIFIED

1. ✅ **reviews.html**
   - Updated filter form HTML
   - Updated filter status display
   - Added rating range validation JavaScript

2. ✅ **SellerReviewController.java**
   - Changed parameters: rating → ratingFrom/ratingTo
   - Changed parameters: userId → customerName
   - Updated model attributes

3. ✅ **ProductReviewService.java**
   - Updated method signature

4. ✅ **ProductReviewsRepository.java**
   - Updated query with LEFT JOIN Users
   - Changed rating condition to range (>=, <=)
   - Changed userId to customerName with LIKE search

---

## 🎨 UI PREVIEW

### Advanced Filters (Expanded):

```
┌─────────────────────────────────────────────────────┐
│ 🔽 Advanced Filters                          [Hide] │
├─────────────────────────────────────────────────────┤
│ ⭐ Rating From    ⭐ Rating To    📅 From Date      │
│ [Any ▼]          [Any ▼]         [________]        │
│                                                     │
│ 📅 To Date       📦 Product ID    👤 Customer Name │
│ [________]       [________]       [Enter name...]  │
│                                                     │
│ [🔍 Apply filters]  [❌ Clear filters]              │
└─────────────────────────────────────────────────────┘
```

### Active Filters Display:

```
┌─────────────────────────────────────────────────────┐
│ 🔍 Active filters:                                  │
│ [Pending Response] [Rating: 3⭐ - 5⭐]              │
│ [Customer: John] [From: 01/11/2025]                │
│                                      [❌ Clear all] │
└─────────────────────────────────────────────────────┘
```

---

## ✅ BUILD STATUS

```
[INFO] BUILD SUCCESS
[INFO] Total time: 9.044 s
```

---

## 🚀 SẴN SÀNG SỬ DỤNG!

Bây giờ bạn có thể:
1. ✅ Filter reviews theo khoảng rating (từ X đến Y sao)
2. ✅ Tìm kiếm reviews theo tên customer (không cần ID nữa)
3. ✅ Kết hợp nhiều filters cùng lúc
4. ✅ Validation tự động khi rating range không hợp lệ

---

**Ngày Hoàn Thành:** November 3, 2025
**Status:** ✅ READY FOR TESTING

