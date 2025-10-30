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

import java.time.LocalDateTime;
import java.util.HashMap;
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

    @MessageMapping("/chat.sendMessage")
    public void sendMessage(@Payload Map<String, Object> messageData) {
        try {
            System.out.println("=== 🚀 WEBSOCKET MESSAGE RECEIVED ===");
            System.out.println("📍 Raw message data: " + messageData);

            // 🚨 CHUYỂN ĐỔI từ Map sang ChatMessage
            ChatMessage message = new ChatMessage();
            message.setConversationId((String) messageData.get("conversationId"));
            // 🚨 KHÔNG set room_id ở đây - để service xử lý
            message.setSenderId(Long.valueOf(messageData.get("senderId").toString()));
            message.setContent((String) messageData.get("content"));

            // Set các field khác
            if (messageData.get("receiverId") != null) {
                message.setReceiverId(Long.valueOf(messageData.get("receiverId").toString()));
            }
            if (messageData.get("senderName") != null) {
                message.setSenderName((String) messageData.get("senderName"));
            }
            if (messageData.get("senderRole") != null) {
                message.setSenderRole((String) messageData.get("senderRole"));
            }

            System.out.println("📍 Parsed message - No room_id set (will be handled by service)");

            // Rate limiting
            Long senderId = message.getSenderId();
            Long lastTime = userLastMessageTime.get(senderId);
            long now = System.currentTimeMillis();

            if (lastTime != null && (now - lastTime) < MESSAGE_RATE_LIMIT_MS) {
                System.err.println("⚠️ Rate limit exceeded for user: " + senderId);
                return;
            }
            userLastMessageTime.put(senderId, now);

            // 🚨 LƯU VÀO DATABASE
            System.out.println("💾 Saving message to database...");
            ChatMessage savedMessage = chatService.addMessage(message);
            System.out.println("✅ Message saved to DB with ID: " + savedMessage.getId());

            // 🚨 Gửi tin nhắn đến CẢ HAI người
            String conversationTopic = "/topic/conversation/" + savedMessage.getConversationId();

            System.out.println("📤 Broadcasting to: " + conversationTopic);
            System.out.println("👤 Sender: " + savedMessage.getSenderId());
            System.out.println("👤 Receiver: " + savedMessage.getReceiverId());

            // Convert để gửi qua WebSocket (KHÔNG gửi room_id)
            Map<String, Object> responseMessage = new HashMap<>();
            responseMessage.put("id", savedMessage.getId());
            responseMessage.put("conversationId", savedMessage.getConversationId());
            responseMessage.put("senderId", savedMessage.getSenderId());
            responseMessage.put("senderName", savedMessage.getSenderName());
            responseMessage.put("senderRole", savedMessage.getSenderRole());
            responseMessage.put("receiverId", savedMessage.getReceiverId());
            responseMessage.put("content", savedMessage.getContent());
            responseMessage.put("messageType", savedMessage.getMessageType());
            responseMessage.put("read", savedMessage.getRead());
            responseMessage.put("createdAt", savedMessage.getCreatedAt().toString());
            responseMessage.put("timestamp", savedMessage.getTimestamp());

            // Gửi đến conversation topic (cả 2 user đều nhận)
            messagingTemplate.convertAndSend(conversationTopic, responseMessage);
            System.out.println("✅ Message broadcasted to conversation");

            System.out.println("🎉 Message delivered successfully");

        } catch (Exception e) {
            System.err.println("💥 CRITICAL ERROR sending message: " + e.getMessage());
            e.printStackTrace();
        }
    }


    @MessageMapping("/chat.typing")
    public void handleTyping(@Payload TypingIndicator indicator) {
        try {
            if (indicator == null || indicator.getConversationId() == null) {
                return;
            }

            String typingTopic = "/topic/conversation/" + indicator.getConversationId() + "/typing";
            messagingTemplate.convertAndSend(typingTopic, indicator);

        } catch (Exception e) {
            System.err.println("Error handling typing indicator: " + e.getMessage());
        }
    }

    @MessageMapping("/chat.userOnline")
    public void handleUserOnline(@Payload Map<String, Object> payload) {
        try {
            Object userIdObj = payload.get("userId");
            if (userIdObj != null) {
                Long userId = Long.valueOf(userIdObj.toString());
                chatService.setUserOnlineStatus(userId, true);

                // Notify all users about online status
                messagingTemplate.convertAndSend("/topic/user.status",
                        Map.of("userId", userId, "online", true, "timestamp", LocalDateTime.now().toString()));
            }
        } catch (Exception e) {
            System.err.println("Error handling user online: " + e.getMessage());
        }
    }

    @MessageMapping("/chat.userOffline")
    public void handleUserOffline(@Payload Map<String, Object> payload) {
        try {
            Object userIdObj = payload.get("userId");
            if (userIdObj != null) {
                Long userId = Long.valueOf(userIdObj.toString());
                chatService.setUserOnlineStatus(userId, false);

                messagingTemplate.convertAndSend("/topic/user.status",
                        Map.of("userId", userId, "online", false, "timestamp", LocalDateTime.now().toString()));
            }
        } catch (Exception e) {
            System.err.println("Error handling user offline: " + e.getMessage());
        }
    }

