package banhangrong.su25.Config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Xử lý khi user không có quyền truy cập (403 Forbidden)
 */
@Component
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    @Override
    public void handle(HttpServletRequest request, 
                      HttpServletResponse response,
                      AccessDeniedException accessDeniedException) throws IOException {
        
        // Nếu cố truy cập seller dashboard mà không phải seller, redirect về customer dashboard
        if (request.getRequestURI().startsWith("/seller/") || request.getRequestURI().startsWith("/api/seller/")) {
            response.sendRedirect("/customer/dashboard?error=access_denied_seller_only");
        } else {
            // Các trường hợp khác, redirect về trang chủ
            response.sendRedirect("/customer/dashboard?error=access_denied");
        }
    }
}

