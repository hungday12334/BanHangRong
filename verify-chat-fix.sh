#!/bin/bash

echo "================================"
echo "Chat Access Fix - Verification"
echo "================================"
echo ""

# Check .env file
if [ -f .env ]; then
    echo "✓ .env file exists"
    export $(cat .env | grep -v '^#' | xargs)
    echo "  DB_HOST: $DB_HOST"
    echo "  DB_NAME: $DB_NAME"
    echo "  DB_USERNAME: $DB_USERNAME"
    echo ""
else
    echo "✗ .env file not found"
    exit 1
fi

# Check modified files
echo "Checking modified files..."
files=(
    "src/main/java/banhangrong/su25/Controller/ChatController.java"
    "src/main/java/banhangrong/su25/service/ChatService.java"
    "src/main/resources/templates/seller/chat.html"
    "src/main/resources/application.properties"
)

all_exist=true
for file in "${files[@]}"; do
    if [ -f "$file" ]; then
        echo "  ✓ $file"
    else
        echo "  ✗ $file (missing)"
        all_exist=false
    fi
done
echo ""

if [ "$all_exist" = false ]; then
    echo "✗ Some required files are missing"
    exit 1
fi

# Check for getUserByUsername method in ChatService
if grep -q "getUserByUsername" "src/main/java/banhangrong/su25/service/ChatService.java"; then
    echo "✓ ChatService.getUserByUsername() method exists"
else
    echo "✗ ChatService.getUserByUsername() method not found"
    exit 1
fi

# Check for Model parameter in ChatController
if grep -q "org.springframework.ui.Model model" "src/main/java/banhangrong/su25/Controller/ChatController.java"; then
    echo "✓ ChatController methods updated with Model parameter"
else
    echo "✗ ChatController methods not updated"
    exit 1
fi

# Check for ${user} in chat.html
if grep -q '\[\[${user}\]\]' "src/main/resources/templates/seller/chat.html"; then
    echo "✓ chat.html template uses correct user variable"
else
    echo "✗ chat.html template still uses old variable"
    exit 1
fi

# Check database configuration
if grep -q 'DB_HOST' "src/main/resources/application.properties"; then
    echo "✓ application.properties configured for environment variables"
else
    echo "✗ application.properties not configured correctly"
    exit 1
fi

echo ""
echo "================================"
echo "All checks passed! ✓"
echo "================================"
echo ""
echo "To run the application:"
echo "  ./run-with-online-db.sh"
echo ""
echo "Or manually:"
echo "  export \$(cat .env | grep -v '^#' | xargs)"
echo "  ./mvnw spring-boot:run"
echo ""

