#!/bin/bash

echo "=================================================="
echo "  🔧 QUICK FIX - Seller Chat Not Showing"
echo "=================================================="
echo ""

echo "Bước 1: Build lại application với code mới"
echo "--------------------------------------------------"
./mvnw clean package -DskipTests
if [ $? -ne 0 ]; then
    echo "❌ Build failed!"
    exit 1
fi
echo "✅ Build thành công!"
echo ""

echo "Bước 2: Khởi động lại application"
echo "--------------------------------------------------"
echo "⚠️  QUAN TRỌNG: Bạn cần STOP application cũ và START lại!"
echo ""
echo "Cách 1: Nếu đang chạy trong terminal:"
echo "  - Nhấn Ctrl+C để stop"
echo "  - Chạy: java -jar target/su25-0.0.1-SNAPSHOT.jar"
echo ""
echo "Cách 2: Nếu đang chạy trong IDE:"
echo "  - Nhấn nút Stop"
echo "  - Nhấn nút Run lại"
echo ""
echo "Sau khi restart, tiếp tục các bước dưới đây..."
echo ""

echo "Bước 3: Test trong browser"
echo "--------------------------------------------------"
echo ""
echo "1. Mở seller chat: http://localhost:8080/seller/chat"
echo ""
echo "2. Mở Console (F12 → Console tab)"
echo ""
echo "3. Tìm các log sau:"
echo "   🔄 Loading conversations for SELLER: [userId]"
echo "   📡 Response status: 200"
echo "   ✓ Loaded conversations: [số lượng]"
echo "   🎨 Rendering conversations list, count: [số lượng]"
echo ""
echo "4. Nếu thấy 'Loaded conversations: 0'"
echo "   → Chạy trong Console:"
echo ""
cat << 'JSEOF'
// Test API trực tiếp
fetch('/api/conversations/' + currentUser.userId)
  .then(res => res.json())
  .then(data => {
    console.log('API Response:', data);
    if (data.length === 0) {
      console.error('❌ API trả về empty! Seller chưa có conversations.');
    } else {
      console.log('✅ API có data:', data.length, 'conversations');
      conversations = data;
      renderConversationsList();
    }
  })
  .catch(err => console.error('❌ API Error:', err));
JSEOF
echo ""

echo "Bước 4: Nếu vẫn không hiện"
echo "--------------------------------------------------"
echo ""
echo "A. Kiểm tra Database có conversations không:"
echo "   - Mở: http://localhost:8080/h2-console"
echo "   - JDBC URL: jdbc:h2:./data/banhangrong_db"
echo "   - Chạy query:"
echo ""
cat << 'SQLEOF'
SELECT c.*,
       (SELECT COUNT(*) FROM chat_messages m WHERE m.conversation_id = c.id) as msg_count
FROM chat_conversations c
WHERE c.seller_id = (SELECT user_id FROM users WHERE user_type = 'SELLER' LIMIT 1)
ORDER BY c.updated_at DESC;
SQLEOF
echo ""
echo "   Nếu kết quả RỖNG → Seller chưa có conversation nào!"
echo "   → Cần customer gửi tin nhắn đến seller trước"
echo ""

echo "B. Tạo test conversation:"
echo "   Chạy SQL này trong H2 Console:"
echo ""
cat << 'SQLEOF'
-- Lấy customer_id và seller_id
SELECT user_id, username, user_type FROM users WHERE user_type IN ('CUSTOMER', 'SELLER') ORDER BY user_type;

-- Tạo conversation (thay customerId và sellerId phù hợp)
INSERT INTO chat_conversations (id, customer_id, customer_name, seller_id, seller_name, created_at, updated_at)
VALUES ('test_' || RANDOM_UUID(),
        (SELECT user_id FROM users WHERE user_type = 'CUSTOMER' LIMIT 1),
        (SELECT username FROM users WHERE user_type = 'CUSTOMER' LIMIT 1),
        (SELECT user_id FROM users WHERE user_type = 'SELLER' LIMIT 1),
        (SELECT username FROM users WHERE user_type = 'SELLER' LIMIT 1),
        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Thêm tin nhắn test
INSERT INTO chat_messages (conversation_id, sender_id, receiver_id, content, created_at, is_read)
SELECT id, customer_id, seller_id, 'Hello from customer!', CURRENT_TIMESTAMP, FALSE
FROM chat_conversations
WHERE id LIKE 'test_%'
ORDER BY created_at DESC LIMIT 1;
SQLEOF
echo ""

echo "C. Reset deleted conversations (nếu bị xóa nhầm):"
echo ""
cat << 'SQLEOF'
UPDATE user_conversation_metadata
SET is_deleted = FALSE, deleted_at = NULL
WHERE user_id = (SELECT user_id FROM users WHERE user_type = 'SELLER' LIMIT 1);
SQLEOF
echo ""

echo "=================================================="
echo "  📝 Summary"
echo "=================================================="
echo ""
echo "✅ Đã build application với:"
echo "   - Fix delete conversation bug"
echo "   - Fix pin conversation sorting"
echo "   - Thêm debug logs cho seller chat"
echo ""
echo "⚠️  PHẢI RESTART application để code mới có hiệu lực!"
echo ""
echo "🔍 Debug steps:"
echo "   1. Check console logs trong browser"
echo "   2. Check API response: /api/conversations/[sellerId]"
echo "   3. Check database có conversations không"
echo "   4. Tạo test conversation nếu cần"
echo ""
echo "📚 Chi tiết: Xem SELLER_CHAT_DEBUG_GUIDE.md"
echo ""
echo "=================================================="

