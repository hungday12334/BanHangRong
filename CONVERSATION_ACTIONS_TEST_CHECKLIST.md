# Conversation Actions - Testing Checklist

## Pre-requisites
- [ ] Application is running
- [ ] Database has `user_conversation_metadata` table
- [ ] At least 2 users exist (1 customer, 1 seller)
- [ ] At least 3 conversations exist

---

## 1. PIN CONVERSATION TESTS

### Customer Side
- [ ] Login as Customer
- [ ] Go to Chat page
- [ ] **Hover Test**: Hover over a conversation → ⋯ button appears
- [ ] **Click Test**: Click ⋯ → context menu appears with "Pin conversation"
- [ ] **Pin Test**: Click "Pin conversation"
  - [ ] Conversation moves to top of list
  - [ ] 📌 icon appears on left
  - [ ] Background changes to yellow/amber tint
  - [ ] Toast shows "Conversation pinned"
- [ ] **Persistence Test**: Refresh page (F5)
  - [ ] Conversation still at top
  - [ ] Still shows 📌 icon
  - [ ] Still has yellow background
- [ ] **Multiple Pin Test**: Pin 2 more conversations
  - [ ] All 3 pinned conversations at top
  - [ ] Most recently pinned is first
  - [ ] All show 📌 icons

### Seller Side
- [ ] Login as Seller
- [ ] Go to Chat page
- [ ] Repeat all Customer tests above
- [ ] Verify same behavior

---

## 2. UNPIN CONVERSATION TESTS

### Customer Side
- [ ] Find a pinned conversation (with 📌)
- [ ] **Hover Test**: Hover → ⋯ button appears
- [ ] **Click Test**: Click ⋯ → menu shows "Unpin conversation"
- [ ] **Unpin Test**: Click "Unpin conversation"
  - [ ] Conversation moves to regular section
  - [ ] 📌 icon disappears
  - [ ] Yellow background removed
  - [ ] Toast shows "Conversation unpinned"
  - [ ] Sorted by last message time
- [ ] **Persistence Test**: Refresh page
  - [ ] Conversation still unpinned
  - [ ] Still in regular section

### Seller Side
- [ ] Repeat all unpin tests
- [ ] Verify same behavior

---

## 3. DELETE CONVERSATION TESTS

### Customer Side - Basic Delete
- [ ] Pick a conversation (not currently active)
- [ ] **Menu Test**: Click ⋯ → see "Delete conversation" in red
- [ ] **Modal Test**: Click "Delete conversation"
  - [ ] Modal appears with correct title
  - [ ] Shows other person's name correctly
  - [ ] Shows warning text about local-only delete
  - [ ] Has Cancel and Delete buttons
- [ ] **Cancel Test**: Click "Cancel"
  - [ ] Modal closes
  - [ ] Nothing happens to conversation
  - [ ] Conversation still in list
- [ ] **Delete Test**: Click ⋯ again → "Delete conversation" → "Delete"
  - [ ] Modal closes
  - [ ] Conversation immediately disappears from list
  - [ ] Toast shows "Conversation deleted"
- [ ] **Persistence Test**: Refresh page
  - [ ] Conversation still not in list

### Customer Side - Delete Active Conversation
- [ ] Open a conversation (so chat area is active)
- [ ] Delete this conversation via ⋯ menu
  - [ ] Conversation disappears from list
  - [ ] Chat area closes
  - [ ] Welcome screen appears
  - [ ] No errors in console

### Seller Side
- [ ] Repeat all delete tests
- [ ] Verify same behavior

---

## 4. CROSS-USER BEHAVIOR TESTS

### Setup
- [ ] Open 2 browser windows/tabs
- [ ] Login as Customer in Window A
- [ ] Login as Seller in Window B
- [ ] Both navigate to Chat
- [ ] Find their shared conversation

### Pin Test (Customer pins)
- [ ] **Window A (Customer)**: Pin the conversation
  - [ ] Window A: Conversation moves to top with 📌
  - [ ] **Window B (Seller)**: Refresh page
    - [ ] Conversation position NOT changed
    - [ ] NO 📌 icon visible
    - [ ] Conversation in normal position

### Pin Test (Seller pins)
- [ ] **Window B (Seller)**: Pin the same conversation
  - [ ] Window B: Conversation moves to top with 📌
  - [ ] **Window A (Customer)**: Refresh page
    - [ ] Customer's pin status unchanged
    - [ ] If customer pinned it, still pinned for customer
    - [ ] If customer didn't pin, still not pinned

### Delete Test (Customer deletes)
- [ ] **Window A (Customer)**: Delete the conversation
  - [ ] Window A: Conversation disappears
  - [ ] **Window B (Seller)**: Refresh page
    - [ ] Conversation STILL THERE
    - [ ] No change on seller side

### Delete Recovery Test
- [ ] **Window B (Seller)**: Send new message in deleted conversation
  - [ ] **Window A (Customer)**: Refresh page
    - [ ] Conversation REAPPEARS in list
    - [ ] Shows the new message
    - [ ] Not pinned anymore

---

## 5. EDGE CASES

### Pinned Conversation with Unread
- [ ] Have someone send you a message
- [ ] Pin that conversation (it has unread badge)
  - [ ] Unread badge still visible
  - [ ] Badge count correct
  - [ ] Conversation at top

