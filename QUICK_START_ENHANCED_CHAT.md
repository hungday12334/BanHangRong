# 🚀 Quick Start Guide: Enhanced Chat Features

## For Users (Customer & Seller)

### How to React to Messages ❤️
1. **Hover** over any message (yours or theirs)
2. Click the **😊 emoji button** that appears
3. Select one of the 6 reactions:
   - ❤️ Love
   - 😂 Haha  
   - 😢 Sad
   - 😡 Angry
   - 😮 Wow
   - 👍 Like
4. Your reaction appears below the message

**To see who reacted:**
- Click on any reaction badge
- A popup shows all users who reacted
- Click "Click to remove reaction" to remove your own

### How to Reply to Messages ↩️
1. **Hover** over the message you want to reply to
2. Click the **↩️ reply button**
3. A reply bar appears showing:
   - Who you're replying to
   - A preview of their message
4. Type your reply and send
5. Your message shows a quoted reference
6. **Click the quote** to jump to the original message

**To cancel a reply:**
- Click the **✕** button in the reply bar

### How to Delete Messages 🗑️
**Soft Delete (Hide content):**
1. **Hover** over YOUR OWN message
2. Click the **🗑️ delete button**
3. Confirm deletion
4. Message shows "This message has been deleted"

**Permanent Delete (Remove completely):**
1. **Hover** over a deleted message
2. Click the **🗑️ trash icon** that appears
3. Confirm permanent deletion
4. Message fades away completely

⚠️ **Note:** You can only delete your own messages!

---

## For Developers

### Testing the Features

#### Test Reactions
```javascript
// Open browser console
// Check if reactions work:
1. Hover over a message
2. Click emoji button
3. Select reaction
4. Check console for: "✅ Added reaction: ❤️ to message: xxx"
5. Verify WebSocket message sent
```

#### Test Replies
```javascript
// Test reply flow:
1. Click reply button on a message
2. Verify reply bar appears
3. Type message and send
4. Check for replyToMessageId in WebSocket payload
5. Verify quoted section appears in new message
```

#### Test Deletes
```javascript
// Test delete flow:
1. Click delete on your message
2. Confirm dialog
3. Check console for: "🗑️ Deleted message: xxx"
4. Verify message shows as deleted
5. Test permanent delete
```

### WebSocket Message Formats

**Adding Reaction:**
```json
{
  "messageId": "msg-uuid",
  "userId": "user-id",
  "userName": "User Name",
  "emoji": "❤️",
  "conversationId": "conv-uuid"
}
```

**Sending Reply:**
```json
{
  "conversationId": "conv-uuid",
  "senderId": "user-id",
  "content": "Reply text",
  "messageType": "TEXT",
  "replyToMessageId": "original-msg-id",
  "replyToSenderName": "Original Sender",
  "replyToContent": "Original message preview"
}
```

**Deleting Message:**
```json
{
  "messageId": "msg-uuid",
  "conversationId": "conv-uuid",
  "userId": "user-id",
  "permanent": false
}
```

### Console Debugging

**Check Connection:**
```javascript
console.log('Is Connected:', isConnected);
console.log('Current Conversation:', currentConversation);
```

**Check Subscriptions:**
```javascript
console.log('Active Subscriptions:', subscriptions);
```

**Monitor WebSocket:**
```javascript
// Open browser DevTools > Network tab
// Filter: WS (WebSocket)
// Watch frames for messages
```

---

## Keyboard Shortcuts

| Action | Shortcut |
|--------|----------|
| Send Message | Enter |
| Cancel Reply | Esc (via ✕ button) |
| Close Popup | Click outside |

---

## Browser Compatibility

✅ **Fully Supported:**
- Chrome 90+
- Firefox 88+
- Safari 14+
- Edge 90+
- Mobile Chrome (Android)
- Mobile Safari (iOS)

---

## Common Issues & Solutions

### Issue: Actions toolbar doesn't appear
**Solution:** Make sure you're hovering directly over the message bubble area

### Issue: Reactions not showing
**Solution:** 
1. Check browser console for errors
2. Verify WebSocket connection is active
3. Check if backend endpoints are running

### Issue: Reply quote not clickable
**Solution:** Ensure the original message still exists (not permanently deleted)

### Issue: Can't delete message
**Solution:** You can only delete YOUR OWN messages

---

## Performance Tips

- **Mobile:** Tap once to show action toolbar, tap again to select action
- **Desktop:** Hover is instant, click immediately
- **Smooth scrolling:** Quoted message jumps work best in modern browsers
- **Real-time:** All updates appear instantly for both parties

---

## Security Notes

🔒 **What's Protected:**
- Only you can delete your own messages
- Reactions require valid user ID
- All WebSocket messages are authenticated
- CSRF protection on all endpoints

---

## Quick Command Reference

### For Testing in Console

```javascript
// Show current reply context
console.log(currentReplyTo);

// Show active reaction picker
console.log(currentReactionPicker);

// List available reactions
console.log(REACTIONS);

// Get message reactions
currentConversation.messages.find(m => m.id === 'msg-id').reactions

// Force close all popups
closeReactionPopup();
cancelReply();
```

---

## Visual Examples

### Message with Reactions
```
[Message Bubble]
Hello! How are you?
❤️ 2  😂 1  👍  3
```

### Message with Reply
```
[Reply Quote]
John: How are you?
─────────────
I'm doing great, thanks!
```

### Deleted Message
```
[Faded Message Bubble]
This message has been deleted 🗑️
```

---

## Need Help?

1. **Check Console Logs:** Look for ✅, ❌, or ⚠️ messages
2. **Verify WebSocket:** Should show "✅ WebSocket Connected"
3. **Test in DevTools:** Open Network tab and watch WebSocket frames
4. **Clear Cache:** Sometimes helps with CSS/JS issues

---

**Status:** ✅ All features fully implemented and working!

**Version:** 1.0.0  
**Last Updated:** November 1, 2025

