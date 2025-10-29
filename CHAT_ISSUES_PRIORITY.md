# 🚨 Chat System Issues - Priority List

## 🔴 CRITICAL (Fix ngay)

### 1. Frontend Error Handling cho Invalid SellerId
**Vấn đề:**
- Khi click "Chat với Shop" với sellerId không tồn tại → không có thông báo lỗi
- Backend throw exception nhưng frontend không bắt

**Fix:**
```javascript
// File: seller/chat.html - trong phần auto-open logic
fetch('/api/conversation', {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    body: `customerId=${currentUser.userId}&sellerId=${targetSellerId}`
})
.then(response => {
    if (!response.ok) {
        return response.json().then(err => {
            throw new Error(err.error || 'Failed to create conversation');
        });
    }
    return response.json();
})
.then(conversation => {
    // Success handling
})
.catch(error => {
    alert('Cannot start chat: ' + error.message);
    console.error('Error:', error);
});
```

---

### 2. WebSocket Disconnect - Disable Send Button
**Vấn đề:**
- Khi WebSocket disconnect, user vẫn có thể click Send
- Message sẽ bị mất

**Fix:**
```javascript
function updateConnectionUI() {
    const sendBtn = document.getElementById('sendBtn');
    const messageInput = document.getElementById('messageInput');
    
    if (!isConnected) {
        sendBtn.disabled = true;
        sendBtn.style.opacity = '0.5';
        messageInput.placeholder = '⚠️ Disconnected - Reconnecting...';
        messageInput.disabled = true;
    } else {
        sendBtn.disabled = false;
        sendBtn.style.opacity = '1';
        messageInput.placeholder = 'Type a message...';
        messageInput.disabled = false;
    }
}

// Call trong onConnected và onError
function onConnected() {
    isConnected = true;
    updateConnectionUI();
    // ...
}

function onError(error) {
    isConnected = false;
    updateConnectionUI();
    // ...
}
```

---

### 3. Message Length Validation
**Vấn đề:**
- Backend limit 5000 chars nhưng frontend không validate
- User gửi message dài → backend reject → no feedback

**Fix:**
```javascript
const MAX_MESSAGE_LENGTH = 5000;

// Add character counter
<div class="chat-input-wrapper">
    <input type="text" class="chat-input" id="messageInput"
           placeholder="Type a message..."
           onkeyup="updateCharCount()"
           onkeypress="handleKeyPress(event)">
    <span id="charCount" style="color: #999; font-size: 12px; margin-right: 10px;">0/5000</span>
    <button class="btn-send" id="sendBtn" onclick="sendMessage()">
        <i class="fas fa-paper-plane"></i>
    </button>
</div>

<script>
function updateCharCount() {
    const input = document.getElementById('messageInput');
    const charCount = document.getElementById('charCount');
    const length = input.value.length;
    
    charCount.textContent = `${length}/${MAX_MESSAGE_LENGTH}`;
    
    if (length > MAX_MESSAGE_LENGTH) {
        charCount.style.color = 'red';
        document.getElementById('sendBtn').disabled = true;
    } else if (length > MAX_MESSAGE_LENGTH * 0.9) {
        charCount.style.color = 'orange';
        document.getElementById('sendBtn').disabled = false;
    } else {
        charCount.style.color = '#999';
        document.getElementById('sendBtn').disabled = false;
    }
}

function sendMessage() {
    const input = document.getElementById('messageInput');
    const content = input.value.trim();
    
    if (!content || !currentConversation || !isConnected) return;
    
    // Validate length
    if (content.length > MAX_MESSAGE_LENGTH) {
        alert(`Message too long! Maximum ${MAX_MESSAGE_LENGTH} characters.`);
        return;
    }
    
    // ... rest of code
}
</script>
```

---

## 🟡 HIGH PRIORITY (Fix trong tuần)

### 4. Rate Limiting Feedback
**Vấn đề:**
- Messages bị drop silently khi spam
- User không biết tại sao message không gửi được

