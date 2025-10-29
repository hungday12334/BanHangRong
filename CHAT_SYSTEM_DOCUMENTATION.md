# Chat System Documentation - BanHangRong

## Overview
This chat system enables real-time communication between customers and sellers on the BanHangRong platform. It uses WebSocket technology for instant messaging.

## Architecture

### Backend Components

#### 1. **Entities**
- **ChatMessage** (`Entity/ChatMessage.java`)
  - Stores individual chat messages
  - Fields: messageId, conversationId, senderId, receiverId, content, messageType, isRead, createdAt
  - Uses Long IDs to integrate with existing Users entity

- **Conversation** (`Entity/Conversation.java`)
  - Represents a conversation between a customer and seller
  - Fields: id, customerId, sellerId, customerName, sellerName, lastMessage, lastMessageTime
  - Unique constraint on (customerId, sellerId) to prevent duplicate conversations

- **Users** (`Entity/Users.java`)
  - Existing user entity with userId (Long), username, fullName, userType (CUSTOMER/SELLER), avatarUrl

#### 2. **Repositories**
- **ConversationRepository** - CRUD operations for conversations
- **MessageRepository** - CRUD operations for messages, with queries for unread counts and marking as read
- **UsersRepository** - Existing repository for user management

#### 3. **Services**
- **ChatService** (`service/ChatService.java`)
  - Business logic for chat operations
  - Key methods:
    - `getOrCreateConversation(customerId, sellerId)` - Create or retrieve conversation
    - `addMessage(message)` - Save and validate message
    - `getConversationsForUser(userId)` - Get all conversations for a user
    - `markConversationAsRead(conversationId, userId)` - Mark messages as read
    - `setUserOnlineStatus(userId, online)` - Track online status (in-memory)

#### 4. **Controllers**
- **ChatController** (`Controller/ChatController.java`)
  - WebSocket message handlers:
    - `@MessageMapping("/sendMessage")` - Handle incoming messages
    - `@MessageMapping("/typing")` - Handle typing indicators
    - `@MessageMapping("/user.connect")` - Handle user connection
    - `@MessageMapping("/user.disconnect")` - Handle user disconnection
  - REST endpoints:
    - `GET /api/users/{userId}` - Get user info
    - `GET /api/sellers` - Get all sellers
    - `GET /api/conversations/{userId}` - Get user's conversations
    - `GET /api/conversation/{conversationId}` - Get specific conversation
    - `POST /api/conversation` - Create/get conversation
    - `POST /api/conversation/{conversationId}/read` - Mark as read

#### 5. **WebSocket Configuration**
- **WebSocketConfig** (`WebSocket/WebSocketConfig.java`)
  - Configures STOMP over WebSocket
  - Endpoint: `/chat`
  - Application prefix: `/app`
  - Topic prefixes: `/topic`, `/queue`
  - Heartbeat enabled for connection health monitoring

- **WebSocketEventListener** (`WebSocket/WebSocketEventListener.java`)
  - Handles connection/disconnection events
  - Updates user online status
  - Broadcasts status changes

### Frontend Components

#### 1. **Chat Interface** (`templates/seller/chat.html`)
- Responsive layout with sidebar and main chat area
- Sidebar shows list of conversations with:
  - User avatar
  - Last message preview
  - Unread message count
  - Timestamp
- Main chat area displays:
  - Chat header with recipient info
  - Message history
  - Input field for new messages
- Uses Thymeleaf for server-side rendering with session data
- SockJS + STOMP.js for WebSocket connection

#### 2. **Key JavaScript Functions**
- `connect()` - Establish WebSocket connection
- `loadConversations()` - Load user's conversations
- `openConversation(conv)` - Open and subscribe to a conversation
- `sendMessage()` - Send a new message
- `displayMessage(message)` - Render a message in the UI
- `markAsRead()` - Mark conversation as read

## Database Schema

### chat_conversations
```sql
CREATE TABLE chat_conversations (
    id VARCHAR(100) PRIMARY KEY,
    customer_id BIGINT NOT NULL,
    customer_name VARCHAR(100),
    seller_id BIGINT NOT NULL,
    seller_name VARCHAR(100),
    last_message TEXT,
    last_message_time TIMESTAMP,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    UNIQUE (customer_id, seller_id)
);
```

