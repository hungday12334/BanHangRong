#!/bin/bash

# Script để test email verification flow
# Sử dụng: ./test-email-verification.sh [username]

USERNAME=${1:-testuser}
BASE_URL="http://localhost:8080"

echo "================================"
echo "TEST EMAIL VERIFICATION"
echo "================================"
echo "Username: $USERNAME"
echo ""

# 1. Kiểm tra xem user có tồn tại không
echo "1. Checking if user exists in database..."
echo "   Run this SQL query in your database:"
echo "   SELECT user_id, username, email, is_email_verified FROM users WHERE username = '$USERNAME';"
echo ""
read -p "Does user exist? (y/n): " user_exists

if [ "$user_exists" != "y" ]; then
    echo "❌ User does not exist. Please register first."
    exit 1
fi

# 2. Kiểm tra email verification status
read -p "Is email already verified? (y/n): " already_verified

if [ "$already_verified" = "y" ]; then
    echo "✅ Email is already verified. No need to test."
    exit 0
fi

# 3. Test resend verification API
echo ""
echo "2. Testing resend verification API..."
response=$(curl -s -X POST "$BASE_URL/api/auth/resend-verification?username=$USERNAME")
echo "Response: $response"

if echo "$response" | grep -q "Verification email has been sent"; then
    echo "✅ API call successful!"
    echo ""
    echo "3. Check your email for verification code"
    echo "   Email should contain:"
    echo "   - 6-digit verification code"
    echo "   - Verification link"
    echo ""

    read -p "Did you receive the email? (y/n): " received_email

    if [ "$received_email" = "y" ]; then
        echo "✅ Email received successfully!"
        echo ""
        echo "4. To verify, you can:"
        echo "   Option A: Click the link in email"
        echo "   Option B: Go to $BASE_URL/customer/verify-code and enter the code"
        echo ""
        read -p "Enter verification code from email: " code

        if [ -n "$code" ]; then
            echo ""
            echo "5. Verify using link method:"
            echo "   Open this URL in browser:"
            echo "   $BASE_URL/customer/verify-email?token=$code"
            echo ""
            echo "   Or run this command:"
            echo "   open '$BASE_URL/customer/verify-email?token=$code'"
        fi
    else
        echo "❌ Email not received. Check:"
        echo "   - Email server configuration in application.properties"
        echo "   - Spam/Junk folder"
        echo "   - Email address is correct"
        echo ""
        echo "Alternative: Verify directly via database:"
        echo "   UPDATE users SET is_email_verified = TRUE WHERE username = '$USERNAME';"
    fi
else
    echo "❌ API call failed"
    echo "Error: $response"
    echo ""
    echo "Possible issues:"
    echo "   - User not found"
    echo "   - Email already verified"
    echo "   - Server not running"
fi

echo ""
echo "================================"
echo "To check verification status, run this SQL:"
echo "SELECT username, email, is_email_verified FROM users WHERE username = '$USERNAME';"
echo ""
echo "To view verification token, run this SQL:"
echo "SELECT evt.token, evt.expires_at, evt.is_used"
echo "FROM email_verification_tokens evt"
echo "JOIN users u ON evt.user_id = u.user_id"
echo "WHERE u.username = '$USERNAME' AND evt.is_used = FALSE"
echo "ORDER BY evt.created_at DESC LIMIT 1;"
echo "================================"

