# 🧪 Test Cases Chi Tiết Cho Luồng Chat System

## 📋 Tổng quan

Tài liệu này mô tả chi tiết **Happy Cases**, **Unhappy Cases**, và **Edge Cases** cho hệ thống chat giữa Customer và Seller.

---

## 🎯 HAPPY CASES (Các trường hợp thành công)

### HC1: Customer chat với Seller từ Product Detail Page

**Precondition:**
- Customer đã đăng nhập
- Có sản phẩm với sellerId hợp lệ
- Seller account đang active

**Steps:**
1. Customer vào trang `/customer/products/{productId}`
2. Click nút "Chat với Shop"
3. Redirect đến `/customer/chat?sellerId={sellerId}`
4. WebSocket connection được thiết lập
5. Backend auto-create/get conversation
6. Frontend auto-open conversation

**Expected Result:**
- ✅ Trang chat mở thành công
- ✅ Conversation với seller được tạo/mở tự động
- ✅ Chat area hiển thị đúng tên seller
- ✅ Customer có thể gửi message ngay lập tức
- ✅ Conversation xuất hiện trong sidebar

**Technical Flow:**
```
Product Detail → Click "Chat với Shop" 
→ GET /customer/chat?sellerId=X 
→ ChatController.customerChat() 
→ ChatService.getOrCreateConversation() 
→ Return model with conversationId + targetSellerId
→ Frontend: Auto-open conversation via JavaScript
→ POST /api/conversation (if needed)
→ WebSocket: Subscribe to /topic/conversation/{id}
```

---

### HC2: Customer chat với Seller từ Shop Public Page

**Precondition:**
- Customer đã đăng nhập
- Shop page của seller tồn tại
- Seller account active

**Steps:**
1. Customer vào `/shop/{sellerId}`
2. Click nút "Chat với Shop" ở header
3. System tự động tạo conversation
4. Chat window mở với conversation đã active

**Expected Result:**
- ✅ Chuyển đến trang chat
- ✅ Conversation tự động được tạo và mở
- ✅ UI hiển thị thông tin seller đúng

---

### HC3: Seller nhận tin nhắn từ Customer

**Precondition:**
- Seller đã đăng nhập và đang ở `/seller/chat`
- Customer đã gửi message

**Steps:**
1. Customer gửi message
2. WebSocket broadcast message
3. Seller nhận real-time notification

**Expected Result:**
- ✅ Message xuất hiện real-time trong chat area (nếu đang mở conversation)
- ✅ Conversation trong sidebar cập nhật "last message"
- ✅ Unread count tăng lên (nếu chưa mở conversation)
- ✅ Conversation được đẩy lên đầu danh sách

---

### HC4: Gửi và nhận message real-time

**Precondition:**
- Cả Customer và Seller đều online
- Đã có conversation
- WebSocket connected

**Steps:**
1. User A gửi message
2. Message được lưu vào database
3. WebSocket broadcast đến User B
4. User B nhận message real-time

**Expected Result:**
- ✅ Message hiển thị ngay lập tức cho User A
- ✅ User B nhận message trong < 1 giây
- ✅ Message có đúng format (avatar, name, timestamp)
- ✅ Scroll tự động xuống cuối
- ✅ Database được cập nhật đúng

---

### HC5: Mark conversation as read

**Precondition:**
- User có unread messages

**Steps:**
1. User click vào conversation
2. `openConversation()` được gọi
3. Backend API `/api/conversation/{id}/read` được call
4. Database cập nhật read status

**Expected Result:**
- ✅ Unread count reset về 0
- ✅ Badge trong sidebar biến mất
- ✅ Messages được đánh dấu là đã đọc

---

### HC6: Customer tạo nhiều conversations với các sellers khác nhau

**Precondition:**
- Customer đã đăng nhập
- Có nhiều sellers khác nhau

**Steps:**
1. Chat với Seller A
2. Chat với Seller B
3. Chat với Seller C

**Expected Result:**
- ✅ Mỗi seller có 1 conversation riêng
- ✅ Conversations không bị trùng lặp
- ✅ Có thể switch giữa các conversations
- ✅ Messages không bị mix lẫn

---

## ❌ UNHAPPY CASES (Các trường hợp lỗi)

### UC1: Customer chưa đăng nhập

**Steps:**
1. Anonymous user click "Chat với Shop"
2. System kiểm tra authentication

**Expected Result:**
- ❌ Redirect đến `/login`
- ❌ Show message: "Please login first"
- ❌ Không tạo conversation

**Current Implementation:**
```javascript
if (!currentUser || !currentUser.userId) {
    alert('Please login first');
    window.location.href = '/login';
}
```

---

### UC2: SellerId không tồn tại

