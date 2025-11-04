-- Fix UTF-8 encoding for chat tables
-- This fixes the "Incorrect string value" error for Vietnamese characters

-- Fix chat_conversations table
ALTER TABLE chat_conversations
  MODIFY COLUMN customer_name VARCHAR(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
  MODIFY COLUMN seller_name VARCHAR(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
  MODIFY COLUMN last_message TEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Fix chat_messages table
ALTER TABLE chat_messages
  MODIFY COLUMN sender_name VARCHAR(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
  MODIFY COLUMN content TEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Convert table charset
ALTER TABLE chat_conversations CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE chat_messages CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Show result
SELECT 'Chat tables updated to UTF8MB4' AS status;
SHOW CREATE TABLE chat_conversations;
SHOW CREATE TABLE chat_messages;

