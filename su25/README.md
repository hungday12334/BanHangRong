# Bán Hàng Rong

Dự án Web xây dựng bằng Spring Boot, Thymeleaf, MySQL.

## Chức năng chính

## Chạy dự án
1. Cài đặt MySQL, tạo database và import file `data_v2.sql`
2. Cấu hình kết nối DB trong `application.properties` bằng username và mật khẩu của bạn
3. Build và chạy:
   ```
   ./mvnw spring-boot:run
   ```
4. Truy cập: http://localhost:8080/

##--------------------[TIPS AND TRICK]--------------------
1. Test thông báo đơn hàng mới 
fetch('/api/orders', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({ userId: 1, totalAmount: 123.45 })
}).then(r => r.json()).then(console.log)

2. Các đoạn liên quan nằm quanh:
Tải chi tiết: khoảng dòng ~548
Lưu (create/update): ~633
Duyệt/publish: ~655
Xóa: ~673
Load “My Products”: ~732, ~1151

