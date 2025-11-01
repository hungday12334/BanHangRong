#!/bin/bash

# Script to apply enhanced chat features database updates

echo "======================================"
echo "Enhanced Chat Features - Database Update"
echo "======================================"
echo ""

# Database configuration
DB_HOST="localhost"
DB_PORT="3306"
DB_NAME="smiledev_wap"
DB_USER="root"
DB_PASS=""

# Path to SQL file
SQL_FILE="sql/add_enhanced_chat_columns.sql"

echo "📊 Database: $DB_NAME"
echo "📁 SQL File: $SQL_FILE"
echo ""

# Check if SQL file exists
if [ ! -f "$SQL_FILE" ]; then
    echo "❌ Error: SQL file not found at $SQL_FILE"
    exit 1
fi

echo "⚠️  This will add the following columns to chat_messages table:"
echo "   - reactions (TEXT)"
echo "   - reply_to_message_id (VARCHAR 255)"
echo "   - reply_to_sender_name (VARCHAR 255)"
echo "   - reply_to_content (TEXT)"
echo "   - deleted (BOOLEAN)"
echo ""

read -p "Do you want to continue? (y/n): " -n 1 -r
echo ""

if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    echo "❌ Operation cancelled"
    exit 1
fi

echo ""
echo "🔄 Applying database updates..."
echo ""

# Execute SQL file
if [ -z "$DB_PASS" ]; then
    mysql -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" "$DB_NAME" < "$SQL_FILE"
else
    mysql -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" -p"$DB_PASS" "$DB_NAME" < "$SQL_FILE"
fi

if [ $? -eq 0 ]; then
    echo ""
    echo "✅ Database updated successfully!"
    echo ""
    echo "📋 Summary:"
    echo "   • Added reactions column for emoji reactions"
    echo "   • Added reply fields for message replies"
    echo "   • Added deleted flag for soft delete"
    echo "   • Created indexes for better performance"
    echo ""
    echo "🚀 Your enhanced chat features are now ready!"
else
    echo ""
    echo "❌ Error: Failed to update database"
    echo "   Please check the error messages above"
    exit 1
fi

