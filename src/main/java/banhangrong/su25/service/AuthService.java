package banhangrong.su25.service;

import banhangrong.su25.DTO.AuthResponse;
import banhangrong.su25.DTO.LoginRequest;
import banhangrong.su25.DTO.RegisterRequest;
import banhangrong.su25.Entity.Users;
import banhangrong.su25.Entity.PasswordResetToken;
import banhangrong.su25.Entity.EmailVerificationToken;
import banhangrong.su25.Repository.UsersRepository;
import banhangrong.su25.Repository.PasswordResetTokenRepository;
import banhangrong.su25.Repository.EmailVerificationTokenRepository;
import banhangrong.su25.Util.JwtUtil;
import banhangrong.su25.email.Email;
import banhangrong.su25.email.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class AuthService {

    @Autowired
    private UsersRepository usersRepository;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    private EmailVerificationTokenRepository emailVerificationTokenRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private CaptchaService captchaService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // ✅ Login
    public AuthResponse login(LoginRequest loginRequest) {
        // TEMPORARY: Disable CAPTCHA for testing
        // TODO: Re-enable CAPTCHA in production
        /*
        if (loginRequest.getCaptchaResponse() == null || !captchaService.verifyCaptcha(loginRequest.getCaptchaResponse())) {
            throw new RuntimeException("CAPTCHA verification failed. Please try again!");
        }
        */

        Users user = usersRepository.findByUsername(loginRequest.getUsername())
                .orElseThrow(() -> new RuntimeException("Username not found"));

        if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
            throw new RuntimeException("Incorrect password!");
        }

        // Sinh JWT bằng JwtUtil
        String jwtToken = jwtUtil.generateToken(user.getUsername());

        return new AuthResponse(
                jwtToken,
                user.getUserId(),
                user.getUsername(),
                user.getEmail(),
                user.getUserType()
        );
    }

    // ✅ Register
    public AuthResponse register(RegisterRequest registerRequest) {
        // TEMPORARY: Disable CAPTCHA for testing
        // TODO: Re-enable CAPTCHA in production
        /*
        if (registerRequest.getCaptchaResponse() == null || !captchaService.verifyCaptcha(registerRequest.getCaptchaResponse())) {
            throw new RuntimeException("CAPTCHA verification failed. Please try again!");
        }
        */

        // Validate username
        if (!isValidUsername(registerRequest.getUsername())) {
            throw new RuntimeException("Invalid username. Username must be 3-20 characters, alphanumeric and underscore only");
        }

        // Validate full name
        if (registerRequest.getFullName() == null || registerRequest.getFullName().trim().isEmpty()) {
            throw new RuntimeException("Full name is required");
        }
        if (registerRequest.getFullName().trim().length() < 2 || registerRequest.getFullName().trim().length() > 100) {
            throw new RuntimeException("Full name must be between 2 and 100 characters");
        }

        // Validate email format
        if (!isValidEmail(registerRequest.getEmail())) {
            throw new RuntimeException("Invalid email format");
        }

        // Validate password strength
        if (!isValidPassword(registerRequest.getPassword())) {
            throw new RuntimeException("Password must be at least 8 characters and contain both letters and numbers");
        }

        // Validate password confirmation
        if (!registerRequest.getPassword().equals(registerRequest.getConfirmPassword())) {
            throw new RuntimeException("Passwords do not match");
        }

        // Validate phone number
        if (!isValidPhone(registerRequest.getPhoneNumber())) {
            throw new RuntimeException("Invalid phone number. Phone number must be 10 digits starting with 03, 05, 07, 08, or 09");
        }

        // Validate gender
        if (registerRequest.getGender() == null || registerRequest.getGender().trim().isEmpty()) {
            throw new RuntimeException("Gender is required");
        }
        String gender = registerRequest.getGender().toUpperCase();
        if (!gender.equals("MALE") && !gender.equals("FEMALE") && !gender.equals("OTHER")) {
            throw new RuntimeException("Invalid gender. Must be Male, Female, or Other");
        }

        // Validate birth date
        if (registerRequest.getBirthDate() != null && !isValidBirthDate(registerRequest.getBirthDate())) {
            throw new RuntimeException("Invalid birth date. You must be at least 13 years old");
        }

        // Validate terms accepted
        if (registerRequest.getTermsAccepted() == null || !registerRequest.getTermsAccepted()) {
            throw new RuntimeException("You must agree to the Terms of Service and Privacy Policy");
        }

        // Check existing email
        if (usersRepository.existsByEmail(registerRequest.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        // Check existing username
        if (usersRepository.existsByUsername(registerRequest.getUsername())) {
            throw new RuntimeException("Username already exists");
        }

        Users newUser = new Users();
        newUser.setUsername(registerRequest.getUsername());
        newUser.setFullName(registerRequest.getFullName().trim());
        newUser.setEmail(registerRequest.getEmail());
        newUser.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
        newUser.setPhoneNumber(registerRequest.getPhoneNumber());
        newUser.setGender(gender);
        newUser.setBirthDate(registerRequest.getBirthDate());
        newUser.setUserType("CUSTOMER");
        newUser.setBalance(new BigDecimal("0.00")); // Khởi tạo balance = 0
        newUser.setIsActive(false); // ⚠️ Account is inactive until email verification
        newUser.setIsEmailVerified(false);
        newUser.setCreatedAt(LocalDateTime.now());
        newUser.setUpdatedAt(LocalDateTime.now());

        usersRepository.save(newUser);

        // ✅ DO NOT send verification email automatically
        // User will request the code manually on verification page

        // ✅ Auto login user after registration to access verify page
        String jwtToken = jwtUtil.generateToken(newUser.getUsername());

        return new AuthResponse(
                jwtToken,
                newUser.getUserId(),
                newUser.getUsername(),
                newUser.getEmail(),
                newUser.getUserType()
        );
    }

    // ✅ Forgot password
    public void forgotPassword(String email) {
        Users user = usersRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Email not found"));

        // Xoá token cũ chưa dùng (nếu có)
        passwordResetTokenRepository.findByUserIdAndIsUsedFalse(user.getUserId())
                .ifPresent(passwordResetTokenRepository::delete);

        // Tạo token mới
        String token = UUID.randomUUID().toString();
        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setUserId(user.getUserId());
        resetToken.setToken(token);
        resetToken.setExpiresAt(LocalDateTime.now().plusMinutes(30)); // hết hạn sau 30p
        resetToken.setIsUsed(false);
        resetToken.setCreatedAt(LocalDateTime.now());

        passwordResetTokenRepository.save(resetToken);

        // Send beautiful HTML email
        String emailContent = """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>Password Reset</title>
                </head>
                <body style="margin: 0; padding: 0; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif; background-color: #f5f7fa; line-height: 1.6;">
                    <table role="presentation" style="width: 100%%; border-collapse: collapse; background-color: #f5f7fa;">
                        <tr>
                            <td style="padding: 40px 20px;">
                                <table role="presentation" style="max-width: 600px; margin: 0 auto; background-color: #ffffff; border-radius: 16px; box-shadow: 0 4px 24px rgba(0,0,0,0.08); overflow: hidden;">
                                    <!-- Header -->
                                    <tr>
                                        <td style="background: linear-gradient(135deg, #f59e0b 0%%, #d97706 100%%); padding: 40px 30px; text-align: center;">
                                            <div style="background: rgba(255,255,255,0.2); width: 80px; height: 80px; border-radius: 50%%; margin: 0 auto 20px; display: flex; align-items: center; justify-content: center;">
                                                <span style="font-size: 40px;">🔐</span>
                                            </div>
                                            <h1 style="color: #ffffff; margin: 0 0 10px; font-size: 28px; font-weight: 700; letter-spacing: -0.5px;">Password Reset Request</h1>
                                            <p style="color: rgba(255,255,255,0.9); margin: 0; font-size: 15px;">We received a request to reset your password</p>
                                        </td>
                                    </tr>
                                    
                                    <!-- Content -->
                                    <tr>
                                        <td style="padding: 40px 30px;">
                                            <p style="color: #1f2937; font-size: 16px; line-height: 1.6; margin: 0 0 20px;">Hello <strong>%s</strong>,</p>
                                            
                                            <p style="color: #4b5563; font-size: 15px; line-height: 1.7; margin: 0 0 25px;">
                                                Someone requested a password reset for your BanHangRong account. If this was you, click the button below to reset your password. This link will expire in <strong>30 minutes</strong>.
                                            </p>
                                            
                                            <!-- Reset Button -->
                                            <table role="presentation" style="margin: 30px 0;">
                                                <tr>
                                                    <td style="text-align: center;">
                                                        <a href="http://localhost:8080/reset-password?token=%s" 
                                                           style="display: inline-block; background: linear-gradient(135deg, #f59e0b 0%%, #d97706 100%%); 
                                                                  color: #ffffff; text-decoration: none; padding: 16px 40px; 
                                                                  border-radius: 12px; font-weight: 600; font-size: 16px; 
                                                                  box-shadow: 0 4px 16px rgba(245, 158, 11, 0.3); transition: all 0.3s;">
                                                            🔑 Reset Password
                                                        </a>
                                                    </td>
                                                </tr>
                                            </table>
                                            
                                            <!-- Alternative Link -->
                                            <div style="background: #f3f4f6; border-left: 4px solid #f59e0b; padding: 16px; border-radius: 8px; margin: 25px 0;">
                                                <p style="color: #6b7280; font-size: 13px; margin: 0 0 8px; font-weight: 600;">Or copy this link:</p>
                                                <p style="color: #374151; font-size: 12px; margin: 0; word-break: break-all; font-family: 'Courier New', monospace;">
                                                    http://localhost:8080/reset-password?token=%s
                                                </p>
                                            </div>
                                            
                                            <!-- Security Notice -->
                                            <div style="background: #fef3c7; border: 1px solid #fbbf24; border-radius: 10px; padding: 16px; margin: 25px 0;">
                                                <p style="color: #92400e; font-size: 14px; margin: 0; line-height: 1.6;">
                                                    <strong>⚠️ Security Notice:</strong> If you didn't request this password reset, please ignore this email. Your password will remain unchanged.
                                                </p>
                                            </div>
                                            
                                            <p style="color: #6b7280; font-size: 14px; line-height: 1.6; margin: 25px 0 0;">
                                                Need help? Contact our support team at 
                                                <a href="mailto:support@banhangrong.com" style="color: #f59e0b; text-decoration: none; font-weight: 500;">support@banhangrong.com</a>
                                            </p>
                                        </td>
                                    </tr>
                                    
                                    <!-- Footer -->
                                    <tr>
                                        <td style="background-color: #f9fafb; padding: 30px; text-align: center; border-top: 1px solid #e5e7eb;">
                                            <p style="color: #9ca3af; font-size: 14px; margin: 0 0 8px; font-weight: 500;">
                                                <strong style="color: #f59e0b;">BanHangRong</strong> - Your Digital Marketplace
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
                """.formatted(user.getUsername(), token, token);

        Email mail = new Email(
                user.getEmail(),
                "🔐 Password Reset Request - BanHangRong",
                emailContent
        );
        emailService.sendEmail(mail);
    }

    // ✅ Reset password
    public void resetPassword(String token, String newPassword, String confirmPassword) {
        if (!newPassword.equals(confirmPassword)) {
            throw new RuntimeException("Passwords do not match!");
        }

        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Invalid token"));

        if (resetToken.getIsUsed() || resetToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Token has expired or already used");
        }

        Users user = usersRepository.findById(resetToken.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setPassword(passwordEncoder.encode(newPassword));
        usersRepository.save(user);

        resetToken.setIsUsed(true);
        passwordResetTokenRepository.save(resetToken);
    }

    // ✅ Lấy thông tin user từ token (fake)
    public Map<String, Object> getUserInfoFromToken(String token) {
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("token", token);
        userInfo.put("username", "testUser"); // TODO: giải mã JWT để lấy user thật
        return userInfo;
    }

    // ✅ Resend verification email for existing user
    public void resendVerificationEmail(String username) {
        Users user = usersRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getIsEmailVerified()) {
            throw new RuntimeException("Email is already verified");
        }

        // Delete old unused tokens
        emailVerificationTokenRepository.findByUserIdAndIsUsedFalse(user.getUserId())
                .ifPresent(emailVerificationTokenRepository::delete);

        // Create new verification token (valid for 2 minutes)
        String verificationCode = String.format("%06d", new Random().nextInt(1_000_000));
        EmailVerificationToken verificationToken = new EmailVerificationToken();
        verificationToken.setUserId(user.getUserId());
        verificationToken.setToken(verificationCode);
        verificationToken.setExpiresAt(LocalDateTime.now().plusMinutes(2));
        verificationToken.setIsUsed(false);
        verificationToken.setCreatedAt(LocalDateTime.now());
        emailVerificationTokenRepository.save(verificationToken);

        // Send beautiful HTML verification email
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
                                            <p style="color: rgba(255,255,255,0.9); margin: 0; font-size: 15px;">Verify your email address to activate your account</p>
                                        </td>
                                    </tr>
                                    
                                    <!-- Content -->
                                    <tr>
                                        <td style="padding: 40px 30px;">
                                            <p style="color: #1f2937; font-size: 16px; line-height: 1.6; margin: 0 0 20px;">Hello <strong>%s</strong>,</p>
                                            
                                            <p style="color: #4b5563; font-size: 15px; line-height: 1.7; margin: 0 0 25px;">
                                                Your email verification code is ready. Please use the code below to verify your email address. This code will expire in <strong style="color: #dc2626;">2 minutes</strong>.
                                            </p>
                                            
                                            <!-- Verification Code Box -->
                                            <div style="background: linear-gradient(135deg, #e0f2fe 0%%, #bae6fd 100%%); border: 2px dashed #0ea5e9; border-radius: 16px; padding: 30px; text-align: center; margin: 30px 0;">
                                                <p style="color: #0c4a6e; font-size: 13px; margin: 0 0 12px; font-weight: 600; text-transform: uppercase; letter-spacing: 1px;">Your Verification Code</p>
                                                <div style="background: #ffffff; border-radius: 12px; padding: 20px; display: inline-block; box-shadow: 0 4px 12px rgba(14, 165, 233, 0.15);">
                                                    <p style="font-size: 36px; font-weight: 700; color: #0ea5e9; margin: 0; letter-spacing: 8px; font-family: 'Courier New', monospace;">%s</p>
                                                </div>
                                            </div>
                                            
                                            <!-- Instructions -->
                                            <div style="background: #f0fdf4; border-left: 4px solid #10b981; padding: 16px; border-radius: 8px; margin: 25px 0;">
                                                <p style="color: #065f46; font-size: 14px; margin: 0 0 10px; font-weight: 600;">📝 How to verify:</p>
                                                <ol style="color: #047857; font-size: 14px; margin: 0; padding-left: 20px; line-height: 1.8;">
                                                    <li>Go to the verification page</li>
                                                    <li>Enter the 6-digit code above</li>
                                                    <li>Click "Verify" to activate your account</li>
                                                </ol>
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
                                                    <strong>⚠️ Security:</strong> If you didn't request this verification code, please ignore this email.
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
                """.formatted(user.getUsername(), verificationCode);

        Email mail = new Email(
                user.getEmail(),
                "📧 Email Verification Code - BanHangRong",
                emailContent
        );
        emailService.sendEmail(mail);
    }

    // ✅ Phone number validation for Vietnamese numbers
    public boolean isValidPhone(String phone) {
        String regex = "^(03|05|07|08|09)\\d{8}$";
        return phone != null && phone.matches(regex);
    }

    // ✅ Birth date validation
    public boolean isValidBirthDate(LocalDate birthDate) {
        if (birthDate == null) {
            return true; // Birth date is optional
        }
        
        try {
            LocalDate today = LocalDate.now();
            LocalDate minDate = today.minusYears(100); // Maximum 100 years old
            LocalDate maxDate = today.minusYears(13); // Minimum 13 years old
            
            // User must be at least 13 years old and not more than 100 years old
            // birthDate must be: minDate <= birthDate <= maxDate
            return !birthDate.isAfter(today) && !birthDate.isBefore(minDate) && (birthDate.isBefore(maxDate) || birthDate.isEqual(maxDate));
        } catch (Exception e) {
            return false;
        }
    }

    // ✅ Username validation
    public boolean isValidUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            return false;
        }
        // Username: 3-20 characters, alphanumeric and underscore only
        String regex = "^[a-zA-Z0-9_]{3,20}$";
        return username.matches(regex);
    }

    // ✅ Email validation
    public boolean isValidEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        String regex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
        return email.matches(regex);
    }

    // ✅ Password strength validation
    public boolean isValidPassword(String password) {
        if (password == null || password.length() < 8) {
            return false;
        }
        // At least 8 characters, must contain at least one letter and one number
        return password.matches(".*[A-Za-z].*") && password.matches(".*\\d.*");
    }
}
