import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class PasswordHashGenerator {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(10);
        String password = "123456";
        String hash = encoder.encode(password);
        
        System.out.println("Password: " + password);
        System.out.println("BCrypt Hash: " + hash);
        System.out.println();
        System.out.println("SQL để update:");
        System.out.println("UPDATE users SET password = '" + hash + "' WHERE username = 'admin';");
        System.out.println("UPDATE users SET password = '" + hash + "' WHERE username = 'seller';");
    }
}
