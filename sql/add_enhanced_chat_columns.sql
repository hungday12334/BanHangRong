-- Enhanced Chat Features - Database Schema Update
-- Thêm các cột mới vào bảng chat_messages

-- Add reactions column (JSON format)
ALTER TABLE chat_messages ADD COLUMN IF NOT EXISTS reactions TEXT;

-- Add reply fields
ALTER TABLE chat_messages ADD COLUMN IF NOT EXISTS reply_to_message_id VARCHAR(255);
ALTER TABLE chat_messages ADD COLUMN IF NOT EXISTS reply_to_sender_name VARCHAR(255);
ALTER TABLE chat_messages ADD COLUMN IF NOT EXISTS reply_to_content TEXT;

-- Add delete status
ALTER TABLE chat_messages ADD COLUMN IF NOT EXISTS deleted BOOLEAN DEFAULT FALSE;

-- Add index for better performance
CREATE INDEX IF NOT EXISTS idx_chat_messages_reply ON chat_messages(reply_to_message_id);
CREATE INDEX IF NOT EXISTS idx_chat_messages_deleted ON chat_messages(deleted);

-- Verify columns were added
SELECT COLUMN_NAME, DATA_TYPE
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_NAME = 'chat_messages'
ORDER BY ORDINAL_POSITION;

