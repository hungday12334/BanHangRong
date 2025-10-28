# Tính năng View Seller từ Order History

## Mô tả
Đã thêm chức năng xem thông tin người bán (seller) từ trang lịch sử đơn hàng (order history).

## Các thay đổi đã thực hiện

### 1. Backend - Controller
**File:** `src/main/java/banhangrong/su25/Controller/CustomerDashboardController.java`

Đã thêm endpoint mới:
```java
@GetMapping("/customer/seller/{sellerId}")
public String viewSeller(@PathVariable Long sellerId, Model model)
```

Endpoint này:
- Lấy thông tin seller từ database
- Lấy danh sách sản phẩm của seller
- Lấy hình ảnh của các sản phẩm
- Tính toán thống kê: tổng sản phẩm, tổng đơn hàng, đánh giá trung bình
- Lấy danh sách đánh giá cho các sản phẩm của seller
- Hiển thị trang `customer/seller-profile.html`

### 2. Entity
**File:** `src/main/java/banhangrong/su25/Entity/ProductReviews.java`

Đã thêm trường transient `username` để hiển thị tên người đánh giá:
```java
@Transient
private String username;
```

### 3. Frontend - Order History HTML
**File:** `src/main/resources/templates/customer/orderhistory.html`

- Đã thêm thuộc tính `data-seller-id` vào nút "View seller"
- Đã thêm hàm JavaScript `viewSeller()` để xử lý click event
- Khi click vào "View seller", sẽ chuyển hướng đến `/customer/seller/{sellerId}`

### 4. Frontend - Seller Profile Template
**File:** `src/main/resources/templates/customer/seller-profile.html`

Đã cập nhật template để sử dụng `seller.username` thay vì `seller.shopName` (vì entity Users không có field shopName).

## Cách test chức năng

### Bước 1: Khởi động ứng dụng
```bash
cd d:\SWP\BanHangRong
.\mvnw.cmd spring-boot:run
```

### Bước 2: Đăng nhập vào hệ thống
- Truy cập: http://localhost:8080/login
- Đăng nhập với tài khoản customer

### Bước 3: Vào trang Order History
- Từ dashboard, click vào "My Orders" hoặc truy cập: http://localhost:8080/orderhistory

### Bước 4: Click "View seller"
- Trong mỗi order card, bạn sẽ thấy nút "View seller" màu đỏ
- Click vào nút này

### Bước 5: Kiểm tra trang Seller Profile
Trang seller profile sẽ hiển thị:
- ✅ Thông tin seller (tên, avatar)
- ✅ **Product Average Rating**: Đánh giá trung bình về chất lượng sản phẩm (từ field `rating` trong reviews)
- ✅ **Service Average Rating**: Đánh giá trung bình về dịch vụ seller (từ field `service_rating` trong reviews)
- ✅ **Đã bán**: Tổng số sản phẩm đã bán (hiển thị số lớn)
- ✅ Danh sách sản phẩm của seller với hình ảnh
- ✅ Danh sách đánh giá từ khách hàng (10 reviews mới nhất)
- ✅ Các nút action: Chat, Follow (sẽ được phát triển sau)

## Cấu trúc URL
```
/customer/seller/{sellerId}
```
Ví dụ: `/customer/seller/1`

## Các tính năng đang hoạt động
✅ Hiển thị thông tin seller với layout 3 cột
✅ **Product Rating**: Tính trung bình từ field `rating` trong `product_reviews`
✅ **Service Rating**: Tính trung bình từ field `service_rating` trong `product_reviews`
✅ Hiển thị số sao (⭐) tương ứng với rating (1-5 sao)
✅ Hiển thị số lượng đã bán (totalSales) lấy từ database
✅ Hiển thị danh sách sản phẩm của seller với hình ảnh
✅ Hiển thị 10 reviews mới nhất với tên người đánh giá
✅ Navigation từ order history đến seller profile
✅ Responsive design (desktop 3 cột, mobile stack)

## Các tính năng sẽ phát triển sau
⏳ Chat với seller (hiện tại hiển thị alert "Coming soon")
⏳ Follow shop (hiện tại hiển thị alert "Coming soon")

## Chi tiết kỹ thuật

### Rating Calculation
```java
// Product Rating: Trung bình từ field 'rating'
averageProductRating = reviews.stream()
    .filter(r -> r.getRating() != null)
    .mapToInt(ProductReviews::getRating)
    .average()

// Service Rating: Trung bình từ field 'service_rating'  
averageServiceRating = reviews.stream()
    .filter(r -> r.getServiceRating() != null)
    .mapToInt(ProductReviews::getServiceRating)
    .average()
```

### Database Fields
- `product_reviews.rating` → Product Rating
- `product_reviews.service_rating` → Service Rating
- Cả 2 field này được lưu khi khách hàng đánh giá sản phẩm

## Lưu ý
- Seller phải có trong database
- Order phải có sellerId hợp lệ
- Nếu seller không tồn tại, sẽ redirect về dashboard với error message
- Rating sẽ hiển thị 0.0 nếu chưa có review nào
- Sao hiển thị màu vàng (⭐) cho phần có điểm, màu xám cho phần không có

