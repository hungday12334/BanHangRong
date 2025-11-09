# View Seller - Luồng Dữ Liệu Chi Tiết

Tài liệu này mô tả chi tiết cách phần "View Seller" hoạt động từ khi người dùng click đến khi hiển thị thông tin trên giao diện.

---

## Trang, code và nơi dữ liệu chảy qua
- View chính: `src/main/resources/templates/customer/seller-profile.html`
- Controller: `CustomerDashboardController`
- Repositories: `UsersRepository`, `ProductsRepository`, `ProductImagesRepository`, `ProductReviewsRepository`, `ShoppingCartRepository`
- JavaScript: `src/main/resources/templates/customer/orderhistory.html` (hàm `viewSeller()`)
- URL Endpoint: `/customer/seller/{sellerId}`

---

## 📋 Tổng Quan

**Mục đích**: Cho phép khách hàng xem thông tin chi tiết về một seller, bao gồm:
- Thông tin seller (tên, avatar)
- Đánh giá trung bình (product rating, service rating)
- Số lượng sản phẩm đã bán
- Danh sách sản phẩm của seller
- Hình ảnh sản phẩm
- Đánh giá từ khách hàng

**URL Endpoint**: `/customer/seller/{sellerId}`

---

## 🔄 Luồng Hoạt Động Chi Tiết

### BƯỚC 1: Người Dùng Tương Tác (Frontend)

#### 1.1. Nơi xuất phát
Người dùng xem trang **Order History** (`/customer/orderhistory`), trong mỗi order card có nút "View seller":

```778:778:src/main/resources/templates/customer/orderhistory.html
                            <button class="action-btn view-seller-btn" th:attr="data-seller-id=${order.sellerId}">View seller</button>
```

**Giải thích**:
- Button có class `view-seller-btn` để JavaScript nhận diện
- Attribute `data-seller-id` chứa ID của seller (lấy từ `order.sellerId`)
- Khi render, Thymeleaf sẽ thay `${order.sellerId}` bằng giá trị thực tế (ví dụ: `data-seller-id="5"`)

#### 1.2. JavaScript xử lý click event

```1283:1294:src/main/resources/templates/customer/orderhistory.html
        // View Seller function
        function viewSeller(button) {
            const sellerId = button.getAttribute('data-seller-id');
            
            if (!sellerId) {
                showNotification('Error', 'Seller information not available', 'error');
                return;
            }
            
            // Redirect to seller profile page
            window.location.href = '/customer/seller/' + sellerId;
        }
```

**Giải thích**:
- Event listener được đăng ký ở dòng 1269-1281 để bắt click vào button có class `view-seller-btn`
- Hàm `viewSeller()` được gọi khi click
- Lấy `sellerId` từ attribute `data-seller-id`
- Kiểm tra nếu không có `sellerId` thì hiển thị lỗi
- Redirect browser đến URL: `/customer/seller/{sellerId}` (ví dụ: `/customer/seller/5`)

---

### BƯỚC 2: Backend Nhận Request (Controller)

#### 2.1. Controller method xử lý

