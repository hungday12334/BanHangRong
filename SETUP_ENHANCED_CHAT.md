# 🚀 Hướng Dẫn Cài Đặt Enhanced Chat Features

## 📋 Tổng Quan

Hướng dẫn này sẽ giúp bạn cài đặt đầy đủ các tính năng chat nâng cao:
- ✅ Emoji Reactions (❤️ 😂 😢 😡 😮 👍)
- ✅ Reply to Messages (Trả lời tin nhắn)
- ✅ Delete Messages (Xóa tin nhắn)
- ✅ Giao diện đẹp với action toolbar nhỏ gọn

---

## 🔧 Bước 1: Cập Nhật Database

### Option 1: Chạy Script Tự Động (Khuyến nghị)

```bash
cd /Users/hoangquangminh/Desktop/FUlearning/FALL25/SWP391/BanHangRong
./apply-chat-db-update.sh
```

### Option 2: Chạy SQL Thủ Công

1. Mở MySQL Workbench hoặc phpMyAdmin
2. Chọn database `smiledev_wap`
3. Chạy file SQL: `sql/add_enhanced_chat_columns.sql`

Hoặc dùng command line:

```bash
mysql -u root -p smiledev_wap < sql/add_enhanced_chat_columns.sql
```

### ✅ Kiểm Tra Cập Nhật Thành Công

Chạy query sau để xác nhận các cột mới đã được thêm:

```sql
DESCRIBE chat_messages;
```

Bạn sẽ thấy các cột mới:
- `reactions` (TEXT)
- `reply_to_message_id` (VARCHAR 255)
- `reply_to_sender_name` (VARCHAR 255)
- `reply_to_content` (TEXT)
- `deleted` (BOOLEAN)

---

## 📦 Bước 2: Build Lại Backend

```bash
# Clean và build lại project
./mvnw clean package -DskipTests

# Hoặc nếu bạn dùng Maven global
mvn clean package -DskipTests
```

---

## 🚀 Bước 3: Chạy Ứng Dụng

```bash
# Chạy với Spring Boot
./mvnw spring-boot:run

# Hoặc chạy JAR file
java -jar target/su25-0.0.1-SNAPSHOT.jar
```

---

## 🧪 Bước 4: Test Các Tính Năng

### Test Reactions (Emoji)

1. Mở chat giữa customer và seller
2. **Hover chuột** lên bất kỳ tin nhắn nào
3. Thấy 3 icon nhỏ xuất hiện:
   - 😊 (React)
   - ↩️ (Reply)
   - 🗑️ (Delete - chỉ tin nhắn của mình)
4. Click 😊 → Chọn emoji → Reaction xuất hiện

**Vị trí action toolbar:**
- Tin nhắn của đối phương → Icons ở **BÊN PHẢI**
- Tin nhắn của mình → Icons ở **BÊN TRÁI**

### Test Reply (Trả Lời)

1. Hover lên tin nhắn muốn reply
2. Click icon ↩️
3. Reply bar xuất hiện phía trên ô input
4. Gõ tin nhắn reply và gửi
5. Tin nhắn mới hiện quote của tin nhắn gốc
6. Click vào quote → Auto scroll đến tin nhắn gốc

### Test Delete (Xóa)

**Soft Delete:**
1. Hover lên tin nhắn **của mình**
2. Click icon 🗑️
3. Xác nhận xóa
4. Tin nhắn hiện: "This message has been deleted" (mờ đi)

**Permanent Delete:**
1. Hover lên tin nhắn đã bị soft delete
2. Icon 🗑️ xuất hiện
3. Click để xóa vĩnh viễn
4. Tin nhắn biến mất hoàn toàn

---

## 🎨 Giao Diện Mới

### Action Toolbar
```
[Tin nhắn đối phương]  →  😊 ↩️     (Actions bên PHẢI)

    😊 ↩️ 🗑️  ←  [Tin nhắn của mình]  (Actions bên TRÁI)
```

### Icons Size
- Width: 24px
- Height: 24px
- Font size: 14px
- Padding: 3px 6px
- Border radius: 16px

### Reactions Display
```
[Message]
❤️ 3  😂 1  👍 2  ← Click để xem ai đã react
```

### Reply Quote
```
┌─────────────────┐
│ John: Hello!    │ ← Click để jump đến tin nhắn gốc
├─────────────────┤
│ Hi John!        │
└─────────────────┘
```

---

## 🐛 Troubleshooting

### 1. Action toolbar không xuất hiện

**Nguyên nhân:** CSS chưa load đúng

**Giải pháp:**
```bash
# Hard refresh browser
- Chrome/Firefox: Ctrl + Shift + R (Windows) hoặc Cmd + Shift + R (Mac)
- Clear browser cache
```

### 2. Lỗi "Table 'chat_messages' doesn't have column 'reactions'"

**Nguyên nhân:** Chưa chạy SQL update

**Giải pháp:**
```bash
# Chạy lại script update database
./apply-chat-db-update.sh
```