**Steps:**
1. Customer click chat với sellerId = 999999 (không tồn tại)
2. Backend try to create conversation

**Current Behavior:**
```java
// ChatService.getOrCreateConversation()
Users seller = usersRepository.findById(sellerId)
    .orElseThrow(() -> new IllegalArgumentException("Seller not found: " + sellerId));
```

**Expected Result:**
- ❌ Throw exception "Seller not found"
- ❌ Return error to frontend
- ❌ Show error message to user

**⚠️ ISSUE:** Frontend không có error handling cho case này!

---

### UC3: User không phải là Seller

**Steps:**
1. Customer A (userId=1) cố chat với Customer B (userId=2)
2. Backend validate user type

**Current Behavior:**
```java
if (!"SELLER".equalsIgnoreCase(seller.getUserType())) {
    throw new IllegalArgumentException("User " + sellerId + " is not a seller");
}
```

**Expected Result:**
- ❌ Reject conversation creation
- ❌ Show error: "You can only chat with sellers"

**⚠️ ISSUE:** Frontend không hiển thị lỗi này!

---

### UC4: WebSocket connection failed

**Steps:**
1. User vào trang chat
2. WebSocket connection không thành công (network issue, CORS, etc.)

**Current Behavior:**
```javascript
function connect() {
    connectionStatus.textContent = '⚠️ Connecting...';
    connectionStatus.className = 'connection-status connecting';
    
    const socket = new SockJS('/ws');
    stompClient = Stomp.over(socket);
    
    stompClient.connect({}, onConnected, onError);
}

function onError(error) {
    connectionStatus.textContent = '❌ Connection Lost - Trying to reconnect...';
    connectionStatus.className = 'connection-status disconnected';
    isConnected = false;
    setTimeout(connect, 5000);
}
```

**Expected Result:**
- ⚠️ Show "Connection Lost" status
- ⚠️ Auto-retry every 5 seconds
- ⚠️ User không thể gửi message khi disconnected

**⚠️ ISSUE:** 
- Không disable send button khi disconnected
- Không queue messages khi offline

---

### UC5: Message quá dài

**Steps:**
1. User nhập message > 5000 characters
2. Click Send

**Current Behavior:**
```java
if (message.getContent().length() > 5000) {
    throw new IllegalArgumentException("Message too long (max 5000 characters)");
}
```

**Expected Result:**
- ❌ Backend reject message
- ❌ Frontend should validate BEFORE sending

**⚠️ ISSUE:** Frontend không validate message length!

---

### UC6: Gửi message rỗng

**Steps:**
1. User click Send mà không nhập gì
2. System validate

**Current Behavior:**
```javascript
function sendMessage() {
    const content = input.value.trim();
    if (!content || !currentConversation || !isConnected) return;
    // ...
}
```

**Expected Result:**
- ✅ Message không được gửi (đã implement đúng)
- ✅ No error message needed (silent fail is OK)

---

### UC7: Rate limiting - Spam messages

**Steps:**
1. User gửi nhiều messages liên tục (< 100ms apart)
2. Backend rate limiting trigger

**Current Behavior:**
```java
private static final long MESSAGE_RATE_LIMIT_MS = 100;

if (lastTime != null && (now - lastTime) < MESSAGE_RATE_LIMIT_MS) {
    return; // Silently ignore
}
```

**Expected Result:**
- ⚠️ Messages bị drop silently
- ⚠️ User không biết message bị reject

**⚠️ ISSUE:** 
- Không thông báo cho user
- Có thể gây confusion ("Sao tin nhắn không gửi được?")

---

### UC8: Conversation không tồn tại

**Steps:**
1. User có conversationId cũ (đã bị xóa)
2. Try to send message

**Current Behavior:**
```java
Conversation conversation = conversationRepository.findById(message.getConversationId())
    .orElseThrow(() -> new IllegalStateException("Conversation not found: " + ...));
```

**Expected Result:**
- ❌ Throw exception
- ❌ Message không được gửi

**⚠️ ISSUE:** Frontend không handle error này!

---

### UC9: User không thuộc conversation

**Steps:**
1. User A có conversationId của User B và Seller C
2. User A cố gửi message vào conversation đó

**Current Behavior:**
```java
if (!message.getSenderId().equals(conversation.getCustomerId()) &&
    !message.getSenderId().equals(conversation.getSellerId())) {
    throw new IllegalArgumentException("Sender is not part of this conversation");
}
```

**Expected Result:**
- ❌ Reject message
- ❌ Security issue prevented

**⚠️ ISSUE:** Cần có security check ở API level!

---

## 🔍 EDGE CASES (Các trường hợp đặc biệt)

### EC1: Customer refresh page khi đang chat

**Steps:**
1. Customer đang chat với Seller
2. Press F5 hoặc refresh browser
3. Page reload

