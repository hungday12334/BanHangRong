package banhangrong.su25.Config;

import banhangrong.su25.Entity.Users;
import banhangrong.su25.Repository.UsersRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Component
public class DataLoader implements CommandLineRunner {

    @Autowired
    private UsersRepository usersRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        System.out.println("=== ĐANG TẠO TÀI KHOẢN TEST ===");
        
        // Tạo user test 1
        createTestUser("nguyenhung1401", "nguyenhung14012k5@gmail.com", "123456", "0338503905", "MALE");
        
        // Tạo user test 2 - Admin
        createTestUser("admin", "admin@banhangrong.com", "admin123", "0123456789", "MALE", "ADMIN");
        
        // Tạo user test 3
        createTestUser("testuser", "test@banhangrong.com", "test123", "0987654321", "FEMALE");
        
        System.out.println("=== HOÀN THÀNH TẠO TÀI KHOẢN TEST ===");
        System.out.println("Tài khoản có thể sử dụng:");
        System.out.println("1. Username: nguyenhung1401 | Password: 123456");
        System.out.println("2. Username: admin | Password: admin123 (ADMIN)");
        System.out.println("3. Username: testuser | Password: test123");
        System.out.println("Lưu ý: Dữ liệu sẽ được lưu vĩnh viễn trong file database!");
    }
    
    private void createTestUser(String username, String email, String password, String phone, String gender) {
        createTestUser(username, email, password, phone, gender, "USER");
    }
    
    private void createTestUser(String username, String email, String password, String phone, String gender, String userType) {
        if (usersRepository.findByUsername(username).isEmpty()) {
            Users testUser = new Users();
            testUser.setUsername(username);
            testUser.setEmail(email);
            testUser.setPassword(passwordEncoder.encode(password));
            testUser.setPhoneNumber(phone);
            testUser.setGender(gender);
            testUser.setUserType(userType);
            testUser.setAvatarUrl("https://via.placeholder.com/150/007bff/ffffff?text=" + username.substring(0, 1).toUpperCase());
            testUser.setIsEmailVerified(true);
            testUser.setIsActive(true);
            testUser.setBalance(BigDecimal.ZERO);
            testUser.setCreatedAt(LocalDateTime.now());
            testUser.setUpdatedAt(LocalDateTime.now());
            
            usersRepository.save(testUser);
            System.out.println("✓ Đã tạo user test: " + username + " với mật khẩu: " + password + " (" + userType + ")");
        } else {
            System.out.println("✓ User " + username + " đã tồn tại");
        }
    }
}
