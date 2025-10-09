package banhangrong.su25.WebSocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.lang.NonNull;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {
    private static final Logger log = LoggerFactory.getLogger(ChatWebSocketHandler.class);

    // Map<roomId, Set<session>>
    private final Map<Long, Set<WebSocketSession>> roomSessions = new ConcurrentHashMap<>();
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public void afterConnectionEstablished(@NonNull WebSocketSession session) throws Exception {
        log.debug("Chat WebSocket connected: {}", session.getId());
        // Send welcome message
        sendJson(session, new ChatPayload("connected", "Kết nối chat thành công", null));
    }

    @Override
    public void handleTextMessage(@NonNull WebSocketSession session, @NonNull TextMessage message) throws Exception {
        try {
            ChatMessage chatMessage = mapper.readValue(message.getPayload(), ChatMessage.class);

            switch (chatMessage.getType()) {
                case "join_room":
                    joinRoom(session, chatMessage.getRoomId());
                    break;
                case "leave_room":
                    leaveRoom(session, chatMessage.getRoomId());
                    break;
                case "send_message":
                    broadcastToRoom(chatMessage.getRoomId(),
                            new ChatPayload("new_message", "Tin nhắn mới", chatMessage));
                    break;
                case "typing":
                    broadcastToRoom(chatMessage.getRoomId(),
                            new ChatPayload("user_typing", "Đang nhập...", chatMessage));
                    break;
            }
        } catch (Exception e) {
            log.error("Error handling chat message", e);
            sendJson(session, new ChatPayload("error", "Lỗi xử lý tin nhắn", null));
        }
    }

    @Override
    public void afterConnectionClosed(@NonNull WebSocketSession session, @NonNull CloseStatus status) throws Exception {
        // Remove from all rooms
        roomSessions.values().forEach(sessions -> sessions.remove(session));
        log.debug("Chat WebSocket closed: {} ({})", session.getId(), status.getCode());
    }

    private void joinRoom(WebSocketSession session, Long roomId) {
        roomSessions.computeIfAbsent(roomId, k -> Collections.newSetFromMap(new ConcurrentHashMap<>()))
                .add(session);
        log.debug("Session {} joined room {}", session.getId(), roomId);
    }

    private void leaveRoom(WebSocketSession session, Long roomId) {
        Set<WebSocketSession> sessions = roomSessions.get(roomId);
        if (sessions != null) {
            sessions.remove(session);
        }
        log.debug("Session {} left room {}", session.getId(), roomId);
    }

    public void broadcastToRoom(Long roomId, Object payload) {
        Set<WebSocketSession> sessions = roomSessions.get(roomId);
        if (sessions != null) {
            sessions.forEach(s -> {
                try {
                    sendJson(s, payload);
                } catch (IOException e) {
                    log.warn("Failed to send to session {} in room {}", s.getId(), roomId, e);
                }
            });
        }
    }

    private void sendJson(WebSocketSession session, Object payload) throws IOException {
        if (!session.isOpen()) return;
        String json = mapper.writeValueAsString(payload);
        session.sendMessage(new TextMessage(json));
    }

    // DTOs for WebSocket communication
    public static class ChatPayload {
        private String type;
        private String message;
        private Object data;

        public ChatPayload() {}
        public ChatPayload(String type, String message, Object data) {
            this.type = type;
            this.message = message;
            this.data = data;
        }

        // Getters and Setters
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public Object getData() { return data; }
        public void setData(Object data) { this.data = data; }
    }

    public static class ChatMessage {
        private String type;
        private Long roomId;
        private Long senderId;
        private String content;
        private String messageType;

        // Getters and Setters
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public Long getRoomId() { return roomId; }
        public void setRoomId(Long roomId) { this.roomId = roomId; }
        public Long getSenderId() { return senderId; }
        public void setSenderId(Long senderId) { this.senderId = senderId; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        public String getMessageType() { return messageType; }
        public void setMessageType(String messageType) { this.messageType = messageType; }
    }
}