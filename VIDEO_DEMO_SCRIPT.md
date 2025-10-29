# 🎬 SCRIPT QUAY VIDEO DEMO HỆ THỐNG CHAT

## 📹 Video 1: Demo Chat Cơ Bản (2 phút)

### Scene 1: Giới thiệu (10 giây)
```
[Màn hình chính]
"Xin chào! Hôm nay mình sẽ demo hệ thống chat real-time 
giữa Customer và Seller trên nền tảng BanHangRong"
```

### Scene 2: Setup (20 giây)
```
[Terminal]
1. Chạy database script
   mysql -u root -p < sql/create_chat_tables.sql
   mysql -u root -p < sql/create_test_data_chat.sql

2. Khởi động app
   mvn spring-boot:run
   
3. Đợi "Started Su25Application..."
```

### Scene 3: Login Customer (15 giây)
```
[Browser 1 - Bên trái màn hình]
1. Mở http://localhost:8080/login
2. Nhập:
   Username: test_customer1
   Password: 123456
3. Click "Đăng nhập"
```

### Scene 4: Vào Chat Customer (15 giây)
```
[Browser 1]
1. Vào http://localhost:8080/customer/chat
2. Thấy sidebar với "Shop Điện Thoại ABC"
3. Click vào Shop
4. Gửi tin: "Xin chào shop, em muốn mua iPhone 15"
5. Thấy tin hiện màu xanh bên phải
```

### Scene 5: Login Seller (15 giây)
```
[Browser 2 - Bên phải màn hình - Incognito]
1. Mở http://localhost:8080/login (Incognito)
2. Nhập:
   Username: test_seller1
   Password: 123456
3. Click "Đăng nhập"
```

### Scene 6: Seller nhận tin (20 giây)
```
[Browser 2]
1. Vào http://localhost:8080/seller/chat
2. ✨ Thấy conversation "Nguyễn Văn An"
3. Badge hiện số "1" (unread)
4. Click vào conversation
5. ✨ Thấy tin nhắn của customer
6. Badge biến mất (đã đọc)
```

### Scene 7: Seller trả lời (15 giây)
```
[Browser 2]
1. Nhập: "Chào bạn! Shop có đủ màu và dung lượng"
2. Enter
3. Tin hiện màu xanh bên phải
```

### Scene 8: Customer nhận tin (10 giây)
```
[Browser 1 - Không cần refresh]
✨ Tin trả lời hiện ngay lập tức màu xám bên trái
"WOW! Real-time messaging hoạt động!"
```

### Scene 9: Chat qua lại (20 giây)
```
[Split screen - 2 browsers]
Customer: "Em muốn màu xanh 256GB"
→ Seller thấy ngay
Seller: "Giá 27 triệu, bảo hành 12 tháng"
→ Customer thấy ngay
Customer: "OK em đặt ạ"
→ Seller thấy ngay
```

### Scene 10: Kết thúc (10 giây)
```
[Fade out]
"Vậy là xong! Chat real-time với WebSocket
✅ Gửi/nhận tức thì
✅ Lưu lịch sử
✅ Đếm tin chưa đọc
✅ Giao diện đẹp

Thanks for watching! 👋"
```

---

## 📹 Video 2: Demo Tính Năng Nâng Cao (5 phút)

### Scene 1: Multi-Customer (1 phút)
```
[3 browsers]
1. Customer 1 gửi: "Hỏi về iPhone 15"
2. Customer 2 gửi: "Hỏi về Samsung S24"
3. Customer 3 gửi: "Hỏi về laptop"

[Seller browser]
✨ Thấy 3 conversations riêng biệt
✨ Mỗi conversation có badge unread
✨ Click từng cái xem tin
✨ Không bị lẫn tin nhắn
```

### Scene 2: Unread Count (30 giây)
```
[Customer gửi 5 tin liên tiếp]
"Tin 1"
"Tin 2"
"Tin 3"
"Tin 4"
"Tin 5"

[Seller sidebar]
✨ Badge hiện "5"

[Seller click vào]
✨ Badge → "0"
✨ Tất cả tin đánh dấu đã đọc
```

