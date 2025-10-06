# BanHangRong - Hệ thống Authentication

## Tổng quan
Dự án BanHangRong đã được nâng cấp với hệ thống xác thực đầy đủ bao gồm đăng nhập, đăng ký và quên mật khẩu.

## Tính năng đã triển khai

### Backend (Spring Boot)
- **Authentication Controller**: Xử lý các API đăng nhập, đăng ký, quên mật khẩu
- **JWT Security**: Sử dụng JWT token cho xác thực
- **Password Encryption**: Mã hóa mật khẩu với BCrypt
- **User Management**: Quản lý người dùng với đầy đủ thông tin

### Frontend (HTML/CSS/JavaScript)
- **Trang đăng nhập**: Giao diện đẹp theo wireframe với toggle password
- **Trang đăng ký**: Form đăng ký với validation
- **Trang quên mật khẩu**: Gửi email reset password
- **Responsive Design**: Tương thích mobile
- **User Session**: Quản lý phiên đăng nhập

## Cấu trúc dự án

```
su25/
├── src/main/java/banhangrong/su25/
│   ├── Controller/
│   │   ├── AuthController.java      # API authentication
│   │   └── PageController.java      # Serve HTML pages
│   ├── Service/
│   │   └── AuthService.java         # Business logic
│   ├── Repository/
│   │   └── UsersRepository.java     # Data access
│   ├── DTO/
│   │   ├── LoginRequest.java        # Login data transfer
│   │   ├── RegisterRequest.java     # Register data transfer
│   │   └── AuthResponse.java        # Response data transfer
│   ├── Config/
│   │   └── SecurityConfig.java      # Spring Security config
│   ├── Util/
│   │   └── JwtUtil.java             # JWT utilities
│   └── Entity/
│       └── Users.java               # User entity
├── src/main/resources/
│   ├── templates/
│   │   ├── login.html               # Trang đăng nhập
│   │   ├── register.html            # Trang đăng ký
│   │   ├── forgot-password.html     # Trang quên mật khẩu
│   │   └── index.html               # Trang chủ
│   ├── static/
│   │   ├── css/
│   │   │   └── style.css            # Styles đẹp
│   │   └── js/
│   │       ├── auth.js              # JavaScript authentication
│   │       └── home.js              # JavaScript trang chủ
│   └── application.properties       # Cấu hình
└── pom.xml                          # Maven dependencies
```

## Cách chạy dự án

### Yêu cầu hệ thống
- Java 21+
- MySQL Database
- Maven (hoặc sử dụng Maven wrapper)

### Cài đặt và chạy

1. **Cấu hình database**:
   - Tạo database MySQL tên `wap`
   - Cập nhật thông tin database trong `application.properties`

2. **Chạy ứng dụng**:
   ```bash
   cd su25
   ./mvnw spring-boot:run
   # Hoặc trên Windows:
   mvnw.cmd spring-boot:run
   ```

3. **Truy cập ứng dụng**:
   - Trang chủ: http://localhost:8080
   - Đăng nhập: http://localhost:8080/login
   - Đăng ký: http://localhost:8080/register
   - Quên mật khẩu: http://localhost:8080/forgot-password

## API Endpoints

### Authentication APIs
- `POST /api/auth/login` - Đăng nhập
- `POST /api/auth/register` - Đăng ký
- `POST /api/auth/forgot-password` - Quên mật khẩu

### Page Routes
- `GET /` - Trang chủ
- `GET /login` - Trang đăng nhập
- `GET /register` - Trang đăng ký
- `GET /forgot-password` - Trang quên mật khẩu

## Tính năng chính

### 1. Đăng nhập
- Nhập username và password
- Toggle hiển thị mật khẩu
- Validation và error handling
- JWT token authentication

### 2. Đăng ký
- Form đăng ký đầy đủ thông tin
- Validation mật khẩu xác nhận
- Kiểm tra username/email trùng lặp
- Tự động đăng nhập sau đăng ký

### 3. Quên mật khẩu
- Nhập email để reset password
- Gửi email đặt lại mật khẩu (cần cấu hình SMTP)

### 4. Quản lý phiên
- Lưu trữ JWT token trong localStorage
- Tự động chuyển hướng khi đã đăng nhập
- Đăng xuất và xóa session

## Cấu hình

### JWT Configuration
```properties
jwt.secret=mySecretKey123456789012345678901234567890
jwt.expiration=86400000
```

### Email Configuration (cho forgot password)
```properties
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=your-email@gmail.com
spring.mail.password=your-app-password
```

## Bảo mật

- Mật khẩu được mã hóa với BCrypt
- JWT token với expiration time
- CORS enabled cho frontend
- Input validation và sanitization
- SQL injection protection với JPA

## Giao diện

- Thiết kế hiện đại với gradient background
- Responsive design cho mobile
- Smooth animations và transitions
- User-friendly error messages
- Loading states cho better UX

## Lưu ý

- Cần cấu hình email SMTP để sử dụng chức năng quên mật khẩu
- Database sẽ tự động tạo tables khi chạy lần đầu
- JWT secret nên được thay đổi trong production
- Có thể cần cài đặt Maven nếu Maven wrapper không hoạt động