```295:391:src/main/java/banhangrong/su25/Controller/CustomerDashboardController.java
    @GetMapping("/customer/seller/{sellerId}")
    public String viewSeller(@PathVariable Long sellerId, Model model) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            Users currentUser = null;
            if (auth != null && auth.isAuthenticated()) {
                String username = auth.getName();
                currentUser = usersRepository.findByUsername(username).orElse(null);
            }
            
            Optional<Users> sellerOptional = usersRepository.findById(sellerId);
            if (sellerOptional.isEmpty()) {
                return "redirect:/customer/dashboard?error=seller_not_found";
            }
            
            Users seller = sellerOptional.get();
            
            List<Products> products = productsRepository.findBySellerId(sellerId);
            
            Map<Long, String> productImages = new HashMap<>();
            for (Products product : products) {
                try {
                    var primary = productImagesRepository.findTop1ByProductIdAndIsPrimaryTrueOrderByImageIdAsc(product.getProductId());
                    if (primary != null && !primary.isEmpty()) {
                        productImages.put(product.getProductId(), primary.get(0).getImageUrl());
                    } else {
                        var any = productImagesRepository.findTop1ByProductIdOrderByImageIdAsc(product.getProductId());
                        if (any != null && !any.isEmpty()) {
                            productImages.put(product.getProductId(), any.get(0).getImageUrl());
                        }
                    }
                } catch (Exception ignored) {}
            }
            
            Long totalProducts = (long) products.size();
            Long totalSales = productsRepository.totalUnitsSoldBySeller(sellerId);
            
            List<ProductReviews> reviews = new java.util.ArrayList<>();
            for (Products product : products) {
                List<ProductReviews> productReviews = productReviewsRepository.findByProductIdOrderByCreatedAtDesc(product.getProductId());
                for (ProductReviews review : productReviews) {
                    usersRepository.findById(review.getUserId()).ifPresent(user -> {
                        review.setUsername(user.getUsername());
                    });
                }
                reviews.addAll(productReviews);
            }
            Long totalReviews = (long) reviews.size();
            
            BigDecimal averageProductRating = BigDecimal.ZERO;
            BigDecimal averageServiceRating = BigDecimal.ZERO;
            
            if (!reviews.isEmpty()) {
                double productSum = reviews.stream()
                    .filter(r -> r.getRating() != null)
                    .mapToInt(ProductReviews::getRating)
                    .average()
                    .orElse(0.0);
                averageProductRating = BigDecimal.valueOf(productSum);
                
                double serviceSum = reviews.stream()
                    .filter(r -> r.getServiceRating() != null)
                    .mapToInt(ProductReviews::getServiceRating)
                    .average()
                    .orElse(0.0);
                averageServiceRating = BigDecimal.valueOf(serviceSum);
            }
            
            reviews.sort((r1, r2) -> r2.getCreatedAt().compareTo(r1.getCreatedAt()));
            if (reviews.size() > 10) {
                reviews = reviews.subList(0, 10);
            }
            
            model.addAttribute("seller", seller);
            model.addAttribute("products", products);
            model.addAttribute("productImages", productImages);
            model.addAttribute("totalProducts", totalProducts);
            model.addAttribute("totalSales", totalSales);
            model.addAttribute("averageProductRating", averageProductRating);
            model.addAttribute("averageServiceRating", averageServiceRating);
            model.addAttribute("totalReviews", totalReviews);
            model.addAttribute("reviews", reviews);
            
            if (currentUser != null) {
                model.addAttribute("user", currentUser);
                try {
                    model.addAttribute("cartCount", shoppingCartRepository.countByUserId(currentUser.getUserId()));
                } catch (Exception ignored) {}
            }
            
            return "customer/seller-profile";
            
        } catch (Exception e) {
            e.printStackTrace();
            return "redirect:/customer/dashboard?error=seller_view_error";
        }
    }
```

**Giải thích từng bước**:

1. **Nhận sellerId từ URL**: `@PathVariable Long sellerId` - Spring tự động lấy giá trị từ URL path
2. **Xác thực người dùng hiện tại**: Lấy thông tin user đang đăng nhập (nếu có) để hiển thị cart count
3. **Kiểm tra seller tồn tại**: Nếu không tìm thấy seller thì redirect về dashboard với lỗi
4. **Thu thập dữ liệu**: Gọi các repository methods để lấy dữ liệu từ database
5. **Xử lý và tính toán**: Tính toán rating trung bình, lọc reviews
6. **Đưa dữ liệu vào Model**: Thêm tất cả dữ liệu vào Spring Model để truyền sang view
7. **Trả về template**: Return tên template `"customer/seller-profile"` để Spring render

---

### BƯỚC 3: Truy Vấn Database (Repository Layer)

#### 3.1. Lấy thông tin Seller

**Repository**: `UsersRepository`

**Method được gọi**:
```java
Optional<Users> sellerOptional = usersRepository.findById(sellerId);
```

**SQL Query thực tế** (JPA tự động generate):
```sql
SELECT * FROM users WHERE user_id = ?
```

**Kết quả**: Trả về object `Users` chứa thông tin seller (username, email, avatar, v.v.)

---

#### 3.2. Lấy danh sách sản phẩm của Seller

**Repository**: `ProductsRepository`

**Method được gọi**:
```java
List<Products> products = productsRepository.findBySellerId(sellerId);
```

**Method definition**:
```35:35:src/main/java/banhangrong/su25/Repository/ProductsRepository.java
    List<Products> findBySellerId(Long sellerId);
```

**SQL Query thực tế** (JPA tự động generate):
```sql
SELECT * FROM products WHERE seller_id = ?
```

**Kết quả**: Trả về danh sách tất cả sản phẩm của seller (không phân biệt status)

---

#### 3.3. Lấy hình ảnh sản phẩm

