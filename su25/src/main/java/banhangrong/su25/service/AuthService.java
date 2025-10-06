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
import org.springframework.stereotype.Service;

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

    // ✅ Login
    public AuthResponse login(LoginRequest loginRequest) {
        // Validate CAPTCHA first
        if (loginRequest.getCaptchaResponse() == null || !captchaService.verifyCaptcha(loginRequest.getCaptchaResponse())) {
            throw new RuntimeException("Xác thực CAPTCHA thất bại. Vui lòng thử lại!");
        }

        Users user = usersRepository.findByUsername(loginRequest.getUsername())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy username"));

        if (!user.getPassword().equals(loginRequest.getPassword())) {
            throw new RuntimeException("Sai mật khẩu!");
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
        // Validate CAPTCHA first
        if (registerRequest.getCaptchaResponse() == null || !captchaService.verifyCaptcha(registerRequest.getCaptchaResponse())) {
            throw new RuntimeException("Xác thực CAPTCHA thất bại. Vui lòng thử lại!");
        }

        if (usersRepository.existsByEmail(registerRequest.getEmail())) {
            throw new RuntimeException("Email đã tồn tại");
        }
        if (usersRepository.existsByUsername(registerRequest.getUsername())) {
            throw new RuntimeException("Username đã tồn tại");
        }

        Users newUser = new Users();
        newUser.setUsername(registerRequest.getUsername());
        newUser.setEmail(registerRequest.getEmail());
        newUser.setPassword(registerRequest.getPassword()); // TODO: mã hoá password bằng BCrypt
        newUser.setUserType("USER");
        newUser.setIsActive(true);
        newUser.setIsEmailVerified(false);
        newUser.setCreatedAt(LocalDateTime.now());

        usersRepository.save(newUser);

        // Gửi email chào mừng
        Email mail = new Email(
                newUser.getEmail(),
                "Chào mừng đến hệ thống",
                "Xin chào " + newUser.getUsername() + ",\nCảm ơn bạn đã đăng ký tài khoản!"
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
                .orElseThrow(() -> new RuntimeException("Email không tồn tại"));

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

        // Gửi email
        Email mail = new Email(
                user.getEmail(),
                "Yêu cầu đặt lại mật khẩu",
                "Click vào link sau để đặt lại mật khẩu: http://localhost:8080/api/auth/reset-password?token=" + token
        );
        emailService.sendEmail(mail);
    }

    // ✅ Reset password
    public void resetPassword(String token, String newPassword, String confirmPassword) {
        if (!newPassword.equals(confirmPassword)) {
            throw new RuntimeException("Mật khẩu nhập lại không khớp!");
        }

        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Token không hợp lệ"));

        if (resetToken.getIsUsed() || resetToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Token đã hết hạn hoặc đã sử dụng");
        }

        Users user = usersRepository.findById(resetToken.getUserId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy user"));

        user.setPassword(newPassword); // TODO: mã hoá bằng BCrypt
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
}