### Delete Conversation with Unread
- [ ] Have unread conversation
- [ ] Note global unread count (e.g., "3" in sidebar)
- [ ] Delete the unread conversation
  - [ ] Global unread count decreases correctly
  - [ ] Badge updates in sidebar

### Multiple Devices (Same User)
- [ ] Open 2 browsers as same user
- [ ] Browser A: Pin a conversation
- [ ] Browser B: Refresh
  - [ ] Conversation is pinned in Browser B too

### Rapidly Pin/Unpin
- [ ] Pin conversation
- [ ] Immediately unpin
- [ ] Pin again
- [ ] Unpin again
  - [ ] No errors
  - [ ] UI updates correctly each time
  - [ ] Final state is correct

### Context Menu Outside Click
- [ ] Click ⋯ to open menu
- [ ] Click anywhere outside menu
  - [ ] Menu closes
  - [ ] No errors

---

## 6. MOBILE/RESPONSIVE TESTS

### Mobile View (< 768px)
- [ ] Open in mobile viewport or real phone
- [ ] **Touch Test**: Tap conversation
  - [ ] Conversation opens
- [ ] **Actions Test**: Tap ⋯ button
  - [ ] Menu appears
  - [ ] Not covered by other elements
  - [ ] Easy to tap menu items
- [ ] **Delete Modal**: Tap Delete conversation
  - [ ] Modal appears
  - [ ] Fits screen properly
  - [ ] Buttons easy to tap
  - [ ] Text readable

### Tablet View (768px - 1024px)
- [ ] Test same as mobile
- [ ] Verify proper spacing

---

## 7. ACCESSIBILITY TESTS

### Keyboard Navigation
- [ ] Use Tab key to focus on ⋯ button
  - [ ] Button gets focus outline
- [ ] Press Enter or Space
  - [ ] Menu opens
- [ ] Use Arrow keys
  - [ ] Can navigate menu items
- [ ] Press Enter on "Pin conversation"
  - [ ] Pins the conversation
- [ ] Press Escape
  - [ ] Menu closes

### Screen Reader
- [ ] Enable screen reader (VoiceOver, NVDA, etc.)
- [ ] Navigate to conversations list
  - [ ] Each conversation announced properly
  - [ ] ⋯ button has proper label
  - [ ] Menu items announced correctly

---

## 8. PERFORMANCE TESTS

### Large Conversation List
- [ ] Create 50+ conversations
- [ ] Pin 10 conversations
- [ ] **Load Test**: Refresh page
  - [ ] Page loads within 2 seconds
  - [ ] All pinned conversations at top
  - [ ] Sorted correctly
- [ ] **Pin Test**: Pin another conversation
  - [ ] Updates immediately (< 500ms)
- [ ] **Delete Test**: Delete a conversation
  - [ ] Removes immediately (< 500ms)

---

## 9. ERROR HANDLING

### Network Error - Pin
- [ ] Open DevTools → Network tab
- [ ] Throttle to "Offline"
- [ ] Try to pin a conversation
  - [ ] Shows error toast
  - [ ] Conversation not pinned
  - [ ] No console errors

### Network Error - Delete
- [ ] Throttle to "Offline"
- [ ] Try to delete a conversation
  - [ ] Shows error toast
  - [ ] Conversation not deleted
  - [ ] No console errors

### Server Error
- [ ] Stop the server
- [ ] Try to pin/delete
  - [ ] Shows appropriate error
  - [ ] Application doesn't crash

---

## 10. DATA VALIDATION

### Database Check - Pin
- [ ] Pin a conversation
- [ ] Check `user_conversation_metadata` table:
```sql
SELECT * FROM user_conversation_metadata 
WHERE user_id = [your_user_id];
```
- [ ] Verify:
  - [ ] Record exists
  - [ ] `is_pinned` = TRUE
  - [ ] `pinned_at` has timestamp
  - [ ] `is_deleted` = FALSE

### Database Check - Delete
- [ ] Delete a conversation
- [ ] Check table again:
- [ ] Verify:
  - [ ] Record exists (not hard-deleted)
  - [ ] `is_deleted` = TRUE
  - [ ] `deleted_at` has timestamp
  - [ ] `is_pinned` = FALSE (auto-unpinned)

---

## 11. CONSOLE CHECKS

Throughout all tests:
- [ ] No JavaScript errors in console
- [ ] No failed network requests (except intentional offline tests)
- [ ] No React/framework warnings (if applicable)
- [ ] Console logs are informative (if any)

---

## FINAL CHECKLIST

- [ ] All pin tests pass ✅
- [ ] All unpin tests pass ✅
- [ ] All delete tests pass ✅
- [ ] Cross-user behavior correct ✅
- [ ] Edge cases handled ✅
- [ ] Mobile responsive ✅
- [ ] Keyboard accessible ✅
- [ ] No performance issues ✅
- [ ] Error handling works ✅
- [ ] Database updates correct ✅

---

## Sign-off

**Tested by**: _________________  
**Date**: _________________  
**Environment**: _________________  
**Browser**: _________________  
**Result**: ☐ PASS  ☐ FAIL  
**Notes**:  
_______________________________________________  
_______________________________________________  
_______________________________________________  


