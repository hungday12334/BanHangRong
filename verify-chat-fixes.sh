#!/bin/bash

# 🧪 Quick Test Script - Verify All Fixes
# Run this after starting the server to test all improvements

echo "╔══════════════════════════════════════════════════════╗"
echo "║   🧪 Chat System - Quick Verification Test         ║"
echo "╚══════════════════════════════════════════════════════╝"
echo ""

GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}This script will guide you through testing all fixes.${NC}"
echo -e "${BLUE}Make sure the server is running on http://localhost:8080${NC}"
echo ""
read -p "Press Enter to start testing..."
echo ""

# Test 1: Error Handling
echo -e "${YELLOW}═══════════════════════════════════════${NC}"
echo -e "${YELLOW}  TEST 1: Error Handling ✅             ${NC}"
echo -e "${YELLOW}═══════════════════════════════════════${NC}"
echo ""
echo "1. Login as customer (username: customer1, password: 123)"
echo "2. Open: http://localhost:8080/customer/chat?sellerId=99999"
echo "3. Check for:"
echo "   ✅ Error toast appears: 'Cannot start chat: Seller not found'"
echo "   ✅ Page redirects after 2 seconds"
echo ""
read -p "Did the test pass? (y/n): " test1
echo ""

# Test 2: WebSocket Disconnect UI
echo -e "${YELLOW}═══════════════════════════════════════${NC}"
echo -e "${YELLOW}  TEST 2: WebSocket Disconnect UI ✅    ${NC}"
echo -e "${YELLOW}═══════════════════════════════════════${NC}"
echo ""
echo "1. Login and open chat page"
echo "2. Open Chrome DevTools (F12)"
echo "3. Go to Network tab"
echo "4. Set throttling to 'Offline'"
echo "5. Check for:"
echo "   ✅ Send button becomes gray and disabled"
echo "   ✅ Input field shows '⚠️ Disconnected - Reconnecting...'"
echo "   ✅ Input field is disabled"
echo "   ✅ Red banner at top: '❌ Connection Lost'"
echo "6. Set back to 'Online'"
echo "7. Check for:"
echo "   ✅ Send button enabled again"
echo "   ✅ Input field enabled"
echo "   ✅ Green banner briefly: '✅ Connected'"
echo ""
read -p "Did the test pass? (y/n): " test2
echo ""

# Test 3: Message Length Validation
echo -e "${YELLOW}═══════════════════════════════════════${NC}"
echo -e "${YELLOW}  TEST 3: Message Length Validation ✅  ${NC}"
echo -e "${YELLOW}═══════════════════════════════════════${NC}"
echo ""
echo "1. Login and open chat"
echo "2. Start typing in message input"
echo "3. Check for:"
echo "   ✅ Character counter appears: '15/5000'"
echo "   ✅ Counter is gray for normal length"
echo "4. Type a very long message (paste Lorem Ipsum 10 times)"
echo "5. Check for:"
echo "   ✅ Counter turns orange when > 4500 characters"
echo "   ✅ Input limited to 5000 characters (can't type more)"
echo ""
read -p "Did the test pass? (y/n): " test3
echo ""

# Test 4: Rate Limiting
echo -e "${YELLOW}═══════════════════════════════════════${NC}"
echo -e "${YELLOW}  TEST 4: Rate Limiting Feedback ✅     ${NC}"
echo -e "${YELLOW}═══════════════════════════════════════${NC}"
echo ""
echo "1. Login and open chat"
echo "2. Rapidly type and send messages (spam Enter key)"
echo "3. Check for:"
echo "   ✅ Toast appears: '⚠️ Please slow down!'"
echo "   ✅ Some messages are prevented from sending"
echo ""
read -p "Did the test pass? (y/n): " test4
echo ""

# Test 5: Persist Conversation
echo -e "${YELLOW}═══════════════════════════════════════${NC}"
echo -e "${YELLOW}  TEST 5: Persist Conversation ✅       ${NC}"
echo -e "${YELLOW}═══════════════════════════════════════${NC}"
echo ""
echo "1. Login as seller"
echo "2. Open chat page"
echo "3. Click on a conversation"
echo "4. Press F5 or Cmd+R to refresh"
echo "5. Check for:"
echo "   ✅ Same conversation opens automatically"
echo "   ✅ Message history is preserved"
echo "   ✅ No need to click again"
echo ""
read -p "Did the test pass? (y/n): " test5
echo ""

# Test 6: Message Status
echo -e "${YELLOW}═══════════════════════════════════════${NC}"
echo -e "${YELLOW}  TEST 6: Message Status Indicators ✅  ${NC}"
echo -e "${YELLOW}═══════════════════════════════════════${NC}"
echo ""
echo "1. Login and open chat"
echo "2. Send a message"
echo "3. Watch the message bubble carefully"
echo "4. Check for:"
echo "   ✅ Status indicator appears: ○ (sending)"
echo "   ✅ Changes to: ✓ (sent) after ~500ms"
echo "5. Go offline (Network tab > Offline)"
echo "6. Send another message"
echo "7. Check for:"
echo "   ✅ Status shows: ⏳ (queued)"
echo "   ✅ Toast: 'Offline - Message will be sent when reconnected'"
echo "8. Go back online"
echo "9. Check for:"
echo "   ✅ Toast: '📤 Sending 1 queued message...'"
echo "   ✅ Message gets sent"
echo ""
read -p "Did the test pass? (y/n): " test6
echo ""