**Fix:**
```javascript
let lastMessageTime = 0;
const RATE_LIMIT_MS = 100;

function sendMessage() {
    const now = Date.now();
    
    if (lastMessageTime && (now - lastMessageTime) < RATE_LIMIT_MS) {
        showToast('⚠️ Please slow down!', 'warning');
        return;
    }
    
    lastMessageTime = now;
    // ... rest of code
}

function showToast(message, type = 'info') {
    // Simple toast notification
    const toast = document.createElement('div');
    toast.className = `toast toast-${type}`;
    toast.textContent = message;
    toast.style.cssText = `
        position: fixed;
        bottom: 20px;
        right: 20px;
        padding: 12px 20px;
        background: ${type === 'warning' ? '#ffc107' : '#28a745'};
        color: white;
        border-radius: 8px;
        box-shadow: 0 4px 12px rgba(0,0,0,0.2);
        z-index: 9999;
        animation: slideIn 0.3s ease;
    `;
    document.body.appendChild(toast);
    setTimeout(() => {
        toast.remove();
    }, 3000);
}
```

---

### 5. Persist Current Conversation
**Vấn đề:**
- Khi refresh page, mất current conversation
- User phải click lại

**Fix:**
```javascript
function openConversation(conv) {
    currentConversation = conv;
    
    // Save to localStorage
    localStorage.setItem('lastConversationId', conv.id);
    
    // ... rest of code
}

// On page load - after loadConversations()
function restoreLastConversation() {
    const lastConvId = localStorage.getItem('lastConversationId');
    if (lastConvId && !targetConversationId && !targetSellerId) {
        const conv = conversations.find(c => c.id === lastConvId);
        if (conv) {
            openConversation(conv);
        }
    }
}

// Call after loadConversations completes
loadConversations().then(() => {
    restoreLastConversation();
});
```

---

### 6. Message Status Indicators
**Vấn đề:**
- Không biết message đã gửi thành công hay chưa
- Không có "sending" state

**Fix:**
```javascript
function sendMessage() {
    const input = document.getElementById('messageInput');
    const content = input.value.trim();

    if (!content || !currentConversation || !isConnected) return;

    const tempId = 'temp_' + Date.now();
    const message = {
        id: tempId,
        conversationId: currentConversation.id,
        senderId: currentUser.userId,
        senderName: currentUser.fullName || currentUser.username,
        senderRole: currentUser.userType,
        receiverId: currentUser.userType === 'CUSTOMER' ? currentConversation.sellerId : currentConversation.customerId,
        content: content,
        type: 'TEXT',
        read: false,
        status: 'sending' // Add status field
    };

    displayMessage(message);
    input.value = '';
    scrollToBottom();

    // Send via WebSocket
    stompClient.send('/app/sendMessage', {}, JSON.stringify(message));
    
    // Update status after 2 seconds (simple approach)
    setTimeout(() => {
        const msgElement = document.querySelector(`[data-temp-id="${tempId}"]`);
        if (msgElement) {
            msgElement.querySelector('.message-status').textContent = '✓';
        }
    }, 2000);
}

function displayMessage(msg) {
    // ... existing code ...
    
    // Add status indicator
    if (msg.senderId === currentUser.userId) {
        const statusSpan = document.createElement('span');
        statusSpan.className = 'message-status';
        statusSpan.style.cssText = 'font-size: 10px; margin-left: 5px;';
        statusSpan.textContent = msg.status === 'sending' ? '○' : '✓';
        messageDiv.querySelector('.message-text').appendChild(statusSpan);
        
        if (msg.id && msg.id.toString().startsWith('temp_')) {
            messageDiv.setAttribute('data-temp-id', msg.id);
        }
    }
    
    // ... rest of code ...
}
```

---

## 🟢 MEDIUM PRIORITY (Nice to have)

### 7. Offline Message Queue
**Concept:**
```javascript
let messageQueue = [];

function sendMessage() {
    // ... validate ...
    
    if (!isConnected) {
        messageQueue.push(message);
        showToast('⚠️ Offline - Message will be sent when reconnected', 'warning');
        displayMessage({...message, status: 'queued'});
        return;
    }
    
    // Send normally
}

function onConnected() {
    isConnected = true;
    updateConnectionUI();
    
    // Send queued messages
    if (messageQueue.length > 0) {
        showToast(`📤 Sending ${messageQueue.length} queued messages...`, 'info');
        messageQueue.forEach(msg => {
            stompClient.send('/app/sendMessage', {}, JSON.stringify(msg));
        });
        messageQueue = [];
    }
    
    // ... rest of code ...
}
```

---

### 8. Pagination cho Conversations List
**Backend:**
```java
@GetMapping("/api/conversations/{userId}")
@ResponseBody
public ResponseEntity<?> getUserConversations(
        @PathVariable Long userId,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size) {
    try {
        if (userId == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "User ID is required"));
        }

        Page<Conversation> conversationsPage = chatService.getConversationsForUser(userId, page, size);
        return ResponseEntity.ok(Map.of(
            "conversations", conversationsPage.getContent(),
            "totalPages", conversationsPage.getTotalPages(),
            "currentPage", page
        ));
    } catch (Exception e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to get conversations"));
    }
}
```

