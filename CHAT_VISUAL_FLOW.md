# 🎨 Chat System - Visual Flow Diagrams

## 📊 Overview Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                       Chat System Architecture                   │
└─────────────────────────────────────────────────────────────────┘

┌──────────────┐         ┌──────────────┐         ┌──────────────┐
│   Customer   │         │   Seller     │         │    Admin     │
│   (Browser)  │         │   (Browser)  │         │   (Future)   │
└──────┬───────┘         └──────┬───────┘         └──────────────┘
       │                        │
       │    HTTP Request        │
       ├────────────────────────┤
       │                        │
       ↓                        ↓
┌────────────────────────────────────────────────────────────────┐
│                      ChatController.java                        │
│  • GET /customer/chat?sellerId=X                               │
│  • GET /seller/chat                                            │
│  • POST /api/conversation                                      │
│  • GET /api/conversations/{userId}                             │
└────────────────┬───────────────────────────────────────────────┘
                 │
                 ↓
┌────────────────────────────────────────────────────────────────┐
│                      ChatService.java                           │
│  • getOrCreateConversation()                                   │
│  • addMessage()                                                │
│  • markConversationAsRead()                                    │
│  • getConversationsForUser()                                   │
└────────────────┬───────────────────────────────────────────────┘
                 │
                 ↓
┌────────────────────────────────────────────────────────────────┐
│                    Database (H2/MySQL)                          │
│  • conversations table                                         │
│  • chat_messages table                                         │
│  • users table                                                 │
└────────────────────────────────────────────────────────────────┘

       ┌─────────────────────────────────────┐
       │      WebSocket Connection           │
       │  (Real-time Communication)          │
       └──────────────┬──────────────────────┘
                      │
      ┌───────────────┴───────────────┐
      ↓                               ↓
┌──────────────┐              ┌──────────────┐
│  Customer    │              │   Seller     │
│  Browser     │◄────────────►│   Browser    │
│  /ws         │   Messages   │   /ws        │
└──────────────┘              └──────────────┘
```

---

## 🎯 Happy Case Flow: Customer Chat với Seller từ Product Page

```
┌─────────────────────────────────────────────────────────────────────┐
│ STEP 1: Customer Views Product                                      │
└─────────────────────────────────────────────────────────────────────┘

Customer Browser
    │
    │ GET /customer/products/1
    ↓
┌─────────────────────────────────┐
│  Product Detail Page            │
│  • Product Info                 │
│  • Price                        │
│  • Seller: "Minh Shop"          │
│  • [ Add to Cart ]              │
│  • [ 💬 Chat với Shop ]  ←─────── Click here!
└─────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────┐
│ STEP 2: Navigate to Chat                                            │
└─────────────────────────────────────────────────────────────────────┘

Customer clicks "Chat với Shop"
    │
    │ Thymeleaf generates link:
    │ th:href="@{/customer/chat(sellerId=${product.sellerId})}"
    ↓
Browser navigates to:
    GET /customer/chat?sellerId=2

┌─────────────────────────────────────────────────────────────────────┐
│ STEP 3: Backend Processing                                          │
└─────────────────────────────────────────────────────────────────────┘

ChatController.customerChat()
    │
    ├─► Validate authentication
    │   └─► Get current user from session
    │
    ├─► Extract sellerId from parameter (sellerId=2)
    │
    ├─► Call ChatService.getOrCreateConversation(customerId=1, sellerId=2)
    │       │
    │       ├─► Check if conversation exists
    │       │   SELECT * FROM conversations 
    │       │   WHERE customer_id=1 AND seller_id=2
    │       │
    │       ├─► If exists: Return existing conversation
    │       │   └─► Load message history
    │       │
    │       └─► If not exists: Create new conversation
    │           └─► INSERT INTO conversations (id, customer_id, seller_id, ...)
    │               VALUES ('conv_1_2', 1, 2, ...)
    │
    ├─► Add to model:
    │   • model.addAttribute("conversationId", "conv_1_2")
    │   • model.addAttribute("targetSellerId", 2)
    │   • model.addAttribute("user", currentUser)
    │
    └─► Return "seller/chat" template

┌─────────────────────────────────────────────────────────────────────┐
│ STEP 4: Frontend Initialization                                     │
└─────────────────────────────────────────────────────────────────────┘

