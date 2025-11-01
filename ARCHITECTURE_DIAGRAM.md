# Enhanced Chat Architecture Diagram

## System Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                     BanHangRong Chat System                      │
│                    (Enhanced with Reactions,                     │
│                   Replies, and Delete Features)                  │
└─────────────────────────────────────────────────────────────────┘
```

## Component Architecture

```
┌──────────────────┐                    ┌──────────────────┐
│   Customer UI    │                    │    Seller UI     │
│   (Light Theme)  │                    │   (Dark Theme)   │
│                  │                    │                  │
│  ┌────────────┐  │                    │  ┌────────────┐  │
│  │ Messages   │  │                    │  │ Messages   │  │
│  │ - Text     │  │                    │  │ - Text     │  │
│  │ - Images   │  │                    │  │ - Images   │  │
│  │ - Files    │  │                    │  │ - Files    │  │
│  │ - Reactions│  │                    │  │ - Reactions│  │
│  │ - Replies  │  │                    │  │ - Replies  │  │
│  └────────────┘  │                    │  └────────────┘  │
│                  │                    │                  │
│  ┌────────────┐  │                    │  ┌────────────┐  │
│  │ Action Bar │  │                    │  │ Action Bar │  │
│  │ - 😊 React │  │                    │  │ - 😊 React │  │
│  │ - ↩️ Reply │  │                    │  │ - ↩️ Reply │  │
│  │ - 🗑️ Delete│  │                    │  │ - 🗑️ Delete│  │
│  └────────────┘  │                    │  └────────────┘  │
└─────────┬────────┘                    └────────┬─────────┘
          │                                      │
          │         ┌──────────────┐             │
          └────────►│   WebSocket  │◄────────────┘
                    │   Gateway    │
                    │  /ws-chat    │
                    └──────┬───────┘
                           │
                           │
          ┌────────────────┴────────────────┐
          │                                 │
          ▼                                 ▼
┌──────────────────┐              ┌──────────────────┐
│  Message Topics  │              │   Chat Topics    │
│                  │              │                  │
│ /topic/          │              │ /topic/          │
│  conversation/   │              │  conversation/   │
│  {id}            │              │  {id}/reactions  │
│                  │              │                  │
│                  │              │ /topic/          │
│                  │              │  conversation/   │
│                  │              │  {id}/deletes    │
└──────────────────┘              └──────────────────┘
```

## Message Flow - Adding Reaction

```
┌─────────┐                                              ┌─────────┐
│Customer │                                              │ Seller  │
└────┬────┘                                              └────┬────┘
     │                                                        │
     │ 1. Hover over message                                 │
     │    Action toolbar appears                             │
     │                                                        │
     │ 2. Click 😊 emoji button                              │
     │    Reaction picker opens                              │
     │                                                        │
     │ 3. Select ❤️ reaction                                 │
     ├──────────────────────────────────────────────────────►│
     │    WebSocket: /app/chat.addReaction                   │
     │    {                                                   │
     │      messageId: "msg-123",                            │
     │      emoji: "❤️",                                      │
     │      userId: "user-456"                               │
     │    }                                                   │
     │                                                        │
     │◄──────────────────────────────────────────────────────┤
     │    Topic: /topic/conversation/{id}/reactions          │
     │    {                                                   │
     │      action: "add",                                   │
     │      emoji: "❤️",                                      │
     │      userId: "user-456"                               │
     │    }                                                   │
     │                                                        │
     │ 4. Reaction badge appears: ❤️ 1                       │
     │                                   4. Sees reaction: ❤️ 1│
     │                                                        │
```

## Message Flow - Reply Feature

```
┌─────────┐                                              ┌─────────┐
│Customer │                                              │ Seller  │
└────┬────┘                                              └────┬────┘
     │                                                        │
     │ 1. Click ↩️ Reply on Seller's message                 │
     │    "Hello! How can I help?"                           │
     │                                                        │
     │ 2. Reply bar appears:                                 │
     │    ┌─────────────────────────────┐                   │
     │    │ Seller: Hello! How can...   │                   │
     │    │ [Your reply here]        [✕]│                   │
     │    └─────────────────────────────┘                   │
     │                                                        │
     │ 3. Types: "I need help with an order"                │
     │    Presses Enter                                      │
     │                                                        │
     ├──────────────────────────────────────────────────────►│
     │    WebSocket: /app/chat.sendMessage                   │
     │    {                                                   │
     │      content: "I need help with an order",            │
     │      replyToMessageId: "msg-789",                     │
     │      replyToContent: "Hello! How can..."              │
     │    }                                                   │
     │                                                        │
     │                              4. Sees message:          │
     │                                 ┌───────────────────┐ │
     │                                 │ Customer replied: │ │
     │                                 │ > Hello! How...   │ │
     │                                 │ I need help with  │ │
     │                                 │ an order          │ │
     │                                 └───────────────────┘ │
     │                                                        │
     │                              5. Clicks quoted section │
     │                                 Scrolls to original   │
     │                                 Highlights message    │
