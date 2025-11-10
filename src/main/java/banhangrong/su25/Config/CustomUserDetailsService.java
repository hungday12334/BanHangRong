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
        Users user = usersRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        // ⚠️ CHANGED: Allow login even if account is not active (unverified email)
        // CustomAuthenticationSuccessHandler will redirect to verify page if needed
        // Only block if account is explicitly disabled by admin (could add a separate field for this)

        // Tạo authorities dựa trên userType
        List<GrantedAuthority> authorities = new ArrayList<>();
        String userType = user.getUserType();
        
        if (userType != null) {
            String normalizedType = userType.trim().toUpperCase();
            
            // Xử lý USER như CUSTOMER
            if ("USER".equals(normalizedType)) {
                authorities.add(new SimpleGrantedAuthority("ROLE_CUSTOMER"));
            } else {
                authorities.add(new SimpleGrantedAuthority("ROLE_" + normalizedType));
            }
        } else {
            // Nếu userType null, mặc định là CUSTOMER
            authorities.add(new SimpleGrantedAuthority("ROLE_CUSTOMER"));
        }
        
        // Thêm role USER mặc định cho tất cả user đã đăng nhập
        authorities.add(new SimpleGrantedAuthority("ROLE_USER"));

        // Tạo UserDetails object
        return User.builder()
                .username(user.getUsername())
                .password(user.getPassword()) // Password sẽ được hash bằng BCrypt
                .authorities(authorities)
                .accountExpired(false)
                .accountLocked(false) // ✅ Don't lock account for unverified email
                .credentialsExpired(false)
                .disabled(false) // ✅ Don't disable account for unverified email
                .build();
    }

    // Method để load user by email (optional)
    public UserDetails loadUserByEmail(String email) throws UsernameNotFoundException {
        Users user = usersRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

        return loadUserByUsername(user.getUsername());
    }
}
