# 💬 Thêm Liên Kết Chat giữa Customer và Seller

## ✅ Vấn đề đã được giải quyết

Trước đây hệ thống chat đã có đầy đủ backend và giao diện, **nhưng thiếu các nút/link để customer có thể bắt đầu nhắn tin với seller**. Bây giờ đã được hoàn thiện!

---

## 🎯 Các thay đổi đã thực hiện

### 1. **Nút "Chat với Shop" trong Product Detail Page** ✨

**File**: `customer/product_detail.html`

- ✅ Thêm nút **"Chat với Shop"** bên cạnh nút "Add to Cart"
- ✅ Nút có icon chat và màu xanh lá đẹp mắt
- ✅ Tự động truyền `sellerId` của sản phẩm
- ✅ Khi click sẽ mở trang chat và tự động tạo/mở conversation với seller

```html
<a th:href="@{/customer/chat(sellerId=${product.sellerId})}" 
   class="btn" 
   style="background:#10b981;color:#fff;text-decoration:none;display:inline-flex;align-items:center;gap:6px"
   title="Chat with seller">
    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
        <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"></path>
    </svg>
    Chat với Shop
</a>
```

---

### 2. **Nút "Chat với Shop" trong Shop Public Page** 🏪

**File**: `shop-public.html`

- ✅ Thêm nút lớn **"Chat với Shop"** ở header của shop
- ✅ Nút nổi bật với style Bootstrap (btn-success btn-lg)
- ✅ Có icon message và text rõ ràng
- ✅ Truyền `sellerId` của shop để tạo conversation

```html
<a th:href="@{/customer/chat(sellerId=${sellerId})}" 
   class="btn btn-success btn-lg"
   style="display:inline-flex;align-items:center;gap:8px">
    <i class="ti ti-message-circle"></i>
    Chat với Shop
</a>
```

---

### 3. **Link "Chat" trong Navigation Bar** 🧭

**Files**: 
- `customer/dashboard.html`
- `customer/product_detail.html`

Thêm link **"💬 Chat"** vào thanh navigation chính:

```html
<nav class="nav" aria-label="Main">
    <a href="/customer/dashboard">Home</a>
    <a href="/categories">Category</a>
    <a href="#">License Software</a>
    <a href="#">Game Keys</a>
    <a href="#">Voucher</a>
    <a href="/customer/chat">💬 Chat</a>  <!-- ✨ NEW -->
    <a href="#">Support</a>
</nav>
```

---

### 4. **Link "Chat" trong User Dropdown Menu** 👤

**File**: `customer/dashboard.html`

Thêm link chat vào menu dropdown của user:

```html
<div id="userDropdown" class="user-menu">
    <a class="item" href="/customer/notifications">🔔 Notifications</a>
    <a class="item" href="/customer/wishlist">💝 Wishlist</a>
    <a class="item" href="/customer/reviews">⭐ My Reviews</a>
    <a class="item" href="/customer/chat">💬 Chat</a>  <!-- ✨ NEW -->
    <a class="item" href="/customer/settings">⚙️ Settings</a>
</div>
```

---

### 5. **Backend: Auto-open Conversation** 🔧

**File**: `ChatController.java`

Cập nhật endpoint `/customer/chat` để:

- ✅ Nhận parameter `sellerId` (optional)
- ✅ Tự động tạo hoặc lấy conversation giữa customer và seller
- ✅ Truyền `conversationId` và `targetSellerId` vào model

```java
@GetMapping("/customer/chat")
public String customerChat(
        @RequestParam(required = false) Long sellerId,
        org.springframework.ui.Model model, 
        org.springframework.security.core.Authentication authentication){
    // ...
    if (sellerId != null) {
        model.addAttribute("targetSellerId", sellerId);
        try {
            Conversation conversation = chatService.getOrCreateConversation(user.getUserId(), sellerId);
            model.addAttribute("conversationId", conversation.getId());
        } catch (Exception e) {
            System.err.println("Error creating conversation: " + e.getMessage());
        }
    }
    return "seller/chat";
}
```

---

