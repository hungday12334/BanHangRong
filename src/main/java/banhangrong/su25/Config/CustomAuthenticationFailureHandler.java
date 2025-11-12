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
        // Check if the exception message indicates account is deactivated
        String exceptionMessage = exception.getMessage();
        if (exceptionMessage != null && exceptionMessage.contains("is not active")) {
            // Account is deactivated
            response.sendRedirect("/login?error=account_deactivated");
        } else {
            // Other authentication errors (invalid credentials, etc.)
            response.sendRedirect("/login?error=true");
        }
    }
}
