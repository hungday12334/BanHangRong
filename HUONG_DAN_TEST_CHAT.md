# HƯỚNG DẪN TEST HỆ THỐNG CHAT

## ⚠️ LƯU Ý QUAN TRỌNG VỀ QUYỀN TRUY CẬP

**HỆ THỐNG CHAT CHO PHÉP CẢ SELLER VÀ CUSTOMER TRUY CẬP:**

- ✅ **CUSTOMER** có thể truy cập: 
  - `/chat`
  - `/customer/chat` 
  
- ✅ **SELLER** có thể truy cập:
  - `/chat`
  - `/seller/chat`
  - `/customer/chat` (cũng được phép!)

**Khuyến nghị:**
- Customer nên dùng: `http://localhost:8080/customer/chat`
- Seller nên dùng: `http://localhost:8080/seller/chat` hoặc `http://localhost:8080/chat`
- Cả hai đều hiển thị cùng giao diện chat, chỉ khác nhau về danh sách conversations

---

## 🔧 XỬ LÝ LỖI "Please login first"

### Nguyên nhân và cách khắc phục:

**1. Chưa đăng nhập hoặc session hết hạn:**
```bash
# Giải pháp: Đăng nhập lại
- Truy cập: http://localhost:8080/login
- Đăng nhập với tài khoản SELLER hoặc CUSTOMER
- Sau đó mới truy cập /chat
```

**2. Session không được lưu đúng cách:**
```bash
# Kiểm tra cookie trong trình duyệt (F12 > Application > Cookies)
# Phải có cookie JSESSIONID

# Nếu không có, thử:
- Xóa cache và cookies
- Khởi động lại trình duyệt
- Đăng nhập lại
```

**3. Ứng dụng cần khởi động lại sau khi update code:**
```bash
# Dừng ứng dụng đang chạy (Ctrl+C)
# Rebuild và chạy lại
cd /Users/hoangquangminh/Desktop/FUlearning/FALL25/SWP391/BanHangRong2
mvn clean package -DskipTests
java -jar target/su25-0.0.1-SNAPSHOT.jar
```

**4. Kiểm tra log để xem lỗi chi tiết:**
```bash
# Xem log trong terminal khi chạy ứng dụng
# Hoặc kiểm tra file app.log
tail -f app.log | grep -i "error\|denied\|forbidden"
```

**5. Test flow đúng:**
```
✅ ĐÚNG:
1. Mở trình duyệt
2. Vào http://localhost:8080/login
3. Đăng nhập (seller1 / 123456)
4. Sau khi đăng nhập thành công → redirect đến dashboard
5. Bây giờ vào http://localhost:8080/seller/chat
6. ✅ Thành công!

❌ SAI:
1. Mở trình duyệt
2. Trực tiếp vào http://localhost:8080/customer/chat
3. ❌ Lỗi "Please login first" (vì chưa đăng nhập)
```

---

## Bước 1: Chuẩn bị Database

### 1.1. Tạo bảng chat
Chạy script SQL để tạo các bảng cần thiết:

```bash
mysql -u root -p banhangrong_db < sql/create_chat_tables.sql
```

Hoặc chạy trực tiếp trong MySQL:

```sql
-- Tạo bảng conversations
CREATE TABLE IF NOT EXISTS chat_conversations (
    id VARCHAR(100) PRIMARY KEY,
    customer_id BIGINT NOT NULL,
    customer_name VARCHAR(100),
    seller_id BIGINT NOT NULL,
    seller_name VARCHAR(100),
    last_message TEXT,
    last_message_time TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_customer_seller (customer_id, seller_id),
    INDEX idx_customer (customer_id),
    INDEX idx_seller (seller_id),
    FOREIGN KEY (customer_id) REFERENCES users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (seller_id) REFERENCES users(user_id) ON DELETE CASCADE
);

-- Tạo bảng messages
CREATE TABLE IF NOT EXISTS chat_messages (
    message_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    conversation_id VARCHAR(100) NOT NULL,
    sender_id BIGINT NOT NULL,
    sender_name VARCHAR(100),
    sender_role VARCHAR(20),
    receiver_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    message_type VARCHAR(20) DEFAULT 'TEXT',
    is_read BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_conversation (conversation_id),
    INDEX idx_sender (sender_id),
    INDEX idx_receiver (receiver_id),
    FOREIGN KEY (conversation_id) REFERENCES chat_conversations(id) ON DELETE CASCADE,
    FOREIGN KEY (sender_id) REFERENCES users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (receiver_id) REFERENCES users(user_id) ON DELETE CASCADE
);
```

