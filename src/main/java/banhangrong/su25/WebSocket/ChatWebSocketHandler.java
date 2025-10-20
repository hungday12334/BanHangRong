package banhangrong.su25.WebSocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
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

    @Override
    public void afterConnectionEstablished(@NonNull WebSocketSession session) throws Exception {
        log.debug("Chat WebSocket connected: {}", session.getId());
        // Send welcome message (plain text to avoid JSON dependencies)
        sendText(session, "{\"type\":\"connected\",\"message\":\"Kết nối chat thành công\"}");
    }

    @Override
    public void handleTextMessage(@NonNull WebSocketSession session, @NonNull TextMessage message) throws Exception {
        try {
            // Minimal, JSON-agnostic handling: echo back and do not parse
            String payload = message.getPayload();
            log.debug("Received WS message from {}: {}", session.getId(), payload);
            // Broadcast raw payload to all sessions in all rooms (since we don't parse roomId here)
            // To keep previous semantics minimally, just send back to sender
            sendText(session, payload);
        } catch (Exception e) {
            log.error("Error handling chat message", e);
            sendText(session, "{\"type\":\"error\",\"message\":\"Lỗi xử lý tin nhắn\"}");
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

    public void broadcastToRoom(Long roomId, String textPayload) {
        Set<WebSocketSession> sessions = roomSessions.get(roomId);
        if (sessions != null) {
            sessions.forEach(s -> {
                try {
                    sendText(s, textPayload);
                } catch (IOException e) {
                    log.warn("Failed to send to session {} in room {}", s.getId(), roomId, e);
                }
            });
        }
    }

    // Overload to keep compatibility with services sending complex payloads
    public void broadcastToRoom(Long roomId, Object payload) {
        String text = (payload instanceof ChatPayload)
                ? toJson((ChatPayload) payload)
                : String.valueOf(payload);
        broadcastToRoom(roomId, text);
    }

    private void sendText(WebSocketSession session, String text) throws IOException {
        if (!session.isOpen()) return;
        session.sendMessage(new TextMessage(text));
    }

    private String toJson(ChatPayload payload) {
        StringBuilder sb = new StringBuilder();
        sb.append('{')
          .append("\"type\":\"").append(escape(payload.getType())).append("\",")
          .append("\"message\":\"").append(escape(payload.getMessage())).append("\",")
          .append("\"data\":");
        if (payload.getData() == null) {
            sb.append("null");
        } else {
            sb.append('"').append(escape(String.valueOf(payload.getData()))).append('"');
        }
        sb.append('}');
        return sb.toString();
    }

    private String escape(String s) {
        return s == null ? "" : s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    // DTOs kept for future use if JSON mapping is restored
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