**Repository**: `ProductImagesRepository`

**Method được gọi** (trong vòng lặp cho từng product):
```java
// Ưu tiên lấy ảnh primary
var primary = productImagesRepository.findTop1ByProductIdAndIsPrimaryTrueOrderByImageIdAsc(product.getProductId());

// Nếu không có primary, lấy ảnh bất kỳ
var any = productImagesRepository.findTop1ByProductIdOrderByImageIdAsc(product.getProductId());
```

**Method definitions**:
```13:14:src/main/java/banhangrong/su25/Repository/ProductImagesRepository.java
    List<ProductImages> findTop1ByProductIdAndIsPrimaryTrueOrderByImageIdAsc(Long productId);
    List<ProductImages> findTop1ByProductIdOrderByImageIdAsc(Long productId);
```

**SQL Query thực tế**:
```sql
-- Query 1: Lấy ảnh primary
SELECT * FROM product_images 
WHERE product_id = ? AND is_primary = true 
ORDER BY image_id ASC 
LIMIT 1;

-- Query 2: Lấy ảnh bất kỳ (nếu không có primary)
SELECT * FROM product_images 
WHERE product_id = ? 
ORDER BY image_id ASC 
LIMIT 1;
```

**Kết quả**: 
- Tạo Map `productImages` với key = `productId`, value = `imageUrl`
- Mỗi product có 1 ảnh đại diện (ưu tiên primary, nếu không có thì lấy ảnh đầu tiên)

---

#### 3.4. Tính tổng số sản phẩm đã bán

**Repository**: `ProductsRepository`

**Method được gọi**:
```java
Long totalSales = productsRepository.totalUnitsSoldBySeller(sellerId);
```

**Method definition**:
```49:50:src/main/java/banhangrong/su25/Repository/ProductsRepository.java
        @Query(value = "SELECT COALESCE(SUM(oi.quantity),0) FROM order_items oi JOIN products p ON p.product_id = oi.product_id JOIN orders o ON o.order_id = oi.order_id WHERE p.seller_id = :sellerId AND UPPER(o.status) = 'COMPLETED'", nativeQuery = true)
    Long totalUnitsSoldBySeller(@Param("sellerId") Long sellerId);
```

**SQL Query thực tế**:
```sql
SELECT COALESCE(SUM(oi.quantity), 0) 
FROM order_items oi 
JOIN products p ON p.product_id = oi.product_id 
JOIN orders o ON o.order_id = oi.order_id 
WHERE p.seller_id = ? 
  AND UPPER(o.status) = 'COMPLETED'
```

**Giải thích**:
- JOIN 3 bảng: `order_items`, `products`, `orders`
- Chỉ tính các order có status = 'COMPLETED'
- SUM tổng số lượng (`quantity`) của tất cả order_items
- `COALESCE` trả về 0 nếu không có kết quả

**Kết quả**: Số lượng sản phẩm đã bán thành công (ví dụ: 150)

---

#### 3.5. Lấy đánh giá (Reviews)

**Repository**: `ProductReviewsRepository`

**Method được gọi** (trong vòng lặp cho từng product):
```java
List<ProductReviews> productReviews = productReviewsRepository.findByProductIdOrderByCreatedAtDesc(product.getProductId());
```

**Method definition**:
```14:14:src/main/java/banhangrong/su25/Repository/ProductReviewsRepository.java
    List<ProductReviews> findByProductIdOrderByCreatedAtDesc(Long productId);
```

**SQL Query thực tế**:
```sql
SELECT * FROM product_reviews 
WHERE product_id = ? 
ORDER BY created_at DESC
```

**Sau đó lấy username của người đánh giá**:
```java
usersRepository.findById(review.getUserId()).ifPresent(user -> {
    review.setUsername(user.getUsername());
});
```

**SQL Query**:
```sql
SELECT * FROM users WHERE user_id = ?
```

**Kết quả**: 
- Tất cả reviews của tất cả sản phẩm của seller
- Mỗi review có thêm thông tin username của người đánh giá

---

#### 3.6. Tính rating trung bình

**Xử lý trong Controller** (không query database, tính toán từ dữ liệu đã có):

```java
// Tính Product Rating trung bình
double productSum = reviews.stream()
    .filter(r -> r.getRating() != null)
    .mapToInt(ProductReviews::getRating)
    .average()
    .orElse(0.0);
averageProductRating = BigDecimal.valueOf(productSum);

// Tính Service Rating trung bình
double serviceSum = reviews.stream()
    .filter(r -> r.getServiceRating() != null)
    .mapToInt(ProductReviews::getServiceRating)
    .average()
    .orElse(0.0);
averageServiceRating = BigDecimal.valueOf(serviceSum);
```

