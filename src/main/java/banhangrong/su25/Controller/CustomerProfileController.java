package banhangrong.su25.Controller;

import banhangrong.su25.Entity.Users;
import banhangrong.su25.Repository.ShoppingCartRepository;
import banhangrong.su25.Repository.EmailVerificationTokenRepository;
import banhangrong.su25.Entity.EmailVerificationToken;
import banhangrong.su25.email.Email;
import banhangrong.su25.email.EmailService;
import banhangrong.su25.Repository.UsersRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class CustomerProfileController {

    private final UsersRepository usersRepository;
    private final ShoppingCartRepository shoppingCartRepository;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final EmailService emailService;

    public CustomerProfileController(UsersRepository usersRepository, ShoppingCartRepository shoppingCartRepository,
                                     EmailVerificationTokenRepository emailVerificationTokenRepository,
                                     EmailService emailService) {
        this.usersRepository = usersRepository;
        this.shoppingCartRepository = shoppingCartRepository;
        this.emailVerificationTokenRepository = emailVerificationTokenRepository;
        this.emailService = emailService;
    }

    @GetMapping("/customer/profile/{username}")
    public String profile(@PathVariable("username") String username, Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Users currentUser = null;
        if (auth != null && auth.isAuthenticated()) {
            currentUser = usersRepository.findByUsername(auth.getName()).orElse(null);
        }

        Users profileUser = usersRepository.findByUsername(username).orElse(null);
        if (profileUser == null) {
            // Fallback: if not found, redirect to dashboard
            return "redirect:/customer/dashboard";
        }

        // Header data
        if (currentUser != null) {
            try { model.addAttribute("cartCount", shoppingCartRepository.countByUserId(currentUser.getUserId())); } catch (Exception ignored) {}
            model.addAttribute("user", currentUser);
        }

        model.addAttribute("profileUser", profileUser);
        return "customer/profile";
    }

    @GetMapping("/customer/profile/{username}/edit")
    public String editProfileForm(@PathVariable("username") String username, Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Users currentUser = null;
        if (auth != null && auth.isAuthenticated()) {
            currentUser = usersRepository.findByUsername(auth.getName()).orElse(null);
        }
        if (currentUser == null || !username.equalsIgnoreCase(currentUser.getUsername())) {
            return "redirect:/customer/profile/" + username;
        }

        try { model.addAttribute("cartCount", shoppingCartRepository.countByUserId(currentUser.getUserId())); } catch (Exception ignored) {}
        model.addAttribute("user", currentUser);
        model.addAttribute("profileUser", currentUser);
        return "customer/profile-edit";
    }

    @PostMapping("/customer/profile/{username}/edit")
    public String editProfileSubmit(@PathVariable("username") String username,
                                    @RequestParam(name = "avatarUrl", required = false) String avatarUrl,
                                    @RequestParam(name = "email", required = false) String email,
                                    @RequestParam(name = "phoneNumber", required = false) String phoneNumber,
                                    @RequestParam(name = "gender", required = false) String gender,
                                    @RequestParam(name = "birthDate", required = false) String birthDateStr) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Users currentUser = null;
        if (auth != null && auth.isAuthenticated()) {
            currentUser = usersRepository.findByUsername(auth.getName()).orElse(null);
        }
        if (currentUser == null || !username.equalsIgnoreCase(currentUser.getUsername())) {
            return "redirect:/customer/profile/" + username;
        }

        // Update allowed fields only
        if (avatarUrl != null) currentUser.setAvatarUrl(avatarUrl.trim());
        if (email != null && !email.trim().equalsIgnoreCase(currentUser.getEmail())) {
            currentUser.setEmail(email.trim());
            currentUser.setIsEmailVerified(false);
            // invalidate previous token
            emailVerificationTokenRepository.findByUserIdAndIsUsedFalse(currentUser.getUserId())
                    .ifPresent(emailVerificationTokenRepository::delete);
            // create new verify token
            String token = java.util.UUID.randomUUID().toString();
            EmailVerificationToken evt = new EmailVerificationToken();
            evt.setUserId(currentUser.getUserId());
            evt.setToken(token);
            evt.setExpiresAt(java.time.LocalDateTime.now().plusDays(1));
            evt.setIsUsed(false);
            evt.setCreatedAt(java.time.LocalDateTime.now());
            emailVerificationTokenRepository.save(evt);
            // send email
            String link = "http://localhost:8080/customer/verify-email?token=" + token;
            emailService.sendEmail(new Email(currentUser.getEmail(), "Verify your email", "Click to verify: " + link));
        }
        if (phoneNumber != null) currentUser.setPhoneNumber(phoneNumber.trim());
        if (gender != null) currentUser.setGender(gender.trim());
        if (birthDateStr != null && !birthDateStr.trim().isEmpty()) {
            try {
                currentUser.setBirthDate(java.time.LocalDate.parse(birthDateStr));
            } catch (Exception ignored) {}
        }
        usersRepository.save(currentUser);
        return "redirect:/customer/profile/" + username + "?updated=1";
    }

    @GetMapping("/customer/verify-email")
    public String verifyEmail(@RequestParam("token") String token) {
        EmailVerificationToken evt = emailVerificationTokenRepository.findByToken(token).orElse(null);
        if (evt == null || Boolean.TRUE.equals(evt.getIsUsed()) || evt.getExpiresAt() == null || evt.getExpiresAt().isBefore(java.time.LocalDateTime.now())) {
            return "redirect:/verify-email-required"; // simple fallback
        }
        Users user = usersRepository.findById(evt.getUserId()).orElse(null);
        if (user == null) return "redirect:/verify-email-required";
        user.setIsEmailVerified(true);
        usersRepository.save(user);
        evt.setIsUsed(true);
        emailVerificationTokenRepository.save(evt);
        return "redirect:/customer/dashboard";
    }
}


