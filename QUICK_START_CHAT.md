# 🚀 Quick Start: Chat Feature

## ✅ Đã thêm các link chat cho Customer và Seller!

---

## 📍 Vị trí các nút Chat

### 1️⃣ **Product Detail Page** (`/customer/products/{id}`)
```
┌─────────────────────────────────────┐
│  Tên sản phẩm                       │
│  Giá: 100,000₫                      │
│                                     │
│  [Quantity: 1] [Add to Cart]       │
│  [💬 Chat với Shop] ← NEW!         │
└─────────────────────────────────────┘
```

### 2️⃣ **Shop Public Page** (`/shop/{sellerId}`)
```
┌─────────────────────────────────────┐
│         🛍️ Cửa Hàng                │
│   Chào mừng đến với cửa hàng       │
│                                     │
│   [💬 Chat với Shop] ← NEW!        │
└─────────────────────────────────────┘
```

### 3️⃣ **Navigation Bar** (Tất cả trang customer)
```
Home | Category | Software | 💬 Chat | Support
                              ↑ NEW!
```

### 4️⃣ **User Dropdown Menu**
```
┌─────────────────┐
│ My Account      │
│ 🔔 Notifications│
│ 💝 Wishlist     │
│ ⭐ My Reviews   │
│ 💬 Chat ← NEW!  │
│ ⚙️ Settings     │
└─────────────────┘
```

---

## 🎯 Test Flow

### **Test 1: Chat từ Product Detail**
1. Login as **Customer**
2. Vào `/customer/products/{productId}` (bất kỳ sản phẩm nào)
3. Click nút **"Chat với Shop"** (màu xanh lá)
4. ✅ Phải mở trang chat và tự động mở conversation với seller

### **Test 2: Chat từ Shop Public**
1. Login as **Customer**
2. Vào `/shop/{sellerId}`
3. Click nút **"Chat với Shop"** ở header
4. ✅ Phải mở trang chat với seller đó

### **Test 3: Chat từ Navigation**
1. Login as **Customer**
2. Click **"💬 Chat"** ở navigation bar
3. ✅ Mở trang chat, hiển thị tất cả conversations

### **Test 4: Chat từ User Menu**
1. Login as **Customer**
2. Click vào username → dropdown
3. Click **"💬 Chat"**
4. ✅ Mở trang chat

### **Test 5: Seller Chat**
1. Login as **Seller**
2. Click icon chat ở sidebar (hoặc `/seller/chat`)
3. ✅ Thấy danh sách customers đã chat
4. ✅ Có thể reply messages

---

## 📂 Files đã thay đổi

```
✅ ChatController.java - Backend xử lý sellerId
✅ customer/dashboard.html - Thêm chat link
✅ customer/product_detail.html - Thêm nút chat + link
✅ shop-public.html - Thêm nút chat
✅ seller/chat.html - Auto-open conversation logic
```

---

## 🔍 URLs quan trọng

| URL | Mô tả |
|-----|-------|
| `/customer/chat` | Trang chat cho customer |
| `/customer/chat?sellerId=1` | Chat với seller ID=1 |
| `/seller/chat` | Trang chat cho seller |
| `/api/conversation` | API tạo conversation |
| `/api/conversations/{userId}` | API lấy danh sách conversations |

---

## 🛠️ Nếu có lỗi

### **Lỗi: "Conversation not found"**
- Kiểm tra sellerId có tồn tại không
- Check database table `conversations`

### **Lỗi: "User not logged in"**
- Đảm bảo đã login trước khi chat
- Check authentication trong controller

### **Lỗi: WebSocket không connect**
- Restart server
- Check console browser F12

---

## ✅ Checklist Test

- [ ] Customer có thể click "Chat với Shop" trong product detail
- [ ] Customer có thể click "Chat với Shop" trong shop public
- [ ] Customer có thể click "💬 Chat" ở navigation
- [ ] Customer có thể click "💬 Chat" ở user menu
- [ ] Conversation tự động được tạo khi chat lần đầu
- [ ] Chat page tự động mở conversation đúng
- [ ] Seller có thể thấy và reply messages
- [ ] WebSocket hoạt động real-time

---

**Build Status**: ✅ BUILD SUCCESS  
**Ready to test**: ✅ YES

