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

import jakarta.servlet.http.HttpServletRequest;
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
    public ResponseEntity<?> verifyCode(@RequestBody Map<String, String> request, HttpServletRequest httpRequest) {
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

        // Verify email and activate account
        user.setIsEmailVerified(true);
        user.setIsActive(true); // ✅ Activate account - convert from temporary to permanent
        usersRepository.save(user);

        token.setIsUsed(true);
        emailVerificationTokenRepository.save(token);

        // Send welcome email
        sendWelcomeEmail(user);

        // Invalidate current session - user must login again
        SecurityContextHolder.clearContext();
        if (httpRequest.getSession(false) != null) {
            httpRequest.getSession().invalidate();
        }

        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "Email verified successfully! Your account is now active. Please login to continue.",
            "redirectUrl", "/login?verified=true"
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

        // Send beautiful HTML email
        try {
            String emailContent = """
                    <!DOCTYPE html>
                    <html lang="en">
                    <head>
                        <meta charset="UTF-8">
                        <meta name="viewport" content="width=device-width, initial-scale=1.0">
                        <title>Email Verification</title>
                    </head>
                    <body style="margin: 0; padding: 0; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif; background-color: #f5f7fa;">
                        <table role="presentation" style="width: 100%%; border-collapse: collapse; background-color: #f5f7fa;">
                            <tr>
                                <td style="padding: 40px 20px;">
                                    <table role="presentation" style="max-width: 600px; margin: 0 auto; background-color: #ffffff; border-radius: 16px; box-shadow: 0 4px 24px rgba(0,0,0,0.08); overflow: hidden;">
                                        <!-- Header -->
                                        <tr>
                                            <td style="background: linear-gradient(135deg, #0ea5e9 0%%, #0284c7 100%%); padding: 40px 30px; text-align: center;">
                                                <div style="background: rgba(255,255,255,0.2); width: 80px; height: 80px; border-radius: 50%%; margin: 0 auto 20px; display: flex; align-items: center; justify-content: center;">
                                                    <span style="font-size: 40px;">📧</span>
                                                </div>
                                                <h1 style="color: #ffffff; margin: 0 0 10px; font-size: 28px; font-weight: 700; letter-spacing: -0.5px;">Email Verification</h1>
                                                <p style="color: rgba(255,255,255,0.9); margin: 0; font-size: 15px;">Your new verification code is ready</p>
                                            </td>
                                        </tr>
                                        
                                        <!-- Content -->
                                        <tr>
                                            <td style="padding: 40px 30px;">
                                                <p style="color: #1f2937; font-size: 16px; line-height: 1.6; margin: 0 0 20px;">Hello <strong>%s</strong>,</p>
                                                
                                                <p style="color: #4b5563; font-size: 15px; line-height: 1.7; margin: 0 0 25px;">
                                                    Here is your new email verification code. Please use it to verify your email address. This code will expire in <strong style="color: #dc2626;">%d minutes</strong>.
                                                </p>
                                                
                                                <!-- Verification Code Box -->
                                                <div style="background: linear-gradient(135deg, #e0f2fe 0%%, #bae6fd 100%%); border: 2px dashed #0ea5e9; border-radius: 16px; padding: 30px; text-align: center; margin: 30px 0;">
                                                    <p style="color: #0c4a6e; font-size: 13px; margin: 0 0 12px; font-weight: 600; text-transform: uppercase; letter-spacing: 1px;">Your Verification Code</p>
                                                    <div style="background: #ffffff; border-radius: 12px; padding: 20px; display: inline-block; box-shadow: 0 4px 12px rgba(14, 165, 233, 0.15);">
                                                        <p style="font-size: 36px; font-weight: 700; color: #0ea5e9; margin: 0; letter-spacing: 8px; font-family: 'Courier New', monospace;">%s</p>
                                                    </div>
                                                </div>
                                                
                                                <!-- Link -->
                                                <table role="presentation" style="margin: 25px 0;">
                                                    <tr>
                                                        <td style="text-align: center;">
                                                            <a href="http://localhost:8080/verify-email-required" 
                                                               style="display: inline-block; background: linear-gradient(135deg, #0ea5e9 0%%, #0284c7 100%%); 
                                                                      color: #ffffff; text-decoration: none; padding: 14px 32px; 
                                                                      border-radius: 10px; font-weight: 600; font-size: 15px; 
                                                                      box-shadow: 0 4px 16px rgba(14, 165, 233, 0.3);">
                                                                ✓ Verify Email Now
                                                            </a>
                                                        </td>
                                                    </tr>
                                                </table>
                                                
                                                <!-- Security Notice -->
                                                <div style="background: #fef3c7; border: 1px solid #fbbf24; border-radius: 10px; padding: 16px; margin: 25px 0;">
                                                    <p style="color: #92400e; font-size: 13px; margin: 0; line-height: 1.6;">
                                                        <strong>⚠️ Security:</strong> If you didn't request this code, please ignore this email.
                                                    </p>
                                                </div>
                                            </td>
                                        </tr>
                                        
                                        <!-- Footer -->
                                        <tr>
                                            <td style="background-color: #f9fafb; padding: 30px; text-align: center; border-top: 1px solid #e5e7eb;">
                                                <p style="color: #9ca3af; font-size: 14px; margin: 0 0 8px; font-weight: 500;">
                                                    <strong style="color: #0ea5e9;">BanHangRong</strong> - Your Digital Marketplace
                                                </p>
                                                <p style="color: #d1d5db; font-size: 12px; margin: 0;">
                                                    © 2025 BanHangRong. All rights reserved.
                                                </p>
                                            </td>
                                        </tr>
                                    </table>
                                </td>
                            </tr>
                        </table>
                    </body>
                    </html>
                    """.formatted(user.getUsername(), TOKEN_EXPIRY_MINUTES, verificationCode);

            emailService.sendEmail(new Email(
                user.getEmail(),
                "📧 New Verification Code - BanHangRong",
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

    /**
     * Send welcome email after successful email verification
     */
    private void sendWelcomeEmail(Users user) {
        try {
            String emailContent = """
                    <!DOCTYPE html>
                    <html lang="en">
                    <head>
                        <meta charset="UTF-8">
                        <meta name="viewport" content="width=device-width, initial-scale=1.0">
                        <title>Welcome to BanHangRong</title>
                    </head>
                    <body style="margin: 0; padding: 0; font-family: 'Arial', 'Helvetica', sans-serif; background-color: #f4f7fa;">
                        <table role="presentation" style="width: 100%; border-collapse: collapse;">
                            <tr>
                                <td style="padding: 40px 20px;">
                                    <table role="presentation" style="max-width: 600px; margin: 0 auto; background-color: #ffffff; border-radius: 16px; box-shadow: 0 4px 20px rgba(0,0,0,0.08); overflow: hidden;">
                                        
                                        <!-- Header with gradient -->
                                        <tr>
                                            <td style="background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); padding: 40px 30px; text-align: center;">
                                                <h1 style="color: #ffffff; margin: 0 0 10px; font-size: 32px; font-weight: bold; letter-spacing: -0.5px;">
                                                    🎉 Welcome to BanHangRong!
                                                </h1>
                                                <p style="color: #e0e7ff; margin: 0; font-size: 16px;">
                                                    Your account has been successfully verified
                                                </p>
                                            </td>
                                        </tr>
                                        
                                        <!-- Main content -->
                                        <tr>
                                            <td style="padding: 40px 30px;">
                                                <p style="color: #2d3748; font-size: 18px; line-height: 1.6; margin: 0 0 20px;">
                                                    Hello <strong style="color: #667eea;">%s</strong>,
                                                </p>
                                                
                                                <p style="color: #4a5568; font-size: 16px; line-height: 1.8; margin: 0 0 25px;">
                                                    Congratulations! Your email has been successfully verified. You now have full access to all the amazing features BanHangRong has to offer.
                                                </p>
                                                
                                                <!-- Features box -->
                                                <table role="presentation" style="width: 100%; border-collapse: collapse; margin: 30px 0;">
                                                    <tr>
                                                        <td style="background: linear-gradient(135deg, #f0f4ff 0%, #e0e7ff 100%); border-radius: 12px; padding: 25px;">
                                                            <h3 style="color: #667eea; margin: 0 0 15px; font-size: 18px; font-weight: bold;">
                                                                ✨ What you can do now:
                                                            </h3>
                                                            <table role="presentation" style="width: 100%%;">
                                                                <tr>
                                                                    <td style="padding: 8px 0;">
                                                                        <span style="color: #10b981; font-size: 18px; margin-right: 10px;">✓</span>
                                                                        <span style="color: #4a5568; font-size: 15px;">Browse thousands of digital products</span>
                                                                    </td>
                                                                </tr>
                                                                <tr>
                                                                    <td style="padding: 8px 0;">
                                                                        <span style="color: #10b981; font-size: 18px; margin-right: 10px;">✓</span>
                                                                        <span style="color: #4a5568; font-size: 15px;">Purchase software licenses and game keys</span>
                                                                    </td>
                                                                </tr>
                                                                <tr>
                                                                    <td style="padding: 8px 0;">
                                                                        <span style="color: #10b981; font-size: 18px; margin-right: 10px;">✓</span>
                                                                        <span style="color: #4a5568; font-size: 15px;">Become a seller and start your business</span>
                                                                    </td>
                                                                </tr>
                                                                <tr>
                                                                    <td style="padding: 8px 0;">
                                                                        <span style="color: #10b981; font-size: 18px; margin-right: 10px;">✓</span>
                                                                        <span style="color: #4a5568; font-size: 15px;">Track your orders and manage your account</span>
                                                                    </td>
                                                                </tr>
                                                            </table>
                                                        </td>
                                                    </tr>
                                                </table>
                                                
                                                <!-- CTA Button -->
                                                <table role="presentation" style="margin: 30px 0;">
                                                    <tr>
                                                        <td style="text-align: center;">
                                                            <a href="http://localhost:8080/customer/dashboard" 
                                                               style="display: inline-block; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); 
                                                                      color: #ffffff; text-decoration: none; padding: 16px 40px; 
                                                                      border-radius: 10px; font-weight: bold; font-size: 16px; 
                                                                      box-shadow: 0 4px 15px rgba(102, 126, 234, 0.4);">
                                                                🚀 Start Exploring Now
                                                            </a>
                                                        </td>
                                                    </tr>
                                                </table>
                                                
                                                <p style="color: #718096; font-size: 14px; line-height: 1.6; margin: 25px 0 0; padding-top: 20px; border-top: 1px solid #e2e8f0;">
                                                    If you have any questions or need assistance, our support team is here to help. Feel free to reach out anytime!
                                                </p>
                                            </td>
                                        </tr>
                                        
                                        <!-- Footer -->
                                        <tr>
                                            <td style="background-color: #f7fafc; padding: 30px; text-align: center; border-top: 1px solid #e2e8f0;">
                                                <p style="color: #a0aec0; font-size: 14px; margin: 0 0 10px;">
                                                    <strong style="color: #667eea;">BanHangRong</strong> - Your Digital Marketplace
                                                </p>
                                                <p style="color: #cbd5e0; font-size: 12px; margin: 0;">
                                                    © 2025 BanHangRong. All rights reserved.
                                                </p>
                                                <p style="color: #cbd5e0; font-size: 12px; margin: 10px 0 0;">
                                                    <a href="http://localhost:8080" style="color: #667eea; text-decoration: none;">Visit Website</a> | 
                                                    <a href="mailto:support@banhangrong.com" style="color: #667eea; text-decoration: none;">Contact Support</a>
                                                </p>
                                            </td>
                                        </tr>
                                    </table>
                                </td>
                            </tr>
                        </table>
                    </body>
                    </html>
                    """.formatted(user.getUsername());

            emailService.sendEmail(new Email(
                user.getEmail(),
                "🎉 Welcome to BanHangRong - Your Account is Verified!",
                emailContent
            ));
        } catch (Exception e) {
            // Log error but don't fail the verification process
            System.err.println("Failed to send welcome email to " + user.getEmail() + ": " + e.getMessage());
        }
    }
}
