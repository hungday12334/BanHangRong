-- Add file attachment columns to chat_messages table
-- Run this migration to support image and file uploads in chat

ALTER TABLE chat_messages
ADD COLUMN file_url VARCHAR(500) AFTER content,
ADD COLUMN file_name VARCHAR(255) AFTER file_url,
ADD COLUMN file_type VARCHAR(50) AFTER file_name,
ADD COLUMN file_size BIGINT AFTER file_type;

-- Add index for messages with attachments
CREATE INDEX idx_messages_with_files ON chat_messages(conversation_id, file_url);

-- Update message_type to support new types if needed
-- Existing types: TEXT
-- New types: IMAGE, FILE
-- message_type column already exists, no need to modify

COMMIT;

