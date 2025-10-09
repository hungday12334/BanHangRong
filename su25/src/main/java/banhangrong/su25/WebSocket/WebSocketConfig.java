package banhangrong.su25.WebSocket;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.support.HttpSessionHandshakeInterceptor;
import org.springframework.lang.NonNull;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final OrderWebSocketHandler orderWebSocketHandler;
    private final ChatWebSocketHandler chatWebSocketHandler;

    public WebSocketConfig(OrderWebSocketHandler orderWebSocketHandler,
                           ChatWebSocketHandler chatWebSocketHandler) {
        this.orderWebSocketHandler = orderWebSocketHandler;
        this.chatWebSocketHandler = chatWebSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(@NonNull WebSocketHandlerRegistry registry) {
        registry.addHandler(orderWebSocketHandler, "/ws/orders")
                .addInterceptors(new HttpSessionHandshakeInterceptor())
                .setAllowedOrigins("*");

        registry.addHandler(chatWebSocketHandler, "/ws/chat")
                .addInterceptors(new HttpSessionHandshakeInterceptor())
                .setAllowedOrigins("*");
    }
}