### 1.2. Tạo tài khoản test
Tạo ít nhất 1 seller và 2 customer để test:

```sql
-- Tạo 1 seller (nếu chưa có)
INSERT INTO users (username, email, password, full_name, user_type, is_active, is_email_verified, balance) 
VALUES ('seller1', 'seller1@test.com', '$2a$10$hashedpassword', 'Shop ABC', 'SELLER', true, true, 0);

-- Tạo 2 customers (nếu chưa có)
INSERT INTO users (username, email, password, full_name, user_type, is_active, is_email_verified, balance) 
VALUES ('customer1', 'customer1@test.com', '$2a$10$hashedpassword', 'Nguyễn Văn A', 'CUSTOMER', true, true, 0);

INSERT INTO users (username, email, password, full_name, user_type, is_active, is_email_verified, balance) 
VALUES ('customer2', 'customer2@test.com', '$2a$10$hashedpassword', 'Trần Thị B', 'CUSTOMER', true, true, 0);
```

## Bước 2: Khởi động ứng dụng

```bash
# Biên dịch và chạy
mvn spring-boot:run

# Hoặc nếu đã build
java -jar target/su25-0.0.1-SNAPSHOT.jar
```

Ứng dụng sẽ chạy tại: `http://localhost:8080`

## Bước 3: Test Chat - Kịch bản 1 (Customer gửi tin cho Seller)

### 3.1. Mở trình duyệt thứ nhất (Customer)
1. Mở Chrome/Firefox ở chế độ bình thường
2. Truy cập: `http://localhost:8080/login`
3. Đăng nhập với tài khoản **customer1**:
   - Username: `customer1`
   - Password: `123456` (hoặc password bạn đã set)

4. Sau khi đăng nhập thành công, truy cập trang chat:
   - URL: `http://localhost:8080/customer/chat`
   - Hoặc: `http://localhost:8080/chat`

5. **Kiểm tra giao diện:**
   - ✅ Sidebar bên trái hiện danh sách seller
   - ✅ Thông tin customer hiện ở đầu sidebar
   - ✅ Thấy seller "Shop ABC" trong danh sách

6. **Gửi tin nhắn:**
   - Click vào "Shop ABC" trong danh sách
   - Nhập tin nhắn: "Xin chào shop, tôi muốn hỏi về sản phẩm"
   - Nhấn Enter hoặc click nút gửi
   - ✅ Tin nhắn hiện ngay lập tức ở bên phải màn hình
   - ✅ Tin nhắn có màu xanh (message sent)

### 3.2. Mở trình duyệt thứ hai (Seller)
1. Mở Chrome/Firefox ở chế độ **Incognito/Private**
2. Truy cập: `http://localhost:8080/login`
3. Đăng nhập với tài khoản **seller1**:
   - Username: `seller1`
   - Password: `123456`

4. Truy cập trang chat:
   - URL: `http://localhost:8080/seller/chat`
   - Hoặc: `http://localhost:8080/chat`

5. **Kiểm tra giao diện:**
   - ✅ Sidebar hiện conversation với "Nguyễn Văn A"
   - ✅ Có badge số tin nhắn chưa đọc (màu xanh)
   - ✅ Tin nhắn cuối hiện preview

6. **Xem và trả lời:**
   - Click vào conversation "Nguyễn Văn A"
   - ✅ Thấy tin nhắn "Xin chào shop..." của customer
   - ✅ Tin nhắn có màu xám (message received)
   - Nhập trả lời: "Chào bạn, shop có thể giúp gì cho bạn?"
   - Nhấn Enter
   - ✅ Tin nhắn hiện ngay màu xanh

### 3.3. Quay lại trình duyệt Customer
1. **Không cần refresh trang**
2. ✅ Tin nhắn trả lời của seller hiện ngay lập tức
3. ✅ Tin nhắn màu xám ở bên trái
4. Tiếp tục chat qua lại để test real-time

