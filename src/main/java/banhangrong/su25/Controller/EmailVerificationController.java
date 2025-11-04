package banhangrong.su25.Controller;

import banhangrong.su25.Entity.EmailVerificationToken;
import banhangrong.su25.Entity.Users;
import banhangrong.su25.Repository.EmailVerificationTokenRepository;
import banhangrong.su25.Repository.UsersRepository;
import banhangrong.su25.email.Email;
import banhangrong.su25.email.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@RestController
@RequestMapping("/api/email-verification")
public class EmailVerificationController {

    @Autowired
    private UsersRepository usersRepository;

    @Autowired
    private EmailVerificationTokenRepository emailVerificationTokenRepository;

    @Autowired
    private EmailService emailService;

    private static final int TOKEN_EXPIRY_MINUTES = 2;
    private static final int RESEND_COOLDOWN_SECONDS = 60;

    @PostMapping("/verify-code")
    public ResponseEntity<?> verifyCode(@RequestBody Map<String, String> request) {
        String code = request.get("code");

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "Please login first"));
        }

        Users user = usersRepository.findByUsername(auth.getName()).orElse(null);
        if (user == null) {
            return ResponseEntity.status(404).body(Map.of("success", false, "message", "User not found"));
        }

        if (Boolean.TRUE.equals(user.getIsEmailVerified())) {
            return ResponseEntity.ok(Map.of("success", false, "message", "Email is already verified"));
        }

        // Find valid token
        var tokenOpt = emailVerificationTokenRepository.findByUserIdAndIsUsedFalse(user.getUserId());
        if (tokenOpt.isEmpty()) {
            return ResponseEntity.ok(Map.of("success", false, "message", "No verification code found. Please request a new code."));
        }

        EmailVerificationToken token = tokenOpt.get();

        // Check if expired
        if (token.getExpiresAt().isBefore(LocalDateTime.now())) {
            return ResponseEntity.ok(Map.of("success", false, "message", "Verification code has expired. Please request a new code."));
        }

        // Check if code matches
        if (!token.getToken().equals(code.trim())) {
            return ResponseEntity.ok(Map.of("success", false, "message", "Invalid verification code. Please try again."));
        }

        // Verify email
        user.setIsEmailVerified(true);
        usersRepository.save(user);

        token.setIsUsed(true);
        emailVerificationTokenRepository.save(token);

        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "Email verified successfully!",
            "redirectUrl", "/customer/dashboard"
        ));
    }

    @PostMapping("/resend-code")
    public ResponseEntity<?> resendCode() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "Please login first"));
        }

        Users user = usersRepository.findByUsername(auth.getName()).orElse(null);
        if (user == null) {
            return ResponseEntity.status(404).body(Map.of("success", false, "message", "User not found"));
        }

        if (Boolean.TRUE.equals(user.getIsEmailVerified())) {
            return ResponseEntity.ok(Map.of("success", false, "message", "Email is already verified"));
        }

        // Check cooldown
        var existingTokenOpt = emailVerificationTokenRepository.findByUserIdAndIsUsedFalse(user.getUserId());
        if (existingTokenOpt.isPresent()) {
            EmailVerificationToken existingToken = existingTokenOpt.get();
            long secondsSinceCreation = Duration.between(existingToken.getCreatedAt(), LocalDateTime.now()).getSeconds();

            if (secondsSinceCreation < RESEND_COOLDOWN_SECONDS) {
                long remainingSeconds = RESEND_COOLDOWN_SECONDS - secondsSinceCreation;
                return ResponseEntity.ok(Map.of(
                    "success", false,
                    "message", "Please wait " + remainingSeconds + " seconds before requesting a new code.",
                    "remainingSeconds", remainingSeconds
                ));
            }

            // Delete old token
            emailVerificationTokenRepository.delete(existingToken);
        }

        // Create new token
        String verificationCode = String.format("%06d", new Random().nextInt(1_000_000));
        EmailVerificationToken newToken = new EmailVerificationToken();
        newToken.setUserId(user.getUserId());
        newToken.setToken(verificationCode);
        newToken.setExpiresAt(LocalDateTime.now().plusMinutes(TOKEN_EXPIRY_MINUTES));
        newToken.setIsUsed(false);
        newToken.setCreatedAt(LocalDateTime.now());
        emailVerificationTokenRepository.save(newToken);

        // Send email
        try {
            String emailContent = "Hello " + user.getUsername() + ",\n\n" +
                    "Your new email verification code is: " + verificationCode + "\n" +
                    "This code is valid for " + TOKEN_EXPIRY_MINUTES + " minutes only.\n\n" +
                    "Please enter this code at: http://localhost:8080/verify-email-required\n\n" +
                    "If you didn't request this, please ignore this email.";

            emailService.sendEmail(new Email(
                user.getEmail(),
                "BanHangRong - New Verification Code",
                emailContent
            ));

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "A new verification code has been sent to your email.",
                "expiresInMinutes", TOKEN_EXPIRY_MINUTES
            ));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                "success", false,
                "message", "Failed to send email. Please try again later."
            ));
        }
    }

    @GetMapping("/status")
    public ResponseEntity<?> getStatus() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(401).body(Map.of("authenticated", false));
        }

        Users user = usersRepository.findByUsername(auth.getName()).orElse(null);
        if (user == null) {
            return ResponseEntity.status(404).body(Map.of("authenticated", false));
        }

        Map<String, Object> response = new HashMap<>();
        response.put("authenticated", true);
        response.put("email", user.getEmail());
        response.put("username", user.getUsername());
        response.put("isEmailVerified", user.getIsEmailVerified());

        // Check if there's an active token
        var tokenOpt = emailVerificationTokenRepository.findByUserIdAndIsUsedFalse(user.getUserId());
        if (tokenOpt.isPresent()) {
            EmailVerificationToken token = tokenOpt.get();
            long secondsUntilExpiry = Duration.between(LocalDateTime.now(), token.getExpiresAt()).getSeconds();
            long secondsSinceCreation = Duration.between(token.getCreatedAt(), LocalDateTime.now()).getSeconds();
            long remainingCooldown = Math.max(0, RESEND_COOLDOWN_SECONDS - secondsSinceCreation);

            response.put("hasActiveToken", true);
            response.put("expiresInSeconds", Math.max(0, secondsUntilExpiry));
            response.put("canResend", remainingCooldown == 0);
            response.put("resendCooldownSeconds", remainingCooldown);
        } else {
            response.put("hasActiveToken", false);
            response.put("canResend", true);
            response.put("resendCooldownSeconds", 0);
        }

        return ResponseEntity.ok(response);
    }
}

