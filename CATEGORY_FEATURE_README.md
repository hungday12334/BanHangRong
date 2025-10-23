# Tính năng Category cho Customer Dashboard

## Tổng quan
Tính năng category cho phép khách hàng duyệt và tìm kiếm sản phẩm theo danh mục. Hệ thống hỗ trợ:

- Xem danh sách tất cả categories
- Xem sản phẩm trong từng category
- Tìm kiếm sản phẩm trong category
- Phân trang cho danh sách sản phẩm

## Các file đã tạo/cập nhật

### 1. Repository Layer
- **CategoriesRepository.java**: Repository để truy vấn categories
- **ProductsRepository.java**: Thêm methods để query products theo category

### 2. Controller Layer
- **CategoryController.java**: Controller xử lý các request liên quan đến category

### 3. Entity Layer
- **Categories.java**: Thêm relationship với Products
- **Products.java**: Thêm relationship với Categories

### 4. Templates
- **categories.html**: Trang hiển thị danh sách categories
- **category-products.html**: Trang hiển thị sản phẩm trong một category

### 5. Database
- **add_categories_data.sql**: Script thêm dữ liệu mẫu cho categories

## Các endpoint

### 1. `/categories`
- **Method**: GET
- **Mô tả**: Hiển thị trang danh sách tất cả categories
- **Template**: `customer/categories.html`

### 2. `/category/{categoryId}`
- **Method**: GET
- **Mô tả**: Hiển thị sản phẩm trong category cụ thể
- **Parameters**:
  - `page`: Số trang (default: 0)
  - `size`: Số sản phẩm mỗi trang (default: 15)
  - `search`: Từ khóa tìm kiếm
- **Template**: `customer/category-products.html`

## Cách sử dụng

### 1. Thêm dữ liệu categories
Chạy script SQL để thêm dữ liệu mẫu:
```sql
-- Chạy file add_categories_data.sql
```

### 2. Gán category cho sản phẩm
```sql
-- Ví dụ: gán category "Software Licenses" cho sản phẩm Office
INSERT INTO categories_products (category_id, product_id) 
SELECT c.category_id, p.product_id 
FROM categories c, products p 
WHERE c.name = 'Software Licenses' AND p.name LIKE '%Office%';
```

### 3. Truy cập tính năng
1. Đăng nhập với tài khoản CUSTOMER
2. Click vào "Category" trong navigation bar
3. Chọn category muốn xem
4. Sử dụng tìm kiếm nếu cần

## Tính năng chính

### 1. Trang Categories (`/categories`)
- Hiển thị tất cả categories có sản phẩm public
- Hiển thị số lượng sản phẩm trong mỗi category
- Responsive design với grid layout

### 2. Trang Category Products (`/category/{id}`)
- Hiển thị sản phẩm trong category được chọn
- Hỗ trợ tìm kiếm trong category
- Phân trang với navigation
- Sắp xếp theo totalSales và createdAt

### 3. Navigation
- Link "Category" trong dashboard đã được cập nhật
- Breadcrumb navigation
- Back to categories link

## Cấu trúc Database

### Bảng categories
```sql
CREATE TABLE categories (
    category_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

### Bảng categories_products (Many-to-Many)
```sql
CREATE TABLE categories_products (
    category_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (category_id, product_id),
    FOREIGN KEY (category_id) REFERENCES categories(category_id),
    FOREIGN KEY (product_id) REFERENCES products(product_id)
);
```

## Lưu ý kỹ thuật

1. **Lazy Loading**: Sử dụng FetchType.LAZY để tối ưu performance
2. **Pagination**: Hỗ trợ phân trang với PageRequest
3. **Search**: Tìm kiếm case-insensitive trong name và description
4. **Security**: Kiểm tra email verification cho CUSTOMER
5. **Error Handling**: Graceful fallback khi không có dữ liệu

## Troubleshooting

### 1. Không hiển thị categories
- Kiểm tra dữ liệu trong bảng categories
- Đảm bảo có sản phẩm public được gán category

### 2. Lỗi relationship
- Kiểm tra foreign key constraints
- Đảm bảo categories_products table có dữ liệu

### 3. Performance issues
- Sử dụng pagination
- Kiểm tra database indexes
- Monitor query execution time
