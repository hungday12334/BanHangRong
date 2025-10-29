package banhangrong.su25.Controller;


import banhangrong.su25.Entity.ChatMessage;
import banhangrong.su25.Entity.Conversation;
import banhangrong.su25.Entity.Users;
import banhangrong.su25.service.ChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Controller
public class ChatController {

    @Autowired
    private ChatService chatService;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    // Rate limiting: track last message time per user
    private final Map<Long, Long> userLastMessageTime = new ConcurrentHashMap<>();
    private static final long MESSAGE_RATE_LIMIT_MS = 100; // Minimum 100ms between messages

    @MessageMapping("/sendMessage")
    public void sendMessage(@Payload ChatMessage message) {
        try {
            // Validate message
            if (message == null || message.getSenderId() == null || message.getContent() == null) {
                return; // Silently reject invalid messages
            }

            // Rate limiting
            Long senderId = message.getSenderId();
            Long lastTime = userLastMessageTime.get(senderId);
            long now = System.currentTimeMillis();

            if (lastTime != null && (now - lastTime) < MESSAGE_RATE_LIMIT_MS) {
                // Too many messages, ignore
                return;
            }
            userLastMessageTime.put(senderId, now);

            // Save message (will throw exception if invalid)
            ChatMessage savedMessage = chatService.addMessage(message);

            // Send to specific conversation topic
            messagingTemplate.convertAndSend(
                    "/topic/conversation/" + message.getConversationId(),
                    savedMessage
            );

            // Notify the receiver about new message
            if (message.getReceiverId() != null) {
                messagingTemplate.convertAndSend(
                        "/topic/user/" + message.getReceiverId() + "/notification",
                        savedMessage
                );
            }

        } catch (Exception e) {
            // Log error but don't expose internal details to client
            System.err.println("Error sending message: " + e.getMessage());
        }
    }

    @MessageMapping("/typing")
    public void handleTyping(@Payload TypingIndicator indicator) {
        try {
            if (indicator == null || indicator.getConversationId() == null || indicator.getUserId() == null) {
                return;
            }

            messagingTemplate.convertAndSend(
                    "/topic/conversation/" + indicator.getConversationId() + "/typing",
                    indicator
            );
        } catch (Exception e) {
            System.err.println("Error handling typing indicator: " + e.getMessage());
        }
    }

    @MessageMapping("/user.connect")
    public void handleUserConnect(@Payload Map<String, Object> payload) {
        try {
            Object userIdObj = payload.get("userId");
            if (userIdObj != null) {
                Long userId = Long.valueOf(userIdObj.toString());
                chatService.setUserOnlineStatus(userId, true);
                messagingTemplate.convertAndSend("/topic/user.status",
                        Map.of("userId", userId, "online", true));
            }
        } catch (Exception e) {
            System.err.println("Error handling user connect: " + e.getMessage());
        }
    }

    @MessageMapping("/user.disconnect")
    public void handleUserDisconnect(@Payload Map<String, Object> payload) {
        try {
            Object userIdObj = payload.get("userId");
            if (userIdObj != null) {
                Long userId = Long.valueOf(userIdObj.toString());
                chatService.setUserOnlineStatus(userId, false);
                messagingTemplate.convertAndSend("/topic/user.status",
                        Map.of("userId", userId, "online", false));
            }
        } catch (Exception e) {
            System.err.println("Error handling user disconnect: " + e.getMessage());
        }
    }

    @GetMapping("/chat")
    public String chat(org.springframework.ui.Model model, org.springframework.security.core.Authentication authentication){
        if (authentication != null && authentication.isAuthenticated()) {
            org.springframework.security.core.userdetails.UserDetails userDetails =
                (org.springframework.security.core.userdetails.UserDetails) authentication.getPrincipal();
            Users user = chatService.getUserByUsername(userDetails.getUsername());
            if (user != null) {
                model.addAttribute("user", user);
            }
        }
        return "seller/chat";
    }

    @GetMapping("/seller/chat")
    public String sellerChat(org.springframework.ui.Model model, org.springframework.security.core.Authentication authentication){
        if (authentication != null && authentication.isAuthenticated()) {
            org.springframework.security.core.userdetails.UserDetails userDetails =
                (org.springframework.security.core.userdetails.UserDetails) authentication.getPrincipal();
            Users user = chatService.getUserByUsername(userDetails.getUsername());
            if (user != null) {
                model.addAttribute("user", user);
            }
        }
        return "seller/chat";
    }

