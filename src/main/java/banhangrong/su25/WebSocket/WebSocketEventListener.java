package banhangrong.su25.WebSocket;

import banhangrong.su25.service.ChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class WebSocketEventListener {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private ChatService chatService;

    // Track user sessions
    private final Map<String, Long> sessionUserMap = new ConcurrentHashMap<>();

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();

        // Get userId from headers if available
        String userIdStr = headerAccessor.getFirstNativeHeader("userId");

        if (userIdStr != null && sessionId != null) {
            try {
                Long userId = Long.valueOf(userIdStr);
                sessionUserMap.put(sessionId, userId);
                chatService.setUserOnlineStatus(userId, true);

                // Broadcast user online status
                messagingTemplate.convertAndSend("/topic/user.status",
                        Map.of("userId", userId, "online", true));

                System.out.println("User connected: " + userId + " (session: " + sessionId + ")");
            } catch (NumberFormatException e) {
                System.err.println("Invalid userId format: " + userIdStr);
            }
        }
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();

        if (sessionId != null) {
            Long userId = sessionUserMap.remove(sessionId);

            if (userId != null) {
                chatService.setUserOnlineStatus(userId, false);

                // Broadcast user offline status
                messagingTemplate.convertAndSend("/topic/user.status",
                        Map.of("userId", userId, "online", false));

                System.out.println("User disconnected: " + userId + " (session: " + sessionId + ")");
            }
        }
    }
}
