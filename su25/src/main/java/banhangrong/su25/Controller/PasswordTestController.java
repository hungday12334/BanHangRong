package banhangrong.su25.Controller;

import banhangrong.su25.Entity.Users;
import banhangrong.su25.Repository.UsersRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/password-test")
public class PasswordTestController {

    @Autowired
    private UsersRepository usersRepository;
    
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @GetMapping("/test-user")
    public Map<String, Object> testUser(@RequestParam String username, @RequestParam String password) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // Tìm user trong database
            Optional<Users> userOpt = usersRepository.findByUsername(username);
            if (!userOpt.isPresent()) {
                result.put("error", "User not found: " + username);
                return result;
            }
            
            Users user = userOpt.get();
            String storedPassword = user.getPassword();
            
            // Test password matching
            boolean matches = encoder.matches(password, storedPassword);
            
            result.put("username", username);
            result.put("storedPassword", storedPassword);
            result.put("inputPassword", password);
            result.put("matches", matches);
            result.put("userType", user.getUserType());
            result.put("isActive", user.getIsActive());
            result.put("isEmailVerified", user.getIsEmailVerified());
            
            // Generate new hash for comparison
            String newHash = encoder.encode(password);
            result.put("newHash", newHash);
            result.put("newHashMatches", encoder.matches(password, newHash));
            
        } catch (Exception e) {
            result.put("error", e.getMessage());
        }
        
        return result;
    }
    
    @GetMapping("/generate-hash")
    public Map<String, String> generateHash(@RequestParam String password) {
        Map<String, String> result = new HashMap<>();
        String hash = encoder.encode(password);
        result.put("password", password);
        result.put("hash", hash);
        result.put("sql", "UPDATE users SET password = '" + hash + "' WHERE username = 'testadmin';");
        return result;
    }
}
