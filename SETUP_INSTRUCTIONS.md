# Hướng dẫn Setup và Chạy License Management Feature

## 📋 Checklist trước khi chạy

### 1. Kiểm tra files đã tạo

Chạy lệnh sau để kiểm tra:
```bash
ls -la src/main/java/banhangrong/su25/Entity/CategoryLicenses.java
ls -la src/main/java/banhangrong/su25/Repository/CategoryLicensesRepository.java
ls -la src/main/java/banhangrong/su25/DTO/LicenseDTO.java
ls -la src/main/java/banhangrong/su25/service/LicenseManagementService.java
ls -la sql/create_category_licenses_table.sql
```

Tất cả files này phải tồn tại.

### 2. Kiểm tra files đã sửa

```bash
# Kiểm tra SellerCategoryController có API endpoints mới
grep -n "api/licenses" src/main/java/banhangrong/su25/Controller/SellerCategoryController.java

# Kiểm tra category-management.html có license functions
grep -n "function openLicenseManager" src/main/resources/templates/seller/category-management.html

# Kiểm tra CSS có styles mới
grep -n "assigned-licenses-section" src/main/resources/static/css/category-management.css
```

## 🗄️ Bước 1: Setup Database

### Option A: MySQL đang chạy
```bash
# Tạo bảng category_licenses
mysql -u root -p banhangrong_db < sql/create_category_licenses_table.sql

# Hoặc nếu bạn dùng user khác
mysql -u [username] -p [database_name] < sql/create_category_licenses_table.sql
```

### Option B: Kiểm tra bảng đã tồn tại chưa
```bash
mysql -u root -p banhangrong_db -e "SHOW TABLES LIKE 'category_licenses';"
```

Nếu bảng đã tồn tại, output sẽ hiển thị:
```
+-----------------------------------------+
| Tables_in_banhangrong_db (category_licenses) |
+-----------------------------------------+
| category_licenses                            |
+-----------------------------------------+
```

### Option C: Xem cấu trúc bảng
```bash
mysql -u root -p banhangrong_db -e "DESCRIBE category_licenses;"
```

Expected output:
```
+----------------------+------------+------+-----+---------+----------------+
| Field                | Type       | Null | Key | Default | Extra          |
+----------------------+------------+------+-----+---------+----------------+
| category_license_id  | bigint     | NO   | PRI | NULL    | auto_increment |
| category_id          | bigint     | NO   | MUL | NULL    |                |
| shop_license_id      | bigint     | NO   | MUL | NULL    |                |
| seller_id            | bigint     | NO   | MUL | NULL    |                |
| created_at           | timestamp  | YES  |     | NULL    |                |
+----------------------+------------+------+-----+---------+----------------+
```

## 🏗️ Bước 2: Compile Project

```bash
# Clean và compile
./mvnw clean compile

# Hoặc nếu muốn skip tests
./mvnw clean compile -DskipTests
```

Kiểm tra không có lỗi compile. Nếu có lỗi, đọc message và fix.

## 🚀 Bước 3: Chạy Application

### Cách 1: Chạy với Maven
```bash
./mvnw spring-boot:run
```

### Cách 2: Build JAR và chạy
```bash
./mvnw clean package -DskipTests
java -jar target/banhangrong-*.jar
```

### Cách 3: Chạy từ IDE
- Mở project trong IntelliJ IDEA / Eclipse
- Tìm file `Su25Application.java`
- Right-click → Run

## 🧪 Bước 4: Test Tính Năng

### 4.1. Đăng nhập
1. Mở browser: http://localhost:8080
2. Đăng nhập với tài khoản seller
3. Nếu chưa có tài khoản seller, tạo mới hoặc promote user hiện tại:
   ```sql
   UPDATE users SET user_type = 'seller' WHERE user_id = [your_user_id];
   ```

### 4.2. Truy cập Category Management
1. Vào URL: http://localhost:8080/seller/categories
2. Hoặc từ seller dashboard → Click "Quản lý Danh mục"

### 4.3. Test License Management

#### Test 1: Mở License Manager (Global)
1. Click nút "Quản lý Licenses" ở header (bên cạnh Export CSV)
2. Modal "Quản lý Licenses / Giấy phép" sẽ hiện ra
3. Kiểm tra:
   - ✅ Form "Thêm License mới" hiển thị
   - ✅ Section "Danh sách Licenses hiện có" hiển thị
   - ✅ Nếu chưa có license: hiển thị "Chưa có license nào"

#### Test 2: Tạo License Mới
1. Trong modal, điền form:
   - Tên License: "Thực phẩm chức năng"
   - Mã (Code): "FOOD_SUPPLEMENT"
   - Mô tả: "Giấy phép cho sản phẩm thực phẩm chức năng"
