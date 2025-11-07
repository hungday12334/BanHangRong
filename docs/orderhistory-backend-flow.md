# TÀI LIỆU CHI TIẾT LUỒNG BACKEND - ORDER HISTORY

## TỔNG QUAN
Tài liệu này mô tả chi tiết luồng xử lý backend của trang **Order History** (`/orderhistory`), bao gồm các endpoint chính và các API liên quan.

---

## 1. ENDPOINT CHÍNH: GET /orderhistory

### 1.1. Controller
**File:** `src/main/java/banhangrong/su25/Controller/CustomerDashboardController.java`

**Method:** `orderHistory()`
**Annotation:** `@GetMapping("/orderhistory")`

### 1.2. Tham số Request (Query Parameters)

| Tham số | Kiểu | Bắt buộc | Mặc định | Mô tả |
|---------|------|----------|----------|-------|
| `page` | int | Không | `0` | Số trang (bắt đầu từ 0) |
| `size` | int | Không | `3` | Số lượng đơn hàng mỗi trang |
| `search` | String | Không | `null` | Từ khóa tìm kiếm (tên sản phẩm hoặc seller ID) |
| `status` | String | Không | `null` | Trạng thái đơn hàng (`completed`, `cancelled`, `all`) |

### 1.3. Luồng Xử Lý Chi Tiết

#### BƯỚC 1: Xác thực người dùng
```java
Authentication auth = SecurityContextHolder.getContext().getAuthentication();
Users currentUser = null;
if (auth != null && auth.isAuthenticated()) {
    String username = auth.getName();
    currentUser = usersRepository.findByUsername(username).orElse(null);
    if (currentUser == null) {
        return "redirect:/login";
    }
} else {
    return "redirect:/login";
}
```

**Mô tả:**
- Lấy thông tin authentication từ SecurityContext
- Kiểm tra user đã đăng nhập chưa
- Lấy username từ authentication
- Tìm user trong database qua `UsersRepository.findByUsername()`
- Nếu không tìm thấy hoặc chưa đăng nhập → redirect về `/login`

**Repository sử dụng:**
- `UsersRepository.findByUsername(String username)`

---

#### BƯỚC 2: Tạo Pageable object
```java
PageRequest pageable = PageRequest.of(
    Math.max(page, 0), 
    Math.max(size, 1), 
    Sort.by(Sort.Order.desc("createdAt"))
);
```

**Mô tả:**
- Tạo đối tượng phân trang với:
  - `page`: Đảm bảo >= 0
  - `size`: Đảm bảo >= 1
  - Sắp xếp theo `createdAt` giảm dần (mới nhất trước)

---

#### BƯỚC 3: Truy vấn Orders theo điều kiện

**3.1. Nếu có search term:**
```java
if (search != null && !search.trim().isEmpty()) {
    ordersPage = ordersRepository.findByUserIdAndSearchTerm(
        currentUser.getUserId(), 
        search.trim(), 
        pageable
    );
}
```

**Repository Method:**
```java
@Query("SELECT DISTINCT o FROM Orders o " +
       "JOIN OrderItems oi ON o.orderId = oi.orderId " +
       "JOIN Products p ON oi.productId = p.productId " +
       "WHERE o.userId = :userId " +
       "AND (LOWER(p.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
       "OR LOWER(CAST(o.sellerId AS string)) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) " +
       "ORDER BY o.createdAt DESC")
Page<Orders> findByUserIdAndSearchTerm(
    @Param("userId") Long userId, 
    @Param("searchTerm") String searchTerm, 
    Pageable pageable
);
```

**Mô tả:**
- Tìm kiếm đơn hàng theo:
  - Tên sản phẩm (không phân biệt hoa thường)
  - Seller ID (không phân biệt hoa thường)
- Chỉ lấy đơn hàng của user hiện tại
- Sắp xếp theo `createdAt` DESC

**SQL tương đương:**
```sql
SELECT DISTINCT o.* 
FROM orders o
JOIN order_items oi ON o.order_id = oi.order_id
JOIN products p ON oi.product_id = p.product_id
WHERE o.user_id = :userId
AND (LOWER(p.name) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
     OR LOWER(CAST(o.seller_id AS CHAR)) LIKE LOWER(CONCAT('%', :searchTerm, '%')))
ORDER BY o.created_at DESC
LIMIT :size OFFSET :page * :size
```

