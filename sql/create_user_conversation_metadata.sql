-- Create user_conversation_metadata table
-- This table stores per-user metadata for conversations (pin/delete status)
-- Local only - does not affect other participants

CREATE TABLE IF NOT EXISTS user_conversation_metadata (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    conversation_id VARCHAR(255) NOT NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    is_pinned BOOLEAN NOT NULL DEFAULT FALSE,
    pinned_at TIMESTAMP NULL,
    deleted_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    UNIQUE KEY unique_user_conversation (user_id, conversation_id),
    INDEX idx_user_id (user_id),
    INDEX idx_conversation_id (conversation_id),
    INDEX idx_is_deleted (is_deleted),
    INDEX idx_is_pinned (is_pinned)
);

-- Add comments
ALTER TABLE user_conversation_metadata
    COMMENT = 'Stores per-user metadata for conversations (pin/delete status)';

