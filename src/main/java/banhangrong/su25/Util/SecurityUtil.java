package banhangrong.su25.Util;

import banhangrong.su25.Entity.Users;
import banhangrong.su25.Repository.UsersRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class SecurityUtil {

    @Autowired
    private UsersRepository usersRepository;

    /**
     * Lấy thông tin user hiện tại từ SecurityContext
     */
    public Optional<Users> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated() || 
            "anonymousUser".equals(authentication.getPrincipal())) {
            return Optional.empty();
        }

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String username = userDetails.getUsername();
        
        return usersRepository.findByUsername(username);
    }

    /**
     * Lấy username hiện tại
     */
    public Optional<String> getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated() || 
            "anonymousUser".equals(authentication.getPrincipal())) {
            return Optional.empty();
        }

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        return Optional.of(userDetails.getUsername());
    }

    /**
     * Kiểm tra user hiện tại có role không
     */
    public boolean hasRole(String role) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        return authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_" + role.toUpperCase()));
    }

    /**
     * Kiểm tra user hiện tại có phải admin không
     */
    public boolean isAdmin() {
        return hasRole("ADMIN");
    }

    /**
     * Kiểm tra user hiện tại có phải seller không
     */
    public boolean isSeller() {
        return hasRole("SELLER");
    }

    /**
     * Lấy Authentication object hiện tại
     */
    public Optional<Authentication> getCurrentAuthentication() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated() || 
            "anonymousUser".equals(authentication.getPrincipal())) {
            return Optional.empty();
        }

        return Optional.of(authentication);
    }

    /**
     * Debug: In ra thông tin SecurityContext
     */
    public void debugSecurityContext() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        System.out.println("=== Security Context Debug ===");
        System.out.println("Authentication: " + (authentication != null ? "Present" : "Null"));
        
        if (authentication != null) {
            System.out.println("Principal: " + authentication.getPrincipal().getClass().getSimpleName());
            System.out.println("Name: " + authentication.getName());
            System.out.println("Authorities: " + authentication.getAuthorities());
            System.out.println("Authenticated: " + authentication.isAuthenticated());
            System.out.println("Credentials: " + (authentication.getCredentials() != null ? "Present" : "Null"));
            System.out.println("Details: " + authentication.getDetails());
        }
        System.out.println("================================");
    }
}