### chat_messages
```sql
CREATE TABLE chat_messages (
    message_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    conversation_id VARCHAR(100) NOT NULL,
    sender_id BIGINT NOT NULL,
    sender_name VARCHAR(100),
    sender_role VARCHAR(20),
    receiver_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    message_type VARCHAR(20) DEFAULT 'TEXT',
    is_read BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

## WebSocket Communication Flow

### 1. **Connection**
```
Client → /chat (SockJS endpoint)
Client → /app/user.connect (send userId)
Server → broadcasts to /topic/user.status
```

### 2. **Sending Message**
```
Client → /app/sendMessage (with message data)
Server → validates and saves message
Server → broadcasts to /topic/conversation/{conversationId}
Server → notifies receiver at /topic/user/{receiverId}/notification
```

### 3. **Receiving Message**
```
Server → /topic/conversation/{conversationId}
Client → displays message in UI
Client → calls markAsRead if conversation is open
```

### 4. **Disconnection**
```
Client → /app/user.disconnect
Server → updates online status
Server → broadcasts to /topic/user.status
```

## Usage Guide

### For Customers:
1. Login to the platform
2. Navigate to `/customer/chat` or `/chat`
3. System automatically creates conversations with all available sellers
4. Click on a seller to start chatting
5. Type message and press Enter or click Send button

### For Sellers:
1. Login to the platform
2. Navigate to `/seller/chat` or `/chat`
3. Conversations appear as customers message the seller
4. Click on a conversation to view and respond
5. Unread message count shown on each conversation

## Features

### Implemented:
✅ Real-time messaging between customer and seller
✅ Message persistence in database
✅ Unread message count
✅ Last message preview in conversation list
✅ Online/offline status tracking (in-memory)
✅ Auto-reconnection on connection loss
✅ Rate limiting (100ms between messages)
✅ Message validation (max 5000 characters)
✅ Integration with existing Users entity
✅ Session-based authentication
✅ Responsive UI design

### Future Enhancements:
- Image/file sharing
- Message read receipts
- Typing indicators (UI prepared, backend ready)
- Message search
- Conversation archiving
- Push notifications
- Emoji support
- Message reactions
- Persistent online status (Redis)
- Message editing/deletion
- Product sharing in chat
- Order status updates in chat

## Security Considerations

1. **Authentication**: Users must be logged in (session-based)
2. **Authorization**: Users can only access their own conversations
3. **Validation**: 
   - Message content validated (non-empty, max length)
   - Sender must be part of conversation
   - Conversation participants validated
4. **Rate Limiting**: 100ms minimum between messages per user
5. **XSS Protection**: HTML content escaped in UI
6. **SQL Injection**: JPA/Hibernate parameterized queries

## Troubleshooting

### Connection Issues:
- Check if WebSocket endpoint `/chat` is accessible
- Verify STOMP connection in browser console
- Check for firewall/proxy blocking WebSocket connections

### Messages Not Sending:
- Verify user is logged in
- Check conversation exists
- Check console for JavaScript errors
- Verify backend ChatService is running

### Database Issues:
- Run `sql/create_chat_tables.sql` to create tables
- Check foreign key constraints on users table
- Verify user_id matches in both tables

## API Endpoints Summary

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | /chat | Chat page (redirects based on user type) |
| GET | /seller/chat | Seller chat page |
| GET | /customer/chat | Customer chat page |
| GET | /api/users/{userId} | Get user info |
| GET | /api/sellers | Get all sellers |
| GET | /api/conversations/{userId} | Get user's conversations |
| GET | /api/conversation/{conversationId} | Get specific conversation |
| POST | /api/conversation | Create/get conversation |
| POST | /api/conversation/{conversationId}/read | Mark messages as read |

## WebSocket Topics

| Topic | Description |
|-------|-------------|
| /topic/conversation/{conversationId} | Messages for specific conversation |
| /topic/user/{userId}/notification | Notifications for specific user |
| /topic/user.status | User online/offline status broadcasts |
| /app/sendMessage | Send a new message |
| /app/typing | Send typing indicator |
| /app/user.connect | Notify user connection |
| /app/user.disconnect | Notify user disconnection |

## Testing

### Manual Testing:
1. Create customer account and login
2. Create seller account and login
3. Open chat as customer in one browser
4. Open chat as seller in another browser/incognito
5. Send messages between both users
6. Verify real-time delivery
7. Check unread counts
8. Test reconnection by temporarily disabling network

### Integration Testing:
```java
// Test conversation creation
GET /api/conversation?customerId=1&sellerId=2

// Test message sending via WebSocket
// Test unread count query
// Test mark as read functionality
```

## Performance Optimization

1. **Database Indexes**: Created on frequently queried columns
2. **Message Pagination**: Limit initial messages loaded
3. **Lazy Loading**: Conversations loaded on demand
4. **Connection Pooling**: Reuse WebSocket connections
5. **Heartbeat**: Keep connections alive efficiently

## Maintenance

### Database Cleanup:
```sql
-- Delete old messages (optional, configure retention policy)
DELETE FROM chat_messages WHERE created_at < DATE_SUB(NOW(), INTERVAL 90 DAY);

-- Archive old conversations
-- Implement based on business requirements
```

### Monitoring:
- Track WebSocket connection count
- Monitor message throughput
- Log connection errors
- Track message delivery success rate

## Deployment Notes

1. Ensure database tables are created before deployment
2. Configure WebSocket allowed origins for production domain
3. Consider using Redis for online status in multi-server setup
4. Enable HTTPS for secure WebSocket connections (WSS)
5. Configure load balancer to support WebSocket sticky sessions
6. Set appropriate timeout values for production

## Support

For issues or questions:
1. Check application logs in `app.log`
2. Verify database tables exist and have correct schema
3. Check browser console for frontend errors
4. Verify user has correct permissions/roles
5. Check WebSocket connection in browser Network tab