    @GetMapping("/customer/chat")
    public String customerChat(
            @RequestParam(required = false) Long sellerId,
            org.springframework.ui.Model model,
            org.springframework.security.core.Authentication authentication){
        if (authentication != null && authentication.isAuthenticated()) {
            org.springframework.security.core.userdetails.UserDetails userDetails =
                (org.springframework.security.core.userdetails.UserDetails) authentication.getPrincipal();
            Users user = chatService.getUserByUsername(userDetails.getUsername());
            if (user != null) {
                model.addAttribute("user", user);

                // If sellerId is provided, add it to model so chat page can auto-open conversation
                if (sellerId != null) {
                    model.addAttribute("targetSellerId", sellerId);

                    // Get or create conversation
                    try {
                        Conversation conversation = chatService.getOrCreateConversation(user.getUserId(), sellerId);
                        model.addAttribute("conversationId", conversation.getId());
                    } catch (Exception e) {
                        System.err.println("Error creating conversation: " + e.getMessage());
                    }
                }
            }
        }
        return "seller/chat"; // Same page, different functionality based on logged-in user
    }

    // REST endpoints for managing conversations

    @GetMapping("/api/users/{userId}")
    @ResponseBody
    public ResponseEntity<?> getUser(@PathVariable Long userId) {
        try {
            if (userId == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "User ID is required"));
            }

            Users user = chatService.getUser(userId);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "User not found"));
            }
            return ResponseEntity.ok(user);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to get user"));
        }
    }

    @GetMapping("/api/sellers")
    @ResponseBody
    public ResponseEntity<?> getSellers() {
        try {
            List<Users> sellers = chatService.getSellers();
            return ResponseEntity.ok(sellers);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to get sellers"));
        }
    }

    @GetMapping("/api/conversations/{userId}")
    @ResponseBody
    public ResponseEntity<?> getUserConversations(@PathVariable Long userId) {
        try {
            if (userId == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "User ID is required"));
            }

            List<Conversation> conversations = chatService.getConversationsForUser(userId);
            return ResponseEntity.ok(conversations);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to get conversations"));
        }
    }

    @GetMapping("/api/conversation/{conversationId}")
    @ResponseBody
    public ResponseEntity<?> getConversation(@PathVariable String conversationId) {
        try {
            if (conversationId == null || conversationId.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Conversation ID is required"));
            }

            Conversation conversation = chatService.getConversation(conversationId);
            if (conversation == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Conversation not found"));
            }
            return ResponseEntity.ok(conversation);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to get conversation"));
        }
    }

    @PostMapping("/api/conversation")
    @ResponseBody
    public ResponseEntity<?> createConversation(@RequestParam Long customerId, @RequestParam Long sellerId) {
        try {
            if (customerId == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Customer ID is required"));
            }
            if (sellerId == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Seller ID is required"));
            }

            Conversation conversation = chatService.getOrCreateConversation(customerId, sellerId);
            return ResponseEntity.ok(conversation);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to create conversation"));
        }
    }

    @PostMapping("/api/conversation/{conversationId}/read")
    @ResponseBody
    public ResponseEntity<?> markAsRead(@PathVariable String conversationId, @RequestParam Long userId) {
        try {
            if (conversationId == null || conversationId.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Conversation ID is required"));
            }
            if (userId == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "User ID is required"));
            }

            chatService.markConversationAsRead(conversationId, userId);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to mark conversation as read"));
        }
    }

    // Inner class for typing indicator
    public static class TypingIndicator {
        private String conversationId;
        private String userId;
        private String userName;
        private boolean isTyping;

        public TypingIndicator() {}

        public String getConversationId() { return conversationId; }
        public void setConversationId(String conversationId) { this.conversationId = conversationId; }

        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }

        public String getUserName() { return userName; }
        public void setUserName(String userName) { this.userName = userName; }

        public boolean isTyping() { return isTyping; }
        public void setTyping(boolean typing) { isTyping = typing; }
    }
}