---

**3.2. Nếu có status filter:**
```java
else if (status != null && !status.trim().isEmpty() && !status.equalsIgnoreCase("all")) {
    ordersPage = ordersRepository.findByUserIdAndStatusOrderByCreatedAtDesc(
        currentUser.getUserId(), 
        status.trim(), 
        pageable
    );
}
```

**Repository Method:**
```java
Page<Orders> findByUserIdAndStatusOrderByCreatedAtDesc(
    Long userId, 
    String status, 
    Pageable pageable
);
```

**Mô tả:**
- Lọc đơn hàng theo trạng thái (`completed`, `cancelled`, `pending`)
- Chỉ lấy đơn hàng của user hiện tại
- Sắp xếp theo `createdAt` DESC

---

**3.3. Nếu không có filter (mặc định):**
```java
else {
    ordersPage = ordersRepository.findByUserIdOrderByCreatedAtDesc(
        currentUser.getUserId(), 
        pageable
    );
}
```

**Repository Method:**
```java
Page<Orders> findByUserIdOrderByCreatedAtDesc(
    Long userId, 
    Pageable pageable
);
```

**Mô tả:**
- Lấy tất cả đơn hàng của user
- Sắp xếp theo `createdAt` DESC

---

#### BƯỚC 4: Lấy danh sách Orders từ Page
```java
orders = ordersPage.getContent();
```

**Mô tả:**
- Lấy danh sách `Orders` từ `Page<Orders>`
- `orders` là `List<Orders>`

---

#### BƯỚC 5: Lấy OrderItems và Products cho mỗi Order

```java
Map<Long, List<OrderItems>> orderItemsMap = new HashMap<>();
Map<Long, String> productNamesMap = new HashMap<>();
Map<Long, Products> productsMap = new HashMap<>();

for (Orders order : orders) {
    // Lấy tất cả OrderItems của order này
    List<OrderItems> items = orderItemsRepository.findByOrderId(order.getOrderId());
    orderItemsMap.put(order.getOrderId(), items);
    
    // Với mỗi OrderItem, lấy thông tin Product
    for (OrderItems item : items) {
        if (item.getProductId() != null) {
            productsRepository.findById(item.getProductId()).ifPresent(product -> {
                productNamesMap.put(item.getProductId(), product.getName());
                productsMap.put(item.getProductId(), product);
            });
        }
    }
}
```

**Repository Methods sử dụng:**

1. **OrderItemsRepository.findByOrderId():**
```java
List<OrderItems> findByOrderId(Long orderId);
```
- Lấy tất cả `OrderItems` của một `orderId`

2. **ProductsRepository.findById():**
```java
Optional<Products> findById(Long productId);
```
- Lấy thông tin `Products` theo `productId`

**Mô tả:**
- Tạo 3 Map để lưu trữ:
  - `orderItemsMap`: Map `orderId` → `List<OrderItems>`
  - `productNamesMap`: Map `productId` → `productName`
  - `productsMap`: Map `productId` → `Products` object
- Với mỗi Order:
  - Lấy tất cả OrderItems
  - Với mỗi OrderItem, lấy thông tin Product và lưu vào Map

**Lưu ý:** 
- Sử dụng Map để tránh truy vấn lặp lại
- Nếu `productId` null thì bỏ qua

---

#### BƯỚC 6: Thêm dữ liệu vào Model

```java
model.addAttribute("orders", orders);
model.addAttribute("orderItemsMap", orderItemsMap);
model.addAttribute("productNamesMap", productNamesMap);
model.addAttribute("productsMap", productsMap);
model.addAttribute("page", ordersPage.getNumber());
model.addAttribute("totalPages", ordersPage.getTotalPages());
model.addAttribute("size", ordersPage.getSize());
model.addAttribute("user", currentUser);
model.addAttribute("search", search);
model.addAttribute("status", status);

try {
    model.addAttribute("cartCount", shoppingCartRepository.countByUserId(currentUser.getUserId()));
} catch (Exception ignored) {}
```

**Repository Method:**
```java
long countByUserId(Long userId);
```