Chat page loads (seller/chat.html)
    │
    ├─► Initialize WebSocket connection
    │   │
    │   ├─► const socket = new SockJS('/ws');
    │   ├─► stompClient = Stomp.over(socket);
    │   └─► stompClient.connect({}, onConnected, onError);
    │
    ├─► Load conversations list
    │   │
    │   └─► GET /api/conversations/1
    │       └─► Returns: [
    │               { id: "conv_1_2", sellerName: "Minh Shop", ... },
    │               { id: "conv_1_3", sellerName: "Tech Store", ... }
    │           ]
    │
    └─► Auto-open target conversation
        │
        ├─► const targetConversationId = "conv_1_2";
        ├─► const targetSellerId = 2;
        │
        └─► Find conversation in list and call openConversation()

┌─────────────────────────────────────────────────────────────────────┐
│ STEP 5: Open Conversation                                           │
└─────────────────────────────────────────────────────────────────────┘

openConversation(conv)
    │
    ├─► Set currentConversation = conv
    │
    ├─► Subscribe to WebSocket topic
    │   │
    │   └─► stompClient.subscribe('/topic/conversation/conv_1_2', handleNewMessage)
    │
    ├─► Load message history
    │   │
    │   └─► GET /api/conversation/conv_1_2
    │       └─► Returns: {
    │               id: "conv_1_2",
    │               messages: [
    │                   { sender: "Customer", content: "Hello!", ... },
    │                   { sender: "Seller", content: "Hi! How can I help?", ... }
    │               ]
    │           }
    │
    ├─► Render messages in UI
    │   │
    │   └─► messages.forEach(msg => displayMessage(msg))
    │
    ├─► Mark conversation as read
    │   │
    │   └─► POST /api/conversation/conv_1_2/read?userId=1
    │
    └─► Focus on message input box

┌─────────────────────────────────────────────────────────────────────┐
│ STEP 6: Send Message                                                │
└─────────────────────────────────────────────────────────────────────┘

Customer types: "What is the price?"
Customer presses Enter or clicks Send button
    │
    ├─► Validate:
    │   • Message not empty? ✓
    │   • WebSocket connected? ✓
    │   • Current conversation exists? ✓
    │
    ├─► Create message object:
    │   {
    │       conversationId: "conv_1_2",
    │       senderId: 1,
    │       senderName: "Customer Name",
    │       senderRole: "CUSTOMER",
    │       receiverId: 2,
    │       content: "What is the price?",
    │       type: "TEXT",
    │       read: false
    │   }
    │
    ├─► Send via WebSocket:
    │   stompClient.send('/app/sendMessage', {}, JSON.stringify(message))
    │
    ├─► Display message immediately (optimistic UI)
    │   └─► displayMessage(message)
    │
    └─► Clear input field

┌─────────────────────────────────────────────────────────────────────┐
│ STEP 7: Backend Message Processing                                  │
└─────────────────────────────────────────────────────────────────────┘

@MessageMapping("/sendMessage")
ChatController.sendMessage(message)
    │
    ├─► Validate message
    │   • Message not null? ✓
    │   • SenderId not null? ✓
    │   • Content not empty? ✓
    │   • Content length <= 5000? ✓
    │
    ├─► Rate limiting check
    │   • Last message time for user1: 2 seconds ago
    │   • Current time - last time > 100ms? ✓ OK
    │   • Update last message time
    │
    ├─► Call ChatService.addMessage(message)
    │   │
    │   ├─► Verify conversation exists
    │   ├─► Verify sender is part of conversation
    │   ├─► Save to database:
    │   │   INSERT INTO chat_messages (...)
    │   │   VALUES (...)
    │   │
    │   └─► Update conversation:
    │       UPDATE conversations
    │       SET last_message = "What is the price?",
    │           last_message_time = NOW()
    │       WHERE id = "conv_1_2"
    │
    ├─► Broadcast via WebSocket:
    │   │
    │   ├─► messagingTemplate.convertAndSend(
    │   │       "/topic/conversation/conv_1_2",
    │   │       savedMessage
    │   │   )
    │   │
    │   └─► messagingTemplate.convertAndSend(
    │           "/topic/user/2/notification",  ← Notify seller!
    │           savedMessage
    │       )
    │
    └─► Done

