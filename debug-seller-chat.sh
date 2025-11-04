#!/bin/bash

echo "=========================================="
echo "  Seller Chat Debug & Fix Script"
echo "=========================================="
echo ""

# Check if H2 console is accessible
echo "Step 1: Checking database..."
echo "-------------------------------------------"
echo "H2 Console URL: http://localhost:8080/h2-console"
echo "JDBC URL: jdbc:h2:./data/banhangrong_db"
echo "Username: sa"
echo "Password: (empty)"
echo ""

# SQL queries to run
echo "Step 2: Run these SQL queries in H2 console:"
echo "-------------------------------------------"
cat << 'EOF'

-- Query 1: Check if seller has conversations
SELECT c.*,
       (SELECT COUNT(*) FROM chat_messages WHERE conversation_id = c.id) as message_count
FROM chat_conversations c
WHERE c.seller_id = 3  -- Replace 3 with your seller userId
ORDER BY c.updated_at DESC;

-- Query 2: Check metadata (deleted/pinned status)
SELECT * FROM user_conversation_metadata
WHERE user_id = 3  -- Replace 3 with your seller userId
ORDER BY updated_at DESC;

-- Query 3: Check if conversations are deleted
SELECT c.id as conversation_id,
       c.customer_name,
       c.seller_name,
       m.is_deleted,
       m.is_pinned,
       m.deleted_at
FROM chat_conversations c
LEFT JOIN user_conversation_metadata m
    ON c.id = m.conversation_id AND m.user_id = 3  -- Replace 3 with seller userId
WHERE c.seller_id = 3  -- Replace 3 with seller userId
ORDER BY c.updated_at DESC;

-- Query 4: Reset deleted conversations (if needed)
-- Uncomment to reset:
-- UPDATE user_conversation_metadata
-- SET is_deleted = FALSE, deleted_at = NULL
-- WHERE user_id = 3 AND is_deleted = TRUE;

-- Query 5: Check recent messages
SELECT m.id, m.conversation_id, m.sender_id, m.content, m.created_at
FROM chat_messages m
WHERE m.conversation_id IN (
    SELECT id FROM chat_conversations WHERE seller_id = 3
)
ORDER BY m.created_at DESC
LIMIT 20;

EOF

echo ""
echo "Step 3: Browser Console Debug Commands"
echo "-------------------------------------------"
echo "Open seller chat page and run these in Console:"
echo ""
cat << 'EOF'

// 1. Check conversations array
console.log('Conversations count:', conversations.length);
console.log('Conversations data:', conversations);

// 2. Check current user
console.log('Current user:', currentUser);
console.log('User type:', currentUser.userType);
console.log('User ID:', currentUser.userId);

// 3. Check list element
const list = document.getElementById('conversationsList');
console.log('List element:', list);
console.log('List children count:', list?.children.length);

// 4. Force reload conversations
loadConversations().then(() => {
    console.log('✅ Reloaded! Conversations:', conversations.length);
    console.log('Conversations:', conversations);
}).catch(err => {
    console.error('❌ Error:', err);
});

// 5. Manual render test
if (conversations.length === 0) {
    console.warn('⚠️ No conversations! Testing with dummy data...');
    conversations = [{
        id: 'test_conv_1',
        customerId: 1,
        customerName: 'Test Customer',
        sellerId: currentUser.userId,
        sellerName: currentUser.username,
        lastMessage: 'Test message',
        lastMessageTime: new Date().toISOString(),
        unreadCount: 0,
        isPinned: false
    }];
    renderConversationsList();
    console.log('Rendered test conversation. Check list!');
}

EOF

echo ""
echo "Step 4: Quick fixes to try"
echo "-------------------------------------------"
echo ""
echo "Fix 1: Clear browser cache and hard refresh"
echo "  - Press: Ctrl+Shift+R (Windows) or Cmd+Shift+R (Mac)"
echo ""
echo "Fix 2: Reset all deleted conversations (SQL)"
echo "  UPDATE user_conversation_metadata SET is_deleted = FALSE WHERE user_id = [SELLER_ID];"
echo ""
echo "Fix 3: Create a test conversation (SQL)"
cat << 'EOF'
  -- Create test conversation
  INSERT INTO chat_conversations (id, customer_id, customer_name, seller_id, seller_name, created_at, updated_at)
  VALUES ('test_conv_1', 1, 'Test Customer', 3, 'Test Seller', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

  -- Add test message
  INSERT INTO chat_messages (conversation_id, sender_id, content, created_at)
  VALUES ('test_conv_1', 1, 'Hello seller!', CURRENT_TIMESTAMP);
EOF
echo ""
echo "Fix 4: Check API endpoint manually"
echo "  Open: http://localhost:8080/api/conversations/[SELLER_USER_ID]"
echo "  Should return JSON array of conversations"
echo ""

echo ""
echo "Step 5: Check application logs"
echo "-------------------------------------------"
echo "Look for these lines in application console:"
echo "  - '=== LOADING CONVERSATIONS FOR USER: [sellerId] ==='"
echo "  - '✓ Found [N] conversations'"
echo "  - '✅ Returning [K] conversations (after filtering deleted)'"
echo ""

echo ""
echo "=========================================="
echo "  Need Help?"
echo "=========================================="
echo ""
echo "If conversations still don't show:"
echo ""
echo "1. Check SELLER_CHAT_DEBUG_GUIDE.md for detailed steps"
echo "2. Share browser console output"
echo "3. Share backend console output"
echo "4. Share SQL query results from Step 2"
echo ""
echo "=========================================="


