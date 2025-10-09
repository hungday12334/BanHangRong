package banhangrong.su25.Controller;

import banhangrong.su25.Entity.ChatRoom;
import banhangrong.su25.Entity.ChatMessage;
import banhangrong.su25.service.ChatService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/seller/chat")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @GetMapping
    public String chatDashboard(Model model) {
        // For demo, using seller ID 1 - in real app, get from authentication
        Long sellerId = 1L;

        List<ChatRoom> chatRooms = chatService.getSellerChatRooms(sellerId);
        Long totalUnread = chatService.getTotalUnreadCount(sellerId);

        model.addAttribute("chatRooms", chatRooms);
        model.addAttribute("totalUnread", totalUnread);
        model.addAttribute("sellerId", sellerId);

        return "seller/chat";
    }

    @GetMapping("/room/{roomId}")
    public String chatRoom(@PathVariable Long roomId, Model model) {
        // For demo, using seller ID 1
        Long sellerId = 1L;

        ChatRoom room = chatService.getOrCreateChatRoom(sellerId, 2L, null); // Demo customer ID 2
        List<ChatMessage> messages = chatService.getRoomMessages(roomId);

        // Mark messages as read when opening room
        chatService.markMessagesAsRead(roomId, sellerId);

        model.addAttribute("room", room);
        model.addAttribute("messages", messages);
        model.addAttribute("sellerId", sellerId);

        return "seller/chat-room";
    }

    @PostMapping("/send")
    @ResponseBody
    public Map<String, Object> sendMessage(@RequestBody Map<String, Object> request) {
        Long roomId = Long.valueOf(request.get("roomId").toString());
        Long senderId = Long.valueOf(request.get("senderId").toString());
        String content = request.get("content").toString();
        String messageType = request.get("messageType") != null ?
                request.get("messageType").toString() : "TEXT";

        ChatMessage message = chatService.sendMessage(roomId, senderId, content, messageType);

        return Map.of(
                "success", true,
                "message", "Tin nhắn đã gửi",
                "data", message
        );
    }

    @GetMapping("/api/rooms")
    @ResponseBody
    public List<ChatRoom> getChatRooms(@RequestParam Long sellerId) {
        return chatService.getSellerChatRooms(sellerId);
    }

    @GetMapping("/api/messages/{roomId}")
    @ResponseBody
    public List<ChatMessage> getMessages(@PathVariable Long roomId) {
        return chatService.getRoomMessages(roomId);
    }

    @GetMapping("/api/unread-count")
    @ResponseBody
    public Map<String, Object> getUnreadCount(@RequestParam Long userId) {
        Long count = chatService.getTotalUnreadCount(userId);
        return Map.of("unreadCount", count);
    }
}