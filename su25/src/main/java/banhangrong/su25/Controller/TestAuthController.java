package banhangrong.su25.Controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/test-auth")
public class TestAuthController {

    @GetMapping("/current-user")
    public Object getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        
        if (auth == null || !auth.isAuthenticated()) {
            return "No user authenticated";
        }
        
        return auth.getPrincipal();
    }
    
    @GetMapping("/auth-details")
    public Object getAuthDetails() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        
        if (auth == null) {
            return "No authentication found";
        }
        
        return new Object() {
            public String name = auth.getName();
            public boolean authenticated = auth.isAuthenticated();
            public Object authorities = auth.getAuthorities();
            public Object principal = auth.getPrincipal();
            public Object credentials = auth.getCredentials();
            public Object details = auth.getDetails();
        };
    }
}