**Giải thích**:
- Lọc reviews có rating không null
- Tính trung bình bằng Java Stream API
- Làm tương tự cho service rating
- Nếu không có review thì trả về 0.0

**Kết quả**: 
- `averageProductRating`: Đánh giá trung bình về sản phẩm (1-5)
- `averageServiceRating`: Đánh giá trung bình về dịch vụ (1-5)

---

#### 3.7. Lấy số lượng sản phẩm trong giỏ hàng (nếu user đã đăng nhập)

**Repository**: `ShoppingCartRepository`

**Method được gọi**:
```java
model.addAttribute("cartCount", shoppingCartRepository.countByUserId(currentUser.getUserId()));
```

**SQL Query** (JPA tự động generate):
```sql
SELECT COUNT(*) FROM shopping_cart WHERE user_id = ?
```

**Kết quả**: Số lượng sản phẩm trong giỏ hàng của user hiện tại

---

### BƯỚC 4: Render Giao Diện (Thymeleaf Template)

#### 4.1. Template file

**File**: `src/main/resources/templates/customer/seller-profile.html`

#### 4.2. Hiển thị thông tin Seller

```319:322:src/main/resources/templates/customer/seller-profile.html
					<div class="seller-profile">
						<div class="seller-avatar" th:text="${seller != null ? seller.username.substring(0,1).toUpperCase() : 'S'}">S</div>
						<h1 class="seller-name" th:text="${seller != null ? seller.username : 'Seller name'}">Seller name</h1>
					</div>
```

**Giải thích**:
- Avatar: Hiển thị chữ cái đầu của username (uppercase)
- Tên: Hiển thị username của seller
- Sử dụng Thymeleaf expression `${seller.username}` để lấy dữ liệu từ Model

---

#### 4.3. Hiển thị Rating

```326:351:src/main/resources/templates/customer/seller-profile.html
						<div class="rating-item">
							<div class="rating-label">Product Average Rating</div>
							<div class="rating-stars">
								<span class="stars" th:if="${averageProductRating != null and averageProductRating > 0}">
									<th:block th:each="i : ${#numbers.sequence(1, 5)}">
										<span th:if="${i <= averageProductRating}">★</span>
										<span th:unless="${i <= averageProductRating}" style="color: #d1d5db;">★</span>
									</th:block>
								</span>
								<span class="stars" th:unless="${averageProductRating != null and averageProductRating > 0}" style="color: #d1d5db;">★★★★★</span>
								<span class="rating-value" th:text="${averageProductRating != null ? #numbers.formatDecimal(averageProductRating, 1, 'COMMA', 1, 'POINT') : '0.0'}">0.0</span>
							</div>
						</div>
						<div class="rating-item">
							<div class="rating-label">Service Average Rating</div>
							<div class="rating-stars">
								<span class="stars" th:if="${averageServiceRating != null and averageServiceRating > 0}">
									<th:block th:each="i : ${#numbers.sequence(1, 5)}">
										<span th:if="${i <= averageServiceRating}">★</span>
										<span th:unless="${i <= averageServiceRating}" style="color: #d1d5db;">★</span>
									</th:block>
								</span>
								<span class="stars" th:unless="${averageServiceRating != null and averageServiceRating > 0}" style="color: #d1d5db;">★★★★★</span>
								<span class="rating-value" th:text="${averageServiceRating != null ? #numbers.formatDecimal(averageServiceRating, 1, 'COMMA', 1, 'POINT') : '0.0'}">0.0</span>
							</div>
						</div>
```

**Giải thích**:
- Tạo 5 ngôi sao, tô màu vàng cho số sao <= rating, xám cho phần còn lại
- Hiển thị số rating dạng decimal (ví dụ: 4.5)
- Làm tương tự cho cả Product Rating và Service Rating

---

#### 4.4. Hiển thị số lượng đã bán

```355:359:src/main/resources/templates/customer/seller-profile.html
					<div class="seller-sales">
						<div class="sales-label">sold</div>
						<div class="sales-value" th:text="${totalSales != null ? totalSales : '0'}">100</div>
						<div class="sales-unit">product</div>
					</div>
```

**Giải thích**: Hiển thị `totalSales` từ Model (số lượng sản phẩm đã bán)