**Mô tả:**
- Thêm tất cả dữ liệu vào Model để render view
- `cartCount`: Số lượng sản phẩm trong giỏ hàng (có try-catch để tránh lỗi)

**Dữ liệu trong Model:**

| Attribute | Kiểu | Mô tả |
|-----------|------|-------|
| `orders` | `List<Orders>` | Danh sách đơn hàng |
| `orderItemsMap` | `Map<Long, List<OrderItems>>` | Map orderId → danh sách OrderItems |
| `productNamesMap` | `Map<Long, String>` | Map productId → tên sản phẩm |
| `productsMap` | `Map<Long, Products>` | Map productId → object Products |
| `page` | `int` | Số trang hiện tại |
| `totalPages` | `int` | Tổng số trang |
| `size` | `int` | Số lượng items mỗi trang |
| `user` | `Users` | Thông tin user hiện tại |
| `search` | `String` | Từ khóa tìm kiếm |
| `status` | `String` | Trạng thái filter |
| `cartCount` | `Long` | Số lượng sản phẩm trong giỏ hàng |

---

#### BƯỚC 7: Return View
```java
return "customer/orderhistory";
```

**Mô tả:**
- Render template `customer/orderhistory.html`
- Template sử dụng Thymeleaf để hiển thị dữ liệu

---

#### BƯỚC 8: Xử lý Exception
```java
catch (Exception e) {
    e.printStackTrace();
    return "redirect:/customer/dashboard?error=orderhistory_error";
}
```

**Mô tả:**
- Nếu có lỗi xảy ra → redirect về dashboard với error parameter
- In stack trace để debug

---

## 2. ENTITY CLASSES

### 2.1. Orders Entity

**File:** `src/main/java/banhangrong/su25/Entity/Orders.java`

**Bảng:** `orders`

**Các trường:**

| Trường | Kiểu | Mô tả |
|--------|------|-------|
| `orderId` | `Long` | ID đơn hàng (Primary Key, Auto Increment) |
| `userId` | `Long` | ID người mua |
| `sellerId` | `Long` | ID người bán |
| `totalAmount` | `BigDecimal` | Tổng tiền đơn hàng |
| `status` | `String` | Trạng thái: `pending`, `completed`, `cancelled` |
| `createdAt` | `LocalDateTime` | Thời gian tạo |
| `updatedAt` | `LocalDateTime` | Thời gian cập nhật |

---

### 2.2. OrderItems Entity

**File:** `src/main/java/banhangrong/su25/Entity/OrderItems.java`

**Bảng:** `order_items`

**Các trường:**

| Trường | Kiểu | Mô tả |
|--------|------|-------|
| `orderItemId` | `Long` | ID item (Primary Key, Auto Increment) |
| `orderId` | `Long` | ID đơn hàng (Foreign Key) |
| `productId` | `Long` | ID sản phẩm (Foreign Key) |
| `quantity` | `Integer` | Số lượng |
| `priceAtTime` | `BigDecimal` | Giá tại thời điểm mua |
| `createdAt` | `LocalDateTime` | Thời gian tạo |

---

### 2.3. Products Entity

**File:** `src/main/java/banhangrong/su25/Entity/Products.java`

**Bảng:** `products`

**Các trường quan trọng:**

| Trường | Kiểu | Mô tả |
|--------|------|-------|
| `productId` | `Long` | ID sản phẩm (Primary Key) |
| `sellerId` | `Long` | ID người bán |
| `name` | `String` | Tên sản phẩm |
| `description` | `String` | Mô tả |
| `price` | `BigDecimal` | Giá gốc |
| `salePrice` | `BigDecimal` | Giá khuyến mãi |
| `quantity` | `Integer` | Số lượng tồn kho |
| `status` | `String` | Trạng thái: `pending`, `public`, `hidden`, `cancelled` |

---

## 3. REPOSITORY INTERFACES

### 3.1. OrdersRepository

**File:** `src/main/java/banhangrong/su25/Repository/OrdersRepository.java`

**Extends:** `JpaRepository<Orders, Long>`

**Methods:**

#### 3.1.1. findByUserIdOrderByCreatedAtDesc()
```java
Page<Orders> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
```
- Lấy tất cả đơn hàng của user, sắp xếp theo `createdAt` DESC

