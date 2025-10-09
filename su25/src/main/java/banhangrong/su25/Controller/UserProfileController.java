package banhangrong.su25.Controller;

import banhangrong.su25.Entity.Users;
import banhangrong.su25.Util.SecurityUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/user")
public class UserProfileController {

    @Autowired
    private SecurityUtil securityUtil;

    /**
     * Lấy thông tin user hiện tại từ SecurityContext
     */
    @GetMapping("/profile")
    public ResponseEntity<?> getCurrentUserProfile() {
        try {
            Optional<Users> currentUser = securityUtil.getCurrentUser();
            
            if (currentUser.isEmpty()) {
                return ResponseEntity.status(401).body(Map.of("error", "User not authenticated"));
            }

            Users user = currentUser.get();
            
            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("userId", user.getUserId());
            userInfo.put("username", user.getUsername());
            userInfo.put("email", user.getEmail());
            userInfo.put("userType", user.getUserType());
            userInfo.put("phoneNumber", user.getPhoneNumber());
            userInfo.put("gender", user.getGender());
            userInfo.put("birthDate", user.getBirthDate());
            userInfo.put("balance", user.getBalance());
            userInfo.put("isActive", user.getIsActive());
            userInfo.put("isEmailVerified", user.getIsEmailVerified());
            userInfo.put("lastLogin", user.getLastLogin());
            userInfo.put("createdAt", user.getCreatedAt());

            return ResponseEntity.ok(userInfo);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Debug: Hiển thị thông tin SecurityContext
     */
    @GetMapping("/debug-security")
    public ResponseEntity<?> debugSecurityContext() {
        try {
            Map<String, Object> debugInfo = new HashMap<>();
            
            // Lấy Authentication từ SecurityContext
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            
            if (authentication != null) {
                debugInfo.put("authenticationPresent", true);
                debugInfo.put("authenticationName", authentication.getName());
                debugInfo.put("authenticationClass", authentication.getClass().getSimpleName());
                debugInfo.put("authenticated", authentication.isAuthenticated());
                debugInfo.put("authorities", authentication.getAuthorities());
                debugInfo.put("credentialsPresent", authentication.getCredentials() != null);
                debugInfo.put("details", authentication.getDetails());
                
                // Principal information
                Object principal = authentication.getPrincipal();
                debugInfo.put("principalClass", principal.getClass().getSimpleName());
                
                if (principal instanceof UserDetails) {
                    UserDetails userDetails = (UserDetails) principal;
                    debugInfo.put("username", userDetails.getUsername());
                    debugInfo.put("authorities", userDetails.getAuthorities());
                    debugInfo.put("accountNonExpired", userDetails.isAccountNonExpired());
                    debugInfo.put("accountNonLocked", userDetails.isAccountNonLocked());
                    debugInfo.put("credentialsNonExpired", userDetails.isCredentialsNonExpired());
                    debugInfo.put("enabled", userDetails.isEnabled());
                }
            } else {
                debugInfo.put("authenticationPresent", false);
            }
            
            // User info từ SecurityUtil
            Optional<Users> currentUser = securityUtil.getCurrentUser();
            debugInfo.put("currentUserPresent", currentUser.isPresent());
            if (currentUser.isPresent()) {
                Users user = currentUser.get();
                debugInfo.put("userId", user.getUserId());
                debugInfo.put("username", user.getUsername());
                debugInfo.put("userType", user.getUserType());
            }
            
            return ResponseEntity.ok(debugInfo);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Kiểm tra quyền của user hiện tại
     */
    @GetMapping("/check-permissions")
    public ResponseEntity<?> checkUserPermissions() {
        try {
            Map<String, Object> permissions = new HashMap<>();
            
            permissions.put("isAdmin", securityUtil.isAdmin());
            permissions.put("isSeller", securityUtil.isSeller());
            permissions.put("hasUserRole", securityUtil.hasRole("USER"));
            permissions.put("hasSellerRole", securityUtil.hasRole("SELLER"));
            permissions.put("hasAdminRole", securityUtil.hasRole("ADMIN"));
            
            Optional<String> username = securityUtil.getCurrentUsername();
            permissions.put("currentUsername", username.orElse("Not authenticated"));
            
            return ResponseEntity.ok(permissions);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
