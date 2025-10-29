# Chat System - Quick Setup Guide

## 1. Database Setup
Run the SQL script to create necessary tables:
```sql
source sql/create_chat_tables.sql
```

Or manually execute:
```bash
mysql -u your_username -p your_database < sql/create_chat_tables.sql
```

## 2. Files Updated/Created

### Backend (Java):
- ✅ `Entity/ChatMessage.java` - Updated to use Long IDs
- ✅ `Entity/Conversation.java` - Updated to use Long IDs  
- ✅ `Entity/Users.java` - Already exists (no changes needed)
- ✅ `Repository/ConversationRepository.java` - Updated
- ✅ `Repository/MessageRepository.java` - Updated
- ✅ `service/ChatService.java` - Completely rewritten
- ✅ `Controller/ChatController.java` - Updated
- ✅ `WebSocket/WebSocketConfig.java` - Already configured
- ✅ `WebSocket/WebSocketEventListener.java` - Updated

### Frontend:
- ✅ `templates/seller/chat.html` - Complete chat interface

### Database:
- ✅ `sql/create_chat_tables.sql` - Table creation script

### Documentation:
- ✅ `CHAT_SYSTEM_DOCUMENTATION.md` - Full documentation
- ✅ `CHAT_SYSTEM_QUICK_SETUP.md` - This file

## 3. Key Features

✅ Real-time messaging between customers and sellers
✅ Message persistence
✅ Unread message count
✅ Online/offline status
✅ Auto-reconnection
✅ Session-based authentication
✅ Responsive UI

## 4. Access URLs

- Customer Chat: `http://localhost:8080/customer/chat`
- Seller Chat: `http://localhost:8080/seller/chat`
- General Chat: `http://localhost:8080/chat`

## 5. How It Works

### For Customers:
1. Login as customer
2. Go to `/customer/chat`
3. System automatically creates conversations with all sellers
4. Click seller name to start chatting

### For Sellers:
1. Login as seller
2. Go to `/seller/chat`
3. Conversations appear when customers message them
4. Click customer name to view and respond

## 6. Requirements

- Spring Boot with WebSocket support
- MySQL database
- Existing Users table with:
  - user_id (BIGINT)
  - username (VARCHAR)
  - full_name (VARCHAR)
  - user_type (VARCHAR) - values: 'CUSTOMER' or 'SELLER'
  - avatar_url (VARCHAR, optional)

## 7. Testing

### Quick Test:
1. Open browser as Customer (normal mode)
2. Open browser as Seller (incognito mode)
3. Login as both users
4. Navigate to chat page on both
5. Send messages from Customer to Seller
6. Verify real-time delivery

## 8. Troubleshooting

### WebSocket not connecting:
- Check browser console for errors
- Verify `/chat` endpoint is accessible
- Check if port 8080 is open

### Messages not sending:
- Verify user is logged in
- Check database tables exist
- Check browser console for errors

### No conversations showing:
- For customers: Check if sellers exist in database
- For sellers: Customers need to send first message
- Verify foreign keys in database

## 9. Integration Notes

The chat system is fully integrated with your existing:
- ✅ Users entity (using userId as Long)
- ✅ Session management
- ✅ User authentication
- ✅ User roles (CUSTOMER/SELLER)

No separate user management needed!

## 10. Next Steps

After setup:
1. Run the SQL script to create tables
2. Restart your Spring Boot application
3. Login as a customer
4. Navigate to /chat
5. Start messaging!

## Support

See `CHAT_SYSTEM_DOCUMENTATION.md` for detailed information about:
- Architecture
- API endpoints
- WebSocket topics
- Security considerations
- Performance optimization
- Deployment notes

