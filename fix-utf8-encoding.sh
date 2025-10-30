#!/bin/bash

echo "=========================================="
echo "  FIX UTF-8 ENCODING FOR CHAT TABLES"
echo "=========================================="
echo ""

# Load .env
if [ -f .env ]; then
    export $(cat .env | grep -v '^#' | xargs)
    echo "✅ .env loaded"
    echo "   Database: $DB_HOST/$DB_NAME"
    echo ""
else
    echo "❌ .env file not found!"
    exit 1
fi

# Check if mysql client is available
if ! command -v mysql &> /dev/null; then
    echo "❌ MySQL client not installed!"
    echo ""
    echo "You need to install MySQL client to run this fix."
    echo "Or manually execute: sql/fix_chat_utf8.sql in phpmyadmin/adminer"
    exit 1
fi

echo "Connecting to database..."
if ! mysql -h $DB_HOST -u $DB_USERNAME -p$DB_PASSWORD $DB_NAME -e "SELECT 1;" &>/dev/null; then
    echo "❌ Cannot connect to database!"
    echo "   Please check your .env credentials"
    exit 1
fi

echo "✅ Connected successfully"
echo ""

# Check current encoding
echo "Checking current encoding..."
CHARSET=$(mysql -h $DB_HOST -u $DB_USERNAME -p$DB_PASSWORD $DB_NAME -e "SELECT CCSA.character_set_name FROM information_schema.TABLES T, information_schema.COLLATION_CHARACTER_SET_APPLICABILITY CCSA WHERE CCSA.collation_name = T.table_collation AND T.table_schema = '$DB_NAME' AND T.table_name = 'chat_conversations';" 2>/dev/null | tail -n 1)

if [ -z "$CHARSET" ]; then
    echo "⚠️  Table 'chat_conversations' not found"
    echo "   Creating tables first..."

    if mysql -h $DB_HOST -u $DB_USERNAME -p$DB_PASSWORD $DB_NAME < sql/create_chat_tables.sql 2>/dev/null; then
        echo "✅ Tables created"
    else
        echo "❌ Failed to create tables"
        exit 1
    fi
else
    echo "   Current charset: $CHARSET"
fi

echo ""
echo "Applying UTF-8 fix..."
echo ""

# Run the fix
if mysql -h $DB_HOST -u $DB_USERNAME -p$DB_PASSWORD $DB_NAME < sql/fix_chat_utf8.sql 2>&1 | tail -10; then
    echo ""
    echo "=========================================="
    echo "✅ UTF-8 ENCODING FIXED!"
    echo "=========================================="
    echo ""
    echo "Chat tables now support Vietnamese characters!"
    echo ""
    echo "Next steps:"
    echo "1. Restart your application"
    echo "2. Try 'Chat với shop' again"
    echo "3. It should work now! 🎉"
    echo ""
else
    echo ""
    echo "❌ Failed to apply UTF-8 fix"
    echo ""
    echo "You can manually run this SQL in phpmyadmin/adminer:"
    echo "   File: sql/fix_chat_utf8.sql"
    exit 1
fi

# Verify the fix
echo "Verifying..."
NEW_CHARSET=$(mysql -h $DB_HOST -u $DB_USERNAME -p$DB_PASSWORD $DB_NAME -e "SELECT CCSA.character_set_name FROM information_schema.TABLES T, information_schema.COLLATION_CHARACTER_SET_APPLICABILITY CCSA WHERE CCSA.collation_name = T.table_collation AND T.table_schema = '$DB_NAME' AND T.table_name = 'chat_conversations';" 2>/dev/null | tail -n 1)

echo "   New charset: $NEW_CHARSET"

if [ "$NEW_CHARSET" = "utf8mb4" ]; then
    echo ""
    echo "✅ Verification passed!"
    echo "   Tables are now using utf8mb4"
else
    echo ""
    echo "⚠️  Charset is $NEW_CHARSET (expected utf8mb4)"
    echo "   But it might still work. Try testing!"
fi

echo ""
echo "=========================================="