**Frontend:**
```javascript
let currentPage = 0;
let totalPages = 1;

async function loadConversations() {
    const response = await fetch(`/api/conversations/${currentUser.userId}?page=${currentPage}&size=20`);
    const data = await response.json();
    
    conversations = data.conversations;
    totalPages = data.totalPages;
    
    renderConversations();
    renderPagination();
}

function renderPagination() {
    // Add pagination controls at bottom of sidebar
}
```

---

### 9. Typing Indicator (Already implemented in backend)
**Just need to activate:**
```javascript
let typingTimeout;

document.getElementById('messageInput').addEventListener('input', () => {
    clearTimeout(typingTimeout);
    
    if (currentConversation) {
        stompClient.send('/app/typing', {}, JSON.stringify({
            conversationId: currentConversation.id,
            userId: currentUser.userId.toString(),
            userName: currentUser.fullName || currentUser.username,
            isTyping: true
        }));
    }
    
    typingTimeout = setTimeout(() => {
        if (currentConversation) {
            stompClient.send('/app/typing', {}, JSON.stringify({
                conversationId: currentConversation.id,
                userId: currentUser.userId.toString(),
                userName: currentUser.fullName || currentUser.username,
                isTyping: false
            }));
        }
    }, 1000);
});

// Subscribe to typing events
stompClient.subscribe('/topic/conversation/' + conversationId + '/typing', (message) => {
    const indicator = JSON.parse(message.body);
    if (indicator.userId !== currentUser.userId.toString() && indicator.isTyping) {
        showTypingIndicator(indicator.userName);
    } else {
        hideTypingIndicator();
    }
});
```

---

### 10. Online Status Indicator
**Already tracked in backend, just need UI:**
```javascript
// Subscribe to user status
stompClient.subscribe('/topic/user.status', (message) => {
    const status = JSON.parse(message.body);
    updateUserOnlineStatus(status.userId, status.online);
});

function updateUserOnlineStatus(userId, isOnline) {
    // Update avatar with online indicator
    const avatars = document.querySelectorAll(`[data-user-id="${userId}"]`);
    avatars.forEach(avatar => {
        const indicator = avatar.querySelector('.online-indicator') || createOnlineIndicator();
        indicator.style.display = isOnline ? 'block' : 'none';
        avatar.appendChild(indicator);
    });
}

function createOnlineIndicator() {
    const indicator = document.createElement('span');
    indicator.className = 'online-indicator';
    indicator.style.cssText = `
        width: 12px;
        height: 12px;
        background: #28a745;
        border: 2px solid white;
        border-radius: 50%;
        position: absolute;
        bottom: 0;
        right: 0;
    `;
    return indicator;
}
```

---

## 📊 SUMMARY

| Priority | Issues | Estimated Time | Impact |
|----------|--------|----------------|--------|
| 🔴 Critical | 3 | 4-6 hours | High |
| 🟡 High | 3 | 6-8 hours | Medium |
| 🟢 Medium | 4 | 8-12 hours | Low |
| **Total** | **10** | **18-26 hours** | **-** |

---

## 🎯 ACTION PLAN

### Week 1 (Critical)
- [ ] Day 1: Fix error handling cho invalid sellerId
- [ ] Day 2: Implement WebSocket disconnect UI
- [ ] Day 3: Add message length validation + char counter

### Week 2 (High Priority)
- [ ] Day 1: Add rate limiting feedback
- [ ] Day 2: Implement persist conversation state
- [ ] Day 3: Add message status indicators

### Week 3 (Medium Priority)
- [ ] Day 1-2: Implement offline message queue
- [ ] Day 3-4: Add pagination
- [ ] Day 5: Activate typing indicator + online status

---

## 🧪 TESTING CHECKLIST

After implementing each fix:

- [ ] Test happy case
- [ ] Test error case
- [ ] Test edge cases
- [ ] Test on Chrome
- [ ] Test on Firefox
- [ ] Test on Safari
- [ ] Test on mobile
- [ ] Test with slow network
- [ ] Test with no network
- [ ] Test concurrent users

---

**Tác giả**: GitHub Copilot  
**Ngày**: 29/10/2025  
**Version**: 1.0  
**Status**: 📋 Ready for Implementation