# Test 7: Offline Message Queue
echo -e "${YELLOW}═══════════════════════════════════════${NC}"
echo -e "${YELLOW}  TEST 7: Offline Message Queue ✅      ${NC}"
echo -e "${YELLOW}═══════════════════════════════════════${NC}"
echo ""
echo "1. Login and open chat"
echo "2. Go offline"
echo "3. Try to send 3 messages"
echo "4. Check for:"
echo "   ✅ All 3 messages appear with ⏳ status"
echo "   ✅ Toast warnings for each"
echo "5. Go back online"
echo "6. Check for:"
echo "   ✅ Toast: 'Sending 3 queued messages...'"
echo "   ✅ All messages get sent"
echo "   ✅ Status changes to ✓"
echo ""
read -p "Did the test pass? (y/n): " test7
echo ""

# Test 8: Toast Notifications
echo -e "${YELLOW}═══════════════════════════════════════${NC}"
echo -e "${YELLOW}  TEST 8: Toast Notification System ✅  ${NC}"
echo -e "${YELLOW}═══════════════════════════════════════${NC}"
echo ""
echo "Throughout testing, verify toast notifications:"
echo "   ✅ Appear at bottom-right corner"
echo "   ✅ Slide in smoothly (animation)"
echo "   ✅ Auto-dismiss after 3 seconds"
echo "   ✅ Slide out when dismissed"
echo "   ✅ Colors correct:"
echo "      - Yellow for warnings"
echo "      - Red for errors"
echo "      - Blue for info"
echo "      - Green for success"
echo ""
read -p "Did the test pass? (y/n): " test8
echo ""

# Calculate results
passed=0
total=8

[[ "$test1" == "y" ]] && ((passed++))
[[ "$test2" == "y" ]] && ((passed++))
[[ "$test3" == "y" ]] && ((passed++))
[[ "$test4" == "y" ]] && ((passed++))
[[ "$test5" == "y" ]] && ((passed++))
[[ "$test6" == "y" ]] && ((passed++))
[[ "$test7" == "y" ]] && ((passed++))
[[ "$test8" == "y" ]] && ((passed++))

percentage=$((passed * 100 / total))

echo ""
echo "╔══════════════════════════════════════════════════════╗"
echo "║                  TEST RESULTS                        ║"
echo "╚══════════════════════════════════════════════════════╝"
echo ""

if [ $percentage -eq 100 ]; then
    echo -e "${GREEN}🎉 ALL TESTS PASSED! 🎉${NC}"
    echo -e "${GREEN}Perfect Score: $passed/$total (100%)${NC}"
    echo ""
    echo -e "${GREEN}✅ All fixes are working correctly!${NC}"
    echo -e "${GREEN}✅ Chat system is production ready!${NC}"
    echo -e "${GREEN}✅ You can deploy with confidence!${NC}"
elif [ $percentage -ge 80 ]; then
    echo -e "${YELLOW}✅ MOST TESTS PASSED${NC}"
    echo -e "${YELLOW}Score: $passed/$total ($percentage%)${NC}"
    echo ""
    echo -e "${YELLOW}⚠️ Some minor issues detected${NC}"
    echo -e "${YELLOW}⚠️ Review failed tests and retry${NC}"
elif [ $percentage -ge 60 ]; then
    echo -e "${YELLOW}⚠️ SOME TESTS FAILED${NC}"
    echo -e "${YELLOW}Score: $passed/$total ($percentage%)${NC}"
    echo ""
    echo -e "${YELLOW}⚠️ Several issues need attention${NC}"
    echo -e "${YELLOW}⚠️ Review and fix before deploying${NC}"
else
    echo -e "${RED}❌ MANY TESTS FAILED${NC}"
    echo -e "${RED}Score: $passed/$total ($percentage%)${NC}"
    echo ""
    echo -e "${RED}❌ Critical issues detected${NC}"
    echo -e "${RED}❌ Do NOT deploy until fixed${NC}"
fi

echo ""
echo "Test Details:"
echo "  1. Error Handling:        $([ "$test1" == "y" ] && echo -e "${GREEN}✅ Pass${NC}" || echo -e "${RED}❌ Fail${NC}")"
echo "  2. WebSocket Disconnect:  $([ "$test2" == "y" ] && echo -e "${GREEN}✅ Pass${NC}" || echo -e "${RED}❌ Fail${NC}")"
echo "  3. Message Length:        $([ "$test3" == "y" ] && echo -e "${GREEN}✅ Pass${NC}" || echo -e "${RED}❌ Fail${NC}")"
echo "  4. Rate Limiting:         $([ "$test4" == "y" ] && echo -e "${GREEN}✅ Pass${NC}" || echo -e "${RED}❌ Fail${NC}")"
echo "  5. Persist Conversation:  $([ "$test5" == "y" ] && echo -e "${GREEN}✅ Pass${NC}" || echo -e "${RED}❌ Fail${NC}")"
echo "  6. Message Status:        $([ "$test6" == "y" ] && echo -e "${GREEN}✅ Pass${NC}" || echo -e "${RED}❌ Fail${NC}")"
echo "  7. Message Queue:         $([ "$test7" == "y" ] && echo -e "${GREEN}✅ Pass${NC}" || echo -e "${RED}❌ Fail${NC}")"
echo "  8. Toast Notifications:   $([ "$test8" == "y" ] && echo -e "${GREEN}✅ Pass${NC}" || echo -e "${RED}❌ Fail${NC}")"
echo ""

echo "═══════════════════════════════════════"
echo "For detailed documentation, see:"
echo "  • CHAT_FIXES_COMPLETED.md"
echo "  • CHAT_ANALYSIS_SUMMARY.md"
echo "  • CHAT_ISSUES_PRIORITY.md"
echo "═══════════════════════════════════════"
echo ""

if [ $percentage -eq 100 ]; then
    echo -e "${GREEN}🚀 Ready to deploy to production!${NC}"
else
    echo -e "${YELLOW}📝 Review failed tests and retry.${NC}"
fi

