package banhangrong.su25.service;

import banhangrong.su25.Entity.ChatRoom;
import banhangrong.su25.Entity.ChatMessage;
import banhangrong.su25.Entity.Users;
import banhangrong.su25.Entity.Products;
import banhangrong.su25.Repository.ChatRoomRepository;
import banhangrong.su25.Repository.ChatMessageRepository;
import banhangrong.su25.Repository.UsersRepository;
import banhangrong.su25.Repository.ProductsRepository;
import banhangrong.su25.WebSocket.ChatWebSocketHandler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class ChatService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final UsersRepository usersRepository;
    private final ProductsRepository productsRepository;
    private final ChatWebSocketHandler chatWebSocketHandler;

    public ChatService(ChatRoomRepository chatRoomRepository,
                       ChatMessageRepository chatMessageRepository,
                       UsersRepository usersRepository,
                       ProductsRepository productsRepository,
                       ChatWebSocketHandler chatWebSocketHandler) {
        this.chatRoomRepository = chatRoomRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.usersRepository = usersRepository;
        this.productsRepository = productsRepository;
        this.chatWebSocketHandler = chatWebSocketHandler;
    }

    @Transactional
    public ChatRoom getOrCreateChatRoom(Long sellerId, Long customerId, Long productId) {
        Users seller = usersRepository.findById(sellerId)
                .orElseThrow(() -> new RuntimeException("Seller not found"));
        Users customer = usersRepository.findById(customerId)
                .orElseThrow(() -> new RuntimeException("Customer not found"));

        Products product = null;
        if (productId != null) {
            product = productsRepository.findById(productId)
                    .orElseThrow(() -> new RuntimeException("Product not found"));
        }

        Optional<ChatRoom> existingRoom;
        if (product != null) {
            existingRoom = chatRoomRepository.findBySellerAndCustomerAndProduct(seller, customer, productId);
        } else {
            existingRoom = chatRoomRepository.findBySellerAndCustomer(seller, customer);
        }

        if (existingRoom.isPresent()) {
            return existingRoom.get();
        }

        ChatRoom newRoom = new ChatRoom();
        newRoom.setSeller(seller);
        newRoom.setCustomer(customer);
        newRoom.setProduct(product);
        newRoom.setIsActive(true);

        return chatRoomRepository.save(newRoom);
    }

    @Transactional
    public ChatMessage sendMessage(Long roomId, Long senderId, String content, String messageType) {
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("Chat room not found"));
        Users sender = usersRepository.findById(senderId)
                .orElseThrow(() -> new RuntimeException("Sender not found"));

        ChatMessage message = new ChatMessage();
        message.setRoom(room);
        message.setSender(sender);
        message.setContent(content);
        message.setMessageType(messageType != null ? messageType : "TEXT");

        ChatMessage savedMessage = chatMessageRepository.save(message);

        // Update room's updatedAt
        room.setUpdatedAt(LocalDateTime.now());
        chatRoomRepository.save(room);

        // Broadcast via WebSocket
        chatWebSocketHandler.broadcastToRoom(roomId,
                new ChatWebSocketHandler.ChatPayload("new_message", "Tin nhắn mới", savedMessage));

        return savedMessage;
    }

    public List<ChatMessage> getRoomMessages(Long roomId) {
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("Chat room not found"));
        return chatMessageRepository.findByRoomOrderByCreatedAtAsc(room);
    }

    public List<ChatRoom> getUserChatRooms(Long userId) {
        Users user = usersRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return chatRoomRepository.findByUser(user);
    }

    public List<ChatRoom> getSellerChatRooms(Long sellerId) {
        Users seller = usersRepository.findById(sellerId)
                .orElseThrow(() -> new RuntimeException("Seller not found"));
        return chatRoomRepository.findBySeller(seller);
    }

    @Transactional
    public void markMessagesAsRead(Long roomId, Long userId) {
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("Chat room not found"));
        chatMessageRepository.markMessagesAsRead(room, userId);
    }

    public Long getUnreadMessageCount(Long roomId, Long userId) {
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("Chat room not found"));
        return chatMessageRepository.countUnreadMessages(room, userId);
    }

    public Long getTotalUnreadCount(Long userId) {
        List<ChatRoom> rooms = getUserChatRooms(userId);
        return rooms.stream()
                .mapToLong(room -> getUnreadMessageCount(room.getRoomId(), userId))
                .sum();
    }

    // Thêm method này vào ChatService class
    @Transactional
    public ChatMessage sendMessageWithAI(Long roomId, Long senderId, String content, String messageType) {
        ChatMessage userMessage = sendMessage(roomId, senderId, content, messageType);

        // Auto-reply for common questions (basic AI)
        String aiResponse = generateAIResponse(content, roomId);
        if (aiResponse != null) {
            // Send AI response as seller
            Users seller = getRoomSeller(roomId);
            sendMessage(roomId, seller.getUserId(), aiResponse, "TEXT");
        }

        return userMessage;
    }

    private String generateAIResponse(String userMessage, Long roomId) {
        String lowerMessage = userMessage.toLowerCase();

        // Basic keyword matching for common questions
        if (lowerMessage.contains("giá") || lowerMessage.contains("bao nhiêu")) {
            return "Xin chào! Vui lòng kiểm tra giá sản phẩm trên trang chi tiết sản phẩm. Tôi có thể hỗ trợ thêm thông tin nếu bạn cần!";
        }

        if (lowerMessage.contains("tải") || lowerMessage.contains("download")) {
            return "Sau khi thanh toán thành công, bạn sẽ nhận được link download và license key qua email và trong tài khoản cá nhân.";
        }

        if (lowerMessage.contains("license") || lowerMessage.contains("key")) {
            return "License key sẽ được gửi tự động sau khi thanh toán. Mỗi key chỉ active được trên 1 thiết bị.";
        }

        if (lowerMessage.contains("cám ơn") || lowerMessage.contains("thanks")) {
            return "Cám ơn bạn! Nếu có thắc mắc gì thêm, tôi sẵn sàng hỗ trợ 😊";
        }

        // Return null for no auto-response
        return null;
    }

    private Users getRoomSeller(Long roomId) {
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("Chat room not found"));
        return room.getSeller();
    }
}