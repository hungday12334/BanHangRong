package banhangrong.su25.service;

import banhangrong.su25.Entity.ChatMessage;
import banhangrong.su25.Entity.Conversation;
import banhangrong.su25.Entity.Users;
import banhangrong.su25.Repository.ConversationRepository;
import banhangrong.su25.Repository.MessageRepository;
import banhangrong.su25.Repository.UsersRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class ChatService {

    private final UsersRepository usersRepository;
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;

    // Track online users in memory (in production, use Redis or similar)
    private final Map<Long, Boolean> onlineUsers = new HashMap<>();

    @Autowired
    public ChatService(UsersRepository usersRepository,
                       ConversationRepository conversationRepository,
                       MessageRepository messageRepository) {
        this.usersRepository = usersRepository;
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
    }

    public Users getUser(Long userId) {
        if (userId == null) {
            return null;
        }
        return usersRepository.findById(userId).orElse(null);
    }

    public Users getUserByUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            return null;
        }
        return usersRepository.findByUsername(username).orElse(null);
    }

    public List<Users> getSellers() {
        // Get all users with SELLER user type
        return usersRepository.findByUserType("SELLER");
    }

    @Transactional
    public void setUserOnlineStatus(Long userId, boolean online) {
        if (userId != null) {
            onlineUsers.put(userId, online);
            // Also update last_login in database
            usersRepository.findById(userId).ifPresent(user -> {
                user.setLastLogin(LocalDateTime.now());
                usersRepository.save(user);
            });
        }
    }

    public boolean isUserOnline(Long userId) {
        return onlineUsers.getOrDefault(userId, false);
    }

    @Transactional
    public Conversation getOrCreateConversation(Long customerId, Long sellerId) {
        if (customerId == null) {
            throw new IllegalArgumentException("Customer ID cannot be null");
        }
        if (sellerId == null) {
            throw new IllegalArgumentException("Seller ID cannot be null");
        }

        Users customer = usersRepository.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found: " + customerId));
        Users seller = usersRepository.findById(sellerId)
                .orElseThrow(() -> new IllegalArgumentException("Seller not found: " + sellerId));

        if (!"SELLER".equalsIgnoreCase(seller.getUserType())) {
            throw new IllegalArgumentException("User " + sellerId + " is not a seller");
        }

        // Try to find existing conversation
        Optional<Conversation> existing = conversationRepository.findByCustomerIdAndSellerId(customerId, sellerId);
        if (existing.isPresent()) {
            Conversation conv = existing.get();
            // Load messages
            conv.setMessages(messageRepository.findByConversationIdOrderByCreatedAtAsc(conv.getId()));
            // Calculate unread count
            conv.setUnreadCount(messageRepository.countUnreadMessages(conv.getId(), customerId).intValue());
            return conv;
        }

        // Create new conversation
        String conversationId = generateConversationId(customerId, sellerId);
        Conversation conv = new Conversation();
        conv.setId(conversationId);
        conv.setCustomerId(customerId);
        conv.setSellerId(sellerId);
        conv.setCustomerName(customer.getFullName() != null ? customer.getFullName() : customer.getUsername());
        conv.setSellerName(seller.getFullName() != null ? seller.getFullName() : seller.getUsername());
        conv.setMessages(new ArrayList<>());
        conv.setUnreadCount(0);
        conv.setCreatedAt(LocalDateTime.now());
        conv.setUpdatedAt(LocalDateTime.now());

        return conversationRepository.save(conv);
    }

    public Conversation getConversation(String conversationId) {
        if (conversationId == null || conversationId.trim().isEmpty()) {
            return null;
        }

        return conversationRepository.findById(conversationId).map(conv -> {
            // Load messages
            conv.setMessages(messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId));
            return conv;
        }).orElse(null);
    }


    public List<Conversation> getConversationsForUser(Long userId) {
        System.out.println("=== LOADING CONVERSATIONS FOR USER: " + userId + " ===");

        if (userId == null) {
            return new ArrayList<>();
        }

        List<Conversation> conversations = conversationRepository.findConversationsByUserId(userId);
        System.out.println("✓ Found " + conversations.size() + " conversations");

        // Load messages and unread counts for each conversation
        for (Conversation conv : conversations) {
            try {
                // Load messages from database
                List<ChatMessage> messages = messageRepository.findByConversationIdOrderByCreatedAtAsc(conv.getId());
                conv.setMessages(messages);
                System.out.println("✓ Loaded " + messages.size() + " messages for conversation " + conv.getId());

                // Load unread count
                long unreadCount = messageRepository.countUnreadMessages(conv.getId(), userId);
                conv.setUnreadCount((int) unreadCount);

                // Set last message and time
                if (!messages.isEmpty()) {
                    ChatMessage lastMessage = messages.get(messages.size() - 1);
                    conv.setLastMessage(lastMessage.getContent());
                    conv.setLastMessageTime(lastMessage.getCreatedAt());
                }
            } catch (Exception e) {
                System.err.println("❌ Error loading conversation " + conv.getId() + ": " + e.getMessage());
            }
        }

        // Sort by last message time
        conversations.sort((c1, c2) -> {
            LocalDateTime time1 = c1.getLastMessageTime() != null ? c1.getLastMessageTime() : c1.getCreatedAt();
            LocalDateTime time2 = c2.getLastMessageTime() != null ? c2.getLastMessageTime() : c2.getCreatedAt();
            return time2.compareTo(time1); // Descending order
        });

        return conversations;
    }

    @Transactional
    public ChatMessage addMessage(ChatMessage message) {
        System.out.println("=== 💾 ADDING MESSAGE TO DATABASE ===");
        System.out.println("📍 Conversation: " + message.getConversationId());
        System.out.println("📍 Sender: " + message.getSenderId());
        System.out.println("📍 Content: " + message.getContent());

        try {
            // Validate conversation exists
            Conversation conversation = conversationRepository.findById(message.getConversationId())
                    .orElseGet(() -> {
                        System.err.println("❌ Conversation not found: " + message.getConversationId());
                        // 🚨 TẠO CONVERSATION NẾU CHƯA CÓ
                        return createConversationFromMessage(message);
                    });

            System.out.println("✅ Conversation found: " + conversation.getId());

            // 🚨 QUAN TRỌNG: Đảm bảo receiverId được set đúng
            if (message.getReceiverId() == null) {
                if (message.getSenderId().equals(conversation.getCustomerId())) {
                    message.setReceiverId(conversation.getSellerId());
                } else {
                    message.setReceiverId(conversation.getCustomerId());
                }
                System.out.println("✅ Auto-set receiver: " + message.getReceiverId());
            }

            // Set các field bắt buộc khác
            if (message.getCreatedAt() == null) {
                message.setCreatedAt(LocalDateTime.now());
            }
            if (message.getMessageType() == null) {
                message.setMessageType("TEXT");
            }
            if (message.getRead() == null) {
                message.setRead(false);
            }

            // Get sender info từ database
            Users sender = usersRepository.findById(message.getSenderId())
                    .orElseThrow(() -> new IllegalArgumentException("Sender not found: " + message.getSenderId()));

            message.setSenderName(sender.getFullName() != null ? sender.getFullName() : sender.getUsername());
            message.setSenderRole(sender.getUserType());

            System.out.println("💽 Saving message to database...");

            // 🚨 LƯU VÀO DATABASE
            ChatMessage savedMessage = messageRepository.save(message);
            System.out.println("✅ Message saved with ID: " + savedMessage.getId());

            // Update conversation
            conversation.setLastMessage(message.getContent());
            conversation.setLastMessageTime(LocalDateTime.now());
            conversation.setUpdatedAt(LocalDateTime.now());
            conversationRepository.save(conversation);

            System.out.println("✅ Conversation updated");

            return savedMessage;

        } catch (Exception e) {
            System.err.println("💥 ERROR saving message to DB: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to save message: " + e.getMessage(), e);
        }
    }

    // 🚨 THÊM METHOD ĐỂ TẠO CONVERSATION NẾU CHƯA CÓ
    private Conversation createConversationFromMessage(ChatMessage message) {
        System.out.println("🆕 Creating new conversation for message...");

        // Phân tích conversationId để lấy customerId và sellerId
        String[] parts = message.getConversationId().split("_");
        if (parts.length >= 3) {
            Long customerId = Long.parseLong(parts[1]);
            Long sellerId = Long.parseLong(parts[2]);

            return getOrCreateConversation(customerId, sellerId);
        } else {
            throw new IllegalStateException("Invalid conversation ID format: " + message.getConversationId());
        }
    }



    @Transactional
    public void markConversationAsRead(String conversationId, Long userId) {
        if (conversationId == null || userId == null) {
            return;
        }

        Conversation conversation = conversationRepository.findById(conversationId).orElse(null);
        if (conversation != null) {
            // Only reset unread count if user is part of conversation
            if (userId.equals(conversation.getCustomerId()) || userId.equals(conversation.getSellerId())) {
                messageRepository.markConversationAsRead(conversationId, userId);
            }
        }
    }

    private String generateConversationId(Long customerId, Long sellerId) {
        // Ensure consistent ordering for same customer-seller pair
        Long minId = Math.min(customerId, sellerId);
        Long maxId = Math.max(customerId, sellerId);
        return "conv_" + minId + "_" + maxId;
    }

    // ===== ENHANCED CHAT FEATURES =====

    /**
     * Add emoji reaction to a message
     */
    @Transactional
    public ChatMessage addReaction(Long messageId, String userId, String emoji) {
        System.out.println("=== 😊 ADDING REACTION TO MESSAGE ===");
        System.out.println("Message ID: " + messageId);
        System.out.println("User ID: " + userId);
        System.out.println("Emoji: " + emoji);

        try {
            ChatMessage message = messageRepository.findById(messageId)
                    .orElseThrow(() -> new IllegalArgumentException("Message not found: " + messageId));

            // Parse existing reactions JSON
            String reactionsJson = message.getReactions();
            Map<String, List<String>> reactions = parseReactions(reactionsJson);

            // Add user to emoji list
            List<String> userList = reactions.getOrDefault(emoji, new ArrayList<>());
            if (!userList.contains(userId)) {
                userList.add(userId);
                reactions.put(emoji, userList);
            }

            // Convert back to JSON and save
            message.setReactions(stringifyReactions(reactions));
            ChatMessage saved = messageRepository.save(message);

            System.out.println("✅ Reaction added successfully");
            return saved;

        } catch (Exception e) {
            System.err.println("❌ Error adding reaction: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to add reaction", e);
        }
    }

    /**
     * Remove emoji reaction from a message
     */
    @Transactional
    public ChatMessage removeReaction(Long messageId, String userId, String emoji) {
        System.out.println("=== 🗑️ REMOVING REACTION FROM MESSAGE ===");
        System.out.println("Message ID: " + messageId);
        System.out.println("User ID: " + userId);
        System.out.println("Emoji: " + emoji);

        try {
            ChatMessage message = messageRepository.findById(messageId)
                    .orElseThrow(() -> new IllegalArgumentException("Message not found: " + messageId));

            // Parse existing reactions JSON
            String reactionsJson = message.getReactions();
            Map<String, List<String>> reactions = parseReactions(reactionsJson);

            // Remove user from emoji list
            List<String> userList = reactions.get(emoji);
            if (userList != null) {
                userList.remove(userId);
                if (userList.isEmpty()) {
                    reactions.remove(emoji);
                } else {
                    reactions.put(emoji, userList);
                }
            }

            // Convert back to JSON and save
            message.setReactions(stringifyReactions(reactions));
            ChatMessage saved = messageRepository.save(message);

            System.out.println("✅ Reaction removed successfully");
            return saved;

        } catch (Exception e) {
            System.err.println("❌ Error removing reaction: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to remove reaction", e);
        }
    }

    /**
     * Soft delete a message (mark as deleted)
     */
    @Transactional
    public ChatMessage softDeleteMessage(Long messageId) {
        System.out.println("=== 🗑️ SOFT DELETING MESSAGE ===");
        System.out.println("Message ID: " + messageId);

        try {
            ChatMessage message = messageRepository.findById(messageId)
                    .orElseThrow(() -> new IllegalArgumentException("Message not found: " + messageId));

            message.setDeleted(true);
            message.setContent("This message has been deleted");
            ChatMessage saved = messageRepository.save(message);

            System.out.println("✅ Message soft deleted successfully");
            return saved;

        } catch (Exception e) {
            System.err.println("❌ Error soft deleting message: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to soft delete message", e);
        }
    }

    /**
     * Permanently delete a message
     */
    @Transactional
    public boolean permanentDeleteMessage(Long messageId) {
        System.out.println("=== 💥 PERMANENTLY DELETING MESSAGE ===");
        System.out.println("Message ID: " + messageId);

        try {
            if (messageRepository.existsById(messageId)) {
                messageRepository.deleteById(messageId);
                System.out.println("✅ Message permanently deleted successfully");
                return true;
            } else {
                System.out.println("⚠️ Message not found");
                return false;
            }

        } catch (Exception e) {
            System.err.println("❌ Error permanently deleting message: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to permanently delete message", e);
        }
    }

    /**
     * Parse reactions JSON string to Map
     */
    private Map<String, List<String>> parseReactions(String reactionsJson) {
        if (reactionsJson == null || reactionsJson.trim().isEmpty() || "null".equals(reactionsJson)) {
            return new HashMap<>();
        }

        try {
            // Simple JSON parsing for format: {"❤️":["user1","user2"],"😂":["user3"]}
            Map<String, List<String>> reactions = new HashMap<>();

            // Remove outer braces
            String content = reactionsJson.trim();
            if (content.startsWith("{")) content = content.substring(1);
            if (content.endsWith("}")) content = content.substring(0, content.length() - 1);

            if (content.trim().isEmpty()) {
                return reactions;
            }

            // Split by emoji entries (looking for "}," or "]," patterns)
            String[] entries = content.split("(?<=\\]),");

            for (String entry : entries) {
                entry = entry.trim();
                if (entry.isEmpty()) continue;

                // Extract emoji and users
                int colonIndex = entry.indexOf(":");
                if (colonIndex > 0) {
                    String emoji = entry.substring(0, colonIndex).replaceAll("[\"{}]", "").trim();
                    String usersStr = entry.substring(colonIndex + 1).trim();

                    // Parse user list
                    usersStr = usersStr.replaceAll("[\\[\\]]", "").trim();
                    List<String> userList = new ArrayList<>();

                    if (!usersStr.isEmpty()) {
                        String[] users = usersStr.split(",");
                        for (String user : users) {
                            String cleanUser = user.replaceAll("\"", "").trim();
                            if (!cleanUser.isEmpty()) {
                                userList.add(cleanUser);
                            }
                        }
                    }

                    if (!emoji.isEmpty()) {
                        reactions.put(emoji, userList);
                    }
                }
            }

            return reactions;

        } catch (Exception e) {
            System.err.println("Error parsing reactions JSON: " + e.getMessage());
            return new HashMap<>();
        }
    }

    /**
     * Convert reactions Map to JSON string
     */
    private String stringifyReactions(Map<String, List<String>> reactions) {
        if (reactions == null || reactions.isEmpty()) {
            return null;
        }

        try {
            StringBuilder json = new StringBuilder("{");
            boolean first = true;

            for (Map.Entry<String, List<String>> entry : reactions.entrySet()) {
                if (!first) {
                    json.append(",");
                }
                first = false;

                json.append("\"").append(entry.getKey()).append("\":[");

                List<String> users = entry.getValue();
                for (int i = 0; i < users.size(); i++) {
                    if (i > 0) json.append(",");
                    json.append("\"").append(users.get(i)).append("\"");
                }

                json.append("]");
            }

            json.append("}");
            return json.toString();

        } catch (Exception e) {
            System.err.println("Error stringifying reactions: " + e.getMessage());
            return null;
        }
    }
}
