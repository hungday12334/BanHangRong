#!/bin/bash
# Run this script to setup chat file upload feature

echo "🚀 Setting up Chat File Upload Feature..."
echo ""

# Step 1: Run SQL migration
echo "📊 Step 1: Running SQL migration..."
mysql -u root -p1234 wap << 'EOF'
-- Add file attachment columns to chat_messages table
ALTER TABLE chat_messages
ADD COLUMN file_url VARCHAR(500) AFTER content,
ADD COLUMN file_name VARCHAR(255) AFTER file_url,
ADD COLUMN file_type VARCHAR(50) AFTER file_name,
ADD COLUMN file_size BIGINT AFTER file_type;

-- Add index for messages with attachments
CREATE INDEX idx_messages_with_files ON chat_messages(conversation_id, file_url);

SELECT 'Migration completed successfully!' AS status;
EOF

if [ $? -eq 0 ]; then
    echo "✅ SQL migration completed successfully!"
else
    echo "❌ SQL migration failed. Please check your database credentials."
    exit 1
fi

echo ""
echo "📁 Step 2: Creating upload directories..."
mkdir -p uploads/chat/images
mkdir -p uploads/chat/files
chmod -R 755 uploads
echo "✅ Upload directories created!"

echo ""
echo "🎉 Setup completed successfully!"
echo ""
echo "Next steps:"
echo "1. Start the application: java -jar target/su25-0.0.1-SNAPSHOT.jar"
echo "2. Test chat file upload features"
echo ""
echo "Features available:"
echo "  📷 Upload and send images (max 5MB)"
echo "  📎 Upload and send files (max 10MB)"
echo "  😀 Insert emojis"
echo ""
echo "Happy chatting! 💬"

