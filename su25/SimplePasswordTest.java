public class SimplePasswordTest {
    public static void main(String[] args) {
        String password = "123456";
        
        // BCrypt hash với rounds=10 (Spring Security mặc định)
        String hash1 = "$2a$10$e0MYzXyjpJS7Pd0RVvHwHeFtVpVKxKBCl7Cn6FGa.Ll8xKKqqfLq2";
        String hash2 = "$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi";
        String hash3 = "$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi";
        
        System.out.println("Password: " + password);
        System.out.println();
        System.out.println("Hash 1: " + hash1);
        System.out.println("Hash 2: " + hash2);
        System.out.println("Hash 3: " + hash3);
        System.out.println();
        System.out.println("SQL để test từng hash:");
        System.out.println("UPDATE users SET password = '" + hash1 + "' WHERE username = 'testadmin';");
        System.out.println("UPDATE users SET password = '" + hash2 + "' WHERE username = 'testadmin';");
        System.out.println("UPDATE users SET password = '" + hash3 + "' WHERE username = 'testadmin';");
    }
}
