#!/bin/bash

echo "=========================================="
echo "  CHECK & SETUP ONLINE DATABASE"
echo "=========================================="
echo ""

# Load .env
if [ -f .env ]; then
    export $(cat .env | grep -v '^#' | xargs)
    echo "✅ Loaded .env file"
    echo "   DB_HOST: $DB_HOST"
    echo "   DB_NAME: $DB_NAME"
    echo "   DB_USERNAME: $DB_USERNAME"
    echo ""
else
    echo "❌ .env file not found!"
    exit 1
fi

# Check if mysql client is available
if ! command -v mysql &> /dev/null; then
    echo "⚠️  MySQL client not installed"
    echo ""
    echo "Skipping database check..."
    echo "The app will auto-create tables with JPA (ddl-auto=update)"
    echo ""
else
    echo "Checking database tables..."
    echo ""

    # Check chat tables
    TABLES=$(mysql -h $DB_HOST -u $DB_USERNAME -p$DB_PASSWORD $DB_NAME -e "SHOW TABLES LIKE 'chat%';" 2>/dev/null | tail -n +2)

    if [ -z "$TABLES" ]; then
        echo "⚠️  Chat tables not found"
        echo ""
        echo "Creating chat tables..."
        mysql -h $DB_HOST -u $DB_USERNAME -p$DB_PASSWORD $DB_NAME < sql/create_chat_tables.sql 2>/dev/null

        if [ $? -eq 0 ]; then
            echo "✅ Chat tables created successfully!"
        else
            echo "⚠️  Could not create tables via mysql client"
            echo "   App will try to create them automatically"
        fi
    else
        echo "✅ Chat tables found:"
        echo "$TABLES"
    fi
    echo ""
fi

echo "=========================================="
echo "  BUILDING APPLICATION"
echo "=========================================="
echo ""

./mvnw clean package -DskipTests

if [ $? -ne 0 ]; then
    echo ""
    echo "❌ Build failed!"
    exit 1
fi

echo ""
echo "✅ Build successful!"
echo ""
echo "=========================================="
echo "  STARTING APPLICATION"
echo "=========================================="
echo ""
echo "Using profile: simple (online database)"
echo "Database: $DB_HOST/$DB_NAME"
echo ""
echo "Server will start on: http://localhost:8080"
echo ""
echo "Press Ctrl+C to stop"
echo ""
echo "=========================================="
echo ""

# Run with simple profile (uses .env for DB config)
java -jar -Dspring.profiles.active=simple target/su25-0.0.1-SNAPSHOT.jar

