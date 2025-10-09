import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class PasswordGenerator {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        
        // Generate hash cho password "seller123"
        String password = "seller123";
        String hash = encoder.encode(password);
        
        System.out.println("========================================");
        System.out.println("PASSWORD GENERATOR");
        System.out.println("========================================");
        System.out.println("Raw Password: " + password);
        System.out.println("BCrypt Hash:  " + hash);
        System.out.println("========================================");
        System.out.println("\nCopy hash này vào SQL:");
        System.out.println("UPDATE users SET password = '" + hash + "' WHERE username = 'seller';");
        System.out.println("========================================");
    }
}

