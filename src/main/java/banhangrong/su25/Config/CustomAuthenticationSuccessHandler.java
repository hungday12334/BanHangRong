package banhangrong.su25.Config;

import banhangrong.su25.Entity.Users;
import banhangrong.su25.Repository.UsersRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;

@Component
public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    @Autowired
    private UsersRepository usersRepository;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, 
                                      HttpServletResponse response, 
                                      Authentication authentication) throws IOException, ServletException {
        
        // Lấy thông tin user từ Authentication object
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String username = userDetails.getUsername();
        
        // Cập nhật last login time
        Users user = usersRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
        
        user.setLastLogin(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        usersRepository.save(user);
        
        // ⭐ IMPORTANT: Lưu user object vào session để controller có thể access
        request.getSession().setAttribute("user", user);
        request.getSession().setAttribute("userId", user.getUserId());
        request.getSession().setAttribute("userType", user.getUserType());
        request.getSession().setAttribute("username", user.getUsername());

        // Set session timeout to 8 hours
        request.getSession().setMaxInactiveInterval(28800);

        // Debug logging
        System.out.println("=== Authentication Success ===");
        System.out.println("Username: " + username);
        System.out.println("User ID: " + user.getUserId());
        System.out.println("User Type: " + user.getUserType());
        System.out.println("Authorities: " + authentication.getAuthorities());
        System.out.println("Session ID: " + request.getSession().getId());
        System.out.println("Session MaxInactiveInterval: " + request.getSession().getMaxInactiveInterval() + " seconds");
        System.out.println("Remote Address: " + request.getRemoteAddr());
        System.out.println("================================");
        
        // Redirect based on user type
        String userType = user.getUserType();
        if ("ADMIN".equalsIgnoreCase(userType) || "admin".equals(userType)) {
            response.sendRedirect("/admin/dashboard");
        } else if ("SELLER".equalsIgnoreCase(userType) || "seller".equals(userType)) {
            response.sendRedirect("/seller/dashboard");
        } else if ("CUSTOMER".equalsIgnoreCase(userType) || "customer".equals(userType)) {
            // Customer phải verify email mới vào được dashboard
            if (Boolean.TRUE.equals(user.getIsEmailVerified())) {
                response.sendRedirect("/customer/dashboard");
            } else {
                response.sendRedirect("/verify-email-required");
            }
        } else {
            response.sendRedirect("/customer/dashboard");
        }
    }
}
