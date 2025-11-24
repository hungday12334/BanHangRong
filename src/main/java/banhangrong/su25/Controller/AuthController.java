package banhangrong.su25.Controller;

import banhangrong.su25.DTO.AuthResponse;
import banhangrong.su25.DTO.ForgotPasswordRequest;
import banhangrong.su25.DTO.LoginRequest;
import banhangrong.su25.DTO.RegisterRequest;
import banhangrong.su25.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    @Autowired
    private AuthService authService;


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
            return ResponseEntity.ok(response); // http 200 ok + Json responnse
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); // http 400 + Json error message
        }
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        try {
            // Log để debug
            System.out.println("=== FORGOT PASSWORD REQUEST ===");
            System.out.println("Request: " + request);
            
            // Validate request
            if (request == null || request.getEmail() == null || request.getEmail().trim().isEmpty()) {
                System.err.println("Email is null or empty");
                return ResponseEntity.badRequest().body(Map.of("error", "Email is required"));
            }
            
            String emailTrimmed = request.getEmail().trim();
            
            // Basic email format validation
            if (!emailTrimmed.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
                System.err.println("Invalid email format: " + emailTrimmed);
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid email format"));
            }
            
            System.out.println("Processing forgot password for: " + emailTrimmed);
            authService.forgotPassword(emailTrimmed);
            return ResponseEntity.ok(Map.of("message", "Password reset email has been sent to " + emailTrimmed));
        } catch (RuntimeException e) {
            String errorMessage = e.getMessage();
            if (errorMessage == null || errorMessage.isEmpty()) {
                errorMessage = "An error occurred. Please try again later.";
            }
            // Log error for debugging
            System.err.println("Forgot password RuntimeException: " + errorMessage);
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("error", errorMessage));
        } catch (Exception e) {
            // Log unexpected errors
            System.err.println("Unexpected error in forgot-password: " + e.getClass().getName() + " - " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("error", "An unexpected error occurred. Please try again later."));
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

    @GetMapping("/user-info")
    public ResponseEntity<?> getUserInfo(@RequestParam String token) {
        try {
            return ResponseEntity.ok(authService.getUserInfoFromToken(token));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<?> resendVerification(@RequestParam String username) {
        try {
            authService.resendVerificationEmail(username);
            return ResponseEntity.ok(Map.of("message", "Verification email has been sent to your email address"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // Exception handler để catch lỗi parse JSON
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<?> handleHttpMessageNotReadable(HttpMessageNotReadableException e) {
        System.err.println("JSON parse error: " + e.getMessage());
        e.printStackTrace();
        return ResponseEntity.badRequest().body(Map.of("error", "Invalid request format. Please send valid JSON with email field."));
    }
    
    // Exception handler để catch lỗi method argument
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> handleIllegalArgument(IllegalArgumentException e) {
        System.err.println("Illegal argument error: " + e.getMessage());
        e.printStackTrace();
        return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }
}