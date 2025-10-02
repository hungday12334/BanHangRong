package banhangrong.su25.Service;

import banhangrong.su25.DTO.AuthResponse;
import banhangrong.su25.DTO.LoginRequest;
import banhangrong.su25.DTO.RegisterRequest;
import banhangrong.su25.Entity.PasswordResetToken;
import banhangrong.su25.Entity.Users;
import banhangrong.su25.Repository.PasswordResetTokenRepository;
import banhangrong.su25.Repository.UsersRepository;
import banhangrong.su25.Util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class AuthService {

    @Autowired
    private UsersRepository usersRepository;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private EmailService emailService;

    public AuthResponse login(LoginRequest loginRequest) {
        Optional<Users> userOptional = usersRepository.findByUsername(loginRequest.getUsername());
        
        if (userOptional.isEmpty()) {
            throw new RuntimeException("Tên đăng nhập không tồn tại");
        }

        Users user = userOptional.get();
        
        if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
            throw new RuntimeException("Mật khẩu không đúng");
        }

        if (!user.getIsActive()) {
            throw new RuntimeException("Tài khoản đã bị khóa");
        }

        // Cập nhật last login
        user.setLastLogin(LocalDateTime.now());
        usersRepository.save(user);

        String token = jwtUtil.generateToken(user.getUsername());
        
        return new AuthResponse(token, user.getUserId(), user.getUsername(), 
                              user.getEmail(), user.getUserType());
    }

    public AuthResponse register(RegisterRequest registerRequest) {
        // Kiểm tra username đã tồn tại
        if (usersRepository.findByUsername(registerRequest.getUsername()).isPresent()) {
            throw new RuntimeException("Tên đăng nhập đã tồn tại");
        }

        // Kiểm tra email đã tồn tại
        if (usersRepository.findByEmail(registerRequest.getEmail()).isPresent()) {
            throw new RuntimeException("Email đã tồn tại");
        }

        // Kiểm tra mật khẩu xác nhận
        if (!registerRequest.getPassword().equals(registerRequest.getConfirmPassword())) {
            throw new RuntimeException("Mật khẩu xác nhận không khớp");
        }

        // Kiểm tra Terms of Service
        if (registerRequest.getTermsAccepted() == null || !registerRequest.getTermsAccepted()) {
            throw new RuntimeException("Vui lòng đồng ý với Terms of Service và Privacy Policy");
        }

        // Tạo user mới
        Users user = new Users();
        user.setUsername(registerRequest.getUsername());
        user.setEmail(registerRequest.getEmail());
        user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
        user.setPhoneNumber(registerRequest.getPhoneNumber());
        user.setGender(registerRequest.getGender() != null ? registerRequest.getGender() : "OTHER");
        user.setBirthDate(registerRequest.getBirthDate());
        user.setUserType("USER");
        user.setIsEmailVerified(false);
        user.setIsActive(true);
        user.setBalance(java.math.BigDecimal.ZERO);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());

        usersRepository.save(user);

        // Gửi email chào mừng
        try {
            emailService.sendWelcomeEmail(user.getEmail(), user.getUsername());
        } catch (Exception e) {
            System.err.println("Không thể gửi email chào mừng: " + e.getMessage());
        }

        String token = jwtUtil.generateToken(user.getUsername());
        
        return new AuthResponse(token, user.getUserId(), user.getUsername(), 
                              user.getEmail(), user.getUserType());
    }

    public void forgotPassword(String email) {
        Optional<Users> userOptional = usersRepository.findByEmail(email);
        
        if (userOptional.isEmpty()) {
            throw new RuntimeException("Email không tồn tại");
        }

        Users user = userOptional.get();
        
        // Tạo token reset password
        String resetToken = UUID.randomUUID().toString();
        
        // Lưu token vào database
        PasswordResetToken passwordResetToken = new PasswordResetToken();
        passwordResetToken.setUserId(user.getUserId());
        passwordResetToken.setToken(resetToken);
        passwordResetToken.setExpiresAt(LocalDateTime.now().plusHours(24)); // Hết hạn sau 24 giờ
        passwordResetToken.setIsUsed(false);
        passwordResetToken.setCreatedAt(LocalDateTime.now());
        
        passwordResetTokenRepository.save(passwordResetToken);
        
        // Gửi email reset password
        emailService.sendPasswordResetEmail(user.getEmail(), resetToken);
    }

    public void forgotPasswordAuto() {
        // Lấy user đầu tiên trong hệ thống (cho demo)
        // Trong thực tế, bạn có thể lấy từ session hoặc JWT token
        java.util.List<Users> allUsers = usersRepository.findAll();
        
        if (allUsers.isEmpty()) {
            throw new RuntimeException("Không có user nào trong hệ thống");
        }

        // Lấy user đầu tiên (hoặc có thể lấy user hiện tại từ session)
        Users user = allUsers.get(0);
        
        // Tạo token reset password
        String resetToken = UUID.randomUUID().toString();
        
        // Lưu token vào database
        PasswordResetToken passwordResetToken = new PasswordResetToken();
        passwordResetToken.setUserId(user.getUserId());
        passwordResetToken.setToken(resetToken);
        passwordResetToken.setExpiresAt(LocalDateTime.now().plusHours(24)); // Hết hạn sau 24 giờ
        passwordResetToken.setIsUsed(false);
        passwordResetToken.setCreatedAt(LocalDateTime.now());
        
        passwordResetTokenRepository.save(passwordResetToken);
        
        // Gửi email reset password về email cá nhân của user
        emailService.sendPasswordResetEmail(user.getEmail(), resetToken);
        
        System.out.println("✅ Đã gửi email đặt lại mật khẩu cho user: " + user.getUsername() + " (" + user.getEmail() + ")");
    }

    public void resetPassword(String token, String newPassword, String confirmPassword) {
        // Kiểm tra token có tồn tại và chưa hết hạn không
        Optional<PasswordResetToken> tokenOptional = passwordResetTokenRepository.findByToken(token);
        
        if (tokenOptional.isEmpty()) {
            throw new RuntimeException("Token không hợp lệ");
        }
        
        PasswordResetToken passwordResetToken = tokenOptional.get();
        
        // Kiểm tra token đã được sử dụng chưa
        if (passwordResetToken.getIsUsed()) {
            throw new RuntimeException("Token đã được sử dụng");
        }
        
        // Kiểm tra token có hết hạn không
        if (passwordResetToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Token đã hết hạn");
        }
        
        // Kiểm tra mật khẩu xác nhận
        if (!newPassword.equals(confirmPassword)) {
            throw new RuntimeException("Mật khẩu xác nhận không khớp");
        }
        
        // Tìm user theo userId
        Optional<Users> userOptional = usersRepository.findById(passwordResetToken.getUserId());
        if (userOptional.isEmpty()) {
            throw new RuntimeException("Người dùng không tồn tại");
        }
        
        Users user = userOptional.get();
        
        // Cập nhật mật khẩu mới
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setUpdatedAt(LocalDateTime.now());
        usersRepository.save(user);
        
        // Đánh dấu token đã được sử dụng
        passwordResetToken.setIsUsed(true);
        passwordResetTokenRepository.save(passwordResetToken);
    }

    public Map<String, Object> getUserInfoFromToken(String token) {
        // Kiểm tra token có tồn tại và chưa hết hạn không
        Optional<PasswordResetToken> tokenOptional = passwordResetTokenRepository.findByToken(token);
        
        if (tokenOptional.isEmpty()) {
            throw new RuntimeException("Token không hợp lệ");
        }
        
        PasswordResetToken passwordResetToken = tokenOptional.get();
        
        // Kiểm tra token đã được sử dụng chưa
        if (passwordResetToken.getIsUsed()) {
            throw new RuntimeException("Token đã được sử dụng");
        }
        
        // Kiểm tra token có hết hạn không
        if (passwordResetToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Token đã hết hạn");
        }
        
        // Tìm user theo userId
        Optional<Users> userOptional = usersRepository.findById(passwordResetToken.getUserId());
        if (userOptional.isEmpty()) {
            throw new RuntimeException("Người dùng không tồn tại");
        }
        
        Users user = userOptional.get();
        
        // Trả về thông tin user
        Map<String, Object> userInfo = new java.util.HashMap<>();
        userInfo.put("userId", user.getUserId());
        userInfo.put("username", user.getUsername());
        userInfo.put("email", user.getEmail());
        userInfo.put("avatarUrl", user.getAvatarUrl());
        userInfo.put("userType", user.getUserType());
        
        return userInfo;
    }
}
