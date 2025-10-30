# Chat Loader Fix - "Initializing..." Issue

## Vấn đề (Problem)
- Khi bấm vào chat với shop, màn hình hiển thị "Initializing..." mãi mãi
- Phần chat bị mờ/disabled ở phía sau loader
- Chat không thể sử dụng được

## Nguyên nhân (Root Cause)
- Body tag có class `loading` được set ban đầu
- JavaScript không remove class `loading` sau khi kết nối và load xong conversations
- Loader overlay vẫn hiển thị vì CSS rule: `body:not(.loading) .app-loader { opacity: 0; pointer-events: none; }`

## Giải pháp (Solution)

### 1. Customer Chat (`customer/chat.html`)
Đã thêm code để remove loading class ở 3 điểm:

#### a) Sau khi load conversations xong:
```javascript
// Initialize
connect();
loadConversations().then(() => {
    // Remove loading state after conversations are loaded
    setTimeout(() => {
        document.body.classList.remove('loading');
    }, 500);
}).catch(() => {
    // Remove loading state even if there's an error
    document.body.classList.remove('loading');
});
```

#### b) Sau khi WebSocket connect thành công:
```javascript
function(frame) {
    console.log('Connected:', frame);
    isConnected = true;
    updateConnectionStatus('connected');

    // Hide loader on connection
    document.body.classList.remove('loading');
    
    // ...existing code...
}
```

#### c) Khi WebSocket connect thất bại:
```javascript
function(error) {
    console.error('STOMP error:', error);
    isConnected = false;
    updateConnectionStatus('disconnected');
    
    // Hide loader even on error
    document.body.classList.remove('loading');
    
    setTimeout(connect, 5000);
}
```

### 2. Seller Chat (`seller/chat.html`)
Áp dụng các fix tương tự như customer chat ở cả 3 điểm trên.

## Kết quả (Result)
✅ Loader "Initializing..." sẽ biến mất sau khi:
- WebSocket kết nối thành công, HOẶC
- Conversations load xong (sau 500ms), HOẶC
- Có lỗi xảy ra

✅ Chat interface sẽ không còn bị mờ/disabled
✅ User có thể sử dụng chat bình thường

## Cách test (How to Test)
1. Build project: `./mvnw clean package -DskipTests`
2. Run application: `java -jar target/su25-0.0.1-SNAPSHOT.jar`
3. Login vào hệ thống
4. Click vào "Chat" hoặc "Chat with shop"
5. Kiểm tra loader biến mất và chat hoạt động bình thường

## Files Changed
- `/src/main/resources/templates/customer/chat.html`
- `/src/main/resources/templates/seller/chat.html`

## Ngày sửa (Date Fixed)
30/10/2025