### Scene 3: Connection Status (1 phút)
```
[Top banner]
1. Load trang: "Connecting..." (màu vàng)
2. Kết nối: "✓ Connected" (màu xanh, tự tắt sau 2s)
3. [Tắt WiFi] → "⚠ Disconnected" (màu đỏ)
4. [Bật WiFi] → "Connecting..." → "✓ Connected"
5. ✨ Auto-reconnect hoạt động!
```

### Scene 4: Message History (30 giây)
```
[Gửi 20 tin nhắn qua lại]
Customer: 10 tin
Seller: 10 tin

[F5 refresh cả 2 browsers]
✨ Tất cả 20 tin vẫn còn đúng thứ tự
✨ Scroll lên xem tin cũ
```

### Scene 5: Last Message Preview (30 giây)
```
[Sidebar conversation]
Customer gửi: "Đây là tin cuối cùng"

✨ Preview update ngay: "Đây là tin cuối cùng"
✨ Time update: "Just now"

[Đợi 2 phút]
✨ Time update: "2m ago"
```

### Scene 6: Database Check (1 phút)
```
[MySQL Workbench]
SELECT * FROM chat_conversations;
→ Thấy 3 conversations

SELECT * FROM chat_messages 
ORDER BY created_at DESC LIMIT 20;
→ Thấy 20 tin mới nhất

SELECT COUNT(*) FROM chat_messages WHERE is_read = false;
→ Đếm tin chưa đọc
```

### Scene 7: Kết thúc (30 giây)
```
"Tính năng đã demo:
✅ Multi-conversation management
✅ Unread count tracking
✅ Auto-reconnect
✅ Message persistence
✅ Real-time preview updates
✅ Database integration

Hệ thống sẵn sàng production! 🚀"
```

---

## 📹 Video 3: Hướng Dẫn Cài Đặt (3 phút)

### Part 1: Prerequisites (30 giây)
```
[Slide]
"Yêu cầu hệ thống:
- Java 21 hoặc 23
- Maven 3.8+
- MySQL 8.0+
- Spring Boot 3.x"

[Terminal]
java -version    → Java 23
mvn -version     → Maven 3.9
mysql --version  → MySQL 8.0
```

### Part 2: Database Setup (1 phút)
```
[MySQL Workbench hoặc Terminal]

1. Tạo bảng
mysql -u root -p banhangrong_db < sql/create_chat_tables.sql

2. Check tables
SHOW TABLES LIKE 'chat_%';
→ chat_conversations
→ chat_messages

3. Tạo dữ liệu test
mysql -u root -p banhangrong_db < sql/create_test_data_chat.sql

4. Check data
SELECT * FROM users WHERE username LIKE 'test_%';
→ 1 seller, 3 customers
```

### Part 3: Build & Run (1 phút)
```
[Terminal]

# Option 1: Maven direct
mvn clean spring-boot:run

# Option 2: Build jar
mvn clean package -DskipTests
java -jar target/su25-0.0.1-SNAPSHOT.jar

# Option 3: Use script (if Lombok error)
chmod +x fix_and_build.sh
./fix_and_build.sh

[Đợi]
"Started Su25Application in 8.5 seconds"
✅ App đã chạy!
```

### Part 4: First Test (30 giây)
```
[Browser]
1. http://localhost:8080/login
2. Login: test_customer1 / 123456
3. http://localhost:8080/customer/chat
4. Gửi tin cho Shop
5. ✅ Thành công!
```

---

## 📹 Video 4: Troubleshooting (5 phút)

### Problem 1: Build Failed - Lombok (1 phút)
```
[Terminal showing error]
"cannot find symbol: method getSenderId()"

[Solution]
mvn dependency:get -Dartifact=org.projectlombok:lombok:1.18.34
mvn clean compile

[Or]
./fix_and_build.sh

✅ Fixed!
```