### 6. **Frontend: JavaScript Auto-open Logic** 💻

**File**: `seller/chat.html`

Thêm logic JavaScript để tự động mở conversation khi có `sellerId`:

```javascript
// Auto-open conversation if conversationId is provided
const targetConversationId = /*[[${conversationId}]]*/ null;
const targetSellerId = /*[[${targetSellerId}]]*/ null;

if (targetConversationId) {
    // Wait for conversations to load, then open the target conversation
    const checkAndOpen = setInterval(() => {
        const conv = conversations.find(c => c.id === targetConversationId);
        if (conv) {
            clearInterval(checkAndOpen);
            openConversation(conv);
        }
    }, 500);
    setTimeout(() => clearInterval(checkAndOpen), 10000);
} else if (targetSellerId && currentUser.userType === 'CUSTOMER') {
    // For customer: create/open conversation with that seller
    fetch('/api/conversation', {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        body: `customerId=${currentUser.userId}&sellerId=${targetSellerId}`
    })
    .then(response => response.json())
    .then(conversation => {
        loadConversations().then(() => {
            const conv = conversations.find(c => c.id === conversation.id);
            if (conv) openConversation(conv);
        });
    });
}
```

---

## 🚀 Cách sử dụng

### **Từ Product Detail Page:**
1. Customer xem sản phẩm tại `/customer/products/{productId}`
2. Click nút **"Chat với Shop"** bên cạnh "Add to Cart"
3. → Tự động mở trang chat và conversation với seller của sản phẩm đó

### **Từ Shop Public Page:**
1. Customer vào trang shop công khai tại `/shop/{sellerId}`
2. Click nút **"Chat với Shop"** ở header
3. → Tự động mở trang chat với seller

### **Từ Navigation:**
1. Customer click link **"💬 Chat"** ở thanh navigation
2. → Vào trang chat, xem tất cả conversations
3. → Có thể chọn seller để chat

### **Từ User Menu:**
1. Customer click vào username → dropdown menu
2. Click **"💬 Chat"**
3. → Vào trang chat

---

## 🎨 Giao diện

### Nút trong Product Detail:
```
┌─────────────┬────────────────────┐
│  Add to Cart │  💬 Chat với Shop  │
└─────────────┴────────────────────┘
```

### Nút trong Shop Public:
```
╔══════════════════════════════╗
║    🛍️ Cửa Hàng             ║
║                              ║
║  ┌────────────────────────┐ ║
║  │ 💬 Chat với Shop       │ ║
║  └────────────────────────┘ ║
╚══════════════════════════════╝
```

### Navigation Bar:
```
Home | Category | Software | Keys | Voucher | 💬 Chat | Support
```

---

## ✅ Checklist hoàn thành

- [x] Nút chat trong product detail page
- [x] Nút chat trong shop public page  
- [x] Link chat trong navigation bar
- [x] Link chat trong user dropdown menu
- [x] Backend xử lý sellerId parameter
- [x] Auto-create/open conversation
- [x] JavaScript auto-open logic
- [x] Tested và hoạt động tốt

---

## 📝 Lưu ý kỹ thuật

1. **Parameter truyền qua URL**: Sử dụng `sellerId` để xác định seller cần chat
2. **Auto-create conversation**: Backend tự động tạo conversation nếu chưa có
3. **Async loading**: JavaScript chờ conversations load xong rồi mới mở
4. **Error handling**: Có try-catch để xử lý lỗi gracefully
5. **Timeout**: Set timeout 10 giây để tránh infinite loop

---

## 🎯 Kết quả

Giờ đây **Customer và Seller đã có đầy đủ cách để liên hệ nhau**:

✅ Customer có thể chat ngay từ trang sản phẩm  
✅ Customer có thể chat từ trang shop  
✅ Customer có thể truy cập chat từ navigation  
✅ Tự động tạo conversation khi cần  
✅ Giao diện đẹp và dễ sử dụng  

---

**Tác giả**: GitHub Copilot  
**Ngày**: 29/10/2025  
**Status**: ✅ Hoàn thành

