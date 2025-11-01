# 🔍 Debug Guide - Seller Chat Không Hiện Conversations

## Vấn đề
Seller login vào chat, cột bên trái (danh sách người đã nhắn) không hiện gì, mặc dù đã có conversations.

## Các bước debug

### Bước 1: Kiểm tra Console Log

1. **Mở Seller Chat**:
   - Login với account seller
   - Vào trang `/seller/chat`

2. **Mở Browser Console** (F12 → Console tab)

3. **Xem logs khi page load**:
   ```
   Nên thấy:
   🔄 Loading conversations for SELLER: [userId]
   📡 Response status: 200
   ✓ Loaded conversations: [số lượng]
   📋 Conversations data: [array of conversations]
   🎨 Rendering conversations list, count: [số lượng]
   ```

4. **Kiểm tra các trường hợp**:

   **Case A: Không thấy log "Loading conversations"**
   → Page không load conversations
   → Check: Page có gọi `loadConversations()` lúc init không?

   **Case B: Response status không phải 200**
   → API lỗi
   → Check: Backend có running không? API endpoint có đúng không?

   **Case C: Loaded conversations: 0**
   → Database không có data hoặc bị filter hết
   → Check: Database có conversations cho seller này không?
   → Check: Conversations có bị mark as deleted không?

   **Case D: Conversations có data nhưng không render**
   → Lỗi trong `renderConversationsList()`
   → Check console có JavaScript error không

### Bước 2: Kiểm tra Network Tab

1. Mở **Network** tab trong DevTools
2. Refresh trang
3. Tìm request: `GET /api/conversations/[sellerId]`
4. Click vào request đó, xem:
   - **Status Code**: Phải là 200
   - **Response**: Xem JSON data có conversations không
   - **Preview**: Xem structure của data

### Bước 3: Kiểm tra Database

```sql
-- 1. Kiểm tra conversations của seller
SELECT * FROM chat_conversations 
WHERE seller_id = [SELLER_USER_ID];

-- 2. Kiểm tra metadata (deleted/pinned status)
SELECT * FROM user_conversation_metadata 
WHERE user_id = [SELLER_USER_ID];

-- 3. Kiểm tra messages
SELECT * FROM chat_messages 
WHERE conversation_id IN (
    SELECT id FROM chat_conversations 
    WHERE seller_id = [SELLER_USER_ID]
)
ORDER BY created_at DESC
LIMIT 10;
```

**Kết quả mong đợi**:
- Có ít nhất 1 conversation
- Metadata không có `is_deleted = TRUE`
- Có messages trong conversation

### Bước 4: Kiểm tra Backend Logs

Xem console output khi seller load chat:

```
Nên thấy:
=== LOADING CONVERSATIONS FOR USER: [sellerId] ===
✓ Found [N] conversations
✓ Loaded [M] messages for conversation [convId]
📌 Conversation is pinned: [convId] (nếu có pinned)
⏭️ Skipping deleted conversation: [convId] (nếu có deleted)
✅ Returning [K] conversations (after filtering deleted)
```

### Bước 5: Kiểm tra HTML Element

Mở **Elements/Inspector** tab:
1. Tìm element: `<div class="conversations-list" id="conversationsList">`
2. Kiểm tra:
   - Element có tồn tại không?
   - Có `style="display:none"` hoặc `hidden` không?
   - Có conversation items bên trong không?

---

## 🔧 Các fix thường gặp

### Fix 1: API không trả về data

**Nguyên nhân**: Backend filter quá nhiều hoặc query sai

**Fix**:
```java
// ChatService.java - Kiểm tra logic này
List<Conversation> conversations = conversationRepository.findConversationsByUserId(userId);
// Đảm bảo query này trả về data cho seller
```

### Fix 2: Tất cả conversations bị deleted

**Nguyên nhân**: Metadata có `is_deleted = TRUE`

**Fix**:
```sql
-- Reset deleted status
UPDATE user_conversation_metadata 
SET is_deleted = FALSE 
WHERE user_id = [SELLER_USER_ID];
```

### Fix 3: JavaScript error

**Nguyên nhân**: Thiếu escapeHtml function hoặc lỗi syntax

**Fix**: Check console có error không, sửa theo error message

### Fix 4: CSS ẩn conversations list

**Nguyên nhân**: CSS rules ẩn element

**Fix**:
```css
/* Check CSS không có rules như: */
.conversations-list { display: none; } /* Remove this */
```

---

## 🎯 Quick Test Script

Chạy trong Browser Console để test:

```javascript
// 1. Check conversations array
console.log('Conversations:', conversations);

// 2. Check list element
const list = document.getElementById('conversationsList');
console.log('List element:', list);
console.log('List innerHTML:', list?.innerHTML);

// 3. Force render
renderConversationsList();

// 4. Check currentUser
console.log('Current user:', currentUser);
console.log('Is seller?', currentUser.userType === 'SELLER');

// 5. Force reload conversations
loadConversations().then(() => {
    console.log('Reloaded conversations:', conversations.length);
});
```

---

## 📞 Các scenario thường gặp

### Scenario 1: Seller mới, chưa có conversation nào
**Dấu hiệu**: 
- API trả về empty array `[]`
- Console log: "Loaded conversations: 0"

**Giải pháp**: 
- Bình thường! Seller chưa nhận tin nhắn nào
- Để customer gửi tin nhắn đến seller → conversation sẽ xuất hiện

### Scenario 2: Conversations bị deleted hết
**Dấu hiệu**: 
- Database có conversations
- API trả về empty array `[]`
- Backend log: "Skipping deleted conversation"

**Giải pháp**:
```sql
-- Xem conversations nào bị deleted
SELECT * FROM user_conversation_metadata 
WHERE user_id = [SELLER_ID] AND is_deleted = TRUE;

-- Reset nếu cần
UPDATE user_conversation_metadata 
SET is_deleted = FALSE 
WHERE user_id = [SELLER_ID];
```

### Scenario 3: API lỗi 500
**Dấu hiệu**: 
- Network tab: Status 500
- Console: Fetch error

**Giải pháp**:
- Check backend logs
- Thường là database connection hoặc query error
- Fix theo error message trong backend console

### Scenario 4: Element không tồn tại
**Dấu hiệu**: 
- Console: "conversationsList element not found!"

**Giải pháp**:
- Check HTML có `<div id="conversationsList"></div>` không
- Có thể bị duplicate ID hoặc typo

---

## ✅ Checklist

Để chắc chắn seller chat hoạt động:

- [ ] Backend running (port 8080)
- [ ] Seller account có userType = "SELLER"
- [ ] Database có ít nhất 1 conversation với seller_id = [seller userId]
- [ ] Conversation không bị mark as deleted trong user_conversation_metadata
- [ ] API `/api/conversations/[sellerId]` trả về 200 với data
- [ ] Browser console không có JavaScript errors
- [ ] HTML có element `<div id="conversationsList">`
- [ ] CSS không hide conversations-list

---

## 🚀 Test nhanh

1. **Tạo conversation mới**:
   - Login as customer
   - Chat với seller
   - Gửi 1 message

2. **Kiểm tra seller**:
   - Login as seller
   - Vào chat
   - Nên thấy conversation với customer

3. **Nếu vẫn không thấy**:
   - Clear browser cache
   - Hard refresh (Ctrl+Shift+R)
   - Check console logs theo hướng dẫn trên

---

**Updated**: November 2, 2025  
**Build**: ✅ Latest build includes debug logs  
**Status**: Ready to debug with enhanced logging

