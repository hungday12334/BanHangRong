package banhangrong.su25.WebSocket;

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
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles WebSocket connections for order notifications.
 */
@Component
public class OrderWebSocketHandler extends TextWebSocketHandler {
    private static final Logger log = LoggerFactory.getLogger(OrderWebSocketHandler.class);
    private final Set<WebSocketSession> sessions = Collections.newSetFromMap(new ConcurrentHashMap<>());

    @Override
    public void afterConnectionEstablished(@NonNull WebSocketSession session) throws Exception {
        sessions.add(session);
        log.debug("WebSocket connected: {}", session.getId());
        // Send a hello ping (plain JSON text)
        sendText(session, asJson(new Payload("welcome", "Hệ thống sẵn sàng", null)));
    }

    @Override
    public void handleTextMessage(@NonNull WebSocketSession session, @NonNull TextMessage message) throws Exception {
        // Echo minimal or ignore (protocol is server -> client only now)
        log.debug("Ignored client message from {}: {}", session.getId(), message.getPayload());
    }

    @Override
    public void afterConnectionClosed(@NonNull WebSocketSession session, @NonNull CloseStatus status) throws Exception {
        sessions.remove(session);
        log.debug("WebSocket closed: {} ({} {})", session.getId(), status.getCode(), status.getReason());
    }

    @Override
    public void handleTransportError(@NonNull WebSocketSession session, @NonNull Throwable exception) throws Exception {
        log.warn("Transport error for session {}", session.getId(), exception);
    }

    /** Broadcast a new order event to all sessions. */
    public void broadcastNewOrder(Long orderId, Double amount) {
        Payload p = new Payload("new-order", "Có đơn hàng mới!", new OrderData(orderId, amount));
        broadcast(asJson(p));
    }

    private void broadcast(String textPayload) {
        sessions.forEach(s -> {
            try {
                sendText(s, textPayload);
            } catch (IOException e) {
                log.warn("Failed to send to session {}", s.getId(), e);
            }
        });
    }

    private void sendText(WebSocketSession session, String text) throws IOException {
        if (!session.isOpen()) return;
        session.sendMessage(new TextMessage(text));
    }

    private String asJson(Payload payload) {
        StringBuilder sb = new StringBuilder();
        sb.append('{')
          .append("\"type\":\"").append(escape(payload.type())).append("\",")
          .append("\"message\":\"").append(escape(payload.message())).append("\",");
        if (payload.data() != null) {
            sb.append("\"data\":{")
              .append("\"orderId\":").append(payload.data().orderId()).append(',')
              .append("\"totalAmount\":").append(payload.data().totalAmount())
              .append('}');
        } else {
            sb.append("\"data\":null");
        }
        sb.append('}');
        return sb.toString();
    }

    private String escape(String s) {
        return s == null ? "" : s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    // Simple DTOs
    public record Payload(String type, String message, OrderData data) { }
    public record OrderData(Long orderId, Double totalAmount) { }
}