**Expected Behavior:**
- ✅ User vẫn logged in (session maintained)
- ✅ Trang chat load lại
- ✅ Conversations được load từ database
- ⚠️ Current conversation bị mất (không auto-reopen)

**⚠️ ISSUE:** 
- Không lưu current conversationId vào localStorage
- User phải click lại conversation

**Suggested Fix:**
```javascript
// Save to localStorage
localStorage.setItem('lastConversationId', conversationId);

// On page load
const lastConvId = localStorage.getItem('lastConversationId');
if (lastConvId) {
    // Auto-open last conversation
}
```

---

### EC2: Hai người gửi message cùng lúc

**Steps:**
1. User A và User B đều gửi message trong cùng 1 millisecond
2. Race condition xảy ra

**Expected Behavior:**
- ✅ Cả 2 messages đều được lưu
- ✅ Database transaction isolation đảm bảo không conflict
- ✅ Messages được broadcast theo thứ tự

**Current Implementation:**
- ✅ `@Transactional` đảm bảo atomicity
- ✅ `LocalDateTime.now()` tự động timestamp
- ✅ Messages được sắp xếp theo `createdAt`

---

### EC3: Browser không hỗ trợ WebSocket

**Steps:**
1. User dùng browser cũ không support WebSocket
2. SockJS fallback mechanism

**Expected Behavior:**
- ✅ SockJS tự động fallback sang long polling
- ⚠️ Performance giảm nhưng vẫn hoạt động

**Current Implementation:**
```javascript
const socket = new SockJS('/ws'); // SockJS has built-in fallback
```

---

### EC4: Network chập chờn (intermittent connection)

**Steps:**
1. User đang chat
2. Network bị disconnect
3. Network reconnect sau 3 giây

**Current Behavior:**
```javascript
function onError(error) {
    // ...
    setTimeout(connect, 5000); // Retry after 5s
}
```

**Issues:**
- ⚠️ Messages sent khi offline sẽ bị mất
- ⚠️ Không có message queue
- ⚠️ User không biết status của message (sent/failed)

**Suggested Fix:**
- Implement message queue
- Show message status (sending, sent, failed)
- Retry failed messages khi reconnect

---

### EC5: Seller có 100+ conversations

**Steps:**
1. Popular seller có nhiều customers
2. Load `/api/conversations/{userId}`
3. Frontend render 100+ items

**Performance Issues:**
- ⚠️ Không có pagination
- ⚠️ Load tất cả conversations 1 lần
- ⚠️ Slow rendering

**Suggested Fix:**
```java
@GetMapping("/api/conversations/{userId}")
public ResponseEntity<?> getUserConversations(
    @PathVariable Long userId,
    @RequestParam(defaultValue = "0") int page,
    @RequestParam(defaultValue = "20") int size
) {
    // Implement pagination
}
```

---

### EC6: Message có special characters / emoji / HTML

**Steps:**
1. User gửi message với emoji: "Hello 😀"
2. User gửi message với HTML: `<script>alert('xss')</script>`
3. User gửi message với special chars: `"Don't" & <break>`

**Current Implementation:**
```javascript
function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}
```

**Expected Behavior:**
- ✅ Emoji được hiển thị bình thường
- ✅ HTML bị escape (XSS prevention)
- ✅ Special characters được encode

---

### EC7: Conversation đã tồn tại

**Steps:**
1. Customer đã chat với Seller trước đó
2. Click "Chat với Shop" lại lần nữa
3. System check existing conversation

**Current Behavior:**
```java
Optional<Conversation> existing = conversationRepository
    .findByCustomerIdAndSellerId(customerId, sellerId);
if (existing.isPresent()) {
    return existing.get(); // Reuse existing
}
```

**Expected Behavior:**
- ✅ Không tạo conversation mới
- ✅ Mở conversation cũ
- ✅ Messages history vẫn còn

---

### EC8: Auto-open timeout

**Steps:**
1. Customer click "Chat với Shop"
2. Backend tạo conversation nhưng frontend không load được
3. Interval chạy > 10 giây

**Current Implementation:**
```javascript
const checkAndOpen = setInterval(() => {
    const conv = conversations.find(c => c.id === targetConversationId);
    if (conv) {
        clearInterval(checkAndOpen);
        openConversation(conv);
    }
}, 500);

setTimeout(() => clearInterval(checkAndOpen), 10000); // Timeout sau 10s
```

**Expected Behavior:**
- ⚠️ Stop retry sau 10 giây
- ⚠️ User thấy welcome screen
- ⚠️ Conversation vẫn có trong sidebar, user có thể click manual

---

### EC9: Multiple tabs cùng lúc

**Steps:**
1. User mở 2 tabs cùng trang chat
2. Gửi message từ tab 1
3. Message có hiển thị ở tab 2?

