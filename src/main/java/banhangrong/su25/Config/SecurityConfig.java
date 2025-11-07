package banhangrong.su25.Config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final CustomAuthenticationSuccessHandler successHandler;
    private final CustomAccessDeniedHandler accessDeniedHandler;

    public SecurityConfig(CustomAuthenticationSuccessHandler successHandler,
                         CustomAccessDeniedHandler accessDeniedHandler) {
        this.successHandler = successHandler;
        this.accessDeniedHandler = accessDeniedHandler;
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
    public SecurityFilterChain filterChain(HttpSecurity http, SessionRegistry sessionRegistry) throws Exception {
        http
            // CSRF - Keep disabled for now
            .csrf(csrf -> csrf.disable())

            // Session management - CRITICAL for maintaining session
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                .sessionFixation().migrateSession() // Migrate session on authentication
                .invalidSessionUrl("/login?expired=true")
                .maximumSessions(5)
                .maxSessionsPreventsLogin(false)
                .expiredUrl("/login?expired=true")
                .sessionRegistry(sessionRegistry)
            )

            // Authorization rules
            .authorizeHttpRequests(auth -> auth
                    // Public endpoints
                    .requestMatchers("/api/auth/**").permitAll()
                    .requestMatchers("/api/email-verification/**").authenticated()
                    .requestMatchers("/api/database/**").permitAll()
                    .requestMatchers("/api/password-hash/**").permitAll()
                    .requestMatchers("/api/debug/**").authenticated() // Debug endpoint - cần authenticated
                    .requestMatchers("/css/**", "/js/**", "/images/**", "/img/**", "/favicon.ico").permitAll()
                    .requestMatchers("/", "/login", "/register", "/forgot-password", "/find-account", "/reset-password", "/verify-email-required").permitAll()
                    // Guest-browsable catalog
                    .requestMatchers("/categories", "/category/**", "/product/**").permitAll()
                    .requestMatchers("/db", "/api/database/**").permitAll()

                // Chat endpoints - CHỈ CẦN AUTHENTICATED
                .requestMatchers("/chat", "/customer/chat", "/seller/chat").authenticated()
                .requestMatchers("/api/conversation/**", "/api/conversations/**").authenticated()
                .requestMatchers("/api/users/**", "/api/sellers/**").authenticated()
                .requestMatchers("/ws/**").authenticated()
                
                // Customer pages
                .requestMatchers("/customer/**", "/cart/**").authenticated()
                
                // Seller pages - CHỈ SELLER mới vào được
                .requestMatchers("/seller/**").hasRole("SELLER")
                
                // Seller API endpoints - CHỈ SELLER mới vào được
                .requestMatchers("/api/seller/**").hasRole("SELLER")

                // Admin pages
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/user/**").authenticated()

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
            )
            
            // Xử lý Access Denied (403)
            .exceptionHandling(exceptions -> exceptions
                .accessDeniedHandler(accessDeniedHandler)
            );

        return http.build();
    }
    @Bean
    SessionRegistry sessionRegistry() {
        return new SessionRegistryImpl();
    }
}
