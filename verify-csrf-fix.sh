#!/bin/bash

echo "======================================"
echo "CSRF FIX VERIFICATION"
echo "======================================"
echo ""

echo "Checking if CSRF meta tags are present..."
echo ""

# Check customer chat
if grep -q '<meta name="_csrf"' src/main/resources/templates/customer/chat.html; then
    echo "✅ customer/chat.html: CSRF meta tags found"
else
    echo "❌ customer/chat.html: CSRF meta tags MISSING"
fi

# Check seller chat
if grep -q '<meta name="_csrf"' src/main/resources/templates/seller/chat.html; then
    echo "✅ seller/chat.html: CSRF meta tags found"
else
    echo "❌ seller/chat.html: CSRF meta tags MISSING"
fi

echo ""
echo "Checking if CSRF token is used in POST requests..."
echo ""

# Check customer chat for CSRF in fetch
CUSTOMER_CSRF_COUNT=$(grep -c "csrfToken = document.querySelector" src/main/resources/templates/customer/chat.html)
echo "✅ customer/chat.html: Found $CUSTOMER_CSRF_COUNT CSRF token usages"

# Check seller chat for CSRF in fetch
SELLER_CSRF_COUNT=$(grep -c "csrfToken = document.querySelector" src/main/resources/templates/seller/chat.html)
echo "✅ seller/chat.html: Found $SELLER_CSRF_COUNT CSRF token usages"

echo ""
echo "======================================"
echo "Summary:"
echo "======================================"
echo ""

if grep -q '<meta name="_csrf"' src/main/resources/templates/customer/chat.html && \
   grep -q '<meta name="_csrf"' src/main/resources/templates/seller/chat.html && \
   [ "$CUSTOMER_CSRF_COUNT" -ge 3 ] && \
   [ "$SELLER_CSRF_COUNT" -ge 3 ]; then
    echo "✅ ALL CHECKS PASSED!"
    echo ""
    echo "You can now run the application:"
    echo "  ./start-with-chat-fix.sh"
    echo ""
    echo "Or manually:"
    echo "  ./mvnw clean package -DskipTests"
    echo "  java -jar target/su25-0.0.1-SNAPSHOT.jar"
    exit 0
else
    echo "⚠️  SOME CHECKS FAILED"
    echo ""
    echo "Please review the changes in:"
    echo "  - src/main/resources/templates/customer/chat.html"
    echo "  - src/main/resources/templates/seller/chat.html"
    exit 1
fi

