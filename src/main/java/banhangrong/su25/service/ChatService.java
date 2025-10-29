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
        return usersRepository.findAll().stream()
                .filter(u -> "SELLER".equalsIgnoreCase(u.getUserType()))
                .toList();
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
            conv.setUnreadCount((int) messageRepository.countUnreadMessages(conv.getId(), customerId));
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
        if (userId == null) {
            return new ArrayList<>();
        }

        Users user = usersRepository.findById(userId).orElse(null);
        if (user == null) {
            return new ArrayList<>();
        }

        List<Conversation> conversations = conversationRepository.findConversationsByUserId(userId);

        // Load unread counts for each conversation
        conversations.forEach(conv -> {
            long unreadCount = messageRepository.countUnreadMessages(conv.getId(), userId);
            conv.setUnreadCount((int) unreadCount);
        });

        return conversations;
    }

    @Transactional
    public ChatMessage addMessage(ChatMessage message) {
        if (message == null) {
            throw new IllegalArgumentException("Message cannot be null");
        }
        if (message.getConversationId() == null || message.getConversationId().trim().isEmpty()) {
            throw new IllegalArgumentException("Conversation ID cannot be empty");
        }
        if (message.getSenderId() == null) {
            throw new IllegalArgumentException("Sender ID cannot be null");
        }
        if (message.getContent() == null || message.getContent().trim().isEmpty()) {
            throw new IllegalArgumentException("Message content cannot be empty");
        }

        // Validate content length
        if (message.getContent().length() > 5000) {
            throw new IllegalArgumentException("Message too long (max 5000 characters)");
        }

        // Verify conversation exists
        Conversation conversation = conversationRepository.findById(message.getConversationId())
                .orElseThrow(() -> new IllegalStateException("Conversation not found: " + message.getConversationId()));

        // Verify sender is part of conversation
        if (!message.getSenderId().equals(conversation.getCustomerId()) &&
                !message.getSenderId().equals(conversation.getSellerId())) {
            throw new IllegalArgumentException("Sender is not part of this conversation");
        }

        // Get sender info
        Users sender = usersRepository.findById(message.getSenderId()).orElse(null);
        if (sender != null) {
            message.setSenderName(sender.getFullName() != null ? sender.getFullName() : sender.getUsername());
            message.setSenderRole(sender.getUserType());
        }

        // Save message to database
        ChatMessage savedMessage = messageRepository.save(message);

        // Update conversation
        conversation.setLastMessage(message.getContent());
        conversation.setLastMessageTime(LocalDateTime.now());
        conversationRepository.save(conversation);

        return savedMessage;
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
        return "conv_" + customerId + "_" + sellerId;
    }
}