//    @MessageMapping("/user.connect")
//    public void handleUserConnect(@Payload Map<String, Object> payload) {
//        try {
//            Object userIdObj = payload.get("userId");
//            if (userIdObj != null) {
//                Long userId = Long.valueOf(userIdObj.toString());
//                chatService.setUserOnlineStatus(userId, true);
//                messagingTemplate.convertAndSend("/topic/user.status",
//                        Map.of("userId", userId, "online", true));
//            }
//        } catch (Exception e) {
//            System.err.println("Error handling user connect: " + e.getMessage());
//        }
//    }
//
//    @MessageMapping("/user.disconnect")
//    public void handleUserDisconnect(@Payload Map<String, Object> payload) {
//        try {
//            Object userIdObj = payload.get("userId");
//            if (userIdObj != null) {
//                Long userId = Long.valueOf(userIdObj.toString());
//                chatService.setUserOnlineStatus(userId, false);
//                messagingTemplate.convertAndSend("/topic/user.status",
//                        Map.of("userId", userId, "online", false));
//            }
//        } catch (Exception e) {
//            System.err.println("Error handling user disconnect: " + e.getMessage());
//        }
//    }

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
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) String productName,
            @RequestParam(required = false) java.math.BigDecimal productPrice,
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

                        // Add product info if available
                        if (productId != null) {
                            model.addAttribute("productId", productId);
                            model.addAttribute("productName", productName);
                            model.addAttribute("productPrice", productPrice);
                        }
                    } catch (Exception e) {
                        System.err.println("Error creating conversation: " + e.getMessage());
                    }
                }
            }
        }
        return "customer/chat"; // Return customer chat view
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
            System.out.println("=== CREATE CONVERSATION REQUEST ===");
            System.out.println("Customer ID: " + customerId);
            System.out.println("Seller ID: " + sellerId);

            if (customerId == null) {
                System.err.println("ERROR: Customer ID is null");
                return ResponseEntity.badRequest().body(Map.of("error", "Customer ID is required"));
            }
            if (sellerId == null) {
                System.err.println("ERROR: Seller ID is null");
                return ResponseEntity.badRequest().body(Map.of("error", "Seller ID is required"));
            }

            Conversation conversation = chatService.getOrCreateConversation(customerId, sellerId);
            System.out.println("✓ Conversation created/found: " + conversation.getId());
            return ResponseEntity.ok(conversation);
        } catch (IllegalArgumentException e) {
            System.err.println("ERROR (IllegalArgumentException): " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            System.err.println("ERROR (Exception): " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to create conversation: " + e.getMessage()));
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

    // Thêm method để subscribe user khi connect
    @MessageMapping("/user.subscribe")
    public void handleUserSubscribe(@Payload Map<String, Object> payload) {
        try {
            Object userIdObj = payload.get("userId");
            if (userIdObj != null) {
                Long userId = Long.valueOf(userIdObj.toString());
                chatService.setUserOnlineStatus(userId, true);

                // Notify all users about online status
                messagingTemplate.convertAndSend("/topic/user.status",
                        Map.of("userId", userId, "online", true, "timestamp", LocalDateTime.now().toString()));
            }
        } catch (Exception e) {
            System.err.println("Error handling user subscribe: " + e.getMessage());
        }
    }

    // Thêm method để unsubscribe user khi disconnect
    @MessageMapping("/user.unsubscribe")
    public void handleUserUnsubscribe(@Payload Map<String, Object> payload) {
        try {
            Object userIdObj = payload.get("userId");
            if (userIdObj != null) {
                Long userId = Long.valueOf(userIdObj.toString());
                chatService.setUserOnlineStatus(userId, false);

                messagingTemplate.convertAndSend("/topic/user.status",
                        Map.of("userId", userId, "online", false, "timestamp", LocalDateTime.now().toString()));
            }
        } catch (Exception e) {
            System.err.println("Error handling user unsubscribe: " + e.getMessage());
        }
    }
}