2. Click "Thêm License"
3. Kiểm tra:
   - ✅ Hiển thị notification "✅ Đã thêm license thành công!"
   - ✅ License mới xuất hiện trong "Danh sách Licenses hiện có"
   - ✅ Form được clear
   - ✅ Badge count cập nhật: "(1)"

#### Test 3: Tạo thêm licenses
Tạo thêm 2-3 licenses để test:
- "Mỹ phẩm" / "COSMETIC"
- "Thiết bị điện tử" / "ELECTRONICS"
- "Dược phẩm" / "PHARMACEUTICAL"

#### Test 4: Mở License Manager cho Category cụ thể
1. Đóng modal license
2. Trong bảng danh sách categories, tìm nút license icon (🎫)
3. Click vào nút license của một category
4. Kiểm tra:
   - ✅ Modal title hiển thị tên category: "Quản lý Licenses - [Tên Category]"
   - ✅ Danh sách licenses hiển thị
   - ✅ Mỗi license có nút "Thêm" (màu xanh)

#### Test 5: Gán License vào Category
1. Trong modal của category, click nút "Thêm" bên cạnh một license
2. Kiểm tra:
   - ✅ Notification "✅ Đã thêm license vào danh mục!"
   - ✅ License item có border màu xanh lá và background nhạt
   - ✅ Badge "Đã gán" xuất hiện bên cạnh tên license
   - ✅ Nút "Thêm" đổi thành "Xóa" (màu đỏ)
   - ✅ Section "Licenses đã gán vào danh mục" xuất hiện (giữa form và danh sách)
   - ✅ License badge xuất hiện trong section đã gán

#### Test 6: Gán nhiều Licenses
1. Click "Thêm" cho 2-3 licenses khác nhau
2. Kiểm tra:
   - ✅ Tất cả licenses đều xuất hiện trong section "Licenses đã gán"
   - ✅ Mỗi license có nút X để xóa
   - ✅ Count "(3)" cập nhật đúng

#### Test 7: Xóa License khỏi Category
1. Có 2 cách xóa:
   - **Cách 1**: Click nút X trên badge trong section "Licenses đã gán"
   - **Cách 2**: Click nút "Xóa" (đỏ) trong danh sách licenses
2. Confirm dialog hiện ra
3. Click OK
4. Kiểm tra:
   - ✅ Notification "✅ Đã xóa license khỏi danh mục!"
   - ✅ License item mất border xanh, background về bình thường
   - ✅ Badge "Đã gán" biến mất
   - ✅ Nút "Xóa" đổi lại thành "Thêm"
   - ✅ License badge biến mất khỏi section đã gán
   - ✅ Count cập nhật

#### Test 8: Kiểm tra License không bị xóa khỏi Database
1. Xóa tất cả licenses khỏi category
2. Đóng modal
3. Mở lại License Manager (global)
4. Kiểm tra:
   - ✅ Tất cả licenses vẫn còn trong danh sách
   - ✅ Không có license nào bị mất

#### Test 9: Gán cùng License cho nhiều Categories
1. Mở License Manager cho category A, gán license X
2. Đóng modal
3. Mở License Manager cho category B, gán cùng license X
4. Kiểm tra:
   - ✅ Cả 2 categories đều có license X
   - ✅ Khi mở modal của category A: license X hiển thị "Đã gán"
   - ✅ Khi mở modal của category B: license X cũng hiển thị "Đã gán"

#### Test 10: Database Verification
```sql
-- Kiểm tra licenses đã tạo
SELECT * FROM shop_licenses WHERE seller_id = [your_seller_id];

-- Kiểm tra assignments
SELECT 
    cl.*,
    c.name as category_name,
    sl.license_name
FROM category_licenses cl
JOIN categories c ON cl.category_id = c.category_id
JOIN shop_licenses sl ON cl.shop_license_id = sl.shop_license_id
WHERE cl.seller_id = [your_seller_id];

-- Kiểm tra cascade delete
-- (Thử xóa một category có licenses - licenses assignments cũng phải bị xóa)
```

## 🎨 Bước 5: Test UI/UX

### 5.1. Visual Tests
- ✅ Modal hiển thị đẹp, không bị lỗi CSS
- ✅ Assigned license badges có màu xanh, hover effect
- ✅ License items có animation smooth khi add/remove
- ✅ Loading state hiển thị khi đang fetch data
- ✅ Empty state hiển thị khi chưa có licenses
- ✅ Notification toast hiển thị và tự động biến mất

### 5.2. Responsive Test
1. Resize browser window
2. Kiểm tra:
   - ✅ Modal responsive trên mobile
   - ✅ License badges wrap correctly
   - ✅ Buttons không bị overlap

### 5.3. Browser Console
1. Mở Developer Tools (F12)
2. Kiểm tra Console tab:
   - ✅ Không có JavaScript errors
   - ✅ Không có 404 errors
   - ✅ API calls thành công (200 status)