## Bước 4: Test Chat - Kịch bản 2 (Nhiều Customer)

### 4.1. Mở trình duyệt thứ ba (Customer 2)
1. Mở thêm cửa sổ trình duyệt mới (Private/Incognito)
2. Đăng nhập với **customer2**
3. Truy cập `/customer/chat`
4. Gửi tin: "Shop ơi, cho em hỏi giá sản phẩm"

### 4.2. Kiểm tra Seller
1. Quay lại trình duyệt Seller
2. ✅ Thấy conversation mới xuất hiện với "Trần Thị B"
3. ✅ Badge unread count = 1
4. Click vào conversation mới
5. ✅ Đọc được tin nhắn của Customer 2

### 4.3. Test chuyển đổi conversation
1. Ở trình duyệt Seller:
   - Click conversation "Nguyễn Văn A" → Chat với Customer 1
   - Click conversation "Trần Thị B" → Chat với Customer 2
   - ✅ Tin nhắn của mỗi conversation hiện đúng
   - ✅ Không bị lẫn lộn tin nhắn

## Bước 5: Test các tính năng nâng cao

### 5.1. Test Unread Count
1. Customer gửi 3 tin nhắn liên tiếp
2. Seller chưa mở conversation
3. ✅ Badge hiện số "3"
4. Seller click vào conversation
5. ✅ Badge biến mất (đã đọc hết)

### 5.2. Test Real-time Update
1. Để 2 trình duyệt cạnh nhau
2. Customer gửi tin
3. ✅ Tin hiện ngay lập tức ở Seller (< 1 giây)
4. Seller trả lời
5. ✅ Tin hiện ngay lập tức ở Customer

### 5.3. Test Connection Status
1. Quan sát thanh status ở đầu trang
2. Khi vừa load trang: "Connecting..."
3. Sau khi kết nối: "✓ Connected" (màu xanh lá, tự động ẩn sau 2s)
4. Tắt mạng: "⚠ Disconnected" (màu đỏ)
5. Bật mạng lại: Tự động reconnect

### 5.4. Test Message History
1. Gửi 10+ tin nhắn
2. Refresh trang
3. ✅ Tất cả tin nhắn cũ vẫn hiện đúng thứ tự
4. ✅ Scroll lên xem tin cũ mượt mà

### 5.5. Test Last Message Preview
1. Gửi tin: "Đây là tin nhắn cuối"
2. ✅ Sidebar conversation hiện preview: "Đây là tin nhắn cuối"
3. ✅ Thời gian cập nhật (vd: "2m ago")

## Bước 6: Test Edge Cases

### 6.1. Test tin nhắn dài
```
Gửi tin nhắn rất dài: Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat...
```
✅ Tin nhắn wrap đúng, không bị tràn

### 6.2. Test tin nhắn đặc biệt
- Emoji: 😀 🎉 ❤️
- Ký tự tiếng Việt có dấu: "Xin chào, mình muốn mua áo dài"
- HTML: `<script>alert('test')</script>`
- ✅ Tất cả hiện đúng, HTML bị escape (không chạy script)

### 6.3. Test nhiều tab cùng lúc
1. Mở 2 tab với cùng account Customer
2. Tab 1: Gửi tin
3. ✅ Tab 2: Cũng thấy tin đã gửi

### 6.4. Test logout và login lại
1. Customer gửi tin rồi logout
2. Login lại
3. ✅ Conversation và tin nhắn vẫn còn

## Bước 7: Kiểm tra Database

### 7.1. Kiểm tra conversations
```sql
SELECT * FROM chat_conversations;
```
✅ Có 2 conversation (customer1-seller1, customer2-seller1)

### 7.2. Kiểm tra messages
```sql
SELECT 
    message_id,
    conversation_id,
    sender_name,
    content,
    is_read,
    created_at 
FROM chat_messages 
ORDER BY created_at DESC 
LIMIT 20;
```
✅ Tất cả tin nhắn được lưu đúng

### 7.3. Kiểm tra unread count
```sql
SELECT 
    conversation_id,
    receiver_id,
    COUNT(*) as unread_count
FROM chat_messages 
WHERE is_read = false 
GROUP BY conversation_id, receiver_id;
```

## Bước 8: Test Performance

