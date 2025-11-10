package banhangrong.su25.Controller;


import banhangrong.su25.DTO.MessageDeleteDTO;
import banhangrong.su25.DTO.MessageReactionDTO;
import banhangrong.su25.Entity.ChatMessage;
import banhangrong.su25.Entity.Conversation;
import banhangrong.su25.Entity.Users;
import banhangrong.su25.service.ChatService;
import com.fasterxml.jackson.databind.ObjectMapper;
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

    private final ObjectMapper objectMapper = new ObjectMapper();

    // Rate limiting: track last message time per user
    private final Map<Long, Long> userLastMessageTime = new ConcurrentHashMap<>();
    private static final long MESSAGE_RATE_LIMIT_MS = 100; // Minimum 100ms between messages

    @MessageMapping("/chat.sendMessage")
    public void sendMessage(@Payload Map<String, Object> messageData) {
        try {
            System.out.println("=== 🚀 WEBSOCKET MESSAGE RECEIVED ===");

            // chuyen từ Map sang ChatMessage
            ChatMessage message = new ChatMessage();
            message.setConversationId((String) messageData.get("conversationId"));
            message.setSenderId(Long.valueOf(messageData.get("senderId").toString()));
            message.setContent((String) messageData.get("content"));


            if (messageData.get("receiverId") != null) {
                message.setReceiverId(Long.valueOf(messageData.get("receiverId").toString()));
            }
            if (messageData.get("senderName") != null) {
                message.setSenderName((String) messageData.get("senderName"));
            }
            if (messageData.get("senderRole") != null) {
                message.setSenderRole((String) messageData.get("senderRole"));
            }

            if (messageData.get("messageType") != null) {
                message.setMessageType((String) messageData.get("messageType"));
            } else {
                message.setMessageType("TEXT");
            }

            if (messageData.get("fileUrl") != null) {
                message.setFileUrl((String) messageData.get("fileUrl"));
            }
            if (messageData.get("fileName") != null) {
                message.setFileName((String) messageData.get("fileName"));
            }
            if (messageData.get("fileType") != null) {
                message.setFileType((String) messageData.get("fileType"));
            }
            if (messageData.get("fileSize") != null) {
                message.setFileSize(Long.valueOf(messageData.get("fileSize").toString()));
            }

            if (messageData.get("replyToMessageId") != null) {
                message.setReplyToMessageId((String) messageData.get("replyToMessageId"));
            }
            if (messageData.get("replyToSenderName") != null) {
                message.setReplyToSenderName((String) messageData.get("replyToSenderName"));
            }
            if (messageData.get("replyToContent") != null) {
                message.setReplyToContent((String) messageData.get("replyToContent"));
            }

            // luu vao database
            System.out.println("💾 Saving message to database...");
            ChatMessage savedMessage = chatService.addMessage(message);
            System.out.println("✅ Message saved to DB with ID: " + savedMessage.getId());

            // gui tin nhan de ca 2 ng
            String conversationTopic = "/topic/conversation/" + savedMessage.getConversationId();

            System.out.println("📤 Broadcasting to: " + conversationTopic);

            // Convert để gửi qua WebSocket
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

            // Add file attachment info if present
            if (savedMessage.getFileUrl() != null) {
                responseMessage.put("fileUrl", savedMessage.getFileUrl());
                responseMessage.put("fileName", savedMessage.getFileName());
                responseMessage.put("fileType", savedMessage.getFileType());
                responseMessage.put("fileSize", savedMessage.getFileSize());
            }

            // Add reply info if present (NEW)
            if (savedMessage.getReplyToMessageId() != null) {
                responseMessage.put("replyToMessageId", savedMessage.getReplyToMessageId());
                responseMessage.put("replyToSenderName", savedMessage.getReplyToSenderName());
                responseMessage.put("replyToContent", savedMessage.getReplyToContent());
            }

            // Add reactions and deleted status (NEW)
            // Parse reactions JSON string to Object for proper frontend display
            Object reactionsObj = parseReactionsJson(savedMessage.getReactions());
            responseMessage.put("reactions", reactionsObj);
            responseMessage.put("deleted", savedMessage.getDeleted());

            // gui duy nhat 1 lan den conversation topic
            messagingTemplate.convertAndSend(conversationTopic, responseMessage);
            System.out.println("✅ Message broadcasted to conversation");

            // Gui thong bao den nguoi nhan neu co
            if (savedMessage.getReceiverId() != null) {
                String receiverTopic = "/topic/user/" + savedMessage.getReceiverId() + "/messages";
                messagingTemplate.convertAndSend(receiverTopic, responseMessage);
                System.out.println("🔔 Notification sent to receiver: " + receiverTopic);
            }


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
    public String chat(org.springframework.ui.Model model, org.springframework.security.core.Authentication authentication) {
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
    public String sellerChat(org.springframework.ui.Model model, org.springframework.security.core.Authentication authentication) {
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
            org.springframework.security.core.Authentication authentication) {
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

        public TypingIndicator() {
        }

        public String getConversationId() {
            return conversationId;
        }

        public void setConversationId(String conversationId) {
            this.conversationId = conversationId;
        }

        public String getUserId() {
            return userId;
        }

        public void setUserId(String userId) {
            this.userId = userId;
        }

        public String getUserName() {
            return userName;
        }

        public void setUserName(String userName) {
            this.userName = userName;
        }

        public boolean isTyping() {
            return isTyping;
        }

        public void setTyping(boolean typing) {
            isTyping = typing;
        }
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

    // ===== ENHANCED CHAT FEATURES =====

    // Add emoji reaction to a message
    @MessageMapping("/chat.addReaction")
    public void addReaction(@Payload MessageReactionDTO reactionDTO) {
        try {
            System.out.println("=== 😊 ADD REACTION ===");
            System.out.println("Message ID: " + reactionDTO.getMessageId());
            System.out.println("Emoji: " + reactionDTO.getEmoji());
            System.out.println("User ID: " + reactionDTO.getUserId());

            // Add reaction to message
            ChatMessage message = chatService.addReaction(
                    Long.valueOf(reactionDTO.getMessageId()),
                    reactionDTO.getUserId(),
                    reactionDTO.getEmoji()
            );

            if (message != null) {
                // Broadcast to conversation
                Map<String, Object> update = new HashMap<>();
                update.put("messageId", reactionDTO.getMessageId());
                update.put("userId", reactionDTO.getUserId());
                update.put("emoji", reactionDTO.getEmoji());
                update.put("action", "add");

                String reactionTopic = "/topic/conversation/" + reactionDTO.getConversationId() + "/reactions";
                messagingTemplate.convertAndSend(reactionTopic, update);

                System.out.println("✅ Reaction added and broadcasted");
            }
        } catch (Exception e) {
            System.err.println("❌ Error adding reaction: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Remove emoji reaction from a message
    @MessageMapping("/chat.removeReaction")
    public void removeReaction(@Payload MessageReactionDTO reactionDTO) {
        try {
            System.out.println("=== 🗑️ REMOVE REACTION ===");
            System.out.println("Message ID: " + reactionDTO.getMessageId());
            System.out.println("Emoji: " + reactionDTO.getEmoji());
            System.out.println("User ID: " + reactionDTO.getUserId());

            // Remove reaction from message
            ChatMessage message = chatService.removeReaction(
                    Long.valueOf(reactionDTO.getMessageId()),
                    reactionDTO.getUserId(),
                    reactionDTO.getEmoji()
            );

            if (message != null) {
                // Broadcast to conversation
                Map<String, Object> update = new HashMap<>();
                update.put("messageId", reactionDTO.getMessageId());
                update.put("userId", reactionDTO.getUserId());
                update.put("emoji", reactionDTO.getEmoji());
                update.put("action", "remove");

                String reactionTopic = "/topic/conversation/" + reactionDTO.getConversationId() + "/reactions";
                messagingTemplate.convertAndSend(reactionTopic, update);

                System.out.println("✅ Reaction removed and broadcasted");
            }
        } catch (Exception e) {
            System.err.println("❌ Error removing reaction: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Soft delete a message
    @MessageMapping("/chat.deleteMessage")
    public void deleteMessage(@Payload MessageDeleteDTO deleteDTO) {
        try {
            System.out.println("=== 🗑️ DELETE MESSAGE (SOFT) ===");
            System.out.println("Message ID: " + deleteDTO.getMessageId());

            // Soft delete the message
            ChatMessage message = chatService.softDeleteMessage(Long.valueOf(deleteDTO.getMessageId()));

            if (message != null) {
                // Broadcast to conversation
                Map<String, Object> update = new HashMap<>();
                update.put("messageId", deleteDTO.getMessageId());
                update.put("deleted", true);
                update.put("permanent", false);

                String deleteTopic = "/topic/conversation/" + deleteDTO.getConversationId() + "/deletes";
                messagingTemplate.convertAndSend(deleteTopic, update);

                System.out.println("✅ Message soft deleted and broadcasted");
            }
        } catch (Exception e) {
            System.err.println("❌ Error deleting message: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Permanent delete a message
    @MessageMapping("/chat.permanentDeleteMessage")
    public void permanentDeleteMessage(@Payload MessageDeleteDTO deleteDTO) {
        try {
            System.out.println("=== 💥 PERMANENT DELETE MESSAGE ===");
            System.out.println("Message ID: " + deleteDTO.getMessageId());

            // Permanently delete the message
            boolean deleted = chatService.permanentDeleteMessage(Long.valueOf(deleteDTO.getMessageId()));

            if (deleted) {
                // Broadcast to conversation
                Map<String, Object> update = new HashMap<>();
                update.put("messageId", deleteDTO.getMessageId());
                update.put("permanent", true);

                String deleteTopic = "/topic/conversation/" + deleteDTO.getConversationId() + "/deletes";
                messagingTemplate.convertAndSend(deleteTopic, update);

                System.out.println("✅ Message permanently deleted and broadcasted");
            }
        } catch (Exception e) {
            System.err.println("❌ Error permanently deleting message: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Phương pháp trợ giúp để phân tích chuỗi JSON phản ứng thành Đối tượng
    private Object parseReactionsJson(String reactionsJson) {
        if (reactionsJson == null || reactionsJson.trim().isEmpty() || "null".equals(reactionsJson)) {
            return null;
        }

        try {
            // Use ObjectMapper to parse JSON string to Map
            return objectMapper.readValue(reactionsJson, Map.class);
        } catch (Exception e) {
            System.err.println("Error parsing reactions JSON: " + e.getMessage());
            return null;
        }
    }

    // ===== CONVERSATION ACTIONS API (PIN & DELETE) =====

    // Pin a conversation for the current user
    @PostMapping("/api/conversations/{conversationId}/pin")
    @ResponseBody
    public ResponseEntity<?> pinConversation(
            @PathVariable String conversationId,
            @RequestParam Long userId) {
        try {
            System.out.println("=== 📌 PIN CONVERSATION REQUEST ===");
            System.out.println("Conversation ID: " + conversationId);
            System.out.println("User ID: " + userId);

            chatService.pinConversation(userId, conversationId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Conversation pinned successfully");
            response.put("conversationId", conversationId);
            response.put("isPinned", true);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("❌ Error pinning conversation: " + e.getMessage());
            e.printStackTrace();

            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Failed to pin conversation: " + e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Unpin a conversation for the current user
     * POST /api/conversations/{conversationId}/unpin
     */
    @PostMapping("/api/conversations/{conversationId}/unpin")
    @ResponseBody
    public ResponseEntity<?> unpinConversation(
            @PathVariable String conversationId,
            @RequestParam Long userId) {
        try {
            System.out.println("=== 📌 UNPIN CONVERSATION REQUEST ===");
            System.out.println("Conversation ID: " + conversationId);
            System.out.println("User ID: " + userId);

            chatService.unpinConversation(userId, conversationId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Conversation unpinned successfully");
            response.put("conversationId", conversationId);
            response.put("isPinned", false);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("❌ Error unpinning conversation: " + e.getMessage());
            e.printStackTrace();

            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Failed to unpin conversation: " + e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Delete a conversation for the current user (soft delete - local only)
     * DELETE /api/conversations/{conversationId}
     */
    @DeleteMapping("/api/conversations/{conversationId}")
    @ResponseBody
    public ResponseEntity<?> deleteConversation(
            @PathVariable String conversationId,
            @RequestParam Long userId) {
        try {
            System.out.println("=== 🗑️ DELETE CONVERSATION REQUEST ===");
            System.out.println("Conversation ID: " + conversationId);
            System.out.println("User ID: " + userId);

            chatService.deleteConversationForUser(userId, conversationId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Conversation deleted successfully");
            response.put("conversationId", conversationId);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("❌ Error deleting conversation: " + e.getMessage());
            e.printStackTrace();

            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Failed to delete conversation: " + e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Get conversation metadata (pinned/deleted status) for the current user
     * GET /api/conversations/{conversationId}/metadata
     */
    @GetMapping("/api/conversations/{conversationId}/metadata")
    @ResponseBody
    public ResponseEntity<?> getConversationMetadata(
            @PathVariable String conversationId,
            @RequestParam Long userId) {
        try {
            boolean isPinned = chatService.isConversationPinned(userId, conversationId);
            boolean isDeleted = chatService.isConversationDeleted(userId, conversationId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("conversationId", conversationId);
            response.put("isPinned", isPinned);
            response.put("isDeleted", isDeleted);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("❌ Error getting conversation metadata: " + e.getMessage());
            e.printStackTrace();

            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Failed to get conversation metadata: " + e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

}