```

## Message Flow - Delete Message

```
┌─────────┐                                              ┌─────────┐
│Customer │                                              │ Seller  │
└────┬────┘                                              └────┬────┘
     │                                                        │
     │ 1. Hover over own message                             │
     │    "Oops, wrong info!"                                │
     │                                                        │
     │ 2. Click 🗑️ Delete button                             │
     │    Confirm deletion dialog                            │
     │                                                        │
     ├──────────────────────────────────────────────────────►│
     │    WebSocket: /app/chat.deleteMessage                 │
     │    {                                                   │
     │      messageId: "msg-999",                            │
     │      permanent: false                                 │
     │    }                                                   │
     │                                                        │
     │◄──────────────────────────────────────────────────────┤
     │    Topic: /topic/conversation/{id}/deletes            │
     │    {                                                   │
     │      messageId: "msg-999",                            │
     │      deleted: true                                    │
     │    }                                                   │
     │                                                        │
     │ 3. Message shows:                                     │
     │    "This message has been deleted"                    │
     │    (faded, italic)                                    │
     │                                   3. Also sees:        │
     │                                      "This message...  │
     │                                       deleted"         │
     │                                                        │
     │ 4. Hover over deleted message                         │
     │    🗑️ icon appears                                     │
     │                                                        │
     │ 5. Click 🗑️ for permanent delete                      │
     │    Confirm permanent deletion                         │
     │                                                        │
     ├──────────────────────────────────────────────────────►│
     │    WebSocket: /app/chat.permanentDeleteMessage        │
     │    {                                                   │
     │      messageId: "msg-999",                            │
     │      permanent: true                                  │
     │    }                                                   │
     │                                                        │
     │ 6. Message fades out & disappears                     │
     │                                   6. Also disappears   │
```

## Data Structure - Enhanced Message

```
Message {
  ┌────────────────────────────────────┐
  │ Core Fields                        │
  ├────────────────────────────────────┤
  │ id: String                         │
  │ conversationId: String             │
  │ senderId: String                   │
  │ senderName: String                 │
  │ senderRole: "CUSTOMER" | "SELLER"  │
  │ content: String                    │
  │ messageType: "TEXT"|"IMAGE"|"FILE" │
  │ timestamp: DateTime                │
  │ read: Boolean                      │
  ├────────────────────────────────────┤
  │ Reply Fields (NEW)                 │
  ├────────────────────────────────────┤
  │ replyToMessageId: String?          │
  │ replyToSenderName: String?         │
  │ replyToContent: String?            │
  ├────────────────────────────────────┤
  │ Reaction Fields (NEW)              │
  ├────────────────────────────────────┤
  │ reactions: Map<String, List<String>>│
  │   "❤️": ["user1", "user2"]         │
  │   "😂": ["user3"]                   │
  │   "👍": ["user1", "user3", "user4"] │
  ├────────────────────────────────────┤
  │ Delete Fields (NEW)                │
  ├────────────────────────────────────┤
  │ deleted: Boolean                   │
  └────────────────────────────────────┘
}
```

## UI Component Tree

```
chat.html
│
├── .topbar (Navigation)
│
├── .chat-page
│   └── .chat-wrapper
│       ├── .conversations-sidebar
│       │   ├── .conversations-header
│       │   ├── .user-info
│       │   └── .conversations-list
│       │       └── .conversation-item (multiple)
│       │
│       └── .chat-main
│           ├── .chat-header
│           │
│           ├── .chat-messages
│           │   └── .message-wrapper (multiple) ← NEW
│           │       ├── .message
│           │       │   ├── .message-avatar
│           │       │   └── .message-content
│           │       │       ├── .message-bubble
│           │       │       │   └── .message-reply-quote? ← NEW
│           │       │       ├── .message-reactions? ← NEW
│           │       │       │   └── .reaction-badge (multiple)
│           │       │       └── .message-time
│           │       │
│           │       └── .message-actions ← NEW
│           │           ├── .emoji-btn
│           │           ├── .reply-btn
│           │           └── .delete-btn
│           │
│           └── .chat-input-container
│               ├── .reply-status-bar ← NEW
│               ├── .upload-preview
│               ├── .features-menu
│               ├── .emoji-picker
│               └── .chat-input-wrapper
│                   ├── .btn-plus
│                   ├── .chat-input
│                   └── .btn-send
│
├── .reaction-users-overlay ← NEW
└── .reaction-users-popup ← NEW
    ├── .reaction-users-header
    └── #reactionUsersContent