### 8.1. Test với nhiều tin nhắn
1. Gửi 100 tin nhắn nhanh (dùng loop)
2. ✅ Không bị lag
3. ✅ Tin nhắn hiện tuần tự đúng thứ tự

### 8.2. Test với nhiều conversation
1. Tạo thêm 10 customer accounts
2. Mỗi customer gửi tin cho seller
3. ✅ Seller thấy 10 conversations
4. ✅ Switch giữa các conversation mượt

## Checklist Test Hoàn Chỉnh

### ✅ Giao diện
- [ ] Sidebar hiện danh sách conversation
- [ ] Avatar hiện đúng chữ cái đầu
- [ ] Màu sắc phân biệt Customer (tím) / Seller (hồng)
- [ ] Badge unread count hiện đúng
- [ ] Last message preview hiện đúng
- [ ] Timestamp format đẹp (Just now, 2m ago, 1h ago)
- [ ] Responsive trên mobile

### ✅ Chức năng
- [ ] Gửi tin nhắn thành công
- [ ] Nhận tin nhắn real-time
- [ ] Unread count cập nhật đúng
- [ ] Mark as read khi mở conversation
- [ ] Conversation sort by last message time
- [ ] Message history load đầy đủ
- [ ] WebSocket auto-reconnect
- [ ] Rate limiting (không spam được)

### ✅ Bảo mật
- [ ] Phải login mới vào được chat
- [ ] Chỉ thấy conversation của mình
- [ ] HTML bị escape (XSS protection)
- [ ] Không send được tin vào conversation không phải của mình

### ✅ Database
- [ ] Messages được lưu đúng
- [ ] Conversations được tạo đúng
- [ ] Foreign key constraints hoạt động
- [ ] Cascade delete hoạt động

## Xử lý lỗi thường gặp

### Lỗi 1: WebSocket không k��t nối
**Triệu chứng:** Status bar hiện "Disconnected", không gửi được tin

**Nguyên nhân:** 
- Spring Boot chưa start xong
- Port 8080 bị chặn
- Firewall block WebSocket

**Giải pháp:**
```bash
# Kiểm tra app đã chạy chưa
curl http://localhost:8080/actuator/health

# Kiểm tra WebSocket endpoint
curl http://localhost:8080/chat/info

# Check logs
tail -f app.log | grep WebSocket
```

### Lỗi 2: Không thấy tin nhắn real-time
**Nguyên nhân:** Chưa subscribe đúng topic

**Giải pháp:**
1. Mở Developer Tools (F12)
2. Tab Console
3. Tìm lỗi WebSocket hoặc STOMP
4. Check logs: "Connected:", "Subscribed to:"

### Lỗi 3: Tin nhắn bị trùng
**Nguyên nhân:** Mở nhiều connection với cùng user

**Giải pháp:**
- Chỉ mở 1 tab/window cho mỗi user
- Hoặc logout tab cũ trước khi login tab mới

### Lỗi 4: Conversations không hiện
**Nguyên nhân:** 
- Chưa có seller trong database
- User chưa đúng role (CUSTOMER/SELLER)

**Giải pháp:**
```sql
-- Check user roles
SELECT user_id, username, user_type FROM users;

-- Update user role nếu sai
UPDATE users SET user_type = 'SELLER' WHERE username = 'seller1';
UPDATE users SET user_type = 'CUSTOMER' WHERE username = 'customer1';
```

## Video Demo (Nếu cần)

### Quay video test:
1. Chia màn hình làm 2 (Customer bên trái, Seller bên phải)
2. Record màn hình
3. Demo flow: Login → Chat → Real-time → Switch conversation

### Upload lên:
- YouTube (unlisted)
- Google Drive
- Loom

## Kết luận

Sau khi test đầy đủ các bước trên, hệ thống chat cần đạt được:

✅ **Real-time messaging**: Tin nhắn gửi/nhận tức thì
✅ **Persistence**: Tin nhắn lưu trong database
✅ **Multi-conversation**: Seller quản lý nhiều customer
✅ **Unread tracking**: Đếm tin chưa đọc chính xác
✅ **User-friendly**: Giao diện đẹp, dễ dùng
✅ **Stable**: Không crash, auto-reconnect

Nếu tất cả checklist trên pass ✅ → Hệ thống chat hoạt động tốt! 🎉