#### 3.1.2. findByUserIdAndStatusOrderByCreatedAtDesc()
```java
Page<Orders> findByUserIdAndStatusOrderByCreatedAtDesc(
    Long userId, 
    String status, 
    Pageable pageable
);
```
- Lấy đơn hàng của user theo trạng thái, sắp xếp theo `createdAt` DESC

#### 3.1.3. findByUserIdAndSearchTerm()
```java
@Query("SELECT DISTINCT o FROM Orders o " +
       "JOIN OrderItems oi ON o.orderId = oi.orderId " +
       "JOIN Products p ON oi.productId = p.productId " +
       "WHERE o.userId = :userId " +
       "AND (LOWER(p.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
       "OR LOWER(CAST(o.sellerId AS string)) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) " +
       "ORDER BY o.createdAt DESC")
Page<Orders> findByUserIdAndSearchTerm(
    @Param("userId") Long userId, 
    @Param("searchTerm") String searchTerm, 
    Pageable pageable
);
```
- Tìm kiếm đơn hàng theo tên sản phẩm hoặc seller ID

---

### 3.2. OrderItemsRepository

**File:** `src/main/java/banhangrong/su25/Repository/OrderItemsRepository.java`

**Extends:** `JpaRepository<OrderItems, Long>`

**Methods:**

#### 3.2.1. findByOrderId()
```java
List<OrderItems> findByOrderId(Long orderId);
```
- Lấy tất cả OrderItems của một order

#### 3.2.2. findByUserId()
```java
@Query("SELECT oi FROM OrderItems oi JOIN Orders o ON oi.orderId = o.orderId WHERE o.userId = :userId")
List<OrderItems> findByUserId(@Param("userId") Long userId);
```
- Lấy tất cả OrderItems của một user

#### 3.2.3. findByOrderItemIdAndUserId()
```java
@Query("SELECT oi FROM OrderItems oi JOIN Orders o ON oi.orderId = o.orderId " +
       "WHERE oi.orderItemId = :orderItemId AND o.userId = :userId")
Optional<OrderItems> findByOrderItemIdAndUserId(
    @Param("orderItemId") Long orderItemId, 
    @Param("userId") Long userId
);
```
- Lấy OrderItem theo ID và kiểm tra thuộc về user

---

### 3.3. ProductsRepository

**File:** `src/main/java/banhangrong/su25/Repository/ProductsRepository.java`

**Extends:** `JpaRepository<Products, Long>`

**Methods sử dụng:**

#### 3.3.1. findById()
```java
Optional<Products> findById(Long productId);
```
- Lấy sản phẩm theo ID

---

### 3.4. UsersRepository

**File:** `src/main/java/banhangrong/su25/Repository/UsersRepository.java`

**Methods sử dụng:**

#### 3.4.1. findByUsername()
```java
Optional<Users> findByUsername(String username);
```
- Lấy user theo username

---

### 3.5. ShoppingCartRepository

**File:** `src/main/java/banhangrong/su25/Repository/ShoppingCartRepository.java`

**Methods sử dụng:**

#### 3.5.1. countByUserId()
```java
long countByUserId(Long userId);
```
- Đếm số lượng sản phẩm trong giỏ hàng của user

---

## 4. API LIÊN QUAN

### 4.1. POST /api/rating/submit

**Controller:** `RatingController.java`

**Mục đích:** Submit đánh giá sản phẩm từ order history

#### 4.1.1. Request Parameters

| Parameter | Kiểu | Bắt buộc | Mô tả |
|-----------|------|----------|-------|
| `orderItemId` | `Long` | Có | ID của OrderItem cần đánh giá |
| `productRating` | `Integer` | Có | Điểm đánh giá sản phẩm (1-5) |
| `serviceRating` | `Integer` | Có | Điểm đánh giá dịch vụ seller (1-5) |
| `comment` | `String` | Có | Nội dung đánh giá |
| `mediaFiles` | `MultipartFile[]` | Không | File ảnh/video đính kèm |

#### 4.1.2. Luồng Xử Lý

