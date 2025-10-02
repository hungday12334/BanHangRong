package banhangrong.su25.Controller;

import banhangrong.su25.DTO.AuthResponse;
import banhangrong.su25.DTO.LoginRequest;
import banhangrong.su25.DTO.RegisterRequest;
import banhangrong.su25.Service.AuthService;
import banhangrong.su25.Service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    @Autowired
    private AuthService authService;

    @Autowired
    private EmailService emailService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        try {
            AuthResponse response = authService.login(loginRequest);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest registerRequest) {
        try {
            AuthResponse response = authService.register(registerRequest);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestParam String email) {
        try {
            authService.forgotPassword(email);
            return ResponseEntity.ok(Map.of("message", "Email đặt lại mật khẩu đã được gửi đến " + email));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> request) {
        try {
            String token = request.get("token");
            String newPassword = request.get("newPassword");
            String confirmPassword = request.get("confirmPassword");
            
            authService.resetPassword(token, newPassword, confirmPassword);
            return ResponseEntity.ok("Đặt lại mật khẩu thành công");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/test-email")
    public ResponseEntity<?> testEmail(@RequestParam String email) {
        try {
            emailService.sendPasswordResetEmail(email, "test-token-123");
            return ResponseEntity.ok(Map.of("message", "Email test đã được gửi thành công!"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Lỗi gửi email: " + e.getMessage()));
        }
    }

    @GetMapping("/user-info")
    public ResponseEntity<?> getUserInfo(@RequestParam String token) {
        try {
            Map<String, Object> userInfo = authService.getUserInfoFromToken(token);
            return ResponseEntity.ok(userInfo);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