┌─────────────────────────────────────────────────────────────────────┐
│ STEP 8: Seller Receives Message                                     │
└─────────────────────────────────────────────────────────────────────┘

Seller is online at /seller/chat
    │
    ├─► WebSocket receives message
    │   └─► /topic/conversation/conv_1_2
    │
    ├─► handleNewMessage(msg) is called
    │   │
    │   ├─► If current conversation is "conv_1_2":
    │   │   └─► displayMessage(msg)  ← Show immediately!
    │   │
    │   └─► If viewing different conversation:
    │       └─► Increase unread count badge
    │
    └─► Update conversation in sidebar
        • Move to top of list
        • Update "last message" preview
        • Show unread badge

┌─────────────────────────────────────────────────────────────────────┐
│ STEP 9: Seller Replies                                              │
└─────────────────────────────────────────────────────────────────────┘

Seller types: "It's $99. Would you like to order?"
Seller clicks Send
    │
    └─► Same flow as STEP 6-7-8
        But now:
        • senderId = 2 (seller)
        • receiverId = 1 (customer)
        • Customer receives message real-time

✅ CONVERSATION COMPLETE!
```

---

## ❌ Unhappy Case Flow: Invalid Seller ID

```
┌─────────────────────────────────────────────────────────────────────┐
│ Scenario: Customer tries to chat with non-existent seller          │
└─────────────────────────────────────────────────────────────────────┘

Customer clicks "Chat với Shop"
    │
    │ Navigate to: /customer/chat?sellerId=99999
    ↓
ChatController.customerChat(sellerId=99999)
    │
    ├─► Call ChatService.getOrCreateConversation(customerId=1, sellerId=99999)
    │       │
    │       ├─► usersRepository.findById(99999)
    │       │   └─► Returns: Optional.empty()
    │       │
    │       └─► Throw: IllegalArgumentException("Seller not found: 99999")
    │
    ├─► Exception is caught by Spring
    │   └─► Returns error to frontend
    │
    └─► ⚠️ PROBLEM: Frontend doesn't handle this error!

Current behavior:
    ❌ No error message shown
    ❌ Chat page loads but can't create conversation
    ❌ User sees empty chat with no explanation

Expected behavior:
    ✅ Show alert: "Cannot start chat: Seller not found"
    ✅ Redirect back to previous page
    ✅ Or show error in chat UI

FIX NEEDED in seller/chat.html:
    fetch('/api/conversation', { ... })
    .then(response => {
        if (!response.ok) {
            return response.json().then(err => {
                throw new Error(err.error || 'Failed to create conversation');
            });
        }
        return response.json();
    })
    .catch(error => {
        alert('Cannot start chat: ' + error.message); ← ADD THIS!
        window.location.href = document.referrer || '/';
    });
```

---

## ⚠️ Edge Case Flow: WebSocket Disconnection

```
┌─────────────────────────────────────────────────────────────────────┐
│ Scenario: Network disconnects while chatting                        │
└─────────────────────────────────────────────────────────────────────┘

Initial state:
    ✅ Customer is chatting with seller
    ✅ WebSocket connected
    ✅ Messages flowing normally

Network issue occurs:
    │
    │ (Wi-Fi drops, cable unplugged, server restart, etc.)
    ↓
WebSocket connection lost
    │
    ├─► onError() callback is triggered
    │   │
    │   ├─► Set isConnected = false
    │   ├─► Show status: "❌ Connection Lost - Trying to reconnect..."
    │   └─► setTimeout(connect, 5000)  ← Retry after 5 seconds
    │
    └─► Meanwhile, customer tries to send message:
        │
        ├─► sendMessage() is called
        │   │
        │   └─► Check: if (!content || !currentConversation || !isConnected)
        │       └─► return;  ← Message is silently dropped!
        │
        └─► ⚠️ PROBLEM: Customer doesn't know message wasn't sent!

After 5 seconds:
    │
    ├─► Retry connection
    │   └─► connect() is called again
    │       │
    │       ├─► If successful:
    │       │   ├─► onConnected()
    │       │   ├─► Show: "✅ Connected"
    │       │   └─► Set isConnected = true
    │       │
    │       └─► If failed:
    │           └─► onError() again
    │               └─► Retry after 5 seconds (loop)
    │
    └─► ⚠️ PROBLEM: Messages sent during offline are lost forever!

