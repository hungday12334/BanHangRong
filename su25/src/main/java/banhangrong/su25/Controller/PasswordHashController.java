package banhangrong.su25.Controller;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/password-hash")
public class PasswordHashController {

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @GetMapping("/generate")
    public Map<String, String> generateHash(@RequestParam String password) {
        String hash = encoder.encode(password);
        
        Map<String, String> response = new HashMap<>();
        response.put("password", password);
        response.put("hash", hash);
        response.put("sql", "UPDATE users SET password = '" + hash + "' WHERE username = 'seller';");
        
        return response;
    }

    @GetMapping("/verify")
    public Map<String, Object> verifyHash(
            @RequestParam String password,
            @RequestParam String hash) {
        boolean matches = encoder.matches(password, hash);
        
        Map<String, Object> response = new HashMap<>();
        response.put("password", password);
        response.put("hash", hash);
        response.put("matches", matches);
        
        return response;
    }
}

