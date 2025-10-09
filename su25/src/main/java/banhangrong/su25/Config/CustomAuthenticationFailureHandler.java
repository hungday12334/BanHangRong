package banhangrong.su25.Config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class CustomAuthenticationFailureHandler implements AuthenticationFailureHandler {

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, 
                                      HttpServletResponse response, 
                                      AuthenticationException exception) throws IOException, ServletException {
        
        // Log authentication failure
        System.out.println("=== Authentication Failure ===");
        System.out.println("Username: " + request.getParameter("username"));
        System.out.println("Remote Address: " + request.getRemoteAddr());
        System.out.println("Exception: " + exception.getMessage());
        System.out.println("================================");
        
        // Redirect back to login with error
        response.sendRedirect("/login?error=true");
    }
}
