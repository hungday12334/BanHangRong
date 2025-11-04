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
        newUser.setIsActive(true);
        newUser.setIsEmailVerified(false);
        newUser.setCreatedAt(LocalDateTime.now());
        newUser.setUpdatedAt(LocalDateTime.now());

        usersRepository.save(newUser);

        // Create verification token (valid for 2 minutes)
        String verificationCode = String.format("%06d", new Random().nextInt(1_000_000));
        EmailVerificationToken verificationToken = new EmailVerificationToken();
        verificationToken.setUserId(newUser.getUserId());
        verificationToken.setToken(verificationCode);
        verificationToken.setExpiresAt(LocalDateTime.now().plusMinutes(2));
        verificationToken.setIsUsed(false);
        verificationToken.setCreatedAt(LocalDateTime.now());
        emailVerificationTokenRepository.save(verificationToken);

        // Send welcome email with verification code
        String emailContent = "Hello " + newUser.getUsername() + ",\n\n" +
                "Thank you for registering an account!\n\n" +
                "Your email verification code is: " + verificationCode + "\n" +
                "This code is valid for 2 minutes only.\n\n" +
                "Please enter this code at: http://localhost:8080/verify-email-required\n\n" +
                "If you didn't create this account, please ignore this email.";

        Email mail = new Email(
                newUser.getEmail(),
                "Welcome to BanHangRong - Verify Your Email",
                emailContent
        );
        emailService.sendEmail(mail);

        return new AuthResponse(
                UUID.randomUUID().toString(),
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

        // Send email
        Email mail = new Email(
                user.getEmail(),
                "Password Reset Request",
                "Click the following link to reset your password: http://localhost:8080/reset-password?token=" + token
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

        // Send verification email
        String emailContent = "Hello " + user.getUsername() + ",\n\n" +
                "Your email verification code is: " + verificationCode + "\n" +
                "This code is valid for 2 minutes only.\n\n" +
                "Please enter this code at: http://localhost:8080/verify-email-required\n\n" +
                "If you didn't request this, please ignore this email.";

        Email mail = new Email(
                user.getEmail(),
                "BanHangRong - Email Verification Code",
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
