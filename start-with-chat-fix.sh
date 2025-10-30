#!/bin/bash

echo "======================================"
echo "CHAT CSRF FIX - Quick Start Guide"
echo "======================================"
echo ""

# Check if application is running
if pgrep -f "su25-0.0.1-SNAPSHOT.jar" > /dev/null; then
    echo "⚠️  Application is already running"
    echo "   Stop it first: pkill -f su25-0.0.1-SNAPSHOT.jar"
    echo ""
    exit 1
fi

echo "Step 1: Cleaning and compiling..."
./mvnw clean compile -DskipTests

if [ $? -ne 0 ]; then
    echo "❌ Compilation failed!"
    exit 1
fi

echo ""
echo "✅ Compilation successful!"
echo ""
echo "Step 2: Packaging..."
./mvnw package -DskipTests

if [ $? -ne 0 ]; then
    echo "❌ Packaging failed!"
    exit 1
fi

echo ""
echo "✅ Package created successfully!"
echo ""
echo "Step 3: Starting application..."
echo "   Server will start on http://localhost:8080"
echo ""
echo "To test the fix:"
echo "1. Open browser: http://localhost:8080"
echo "2. Login as customer"
echo "3. Go to product detail page"
echo "4. Click 'Chat với shop'"
echo "5. Check if conversation is created successfully"
echo ""
echo "Press Ctrl+C to stop the server"
echo ""
echo "======================================"
echo ""

java -jar target/su25-0.0.1-SNAPSHOT.jar