**BƯỚC 1: Xác thực User**
```java
Authentication auth = SecurityContextHolder.getContext().getAuthentication();
if (auth == null || !auth.isAuthenticated()) {
    return ResponseEntity.badRequest().body(response);
}
String username = auth.getName();
Users user = usersRepository.findByUsername(username).orElse(null);
```

**BƯỚC 2: Kiểm tra OrderItem**
```java
OrderItems orderItem = orderItemsRepository
    .findByOrderItemIdAndUserId(orderItemId, user.getUserId())
    .orElse(null);
```
- Kiểm tra OrderItem tồn tại và thuộc về user

**BƯỚC 3: Kiểm tra đã review chưa**
```java
if (productReviewsRepository.existsByOrderItemId(orderItemId)) {
    return ResponseEntity.badRequest().body(response);
}
if (productReviewsRepository.existsByUserIdAndProductId(
    user.getUserId(), 
    orderItem.getProductId()
)) {
    return ResponseEntity.badRequest().body(response);
}
```

**BƯỚC 4: Upload Media Files**
```java
StringBuilder mediaUrls = new StringBuilder();
if (mediaFiles != null && mediaFiles.length > 0) {
    for (MultipartFile file : mediaFiles) {
        String fileName = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
        Path filePath = Paths.get(UPLOAD_DIR + fileName);
        Files.createDirectories(filePath.getParent());
        Files.write(filePath, file.getBytes());
        mediaUrls.append("/uploads/").append(fileName);
    }
}
```

**BƯỚC 5: Tạo ProductReviews**
```java
ProductReviews review = new ProductReviews();
review.setProductId(orderItem.getProductId());
review.setUserId(user.getUserId());
review.setOrderItemId(orderItemId);
review.setRating(productRating);
review.setComment(comment);
review.setMediaUrls(mediaUrls.toString());
review.setServiceRating(serviceRating);
review.setCreatedAt(LocalDateTime.now());
productReviewsRepository.save(review);
```

**BƯỚC 6: Cập nhật Average Rating**
```java
ratingCalculationService.updateProductAverageRating(orderItem.getProductId());
```

**BƯỚC 7: Gửi Notification**
```java
notificationService.createReviewNotification(
    user.getUserId(), 
    review.getReviewId(), 
    productName
);
```

#### 4.1.3. Response

**Success:**
```json
{
    "success": true,
    "message": "Rating submitted successfully"
}
```

**Error:**
```json
{
    "success": false,
    "message": "Error message"
}
```

---

## 5. SƠ ĐỒ LUỒNG DỮ LIỆU

```
┌─────────────────┐
│   User Request  │
│ GET /orderhistory│
└────────┬────────┘
         │
         ▼
┌─────────────────────────┐
│ CustomerDashboardController│
│   orderHistory()        │
└────────┬────────────────┘
         │
         ├─► Xác thực User
         │   └─► UsersRepository.findByUsername()
         │
         ├─► Tạo Pageable
         │
         ├─► Truy vấn Orders
         │   ├─► Có search?
         │   │   └─► OrdersRepository.findByUserIdAndSearchTerm()
         │   ├─► Có status?
         │   │   └─► OrdersRepository.findByUserIdAndStatusOrderByCreatedAtDesc()
         │   └─► Mặc định
         │       └─► OrdersRepository.findByUserIdOrderByCreatedAtDesc()
         │
         ├─► Lấy OrderItems
         │   └─► OrderItemsRepository.findByOrderId()
         │
         ├─► Lấy Products
         │   └─► ProductsRepository.findById()
         │
         ├─► Lấy Cart Count
         │   └─► ShoppingCartRepository.countByUserId()
         │
         └─► Return View
             └─► customer/orderhistory.html
```

---

## 6. CÁC TRƯỜNG HỢP XỬ LÝ ĐẶC BIỆT

### 6.1. User chưa đăng nhập
- **Xử lý:** Redirect về `/login`
- **Code:** `return "redirect:/login";`

### 6.2. User không tồn tại
- **Xử lý:** Redirect về `/login`
- **Code:** `return "redirect:/login";`

### 6.3. Không có đơn hàng
- **Xử lý:** Hiển thị empty state trong view
- **View:** `th:if="${orders == null or orders.empty}"`

