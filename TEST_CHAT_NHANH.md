# 🚀 HƯỚNG DẪN TEST CHAT NHANH - 5 PHÚT

## ⚡ BƯỚC 1: Chạy Database Script (2 phút)

```bash
# Tạo bảng chat
mysql -u root -p banhangrong_db < sql/create_chat_tables.sql

# Tạo dữ liệu test
mysql -u root -p banhangrong_db < sql/create_test_data_chat.sql
```

**Kết quả:** Có 1 seller và 3 customer với password đều là `123456`

## ⚡ BƯỚC 2: Khởi động App (1 phút)

```bash
mvn spring-boot:run
```

Đợi đến khi thấy: "Started Su25Application in X seconds"

## ⚡ BƯỚC 3: Test Chat (2 phút)

### 📱 Trình duyệt 1 - CUSTOMER
1. Mở Chrome: `http://localhost:8080/login`
2. Đăng nhập:
   - Username: `test_customer1`
   - Password: `123456`
3. Vào chat: `http://localhost:8080/customer/chat`
4. Click vào "Shop Điện Thoại ABC"
5. Gửi tin: "Xin chào shop!"

### 🏪 Trình duyệt 2 - SELLER  
1. Mở Chrome Incognito: `http://localhost:8080/login`
2. Đăng nhập:
   - Username: `test_seller1`
   - Password: `123456`
3. Vào chat: `http://localhost:8080/seller/chat`
4. **✅ Thấy tin nhắn từ "Nguyễn Văn An"**
5. Click vào conversation và trả lời: "Chào bạn!"

### ✅ Kiểm tra
- Quay lại trình duyệt Customer
- **✅ Thấy tin trả lời hiện ngay lập tức!**

---

## 🎯 Test Thành Công Khi:

✅ Tin nhắn từ Customer hiện ngay ở Seller (< 1 giây)  
✅ Tin trả lời từ Seller hiện ngay ở Customer  
✅ Badge unread count hiện đúng  
✅ Conversation list cập nhật real-time  
✅ Không bị lỗi trong Console (F12)

---

## 🐛 Nếu Gặp Lỗi:

### Lỗi: WebSocket không kết nối
```bash
# Kiểm tra app đã chạy
curl http://localhost:8080/actuator/health

# Xem log
tail -f app.log | grep -i websocket
```

### Lỗi: Không thấy conversation
```sql
-- Kiểm tra user_type
SELECT user_id, username, user_type FROM users WHERE username LIKE 'test_%';

-- Phải có: test_seller1 = SELLER, test_customer1 = CUSTOMER
```

### Lỗi: Không gửi được tin
- Kiểm tra đã login chưa
- Mở F12 → Console → Xem lỗi gì
- Kiểm tra database có bảng `chat_conversations` và `chat_messages` chưa

---

## 📊 Kiểm Tra Database

```sql
-- Xem conversations
SELECT * FROM chat_conversations;

-- Xem messages
SELECT 
    sender_name,
    content,
    is_read,
    created_at 
FROM chat_messages 
ORDER BY created_at DESC 
LIMIT 10;

-- Đếm unread messages
SELECT 
    receiver_id,
    COUNT(*) as unread 
FROM chat_messages 
WHERE is_read = false 
GROUP BY receiver_id;
```

---

## 🎬 Kịch Bản Test Đầy Đủ

### Test 1: Customer → Seller (2 phút)
1. Customer login và gửi 5 tin
2. Seller login và thấy 5 tin (badge = 5)
3. Seller click vào conversation (badge biến mất)
4. Seller trả lời 3 tin
5. Customer thấy 3 tin trả lời ngay lập tức

### Test 2: Multi Customer (3 phút)
1. Customer1 gửi tin
2. Customer2 gửi tin (trình duyệt khác)
3. Customer3 gửi tin (trình duyệt khác)
4. Seller thấy 3 conversations riêng biệt
5. Seller chat với từng customer không bị lẫn

### Test 3: Real-time (1 phút)
1. Để 2 trình duyệt cạnh nhau
2. Customer gửi tin → Seller thấy ngay (< 1s)
3. Seller trả lời → Customer thấy ngay (< 1s)

---

## 📱 Test Trên Nhiều Thiết Bị

### Desktop
- Chrome: Test Customer
- Firefox: Test Seller
- Edge: Test Customer2

### Mobile
- Mở `http://[IP-máy-bạn]:8080` trên điện thoại
- Test responsive UI
- Test gửi/nhận tin trên mobile

---

## 🎯 Checklist Nhanh

Đánh dấu ✅ vào những mục đã test thành công:

**Cơ bản:**
- [ ] Login được Customer
- [ ] Login được Seller  
- [ ] Gửi tin từ Customer → Seller
- [ ] Trả lời từ Seller → Customer
- [ ] Tin nhắn hiện real-time (< 2s)

**Nâng cao:**
- [ ] Unread count đúng
- [ ] Mark as read hoạt động
- [ ] Switch conversation không bị lỗi
- [ ] Refresh trang vẫn giữ tin nhắn
- [ ] Logout rồi login lại vẫn thấy chat

**Performance:**
- [ ] Gửi 10 tin nhanh không lag
- [ ] Có 5 conversations không chậm
- [ ] WebSocket auto-reconnect

---

## 💡 Tips Test Nhanh

### Dùng Postman Test API
```
GET http://localhost:8080/api/sellers
→ Phải trả về danh sách sellers

GET http://localhost:8080/api/conversations/1
→ Phải trả về conversations của user_id=1

POST http://localhost:8080/api/conversation?customerId=1&sellerId=2
→ Tạo conversation mới
```

### Dùng Browser Console Test WebSocket
```javascript
// Mở F12 → Console → Paste code này
console.log('WebSocket Status:', window.stompClient ? 'Connected' : 'Not Connected');
```

### Dùng MySQL Workbench
- Mở table `chat_messages`
- F5 refresh liên tục
- Thấy tin nhắn mới insert real-time

---

## 📞 Cần Trợ Giúp?

Xem file hướng dẫn chi tiết: `HUONG_DAN_TEST_CHAT.md`

Hoặc check documentation: `CHAT_SYSTEM_DOCUMENTATION.md`

---

## ✅ Kết Luận

Nếu tất cả test pass → **Hệ thống chat hoạt động hoàn hảo! 🎉**

Thời gian test tối thiểu: **5 phút**  
Thời gian test đầy đủ: **30 phút**  
Thời gian test tất cả edge cases: **2 giờ**

