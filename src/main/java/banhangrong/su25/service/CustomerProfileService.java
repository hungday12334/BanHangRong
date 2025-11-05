package banhangrong.su25.service;

import banhangrong.su25.Entity.EmailVerificationToken;
import banhangrong.su25.Entity.Users;
import banhangrong.su25.Repository.EmailVerificationTokenRepository;
import banhangrong.su25.Repository.ShoppingCartRepository;
import banhangrong.su25.Repository.UsersRepository;
import banhangrong.su25.email.Email;
import banhangrong.su25.email.EmailService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class CustomerProfileService {

    private final UsersRepository usersRepository;
    private final ShoppingCartRepository shoppingCartRepository;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    public CustomerProfileService(UsersRepository usersRepository,
                                  ShoppingCartRepository shoppingCartRepository,
                                  EmailVerificationTokenRepository emailVerificationTokenRepository,
                                  EmailService emailService,
                                  PasswordEncoder passwordEncoder) {
        this.usersRepository = usersRepository;
        this.shoppingCartRepository = shoppingCartRepository;
        this.emailVerificationTokenRepository = emailVerificationTokenRepository;
        this.emailService = emailService;
        this.passwordEncoder = passwordEncoder;
    }

    public Users getCurrentUserOrNull() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return null;
        return usersRepository.findByUsername(auth.getName()).orElse(null);
    }

    public Users findByUsernameOrNull(String username) {
        return usersRepository.findByUsername(username).orElse(null);
    }

    public long countUsers() {
        return usersRepository.count();
    }

    public Users createDefaultUser(String username) {
        Users u = new Users();
        u.setUsername(username);
        u.setEmail(username + "@example.com");
        u.setPassword(passwordEncoder.encode("123456"));
        u.setUserType("customer");
        u.setIsActive(true);
        u.setIsEmailVerified(false);
        u.setPhoneNumber("0123456789");
        u.setGender("other");
        u.setBalance(java.math.BigDecimal.ZERO);
        u.setCreatedAt(LocalDateTime.now());
        u.setUpdatedAt(LocalDateTime.now());
        return usersRepository.save(u);
    }

    public Long getCartCount(Long userId) {
        if (userId == null) return 0L;
        return shoppingCartRepository.countByUserId(userId);
    }

    public void updateEmailAndUnverify(Long userId, String newEmail) {
        usersRepository.updateEmailAndUnverify(userId, newEmail);
    }

    public Optional<Users> findByEmail(String email) {
        return usersRepository.findByEmail(email);
    }

    public Optional<EmailVerificationToken> findUnusedTokenByUser(Long userId) {
        return emailVerificationTokenRepository.findByUserIdAndIsUsedFalse(userId);
    }

    public void deleteToken(EmailVerificationToken token) {
        emailVerificationTokenRepository.delete(token);
    }

    public EmailVerificationToken createAndSaveToken(Long userId, String token) {
        EmailVerificationToken evt = new EmailVerificationToken();
        evt.setUserId(userId);
        evt.setToken(token);
        evt.setExpiresAt(LocalDateTime.now().plusDays(1));
        evt.setIsUsed(false);
        evt.setCreatedAt(LocalDateTime.now());
        return emailVerificationTokenRepository.save(evt);
    }

    public void sendEmailSafe(String to, String subject, String content) {
        try { emailService.sendEmail(new Email(to, subject, content)); } catch (Exception ignored) {}
    }

    public Optional<EmailVerificationToken> findByToken(String token) {
        return emailVerificationTokenRepository.findByToken(token);
    }

    public Users findByIdOrNull(Long id) {
        return usersRepository.findById(id).orElse(null);
    }

    public Users saveUser(Users user) {
        return usersRepository.save(user);
    }

    public Users saveAndFlushUser(Users user) {
        return usersRepository.saveAndFlush(user);
    }

    public boolean checkPassword(String raw, String encoded) {
        return passwordEncoder.matches(raw, encoded);
    }

    public String encodePassword(String raw) {
        return passwordEncoder.encode(raw);
    }

    public LocalDate parseDateOrNull(String dateStr) {
        try { return LocalDate.parse(dateStr); } catch (Exception ignored) { return null; }
    }
}


