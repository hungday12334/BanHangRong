# 📌 Conversation Actions Feature - README

## Quick Start

```bash
# 1. Build and setup
./setup-conversation-actions.sh

# 2. Run application
java -jar target/su25-0.0.1-SNAPSHOT.jar

# 3. Test in browser
# - Login as Customer or Seller
# - Go to Chat
# - Hover conversation → Click ⋯ → Try Pin/Delete
```

## What's New?

✅ **Pin Conversation** - Ghim cuộc trò chuyện lên đầu (chỉ phía mình)  
✅ **Delete Conversation** - Xóa khỏi danh sách (chỉ phía mình)

## Documentation

- 📖 **[IMPLEMENTATION_COMPLETE.md](IMPLEMENTATION_COMPLETE.md)** - Tổng kết hoàn chỉnh
- 📚 **[CONVERSATION_ACTIONS_FEATURE.md](CONVERSATION_ACTIONS_FEATURE.md)** - Chi tiết kỹ thuật
- 🚀 **[CONVERSATION_ACTIONS_SUMMARY.md](CONVERSATION_ACTIONS_SUMMARY.md)** - Tóm tắt nhanh
- 🎨 **[CONVERSATION_ACTIONS_DIAGRAM.md](CONVERSATION_ACTIONS_DIAGRAM.md)** - Sơ đồ flow
- ✅ **[CONVERSATION_ACTIONS_TEST_CHECKLIST.md](CONVERSATION_ACTIONS_TEST_CHECKLIST.md)** - Test cases

## Files Changed

### New (9 files):
1. `UserConversationMetadata.java` - Entity
2. `UserConversationMetadataRepository.java` - Repository
3. `create_user_conversation_metadata.sql` - SQL
4. `setup-conversation-actions.sh` - Setup script
5-9. Documentation files

### Modified (5 files):
1. `Conversation.java` - Added `isPinned`
2. `ChatService.java` - Added pin/delete methods
3. `ChatController.java` - Added REST endpoints
4. `customer/chat.html` - UI + JavaScript
5. `seller/chat.html` - UI + JavaScript

## API Endpoints

```
POST   /api/conversations/{id}/pin?userId={userId}
POST   /api/conversations/{id}/unpin?userId={userId}
DELETE /api/conversations/{id}?userId={userId}
GET    /api/conversations/{id}/metadata?userId={userId}
```

## Key Features

- 📌 Pin to top (local only)
- 🗑️ Delete from view (local only)
- 📱 Mobile responsive
- ⌨️ Keyboard accessible
- 🔄 Auto-recovery on new message
- 💾 Persistent across sessions

## Status

✅ **COMPLETE** - Ready for testing (Nov 2, 2025)

---

**Need help?** See [IMPLEMENTATION_COMPLETE.md](IMPLEMENTATION_COMPLETE.md)

