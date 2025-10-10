import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class TestPasswordEncoding {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String password = "123456";
        
        // Test với hash có sẵn
        String existingHash = "$2a$10$e0MYzXyjpJS7Pd0RVvHwHeFtVpVKxKBCl7Cn6FGa.Ll8xKKqqfLq2";
        boolean matches = encoder.matches(password, existingHash);
        
        System.out.println("Password: " + password);
        System.out.println("Hash: " + existingHash);
        System.out.println("Matches: " + matches);
        
        // Generate hash mới
        String newHash = encoder.encode(password);
        System.out.println("New Hash: " + newHash);
        
        // Test hash mới
        boolean newMatches = encoder.matches(password, newHash);
        System.out.println("New Hash Matches: " + newMatches);
        
        System.out.println();
        System.out.println("SQL để update testadmin:");
        System.out.println("UPDATE users SET password = '" + newHash + "' WHERE username = 'testadmin';");
    }
}