Expected behavior:
    ✅ Disable send button when disconnected
    ✅ Show warning in message input
    ✅ Queue messages sent while offline
    ✅ Send queued messages when reconnected
    ✅ Show message status: sending/sent/failed

FIX NEEDED:
    1. Add updateConnectionUI() function
    2. Disable input and button when disconnected
    3. Implement message queue
    4. Add retry logic for failed messages
```

---

## 🔄 State Diagram: Conversation Lifecycle

```
┌──────────────────────────────────────────────────────────────────┐
│             Conversation State Lifecycle                         │
└──────────────────────────────────────────────────────────────────┘

                    [Customer clicks "Chat"]
                            │
                            ↓
                  ┌─────────────────────┐
                  │  Check if exists    │
                  └──────────┬──────────┘
                             │
                ┌────────────┴──────────────┐
                │                           │
                ↓                           ↓
    ┌───────────────────────┐   ┌───────────────────────┐
    │  Conversation EXISTS  │   │  Conversation NEW     │
    │  Status: ACTIVE       │   │  Create in DB         │
    └───────────┬───────────┘   └───────────┬───────────┘
                │                           │
                │  Load messages            │  No messages yet
                │                           │
                └────────────┬──────────────┘
                             │
                             ↓
                  ┌─────────────────────┐
                  │  OPEN in UI         │
                  │  Status: ACTIVE     │
                  │  Messages: 0-1000   │
                  └──────────┬──────────┘
                             │
              ┌──────────────┼──────────────┐
              │              │              │
              ↓              ↓              ↓
    ┌─────────────┐ ┌─────────────┐ ┌─────────────┐
    │ User sends  │ │ User reads  │ │ User closes │
    │ message     │ │ messages    │ │ conversation│
    └──────┬──────┘ └──────┬──────┘ └──────┬──────┘
           │               │               │
           ↓               ↓               ↓
    ┌─────────────┐ ┌─────────────┐ ┌─────────────┐
    │ last_message│ │ unread_count│ │ Return to   │
    │ updated     │ │ = 0         │ │ list view   │
    └─────────────┘ └─────────────┘ └─────────────┘
```

---

## 📱 UI State Transitions

```
┌──────────────────────────────────────────────────────────────────┐
│                     Chat Page UI States                          │
└──────────────────────────────────────────────────────────────────┘

STATE 1: Initial Load (No conversation selected)
┌────────────────────────────────────────────────────────────────┐
│ Sidebar             │ Main Area                                │
│                     │                                          │
│ [Conversations]     │      💬 Welcome to Chat                  │
│  • Minh Shop        │                                          │
│  • Tech Store       │      Select a conversation               │
│                     │      to start messaging                  │
└────────────────────────────────────────────────────────────────┘

                        ↓ (Click conversation)

STATE 2: Conversation Open (Empty - no messages yet)
┌────────────────────────────────────────────────────────────────┐
│ Sidebar             │ Main Area                                │
│                     │ ┌────────────────────────────────────┐   │
│ [Conversations]     │ │ 👤 Minh Shop              [Active] │   │
│  • Minh Shop  ◄─────┼─┤                                    │   │
│  • Tech Store       │ └────────────────────────────────────┘   │
│                     │                                          │
│                     │ (No messages yet)                        │
│                     │                                          │
│                     │ ┌────────────────────────────────────┐   │
│                     │ │ [Type a message...]         [Send] │   │
│                     │ └────────────────────────────────────┘   │
└────────────────────────────────────────────────────────────────┘

                        ↓ (Send message)

STATE 3: Active Conversation (With messages)
┌────────────────────────────────────────────────────────────────┐
│ Sidebar             │ Main Area                                │
│                     │ ┌────────────────────────────────────┐   │
│ [Conversations]     │ │ 👤 Minh Shop              [Active] │   │
│  • Minh Shop  (1)   │ └────────────────────────────────────┘   │
│  • Tech Store       │                                          │
│                     │ ┌──────────────────────────┐             │
│                     │ │ Hello! What is the       │ [You]       │
│                     │ │ price of this product?   │ 10:30       │
│                     │ └──────────────────────────┘             │
│                     │                                          │
│                     │       ┌──────────────────────────┐       │
│                     │ [Seller]│ It's $99. Would you  │         │
│                     │ 10:31  │ like to order?         │         │
│                     │       └──────────────────────────┘       │
│                     │                                          │
│                     │ ┌────────────────────────────────────┐   │
│                     │ │ [Type a message...]         [Send] │   │
│                     │ └────────────────────────────────────┘   │
└────────────────────────────────────────────────────────────────┘

                        ↓ (WebSocket disconnects)

