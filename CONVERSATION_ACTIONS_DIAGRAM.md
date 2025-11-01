## Conversation Actions - Visual Flow Diagram

```
┌─────────────────────────────────────────────────────────────────────┐
│                        USER INTERFACE                               │
└─────────────────────────────────────────────────────────────────────┘

┌────────────────────────────────────────────────────────────────────┐
│  Conversation List (Left Column)                                   │
│                                                                    │
│  ┌───────────────────────────────────────┐                        │
│  │ 📌 [Seller A]            2m     [⋯] │ ← Pinned (yellow bg)     │
│  │    Hi, how are you?              │   │                          │
│  └───────────────────────────────────────┘                        │
│                                                                    │
│  ┌───────────────────────────────────────┐                        │
│  │    [Seller B]            1h     [⋯] │ ← Hover shows ⋯         │
│  │    Thank you!                    │   │                          │
│  └───────────────────────────────────────┘                        │
│                                                                    │
│  ┌───────────────────────────────────────┐                        │
│  │    [Customer C]         2h      [⋯] │                          │
│  │    See you tomorrow              │   │                          │
│  └───────────────────────────────────────┘                        │
└────────────────────────────────────────────────────────────────────┘

        │
        │ Click ⋯
        ▼

┌────────────────────────────────────────────────────────────────────┐
│  Context Menu                                                      │
│  ┌─────────────────────────┐                                      │
│  │ 📌 Pin conversation      │ ← or "Unpin conversation"            │
│  │ 🗑️ Delete conversation   │                                      │
│  └─────────────────────────┘                                      │
└────────────────────────────────────────────────────────────────────┘

        │
        ├─── Pin ───────────────────────┬─── Delete ──────────────┐
        │                               │                          │
        ▼                               ▼                          │
                                                                   │
┌──────────────────────────┐    ┌──────────────────────────────────┐
│  PIN ACTION              │    │  DELETE CONFIRMATION MODAL       │
│                          │    │                                  │
│  1. Send POST to:        │    │  ┌────────────────────────────┐ │
│     /api/conversations/  │    │  │ Delete Conversation?       │ │
│     {id}/pin             │    │  │                            │ │
│                          │    │  │ This will remove from your │ │
│  2. Update local data:   │    │  │ account only. [Name] will  │ │
│     conv.isPinned = true │    │  │ still have it.             │ │
│                          │    │  │                            │ │
│  3. Re-render list:      │    │  │ [Cancel]  [Delete]         │ │
│     - Move to top        │    │  └────────────────────────────┘ │
│     - Show 📌            │    └──────────────────────────────────┘
│     - Yellow background  │                    │
│                          │                    │ Confirm
│  4. Show toast:          │                    ▼
│     "Conversation pinned"│    ┌──────────────────────────────────┐
└──────────────────────────┘    │  DELETE ACTION                   │
                                │                                  │
                                │  1. Send DELETE to:              │
                                │     /api/conversations/{id}      │
                                │                                  │
                                │  2. Update local data:           │
                                │     Remove from conversations[]  │
                                │                                  │
                                │  3. Re-render list:              │
                                │     - Conversation disappears    │
                                │                                  │
                                │  4. Close chat if active:        │
                                │     - Show welcome screen        │
                                │                                  │
                                │  5. Show toast:                  │
                                │     "Conversation deleted"       │
                                └──────────────────────────────────┘


═══════════════════════════════════════════════════════════════════════
                        DATABASE FLOW
═══════════════════════════════════════════════════════════════════════

┌─────────────────────────────────────────────────────────────────────┐
│                     user_conversation_metadata                      │
├──────────┬──────────────────┬────────────┬───────────┬─────────────┤
│ user_id  │ conversation_id  │ is_deleted │ is_pinned │ pinned_at   │
├──────────┼──────────────────┼────────────┼───────────┼─────────────┤
│   123    │   c_123_456      │   FALSE    │   TRUE    │ 2025-11-02  │ ← Pinned
│   123    │   c_123_789      │   TRUE     │   FALSE   │    NULL     │ ← Deleted
│   456    │   c_123_456      │   FALSE    │   FALSE   │    NULL     │ ← Normal
└──────────┴──────────────────┴────────────┴───────────┴─────────────┘

                              ▲
                              │
                              │ API calls update this table
                              │


═══════════════════════════════════════════════════════════════════════
                     SORTING LOGIC
═══════════════════════════════════════════════════════════════════════

When loading conversations for User 123:

1. Load all conversations where user participates
   
2. Load metadata from user_conversation_metadata where user_id = 123

3. Filter: Remove conversations where is_deleted = TRUE

4. Set isPinned flag on each conversation

5. Sort:
   ┌─────────────────────────────────────────────────────┐
   │  PINNED (isPinned = true)                           │
   │  ↓ Sort by: pinned_at DESC (newest pin first)      │
   │                                                     │
   │  ┌───────────────────────┐                         │
   │  │ 📌 Conv A (pinned 2h ago)                       │
   │  │ 📌 Conv B (pinned 3h ago)                       │
   │  └───────────────────────┘                         │
   └─────────────────────────────────────────────────────┘
   
   ┌─────────────────────────────────────────────────────┐
   │  NOT PINNED (isPinned = false)                      │
   │  ↓ Sort by: lastMessageTime DESC (newest msg first)│
   │                                                     │
   │  ┌───────────────────────┐                         │
   │  │    Conv C (msg 1h ago)                          │
   │  │    Conv D (msg 5h ago)                          │
   │  │    Conv E (msg 1d ago)                          │
   │  └───────────────────────┘                         │
   └─────────────────────────────────────────────────────┘


═══════════════════════════════════════════════════════════════════════
                  CROSS-USER BEHAVIOR
═══════════════════════════════════════════════════════════════════════

User A (Customer)                       User B (Seller)
      │                                       │
      │ Pins conversation                     │
      ├─────────────────────────────────────► │
      │                                       │ NO EFFECT
      │ Sees:                                 │ Still sees:
      │ 📌 [User B] ← pinned, at top          │    [User A] ← normal position
      │                                       │
      │                                       │
      │ Deletes conversation                  │
      ├─────────────────────────────────────► │
      │                                       │ NO EFFECT
      │ Sees:                                 │ Still sees:
      │ (conversation gone)                   │    [User A] ← still there
      │                                       │
      │                                       │
      │ ◄─────────────────────────────────────┤ User B sends new message
      │ Conversation reappears!               │
      │    [User B] ← back in list            │
      │    (is_deleted set to false)          │
      │                                       │


═══════════════════════════════════════════════════════════════════════
                      COMPONENT ARCHITECTURE
═══════════════════════════════════════════════════════════════════════

Frontend (customer/chat.html, seller/chat.html)
  │
  ├─ UI Components
  │   ├─ conversation-item (with ⋯ button)
  │   ├─ conversation-context-menu
  │   ├─ delete-conversation-modal
  │   └─ pin-indicator (📌)
  │
  ├─ JavaScript Functions
  │   ├─ showConversationActions()
  │   ├─ handlePinConversation()
  │   ├─ handleDeleteConversation()
  │   ├─ confirmDeleteConversation()
  │   └─ renderConversationsList() [updated]
  │
  └─ API Calls
      ├─ POST /api/conversations/{id}/pin
      ├─ POST /api/conversations/{id}/unpin
      └─ DELETE /api/conversations/{id}

                    ▼

Backend (Spring Boot)
  │
  ├─ Controller (ChatController)
  │   ├─ pinConversation()
  │   ├─ unpinConversation()
  │   ├─ deleteConversation()
  │   └─ getConversationMetadata()
  │
  ├─ Service (ChatService)
  │   ├─ pinConversation()
  │   ├─ unpinConversation()
  │   ├─ deleteConversationForUser()
  │   ├─ getConversationsForUser() [updated]
  │   └─ isConversationPinned/Deleted()
  │
  ├─ Repository
  │   └─ UserConversationMetadataRepository
  │
  └─ Entity
      ├─ UserConversationMetadata [new]
      └─ Conversation [updated: +isPinned field]

                    ▼

Database (H2/MySQL)
  │
  └─ Tables
      ├─ user_conversation_metadata [new]
      ├─ chat_conversations
      └─ chat_messages


═══════════════════════════════════════════════════════════════════════
```

