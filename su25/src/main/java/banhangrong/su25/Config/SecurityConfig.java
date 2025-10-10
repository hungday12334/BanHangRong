package banhangrong.su25.Config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final CustomAuthenticationSuccessHandler successHandler;

    public SecurityConfig(CustomAuthenticationSuccessHandler successHandler) {
        this.successHandler = successHandler;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            
            // Cấu hình session management
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                .maximumSessions(1)
                .maxSessionsPreventsLogin(false)
            )
            
            // Cấu hình authorization
            .authorizeHttpRequests(auth -> auth
                    // Public endpoints
                    .requestMatchers("/api/auth/**").permitAll()
                    .requestMatchers("/api/database/**").permitAll()
                    .requestMatchers("/api/password-hash/**").permitAll()
                    .requestMatchers("/api/debug/**").permitAll()
                    .requestMatchers("/api/test-auth/**").permitAll()
                    .requestMatchers("/api/password-test/**").permitAll()
                    .requestMatchers("/css/**", "/js/**", "/images/**", "/img/**", "/favicon.ico").permitAll()
                    .requestMatchers("/", "/login", "/register", "/forgot-password", "/find-account", "/reset-password", "/verify-email-required").permitAll()
                    .requestMatchers("/db", "/api/database/**").permitAll()
                    .requestMatchers("/test_login.html").permitAll()
                    .requestMatchers("/test_accounts.html").permitAll()
                    .requestMatchers("/test-logout/**").permitAll()
                
                // Customer pages - cho phép tất cả authenticated users
                .requestMatchers("/customer/**", "/product/**", "/cart/**").authenticated()
                
                // Role-based access
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers("/seller/**").hasAnyRole("SELLER", "ADMIN")
                .requestMatchers("/api/user/**").hasAnyRole("USER", "SELLER", "ADMIN")
                
                // Default: require authentication
                .anyRequest().authenticated()
            )
            
            // Cấu hình form login
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/perform-login")
                .successHandler(successHandler)
                .failureUrl("/login?error=true")
                .usernameParameter("username")
                .passwordParameter("password")
                .permitAll()
            )
            
            // Cấu hình logout
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout=true")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            );

        return http.build();
    }
}