STATE 4: Disconnected
┌────────────────────────────────────────────────────────────────┐
│ ⚠️ Connection Lost - Trying to reconnect...                   │
├────────────────────────────────────────────────────────────────┤
│ Sidebar             │ Main Area                                │
│                     │ ┌────────────────────────────────────┐   │
│ [Conversations]     │ │ 👤 Minh Shop         [Reconnect...] │  │
│  • Minh Shop        │ └────────────────────────────────────┘   │
│  • Tech Store       │                                          │
│                     │ (Previous messages still visible)        │
│                     │                                          │
│                     │ ┌────────────────────────────────────┐   │
│                     │ │ [Reconnecting...] [Send - Disabled]│   │
│                     │ └────────────────────────────────────┘   │
└────────────────────────────────────────────────────────────────┘

                        ↓ (Reconnected)

STATE 5: Reconnected
┌────────────────────────────────────────────────────────────────┐
│ ✅ Connected                                                    │
├────────────────────────────────────────────────────────────────┤
│ (Back to STATE 3 - Active Conversation)                        │
└────────────────────────────────────────────────────────────────┘
```

---

## 🔐 Security Flow

```
┌──────────────────────────────────────────────────────────────────┐
│           Security Checks in Chat System                         │
└──────────────────────────────────────────────────────────────────┘

REQUEST: POST /app/sendMessage (via WebSocket)
    │
    ├─► 1. Authentication Check
    │   │
    │   └─► Is user logged in?
    │       ├─► YES: Continue
    │       └─► NO: Connection rejected at WebSocket level
    │
    ├─► 2. Message Validation
    │   │
    │   ├─► Message not null? ✓
    │   ├─► SenderId matches authenticated user? ✓
    │   ├─► Content not empty? ✓
    │   └─► Content length <= 5000? ✓
    │
    ├─► 3. Rate Limiting
    │   │
    │   └─► Last message < 100ms ago?
    │       ├─► YES: Drop message (anti-spam)
    │       └─► NO: Continue
    │
    ├─► 4. Conversation Verification
    │   │
    │   ├─► Conversation exists in DB? ✓
    │   └─► User is participant?
    │       • senderId == customerId? OR
    │       • senderId == sellerId? ✓
    │
    ├─► 5. XSS Prevention
    │   │
    │   └─► Frontend: escapeHtml(message.content)
    │       └─► HTML entities are escaped
    │
    └─► 6. SQL Injection Prevention
        │
        └─► Backend: JPA PreparedStatements
            └─► All queries are parameterized ✓

✅ ALL CHECKS PASSED → Message is saved and broadcast
```

---

## 📊 Performance Considerations

```
┌──────────────────────────────────────────────────────────────────┐
│                Performance Bottlenecks                           │
└──────────────────────────────────────────────────────────────────┘

1. Load All Conversations at Once
   Problem: SELECT * FROM conversations WHERE ... (no LIMIT)
   Impact: Slow for sellers with 100+ conversations
   Fix: Implement pagination (LIMIT + OFFSET)

2. Load All Messages in Conversation
   Problem: Some conversations have 1000+ messages
   Impact: Slow initial load, heavy DOM rendering
   Fix: Load last 50 messages, lazy load older messages

3. No Caching
   Problem: Every page load fetches from database
   Impact: Increased DB load, slower response
   Fix: Implement Redis cache for conversations

4. WebSocket Broadcasting
   Problem: Broadcast to all subscribers
   Impact: Scalability issues with many users
   Fix: Already optimal (topic-based subscription)

5. Unread Count Calculation
   Problem: Runs COUNT query for each conversation
   Impact: N+1 query problem
   Fix: Batch query or denormalize unread_count

Performance Targets:
   • Page load: < 2 seconds
   • Message send/receive: < 500ms
   • Load 100 conversations: < 3 seconds
   • Support 1000 concurrent users
```

---

**Tác giả**: GitHub Copilot  
**Ngày**: 29/10/2025  
**Version**: 1.0