### 5.4. Network Tab
1. Mở Network tab trong DevTools
2. Thực hiện các actions (add, assign, remove)
3. Kiểm tra:
   - ✅ API endpoints được call đúng
   - ✅ Request body đúng format
   - ✅ Response trả về success
   - ✅ No CORS errors

## 🐛 Troubleshooting

### Lỗi 1: "Cannot resolve table 'category_licenses'"
**Nguyên nhân**: Chưa chạy SQL migration
**Fix**: 
```bash
mysql -u root -p banhangrong_db < sql/create_category_licenses_table.sql
```

### Lỗi 2: "401 Unauthorized" khi call API
**Nguyên nhân**: Chưa đăng nhập hoặc session expired
**Fix**: Đăng xuất và đăng nhập lại

### Lỗi 3: Modal không hiển thị
**Nguyên nhân**: JavaScript error hoặc CSS không load
**Fix**: 
- Check browser console for errors
- Hard refresh (Cmd+Shift+R / Ctrl+Shift+R)
- Clear browser cache

### Lỗi 4: "License đã được gán cho danh mục này"
**Nguyên nhân**: Đang cố gán duplicate license
**Fix**: Bình thường, working as intended. Mỗi license chỉ gán 1 lần per category.

### Lỗi 5: Compile error "Cannot find symbol"
**Nguyên nhân**: Maven chưa sync dependencies
**Fix**:
```bash
./mvnw clean install -DskipTests
```

### Lỗi 6: "Foreign key constraint fails"
**Nguyên nhân**: Đang cố xóa category/license có liên kết
**Fix**: Bình thường, CASCADE DELETE sẽ xử lý. Nếu lỗi vẫn xảy ra, check foreign key constraints.

## 📊 Kiểm tra Data trong Database

### Query 1: Xem tất cả licenses
```sql
SELECT 
    sl.*,
    COUNT(cl.category_license_id) as assigned_count
FROM shop_licenses sl
LEFT JOIN category_licenses cl ON sl.shop_license_id = cl.shop_license_id
WHERE sl.seller_id = [your_seller_id]
GROUP BY sl.shop_license_id;
```

### Query 2: Xem licenses của category
```sql
SELECT 
    c.name as category_name,
    sl.license_name,
    sl.license_type,
    cl.created_at as assigned_at
FROM category_licenses cl
JOIN categories c ON cl.category_id = c.category_id
JOIN shop_licenses sl ON cl.shop_license_id = sl.shop_license_id
WHERE cl.category_id = [category_id]
AND cl.seller_id = [seller_id];
```

### Query 3: Xem categories của một license
```sql
SELECT 
    sl.license_name,
    c.name as category_name,
    cl.created_at as assigned_at
FROM category_licenses cl
JOIN categories c ON cl.category_id = c.category_id
JOIN shop_licenses sl ON cl.shop_license_id = sl.shop_license_id
WHERE cl.shop_license_id = [license_id]
AND cl.seller_id = [seller_id];
```

## ✅ Acceptance Criteria Checklist

Theo yêu cầu ban đầu:

- ✅ **"Phần quản lý danh mục sẽ thấy thông tin, số lượng các sản phẩm"**
  → Stats cards hiển thị số lượng products per category

- ✅ **"Mỗi seller có sản phẩm riêng"**
  → All queries filter by seller_id

- ✅ **"Nếu không có sản phẩm thì để là không có sản phẩm"**
  → Empty states implemented

- ✅ **"Nút quản lí license có thể thêm license mới"**
  → Form "Thêm License mới" working

- ✅ **"Khi thêm thì sản phẩm đấy sẽ được đẩy lên database"**
  → POST /api/licenses saves to shop_licenses table

- ✅ **"Danh sách Licenses hiện có"**
  → Section "Danh sách Licenses hiện có" implemented

- ✅ **"Có nút add để seller sẽ có thêm license ấy vào mục"**
  → "Thêm" button assigns to category_licenses

- ✅ **"Khi thêm vào rồi thì nó sẽ hiện ở giữa mục thêm licenses và danh sách"**
  → Section "Licenses đã gán vào danh mục" appears between form and list

- ✅ **"Có thể xoá license ấy ra khỏi mục (chứ không phải xoá luôn trong database)"**
  → Remove only deletes from category_licenses, not shop_licenses

## 🎉 Success!

Nếu tất cả tests pass, congratulations! Feature đã hoàn chỉnh và ready to use.

### Next Steps:
1. Test với real data nhiều hơn
2. Invite team members để test UAT
3. Document any edge cases discovered
4. Consider future enhancements (xem LICENSE_MANAGEMENT_README.md)

---

**Created**: 2025-01-03
**Author**: GitHub Copilot
**Version**: 1.0.0

