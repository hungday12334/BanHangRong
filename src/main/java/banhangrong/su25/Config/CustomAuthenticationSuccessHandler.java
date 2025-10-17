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
        
        // SecurityContext đã được tự động set bởi Spring Security
        // Authentication object chứa:
        // - Principal: UserDetails object
        // - Authorities: List<GrantedAuthority>
        // - Credentials: password (sẽ bị clear)
        // - Details: WebAuthenticationDetails (IP, session ID)
        // - Authenticated: true
        
        // Có thể thêm custom logic ở đây
        System.out.println("=== Authentication Success ===");
        System.out.println("Username: " + username);
        System.out.println("Authorities: " + authentication.getAuthorities());
        System.out.println("Session ID: " + request.getSession().getId());
        System.out.println("Remote Address: " + request.getRemoteAddr());
        System.out.println("================================");
        
        // Redirect based on user type
        if (user.getUserType().equals("ADMIN")) {
            response.sendRedirect("/admin/dashboard");
        } else if (user.getUserType().equals("SELLER")) {
            response.sendRedirect("/seller/dashboard");
        } else if (user.getUserType().equals("CUSTOMER")) {
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
