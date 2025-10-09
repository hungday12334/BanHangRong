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

fetch('/api/orders', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({ userId: 1, totalAmount: 123.45 })
}).then(r => r.json()).then(console.log)
<!-- Test đơn hàng mới  -->