### Problem 2: WebSocket Not Connecting (1 phút)
```
[Browser Console]
"WebSocket connection failed"

[Solution 1: Check app running]
curl http://localhost:8080/actuator/health
→ Should return {"status":"UP"}

[Solution 2: Check logs]
tail -f app.log | grep -i websocket
→ Should see "Mapped '/'..."

✅ Fixed!
```

### Problem 3: No Conversations (1 phút)
```
[Empty sidebar]

[Solution: Check user_type]
SELECT user_id, username, user_type FROM users;

[Fix if wrong]
UPDATE users SET user_type = 'SELLER' WHERE username = 'test_seller1';
UPDATE users SET user_type = 'CUSTOMER' WHERE username = 'test_customer1';

✅ Fixed!
```

### Problem 4: Tables Not Found (1 phút)
```
[Error: Table 'chat_conversations' doesn't exist]

[Solution]
mysql -u root -p banhangrong_db < sql/create_chat_tables.sql

[Verify]
SHOW TABLES LIKE 'chat_%';

✅ Fixed!
```

### Problem 5: Messages Not Real-time (1 phút)
```
[Messages delayed or not showing]

[F12 → Console]
Look for WebSocket errors

[Solution: Reconnect]
Refresh page
Or restart application

[Check status bar]
Should show "✓ Connected"

✅ Fixed!
```

---

## 🎬 Tips Quay Video

### Chuẩn Bị
- [ ] Screen resolution: 1920x1080
- [ ] Font size: 16px (dễ đọc)
- [ ] Close unnecessary apps
- [ ] Disable notifications
- [ ] Prepare test accounts
- [ ] Clear browser cache
- [ ] Test audio/mic

### Trong Khi Quay
- [ ] Nói rõ ràng, không vội
- [ ] Pause giữa các bước
- [ ] Highlight mouse cursor
- [ ] Zoom vào phần quan trọng
- [ ] Show results clearly
- [ ] Repeat important steps

### Editing
- [ ] Add text overlays
- [ ] Add arrows/highlights
- [ ] Speed up waiting parts (2x)
- [ ] Add background music (soft)
- [ ] Add timestamps in description
- [ ] Add chapter markers

### Publishing
- [ ] Upload to YouTube
- [ ] Title: "Hệ Thống Chat Real-time - BanHangRong"
- [ ] Tags: spring boot, websocket, chat, real-time
- [ ] Add timestamps to description
- [ ] Pin comment with links to docs

---

## 📝 Video Description Template

```
🔥 Demo Hệ Thống Chat Real-time Customer - Seller | Spring Boot + WebSocket

Trong video này mình sẽ demo đầy đủ tính năng chat real-time 
giữa khách hàng và người bán trên nền tảng thương mại điện tử.

⏱️ TIMESTAMPS:
0:00 - Giới thiệu
0:30 - Setup database
1:00 - Chạy ứng dụng
1:30 - Login Customer
2:00 - Gửi tin nhắn
2:30 - Login Seller
3:00 - Nhận tin real-time
3:30 - Chat qua lại
4:00 - Demo tính năng nâng cao
5:00 - Kết luận

🛠️ TECH STACK:
- Backend: Spring Boot 3.x
- WebSocket: STOMP + SockJS
- Database: MySQL 8.0
- Frontend: Thymeleaf + Bootstrap 5

✨ FEATURES:
✅ Real-time messaging (< 1 second)
✅ Multi-conversation management
✅ Unread message count
✅ Message persistence
✅ Auto-reconnect
✅ Responsive UI

📚 DOCUMENTATION:
→ GitHub: [link]
→ API Docs: [link]
→ Setup Guide: [link]

💬 CÓ THẮC MẮC?
Comment bên dưới, mình sẽ trả lời!

#springboot #websocket #chat #realtime #java #mysql
```

---

## ✅ Checklist Trước Khi Quay

- [ ] Database có dữ liệu test
- [ ] App build thành công
- [ ] Test chat hoạt động
- [ ] 2 browsers sẵn sàng
- [ ] Screen recording software ready
- [ ] Microphone tested
- [ ] Script đã đọc qua
- [ ] Backup plan nếu lỗi

**Sẵn sàng quay video! 🎬**

