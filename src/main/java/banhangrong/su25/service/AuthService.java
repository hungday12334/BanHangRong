package banhangrong.su25.service;

import banhangrong.su25.DTO.AuthResponse;
import banhangrong.su25.DTO.LoginRequest;
import banhangrong.su25.DTO.RegisterRequest;
import banhangrong.su25.Entity.Users;
import banhangrong.su25.Entity.PasswordResetToken;
import banhangrong.su25.Repository.UsersRepository;
import banhangrong.su25.Repository.PasswordResetTokenRepository;
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

        // Validate phone number
        if (!isValidPhone(registerRequest.getPhoneNumber())) {
            throw new RuntimeException("Invalid phone number");
        }

        // Validate birth date
        if (registerRequest.getBirthDate() != null && !isValidBirthDate(registerRequest.getBirthDate())) {
            throw new RuntimeException("Invalid birth date");
        }

        if (usersRepository.existsByEmail(registerRequest.getEmail())) {
            throw new RuntimeException("Email already exists");
        }
        if (usersRepository.existsByUsername(registerRequest.getUsername())) {
            throw new RuntimeException("Username already exists");
        }

        Users newUser = new Users();
        newUser.setUsername(registerRequest.getUsername());
        newUser.setEmail(registerRequest.getEmail());
        newUser.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
        newUser.setPhoneNumber(registerRequest.getPhoneNumber());
        newUser.setGender(registerRequest.getGender());
        newUser.setBirthDate(registerRequest.getBirthDate());
        newUser.setUserType("CUSTOMER");
        newUser.setBalance(new BigDecimal("0.00")); // Khởi tạo balance = 0
        newUser.setIsActive(true);
        newUser.setIsEmailVerified(false);
        newUser.setCreatedAt(LocalDateTime.now());
        newUser.setUpdatedAt(LocalDateTime.now());

        usersRepository.save(newUser);

        // Send welcome email
        Email mail = new Email(
                newUser.getEmail(),
                "Welcome to BanHangRong",
                "Hello " + newUser.getUsername() + ",\nThank you for registering an account!"
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
            
            return !birthDate.isAfter(today) && !birthDate.isBefore(minDate) && !birthDate.isAfter(maxDate);
        } catch (Exception e) {
            return false;
        }
    }
}