---

#### 4.5. Hiển thị danh sách sản phẩm

```371:389:src/main/resources/templates/customer/seller-profile.html
			<div class="products-grid" th:if="${products != null and !products.empty}">
				<a class="product-card" th:each="product : ${products}" th:href="@{'/product/' + ${product.productId}}">
					<div class="product-image">
						<img th:if="${productImages != null and productImages[product.productId] != null}" 
						     th:src="${productImages[product.productId]}" 
						     th:alt="${product.name}"
						     onerror="this.style.display='none'; this.parentElement.innerHTML='📦';">
						<span th:unless="${productImages != null and productImages[product.productId] != null}">📦</span>
					</div>
					<div class="product-info">
						<div class="product-name" th:text="${product.name}">Microsoft Office 2021</div>
						<div class="product-price" th:text="${#numbers.formatDecimal(product.price, 0, 'COMMA', 0, 'POINT')} + '₫'">1,250,000₫</div>
						<div class="product-meta">
							<span>⭐ <span th:text="${product.averageRating != null ? #numbers.formatDecimal(product.averageRating, 1, 'COMMA', 1, 'POINT') : '0.0'}">4.5</span></span>
							<span>Sold: <span th:text="${product.totalSales != null ? product.totalSales : 0}">0</span></span>
						</div>
					</div>
				</a>
			</div>
```

**Giải thích**:
- Dùng `th:each` để lặp qua danh sách `products`
- Mỗi product card hiển thị:
  - Hình ảnh: Lấy từ Map `productImages[product.productId]`
  - Tên sản phẩm: `product.name`
  - Giá: Format số với dấu phẩy, thêm ký hiệu ₫
  - Rating và số lượng đã bán
- Click vào card sẽ điều hướng đến trang chi tiết sản phẩm

---

## 📊 Sơ Đồ Luồng Dữ Liệu

```
┌─────────────────────────────────────────────────────────────┐
│ 1. USER CLICKS "View Seller" BUTTON                         │
│    Location: orderhistory.html                              │
│    Action: JavaScript redirects to /customer/seller/{id}     │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────┐
│ 2. SPRING MVC ROUTING                                       │
│    @GetMapping("/customer/seller/{sellerId}")               │
│    Controller: CustomerDashboardController.viewSeller()    │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────┐
│ 3. DATABASE QUERIES (Repository Layer)                        │
│                                                             │
│    ┌─────────────────────────────────────────┐             │
│    │ UsersRepository.findById(sellerId)      │             │
│    │ → SELECT * FROM users WHERE user_id = ?  │             │
│    └─────────────────────────────────────────┘             │
│                                                             │
│    ┌─────────────────────────────────────────┐             │
│    │ ProductsRepository.findBySellerId()     │             │
│    │ → SELECT * FROM products WHERE seller_id│             │
│    └─────────────────────────────────────────┘             │
│                                                             │
│    ┌─────────────────────────────────────────┐             │
│    │ ProductImagesRepository (for each prod) │             │
│    │ → SELECT * FROM product_images WHERE... │             │
│    └─────────────────────────────────────────┘             │
│                                                             │
│    ┌─────────────────────────────────────────┐             │
│    │ ProductsRepository.totalUnitsSoldBySeller()│          │
│    │ → SELECT SUM(quantity) FROM order_items...│            │
│    └─────────────────────────────────────────┘             │
│                                                             │
│    ┌─────────────────────────────────────────┐             │
│    │ ProductReviewsRepository (for each prod)│             │
│    │ → SELECT * FROM product_reviews WHERE...│             │
│    └─────────────────────────────────────────┘             │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────┐
│ 4. DATA PROCESSING (Controller)                             │
│    - Calculate average ratings                              │
│    - Sort and limit reviews (top 10)                       │
│    - Build productImages Map                                │
│    - Add all data to Spring Model                          │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────┐
│ 5. THYMELEAF TEMPLATE RENDERING                            │
│    Template: customer/seller-profile.html                  │
│    - Render seller info                                     │
│    - Render ratings                                         │
│    - Render product grid                                    │
│    - Render reviews (if any)                                 │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────┐
│ 6. HTML RESPONSE SENT TO BROWSER                            │
│    User sees seller profile page                            │
└─────────────────────────────────────────────────────────────┘
```

---

## 🔍 Chi Tiết Các Truy Vấn Database