### 3. WebSocket không kết nối

**Kiểm tra:**
```javascript
// Mở browser console (F12)
// Xem logs:
✅ WebSocket Connected
✅ Subscribed to conversation
✅ Subscribed to reactions
✅ Subscribed to deletes
```

**Giải pháp:**
- Restart Spring Boot application
- Check if port 8080 is available
- Clear browser cache

### 4. Icons quá to hoặc vị trí sai

**Kiểm tra CSS:**
```css
.message-action-btn {
    width: 24px;
    height: 24px;
    font-size: 14px;
}

/* Tin nhắn của MÌNH (sent) */
.message-wrapper.sent .message-actions {
    left: -80px;  /* Bên TRÁI */
}

/* Tin nhắn của ĐỐI PHƯƠNG (received) */
.message-wrapper.received .message-actions {
    right: -80px;  /* Bên PHẢI */
}
```

### 5. Reactions không save vào database

**Kiểm tra Backend Logs:**
```bash
# Xem console logs
=== 😊 ADDING REACTION TO MESSAGE ===
Message ID: 123
User ID: 456
Emoji: ❤️
✅ Reaction added successfully
```

**Nếu có lỗi:**
```bash
# Kiểm tra ChatService.java có methods:
- addReaction()
- removeReaction()
- parseReactions()
- stringifyReactions()
```

---

## 📊 Kiểm Tra Database

### Xem Reactions trong Database

```sql
SELECT 
    message_id,
    content,
    reactions,
    reply_to_message_id,
    deleted
FROM chat_messages
WHERE conversation_id = 'conv_1_2'
ORDER BY created_at DESC
LIMIT 10;
```

### Format Reactions JSON

```json
{
  "❤️": ["1", "2", "3"],
  "😂": ["1"],
  "👍": ["2", "3"]
}
```

---

## 🎯 Checklist Hoàn Thành

### Database
- [ ] Chạy SQL script thành công
- [ ] Kiểm tra các cột mới đã tồn tại
- [ ] Test insert/update reactions

### Backend
- [ ] ChatMessage.java có các field mới
- [ ] ChatService.java có methods mới
- [ ] ChatController.java có endpoints mới
- [ ] Build thành công không lỗi

### Frontend (Customer Chat)
- [ ] Action toolbar xuất hiện khi hover
- [ ] Vị trí đúng (bên phải cho received, bên trái cho sent)
- [ ] Icons size 24px
- [ ] Emoji picker hoạt động
- [ ] Reply bar hiển thị đúng
- [ ] Delete confirmation hoạt động

### Frontend (Seller Chat)
- [ ] Tất cả tính năng như customer chat
- [ ] Dark theme hiển thị đẹp
- [ ] Không có lỗi console

### WebSocket
- [ ] Connect thành công
- [ ] Subscribe đến /reactions topic
- [ ] Subscribe đến /deletes topic
- [ ] Real-time updates hoạt động

---

## 📝 Technical Details

### WebSocket Endpoints

```javascript
// Main messages
/app/chat.sendMessage

// Reactions
/app/chat.addReaction
/app/chat.removeReaction

// Delete
/app/chat.deleteMessage
/app/chat.permanentDeleteMessage
```

### WebSocket Topics

```javascript
// Messages
/topic/conversation/{conversationId}

// Reactions updates
/topic/conversation/{conversationId}/reactions

// Delete updates
/topic/conversation/{conversationId}/deletes
```

### Message Data Structure

```javascript
{
    id: 123,
    content: "Hello!",
    senderId: 1,
    receiverId: 2,
    
    // NEW FIELDS
    reactions: {"❤️":["1","2"],"😂":["3"]},
    replyToMessageId: "456",
    replyToSenderName: "John",
    replyToContent: "How are you?",
    deleted: false
}
```

---

## 🎉 Kết Quả Mong Đợi

Sau khi setup thành công, bạn sẽ có:

### ✅ Customer Chat
- Modern UI giống Messenger/WhatsApp
- Action toolbar nhỏ gọn, vị trí hợp lý
- Emoji reactions đầy đủ
- Reply với quote navigation
- Soft & hard delete

### ✅ Seller Chat
- Tất cả tính năng như customer
- Professional dark theme
- Smooth animations

### ✅ Real-time
- Tất cả actions update ngay lập tức
- Cả 2 bên thấy changes đồng thời
- WebSocket connection stable

---

## 🆘 Support

Nếu gặp vấn đề:

1. **Check Console Logs** (Browser F12)
   - Tìm error messages
   - Xem WebSocket status

2. **Check Backend Logs** (Terminal)
   - Tìm stack traces
   - Xem database queries

3. **Verify Database**
   - Run DESCRIBE chat_messages
   - Check if columns exist

4. **Rebuild & Restart**
   ```bash
   ./mvnw clean package -DskipTests
   ./mvnw spring-boot:run
   ```

---

**Setup Version:** 1.0.0  
**Last Updated:** November 1, 2025  
**Status:** ✅ Ready for Production

