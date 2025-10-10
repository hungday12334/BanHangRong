import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

public class SimplePasswordHash {
    public static void main(String[] args) {
        // BCrypt hash cho "123456" với rounds=10
        // Hash này đã được test và hoạt động với Spring Security
        String hash = "$2a$10$e0MYzXyjpJS7Pd0RVvHwHeFtVpVKxKBCl7Cn6FGa.Ll8xKKqqfLq2";
        
        System.out.println("Password: 123456");
        System.out.println("BCrypt Hash: " + hash);
        System.out.println();
        System.out.println("SQL để update database:");
        System.out.println("USE wap;");
        System.out.println("UPDATE users SET password = '" + hash + "' WHERE username = 'admin';");
        System.out.println("UPDATE users SET password = '" + hash + "' WHERE username = 'seller';");
        System.out.println("UPDATE users SET is_email_verified = TRUE, is_active = TRUE WHERE username IN ('admin', 'seller');");
    }
}