### Query 1: Lấy thông tin Seller
```sql
SELECT * FROM users WHERE user_id = ?
```
**Bảng**: `users`  
**Điều kiện**: `user_id = sellerId`  
**Kết quả**: 1 row (thông tin seller)

---

### Query 2: Lấy danh sách sản phẩm
```sql
SELECT * FROM products WHERE seller_id = ?
```
**Bảng**: `products`  
**Điều kiện**: `seller_id = sellerId`  
**Kết quả**: N rows (tất cả sản phẩm của seller)

---

### Query 3: Lấy hình ảnh sản phẩm (cho mỗi product)
```sql
-- Ưu tiên ảnh primary
SELECT * FROM product_images 
WHERE product_id = ? AND is_primary = true 
ORDER BY image_id ASC 
LIMIT 1;

-- Nếu không có primary, lấy ảnh đầu tiên
SELECT * FROM product_images 
WHERE product_id = ? 
ORDER BY image_id ASC 
LIMIT 1;
```
**Bảng**: `product_images`  
**Điều kiện**: `product_id = productId`  
**Kết quả**: 1 row per product (URL của ảnh)

---

### Query 4: Tính tổng số lượng đã bán
```sql
SELECT COALESCE(SUM(oi.quantity), 0) 
FROM order_items oi 
JOIN products p ON p.product_id = oi.product_id 
JOIN orders o ON o.order_id = oi.order_id 
WHERE p.seller_id = ? 
  AND UPPER(o.status) = 'COMPLETED'
```
**Bảng**: `order_items`, `products`, `orders`  
**JOIN**: 
- `order_items` ↔ `products` (qua `product_id`)
- `order_items` ↔ `orders` (qua `order_id`)
**Điều kiện**: 
- `seller_id = sellerId`
- `status = 'COMPLETED'`
**Kết quả**: 1 số (tổng quantity)

---

### Query 5: Lấy đánh giá (cho mỗi product)
```sql
SELECT * FROM product_reviews 
WHERE product_id = ? 
ORDER BY created_at DESC
```
**Bảng**: `product_reviews`  
**Điều kiện**: `product_id = productId`  
**Kết quả**: N rows (tất cả reviews của product)

---

### Query 6: Lấy username của người đánh giá (cho mỗi review)
```sql
SELECT * FROM users WHERE user_id = ?
```
**Bảng**: `users`  
**Điều kiện**: `user_id = review.userId`  
**Kết quả**: 1 row (username của reviewer)

---

## 📝 Tóm Tắt

### Dữ liệu được truyền vào Model:
1. `seller` - Thông tin seller (Users object)
2. `products` - Danh sách sản phẩm (List<Products>)
3. `productImages` - Map<productId, imageUrl>
4. `totalProducts` - Tổng số sản phẩm (Long)
5. `totalSales` - Tổng số lượng đã bán (Long)
6. `averageProductRating` - Rating trung bình sản phẩm (BigDecimal)
7. `averageServiceRating` - Rating trung bình dịch vụ (BigDecimal)
8. `totalReviews` - Tổng số đánh giá (Long)
9. `reviews` - Danh sách đánh giá (List<ProductReviews>, tối đa 10)
10. `user` - User hiện tại (nếu đã đăng nhập)
11. `cartCount` - Số sản phẩm trong giỏ hàng (nếu đã đăng nhập)

### Số lượng truy vấn database:
- **1 query** để lấy seller
- **1 query** để lấy danh sách products
- **N queries** để lấy hình ảnh (N = số products)
- **1 query** để tính totalSales
- **N queries** để lấy reviews (N = số products)
- **M queries** để lấy username của reviewers (M = số reviews)
- **1 query** để lấy cartCount (nếu user đã đăng nhập)

**Tổng cộng**: Khoảng **2N + M + 3-4 queries** (có thể tối ưu bằng batch queries hoặc JOIN)

---

## ⚠️ Lưu Ý

1. **Performance**: Hiện tại có N+1 query problem (lặp qua products để lấy images và reviews). Có thể tối ưu bằng:
   - Batch queries
   - JOIN queries
   - Fetch join trong JPA

2. **Error Handling**: Controller có try-catch để xử lý lỗi, redirect về dashboard nếu có lỗi

3. **Security**: Không có kiểm tra quyền truy cập - bất kỳ ai cũng có thể xem seller profile (có thể là tính năng mong muốn)

4. **Data Validation**: Kiểm tra seller tồn tại, nhưng không kiểm tra seller có phải là SELLER type không

