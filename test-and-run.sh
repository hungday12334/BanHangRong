#!/bin/bash

echo "=========================================="
echo "  QUICK TEST & FIX"
echo "=========================================="
echo ""

# Load .env
if [ -f .env ]; then
    export $(cat .env | grep -v '^#' | xargs)
    echo "✅ .env loaded"
else
    echo "❌ .env not found!"
    exit 1
fi

echo ""
echo "Testing database connection..."
echo ""

# Test connection (if mysql available)
if command -v mysql &> /dev/null; then
    echo "Connecting to $DB_HOST/$DB_NAME..."

    # Test basic connection
    if mysql -h $DB_HOST -u $DB_USERNAME -p$DB_PASSWORD $DB_NAME -e "SELECT 1;" &>/dev/null; then
        echo "✅ Database connection OK"
        echo ""

    # Check chat tables
    echo "Checking chat tables..."
    CHAT_TABLES=$(mysql -h $DB_HOST -u $DB_USERNAME -p$DB_PASSWORD $DB_NAME -e "SHOW TABLES LIKE 'chat_%';" 2>/dev/null | tail -n +2)

    if [ -z "$CHAT_TABLES" ]; then
        echo "❌ Chat tables NOT found"
        echo ""
        echo "Creating chat tables..."

        if mysql -h $DB_HOST -u $DB_USERNAME -p$DB_PASSWORD $DB_NAME < sql/create_chat_tables.sql 2>/dev/null; then
            echo "✅ Chat tables created!"

            # Fix UTF-8 encoding immediately after creating tables
            echo "Fixing UTF-8 encoding..."
            if mysql -h $DB_HOST -u $DB_USERNAME -p$DB_PASSWORD $DB_NAME < sql/fix_chat_utf8.sql 2>/dev/null; then
                echo "✅ UTF-8 encoding fixed!"
            fi
        else
            echo "⚠️  Failed to create via mysql client"
            echo "   App will try to auto-create them"
        fi
    else
        echo "✅ Chat tables found:"
        echo "$CHAT_TABLES" | sed 's/^/   - /'

        # Check and fix UTF-8 encoding
        echo ""
        echo "Checking UTF-8 encoding..."
        CHARSET=$(mysql -h $DB_HOST -u $DB_USERNAME -p$DB_PASSWORD $DB_NAME -e "SELECT CCSA.character_set_name FROM information_schema.TABLES T, information_schema.COLLATION_CHARACTER_SET_APPLICABILITY CCSA WHERE CCSA.collation_name = T.table_collation AND T.table_schema = '$DB_NAME' AND T.table_name = 'chat_conversations';" 2>/dev/null | tail -n 1)

        if [ "$CHARSET" != "utf8mb4" ]; then
            echo "⚠️  Tables not using utf8mb4 (current: $CHARSET)"
            echo "   Fixing encoding..."

            if mysql -h $DB_HOST -u $DB_USERNAME -p$DB_PASSWORD $DB_NAME < sql/fix_chat_utf8.sql 2>/dev/null; then
                echo "✅ UTF-8 encoding fixed!"
            else
                echo "⚠️  Could not fix encoding, but will try to continue"
            fi
        else
            echo "✅ UTF-8 encoding is correct (utf8mb4)"
        fi
    fi
        echo ""

        # Check sellers
        echo "Checking sellers..."
        SELLER_COUNT=$(mysql -h $DB_HOST -u $DB_USERNAME -p$DB_PASSWORD $DB_NAME -e "SELECT COUNT(*) FROM users WHERE user_type='SELLER';" 2>/dev/null | tail -n 1)

        if [ "$SELLER_COUNT" -eq 0 ]; then
            echo "❌ No sellers found in database!"
            echo ""
            echo "You need to create at least one seller account:"
            echo "   1. Register a new account"
            echo "   2. Update in database: UPDATE users SET user_type='SELLER' WHERE user_id=X;"
        else
            echo "✅ Found $SELLER_COUNT seller(s)"

            # Show sellers
            echo ""
            echo "Available sellers:"
            mysql -h $DB_HOST -u $DB_USERNAME -p$DB_PASSWORD $DB_NAME -e "SELECT user_id, username, full_name, user_type FROM users WHERE user_type='SELLER' LIMIT 5;" 2>/dev/null | tail -n +2 | while read line; do
                echo "   - $line"
            done
        fi

    else
        echo "❌ Cannot connect to database"
        echo "   Host: $DB_HOST"
        echo "   Database: $DB_NAME"
        echo "   Username: $DB_USERNAME"
        exit 1
    fi
else
    echo "⚠️  MySQL client not installed"
    echo "   Skipping database check"
    echo "   App will auto-create tables if needed"
fi

echo ""
echo "=========================================="
echo "  BUILDING & STARTING APPLICATION"
echo "=========================================="
echo ""

# Build
echo "Building..."
./mvnw clean package -DskipTests -q

if [ $? -ne 0 ]; then
    echo "❌ Build failed!"
    exit 1
fi

echo "✅ Build successful!"
echo ""
echo "Starting with online database..."
echo "   Profile: simple"
echo "   Database: $DB_HOST/$DB_NAME"
echo "   URL: http://localhost:8080"
echo ""
echo "Watch the logs below for any errors..."
echo "Press Ctrl+C to stop"
echo ""
echo "=========================================="
echo ""

# Run
java -jar -Dspring.profiles.active=simple target/su25-0.0.1-SNAPSHOT.jar