### 6.4. Product không tồn tại
- **Xử lý:** Bỏ qua OrderItem đó (không hiển thị)
- **Code:** `ifPresent()` trong Optional

### 6.5. Lỗi khi lấy cartCount
- **Xử lý:** Bỏ qua, không hiển thị cartCount
- **Code:** `try-catch` với `ignored`

### 6.6. Exception tổng quát
- **Xử lý:** Redirect về dashboard với error parameter
- **Code:** `return "redirect:/customer/dashboard?error=orderhistory_error";`

---

## 7. PERFORMANCE CONSIDERATIONS

### 7.1. N+1 Query Problem
**Vấn đề:** 
- Với mỗi Order, truy vấn OrderItems riêng
- Với mỗi OrderItem, truy vấn Product riêng

**Giải pháp hiện tại:**
- Sử dụng Map để cache kết quả
- Tránh truy vấn lặp lại cho cùng một Product

**Cải thiện có thể:**
- Sử dụng `@EntityGraph` hoặc JOIN FETCH trong query
- Batch load Products trong một query duy nhất

### 7.2. Pagination
- Sử dụng Spring Data Pagination
- Chỉ load dữ liệu của trang hiện tại
- Giảm memory usage

---

## 8. SECURITY CONSIDERATIONS

### 8.1. Authentication
- Kiểm tra user đã đăng nhập
- Redirect về login nếu chưa đăng nhập

### 8.2. Authorization
- Chỉ hiển thị đơn hàng của user hiện tại
- Filter theo `userId` trong tất cả queries

### 8.3. SQL Injection
- Sử dụng Parameterized Queries
- Spring Data JPA tự động escape parameters

---

## 9. TESTING SCENARIOS

### 9.1. Test Cases

1. **User chưa đăng nhập**
   - Expected: Redirect về `/login`

2. **User có đơn hàng**
   - Expected: Hiển thị danh sách đơn hàng

3. **User không có đơn hàng**
   - Expected: Hiển thị empty state

4. **Search theo tên sản phẩm**
   - Expected: Chỉ hiển thị đơn hàng có sản phẩm khớp

5. **Filter theo status**
   - Expected: Chỉ hiển thị đơn hàng có status tương ứng

6. **Pagination**
   - Expected: Hiển thị đúng số lượng items mỗi trang

---

## 10. DEPENDENCIES

### 10.1. Spring Framework
- `spring-boot-starter-web`
- `spring-boot-starter-data-jpa`
- `spring-boot-starter-security`
- `spring-boot-starter-thymeleaf`

### 10.2. Database
- MySQL (qua HikariCP connection pool)

---

## 11. FILES LIÊN QUAN

### 11.1. Controller
- `src/main/java/banhangrong/su25/Controller/CustomerDashboardController.java`
- `src/main/java/banhangrong/su25/Controller/RatingController.java`

### 11.2. Repository
- `src/main/java/banhangrong/su25/Repository/OrdersRepository.java`
- `src/main/java/banhangrong/su25/Repository/OrderItemsRepository.java`
- `src/main/java/banhangrong/su25/Repository/ProductsRepository.java`
- `src/main/java/banhangrong/su25/Repository/UsersRepository.java`
- `src/main/java/banhangrong/su25/Repository/ShoppingCartRepository.java`

### 11.3. Entity
- `src/main/java/banhangrong/su25/Entity/Orders.java`
- `src/main/java/banhangrong/su25/Entity/OrderItems.java`
- `src/main/java/banhangrong/su25/Entity/Products.java`
- `src/main/java/banhangrong/su25/Entity/Users.java`

### 11.4. View
- `src/main/resources/templates/customer/orderhistory.html`

---

## 12. KẾT LUẬN

Luồng backend của Order History được thiết kế với các đặc điểm:

1. **Bảo mật:** Kiểm tra authentication và authorization
2. **Hiệu năng:** Sử dụng pagination và caching với Map
3. **Linh hoạt:** Hỗ trợ search và filter
4. **Dễ bảo trì:** Code rõ ràng, có exception handling

**Điểm cần cải thiện:**
- Tối ưu N+1 query problem
- Thêm caching layer cho Products
- Thêm unit tests và integration tests

---

**Tài liệu được tạo:** 2025-01-06
**Phiên bản:** 1.0

