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
@RequestMapping("/customer/chat")
public class CustomerChatController {

    private final ChatService chatService;

    public CustomerChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @GetMapping("/start/{productId}")
    @ResponseBody
    public Map<String, Object> startChat(@PathVariable Long productId,
                                         @RequestParam Long customerId) {
        // For demo - in real app, get sellerId from product
        Long sellerId = 1L; // Default seller

        ChatRoom room = chatService.getOrCreateChatRoom(sellerId, customerId, productId);

        return Map.of(
                "success", true,
                "roomId", room.getRoomId(),
                "message", "Đã bắt đầu chat với người bán"
        );
    }

    @GetMapping("/room/{roomId}")
    public String customerChatRoom(@PathVariable Long roomId, Model model) {
        // For demo
        Long customerId = 2L;

        List<ChatMessage> messages = chatService.getRoomMessages(roomId);
        ChatRoom room = chatService.getOrCreateChatRoom(1L, customerId, null);

        // Mark as read
        chatService.markMessagesAsRead(roomId, customerId);

        model.addAttribute("room", room);
        model.addAttribute("messages", messages);
        model.addAttribute("customerId", customerId);

        return "customer/chat-room";
    }

    @PostMapping("/send")
    @ResponseBody
    public Map<String, Object> sendCustomerMessage(@RequestBody Map<String, Object> request) {
        Long roomId = Long.valueOf(request.get("roomId").toString());
        Long senderId = Long.valueOf(request.get("senderId").toString());
        String content = request.get("content").toString();

        ChatMessage message = chatService.sendMessage(roomId, senderId, content, "TEXT");

        return Map.of(
                "success", true,
                "message", "Tin nhắn đã gửi",
                "data", message
        );
    }
}