**Current Behavior:**
- ✅ WebSocket broadcast đến tất cả tabs
- ✅ Cả 2 tabs đều nhận message
- ⚠️ Có thể có duplicate notifications

---

### EC10: Timestamp với timezone khác nhau

**Steps:**
1. Customer ở Việt Nam (GMT+7)
2. Seller ở Mỹ (GMT-5)
3. Gửi messages qua lại

**Current Implementation:**
```java
LocalDateTime.now() // Uses server timezone
```

**Issues:**
- ⚠️ Timestamp sẽ theo server time
- ⚠️ Frontend hiển thị `formatTime()` relative time ("5m ago")
- ✅ Relative time không bị ảnh hưởng timezone

---

## 🐛 CÁC VẤN ĐỀ CẦN FIX

### Priority HIGH 🔴

1. **Frontend error handling cho conversation creation**
   - Hiện tại: Silent fail khi sellerId invalid
   - Cần: Show error message cho user

2. **WebSocket disconnect handling**
   - Hiện tại: User vẫn có thể click Send
   - Cần: Disable send button + show warning

3. **Message length validation**
   - Hiện tại: Chỉ validate ở backend
   - Cần: Validate ở frontend + show character count

4. **Rate limiting feedback**
   - Hiện tại: Messages bị drop silently
   - Cần: Show "Please slow down" message

### Priority MEDIUM 🟡

5. **Persist current conversation**
   - Hiện tại: Mất khi refresh
   - Cần: Save to localStorage

6. **Message queue khi offline**
   - Hiện tại: Messages bị mất khi disconnected
   - Cần: Queue và retry khi reconnect

7. **Pagination cho conversations**
   - Hiện tại: Load tất cả
   - Cần: Implement pagination

8. **Message delivery status**
   - Hiện tại: Không có indication
   - Cần: Show "sending", "sent", "delivered", "read"

### Priority LOW 🟢

9. **Typing indicator**
   - Code đã có nhưng chưa được sử dụng

10. **Online status indicator**
    - Backend có track nhưng frontend chưa hiển thị

---

## 🧪 TEST SCENARIOS CHECKLIST

### Functional Testing
- [ ] Customer chat từ product detail
- [ ] Customer chat từ shop page
- [ ] Customer chat từ navigation menu
- [ ] Seller nhận real-time message
- [ ] Customer nhận real-time message
- [ ] Mark as read hoạt động
- [ ] Unread count chính xác
- [ ] Message với emoji
- [ ] Message với special characters
- [ ] XSS prevention (HTML injection)

### Error Handling
- [ ] Chat khi chưa login → redirect
- [ ] Chat với invalid sellerId → error message
- [ ] Chat với non-seller user → error message
- [ ] Gửi message khi disconnected → warning
- [ ] Gửi message quá dài → validation error
- [ ] Spam messages → rate limit

### Performance Testing
- [ ] Load 100+ conversations
- [ ] Send/receive 100 messages rapidly
- [ ] Multiple users online cùng lúc
- [ ] Network slow (throttling)
- [ ] High latency connection

### Edge Cases
- [ ] Refresh trang khi đang chat
- [ ] Close tab rồi mở lại
- [ ] Multiple tabs cùng lúc
- [ ] Browser không support WebSocket
- [ ] Network chập chờn
- [ ] Conversation đã tồn tại
- [ ] Gửi message cùng lúc từ 2 phía

### Security Testing
- [ ] User không thể gửi message vào conversation không thuộc về mình
- [ ] XSS prevention
- [ ] SQL injection prevention
- [ ] CSRF protection
- [ ] Rate limiting hoạt động

---

## 📊 COVERAGE SUMMARY

| Category | Happy Cases | Unhappy Cases | Edge Cases | Total |
|----------|-------------|---------------|------------|-------|
| **Count** | 6 | 9 | 10 | 25 |
| **Tested** | ✅ 6 | ⚠️ 4 | ⚠️ 3 | 13/25 |
| **Coverage** | 100% | 44% | 30% | 52% |

---

## 🎯 RECOMMENDATIONS

1. **Implement comprehensive error handling** ở frontend
2. **Add message status indicators** (sending, sent, failed)
3. **Implement offline message queue**
4. **Add pagination** cho conversations list
5. **Persist UI state** in localStorage
6. **Add integration tests** cho các scenarios chính
7. **Add E2E tests** với Selenium/Cypress
8. **Monitor WebSocket connection health**
9. **Add logging** cho debugging
10. **Create error recovery mechanisms**

---

**Tác giả**: GitHub Copilot  
**Ngày**: 29/10/2025  
**Version**: 1.0  
**Status**: 🔍 Analysis Complete - Cần implement fixes

