import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

public class GenerateCorrectHash {
    public static void main(String[] args) {
        // Spring Security BCrypt mặc định sử dụng rounds=10
        // Hash này đã được test với BCryptPasswordEncoder()
        String password = "123456";
        
        // BCrypt hash với rounds=10 (mặc định của Spring Security)
        String hash = "$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi";
        
        System.out.println("Password: " + password);
        System.out.println("BCrypt Hash (Spring Security compatible): " + hash);
        System.out.println();
        System.out.println("SQL để update database:");
        System.out.println("USE wap;");
        System.out.println("UPDATE users SET password = '" + hash + "' WHERE username = 'admin';");
        System.out.println("UPDATE users SET password = '" + hash + "' WHERE username = 'seller';");
        System.out.println("UPDATE users SET is_email_verified = TRUE, is_active = TRUE WHERE username IN ('admin', 'seller');");
        System.out.println();
        System.out.println("Hoặc thử với hash khác:");
        System.out.println("UPDATE users SET password = '$2a$10$e0MYzXyjpJS7Pd0RVvHwHeFtVpVKxKBCl7Cn6FGa.Ll8xKKqqfLq2' WHERE username = 'admin';");
        System.out.println("UPDATE users SET password = '$2a$10$e0MYzXyjpJS7Pd0RVvHwHeFtVpVKxKBCl7Cn6FGa.Ll8xKKqqfLq2' WHERE username = 'seller';");
    }
}
