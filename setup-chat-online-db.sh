#!/bin/bash

echo "================================"
echo "SETUP CHAT TABLES FOR ONLINE DB"
echo "================================"
echo ""

# Load environment variables from .env file
if [ -f .env ]; then
    export $(cat .env | grep -v '^#' | xargs)
    echo "✓ Loaded environment variables from .env"
    echo "  DB_HOST: $DB_HOST"
    echo "  DB_NAME: $DB_NAME"
    echo "  DB_USERNAME: $DB_USERNAME"
else
    echo "❌ Error: .env file not found"
    exit 1
fi

echo ""
echo "================================"
echo "Creating chat tables..."
echo "================================"

# Create SQL script for chat tables
cat > /tmp/create_chat_tables.sql << 'EOF'
-- Chat System Database Schema
-- This script creates the necessary tables for the chat feature between customers and sellers

-- Create chat_conversations table
CREATE TABLE IF NOT EXISTS chat_conversations (
    id VARCHAR(100) PRIMARY KEY,
    customer_id BIGINT NOT NULL,
    customer_name VARCHAR(100),
    seller_id BIGINT NOT NULL,
    seller_name VARCHAR(100),
    last_message TEXT,
    last_message_time TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_customer_seller (customer_id, seller_id),
    INDEX idx_customer (customer_id),
    INDEX idx_seller (seller_id),
    INDEX idx_last_message_time (last_message_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Create chat_messages table
CREATE TABLE IF NOT EXISTS chat_messages (
    message_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    conversation_id VARCHAR(100) NOT NULL,
    sender_id BIGINT NOT NULL,
    sender_name VARCHAR(100),
    sender_role VARCHAR(20),
    receiver_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    message_type VARCHAR(20) DEFAULT 'TEXT',
    is_read BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_conversation (conversation_id),
    INDEX idx_sender (sender_id),
    INDEX idx_receiver (receiver_id),
    INDEX idx_created_at (created_at),
    INDEX idx_is_read (is_read),
    INDEX idx_unread_messages (conversation_id, receiver_id, is_read),
    INDEX idx_conversation_messages (conversation_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Show tables
SELECT 'Tables created successfully!' as status;
SHOW TABLES LIKE 'chat_%';
EOF

# Execute SQL script using PHP mysqli (since mysql command might not be available)
php << 'PHPCODE'
<?php
$host = getenv('DB_HOST');
$database = getenv('DB_NAME');
$username = getenv('DB_USERNAME');
$password = getenv('DB_PASSWORD');

echo "\nConnecting to database...\n";
$mysqli = new mysqli($host, $username, $password, $database);

if ($mysqli->connect_error) {
    die("❌ Connection failed: " . $mysqli->connect_error . "\n");
}

echo "✓ Connected to database successfully!\n\n";

// Read and execute SQL file
$sql = file_get_contents('/tmp/create_chat_tables.sql');

if ($mysqli->multi_query($sql)) {
    do {
        if ($result = $mysqli->store_result()) {
            while ($row = $result->fetch_assoc()) {
                print_r($row);
            }
            $result->free();
        }
        if ($mysqli->more_results()) {
            echo "\n";
        }
    } while ($mysqli->next_result());

    echo "\n✓ Chat tables created successfully!\n";
} else {
    echo "❌ Error executing SQL: " . $mysqli->error . "\n";
}

$mysqli->close();
?>
PHPCODE

# Clean up
rm -f /tmp/create_chat_tables.sql

echo ""
echo "================================"
echo "Setup completed!"
echo "================================"
echo ""
echo "Now you can run the application with:"
echo "  ./run-with-online-db.sh"
echo ""

