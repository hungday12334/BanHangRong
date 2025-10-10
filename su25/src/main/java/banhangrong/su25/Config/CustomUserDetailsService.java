package banhangrong.su25.Config;

import banhangrong.su25.Entity.Users;
import banhangrong.su25.Repository.UsersRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UsersRepository usersRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        System.out.println("=== DEBUG: Loading user by username: " + username + " ===");
        
        Users user = usersRepository.findByUsername(username)
                .orElseThrow(() -> {
                    System.out.println("=== DEBUG: User not found in database: " + username + " ===");
                    return new UsernameNotFoundException("User not found: " + username);
                });
        
        System.out.println("=== DEBUG: User found: " + user.getUsername() + ", Type: " + user.getUserType() + ", Active: " + user.getIsActive() + " ===");
        System.out.println("=== DEBUG: Password hash: " + user.getPassword() + " ===");

        // Kiểm tra user có active không
        if (!user.getIsActive()) {
            System.out.println("=== DEBUG: User is not active: " + username + " ===");
            throw new UsernameNotFoundException("User is not active: " + username);
        }

        // Tạo authorities dựa trên userType
        List<GrantedAuthority> authorities = new ArrayList<>();
        String userType = user.getUserType();
        if (userType != null) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + userType.toUpperCase()));
        }
        
        // Thêm role USER mặc định
        authorities.add(new SimpleGrantedAuthority("ROLE_USER"));

        // Tạo UserDetails object
        return User.builder()
                .username(user.getUsername())
                .password(user.getPassword()) // Password sẽ được hash bằng BCrypt
                .authorities(authorities)
                .accountExpired(false)
                .accountLocked(!user.getIsActive())
                .credentialsExpired(false)
                .disabled(!user.getIsActive())
                .build();
    }

    // Method để load user by email (optional)
    public UserDetails loadUserByEmail(String email) throws UsernameNotFoundException {
        Users user = usersRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

        return loadUserByUsername(user.getUsername());
    }
}
