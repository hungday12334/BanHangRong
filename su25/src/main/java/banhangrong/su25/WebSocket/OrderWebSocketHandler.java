package banhangrong.su25.WebSocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
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
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        sessions.add(session);
        log.debug("WebSocket connected: {}", session.getId());
        // Send a hello ping
        sendJson(session, new Payload("welcome", "Kết nối realtime thành công", null));
    }

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        // Echo minimal or ignore (protocol is server -> client only now)
        log.debug("Ignored client message from {}: {}", session.getId(), message.getPayload());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        sessions.remove(session);
        log.debug("WebSocket closed: {} ({} {})", session.getId(), status.getCode(), status.getReason());
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.warn("Transport error for session {}", session.getId(), exception);
    }

    /** Broadcast a new order event to all sessions. */
    public void broadcastNewOrder(Long orderId, Double amount) {
        Payload p = new Payload("new-order", "Có đơn hàng mới!", new OrderData(orderId, amount));
        broadcast(p);
    }

    private void broadcast(Object payload) {
        sessions.forEach(s -> {
            try {
                sendJson(s, payload);
            } catch (IOException e) {
                log.warn("Failed to send to session {}", s.getId(), e);
            }
        });
    }

    private void sendJson(WebSocketSession session, Object payload) throws IOException {
        if (!session.isOpen()) return;
        String json = mapper.writeValueAsString(payload);
        session.sendMessage(new TextMessage(json));
    }

    // Simple DTOs
    public record Payload(String type, String message, OrderData data) { }
    public record OrderData(Long orderId, Double totalAmount) { }
}