```

## JavaScript Function Map

```
Enhanced Chat Functions
│
├── Reaction Functions
│   ├── showReactionPicker(messageId)
│   ├── addReaction(messageId, emoji)
│   ├── removeReaction(messageId, emoji)
│   ├── showReactionUsers(messageId, emoji)
│   ├── closeReactionPopup()
│   └── handleReactionUpdate(update)
│
├── Reply Functions
│   ├── replyToMessage(messageId, sender, content)
│   ├── cancelReply()
│   └── scrollToMessage(messageId)
│
├── Delete Functions
│   ├── deleteMessage(messageId)
│   ├── permanentlyDeleteMessage(messageId)
│   └── handleDeleteUpdate(update)
│
├── Message Display
│   └── displayMessage(message, shouldScroll)
│       ├── Render reply quote
│       ├── Render reactions
│       ├── Render action toolbar
│       └── Handle deleted state
│
└── WebSocket Management
    ├── subscribeToConversation(id)
    │   ├── Subscribe to messages
    │   ├── Subscribe to reactions ← NEW
    │   └── Subscribe to deletes ← NEW
    │
    └── sendMessage()
        ├── Handle normal messages
        ├── Handle replies ← NEW
        └── Handle file uploads
```

## State Management

```
Global State Variables
│
├── currentUser: User
│   └── { userId, username, userType }
│
├── currentConversation: Conversation
│   └── { id, messages[], sellerId, customerId }
│
├── conversations: Conversation[]
│
├── stompClient: StompClient (WebSocket)
│
├── isConnected: Boolean
│
├── subscriptions: Map
│   ├── conversation
│   ├── notifications
│   ├── reactions ← NEW
│   └── deletes ← NEW
│
├── currentReplyTo: ReplyContext? ← NEW
│   └── { id, senderName, content }
│
└── currentReactionPicker: HTMLElement? ← NEW
```

## Event Flow

```
User Action
    │
    ├─► Hover Message
    │       │
    │       ├─► Show Action Toolbar
    │       │       │
    │       │       ├─► Click Emoji → Show Picker → Select → Send Reaction
    │       │       ├─► Click Reply → Show Reply Bar → Type → Send Reply
    │       │       └─► Click Delete → Confirm → Send Delete
    │       │
    │       └─► Hide Action Toolbar (on mouse leave)
    │
    ├─► Click Reaction Badge
    │       │
    │       └─► Show Reaction Users Popup
    │               │
    │               └─► Click to Remove Own Reaction
    │
    ├─► Click Reply Quote
    │       │
    │       └─► Scroll to Original Message
    │               │
    │               └─► Highlight Animation
    │
    └─► Click Outside Popup
            │
            └─► Close All Popups
```

## WebSocket Event Flow

```
Client Action                Server                 All Clients
     │                          │                        │
     ├─ addReaction ────────────►│                        │
     │                          │                        │
     │                          ├─ Validate              │
     │                          ├─ Save to DB            │
     │                          │                        │
     │◄─────────────────────────┤                        │
     │   Reaction Added         │                        │
     │                          │                        │
     │                          ├─ Broadcast ───────────►│
     │                          │  /reactions topic      │
     │                          │                        │
     │                          │                   Update UI
     │                          │                   Show Badge
     │                          │                        │
```

---

## Performance Optimization Points

```
┌──────────────────────────────────────┐
│  Optimization Strategies             │
├──────────────────────────────────────┤
│ 1. CSS Animations                    │
│    - Hardware accelerated            │
│    - transform & opacity only        │
│                                      │
│ 2. DOM Updates                       │
│    - Targeted updates only           │
│    - No full re-renders              │
│                                      │
│ 3. Event Listeners                   │
│    - Debounced where appropriate     │
│    - Event delegation                │
│                                      │
│ 4. WebSocket                         │
│    - Persistent connection           │
│    - Efficient binary protocol       │
│                                      │
│ 5. Optimistic UI                     │
│    - Instant visual feedback         │
│    - Background sync                 │
└──────────────────────────────────────┘
```

---

**Legend:**
```
─►  : Data flow / Action
│   : Hierarchy / Contains
◄─► : Bidirectional communication
?   : Optional field/component
```

---

*Architecture Version: 1.0.0*  
*Last Updated: November 1, 2025*

