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
        
        // Redirect based on user type
        String userType = user.getUserType();
        String redirectUrl = null;
        
        if (userType == null || userType.trim().isEmpty()) {
            userType = "CUSTOMER";
        } else {
            userType = userType.trim().toUpperCase();
        }
        
        if ("ADMIN".equals(userType)) {
            redirectUrl = "/admin/dashboard";
        } else if ("SELLER".equals(userType)) {
            redirectUrl = "/seller/dashboard";
        } else if ("CUSTOMER".equals(userType) || "USER".equals(userType)) {
            if (Boolean.TRUE.equals(user.getIsEmailVerified())) {
                redirectUrl = "/customer/dashboard";
            } else {
                redirectUrl = "/verify-email-required";
            }
        } else {
            if (Boolean.TRUE.equals(user.getIsEmailVerified())) {
                redirectUrl = "/customer/dashboard";
            } else {
                redirectUrl = "/verify-email-required";
            }
        }
        
        response.sendRedirect(redirectUrl);
    }
}
