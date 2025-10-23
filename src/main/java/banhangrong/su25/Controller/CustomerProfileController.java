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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class CustomerProfileController {
    private static final int VERIFY_CODE_COOLDOWN_SECONDS = 60;

    private final UsersRepository usersRepository;
    private final ShoppingCartRepository shoppingCartRepository;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    public CustomerProfileController(UsersRepository usersRepository, ShoppingCartRepository shoppingCartRepository,
                                     EmailVerificationTokenRepository emailVerificationTokenRepository,
                                     EmailService emailService, PasswordEncoder passwordEncoder) {
        this.usersRepository = usersRepository;
        this.shoppingCartRepository = shoppingCartRepository;
        this.emailVerificationTokenRepository = emailVerificationTokenRepository;
        this.emailService = emailService;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/customer/profile/{username}")
    public String profile(@PathVariable("username") String username, Model model) {
        System.out.println("=== CUSTOMER PROFILE LOADING ===");
        System.out.println("Requested username: " + username);
        
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Users currentUser = null;
        if (auth != null && auth.isAuthenticated()) {
            currentUser = usersRepository.findByUsername(auth.getName()).orElse(null);
            System.out.println("Current user: " + (currentUser != null ? currentUser.getUsername() : "null"));
        }

        Users profileUser = usersRepository.findByUsername(username).orElse(null);
        if (profileUser == null) {
            System.out.println("❌ Profile user not found: " + username);
            // Try to create a default user if none exists
            if (usersRepository.count() == 0) {
                System.out.println("No users in database, creating default user...");
                profileUser = createDefaultUser(username);
                if (profileUser == null) {
                    System.out.println("❌ Failed to create default user");
                    return "redirect:/customer/dashboard";
                }
            } else {
                // Fallback: if not found, redirect to dashboard
                return "redirect:/customer/dashboard";
            }
        }

        System.out.println("✅ Profile user found: " + profileUser.getUsername());
        System.out.println("Email: " + profileUser.getEmail());
        System.out.println("User type: " + profileUser.getUserType());

        // Header data
        if (currentUser != null) {
            try { model.addAttribute("cartCount", shoppingCartRepository.countByUserId(currentUser.getUserId())); } catch (Exception ignored) {}
            model.addAttribute("user", currentUser);
        } else {
            // Fallback: use profileUser as current user if no authentication
            model.addAttribute("user", profileUser);
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
                                    @RequestParam(name = "birthDate", required = false) String birthDateStr,
                                    Model model) {
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
        if (email != null && !email.trim().isEmpty() && !email.trim().equalsIgnoreCase(currentUser.getEmail())) {
            String newEmail = email.trim();
            // Validate duplicate email (belongs to another user)
            var existing = usersRepository.findByEmail(newEmail).orElse(null);
            if (existing != null && !existing.getUserId().equals(currentUser.getUserId())) {
                // stay on edit page with inline error
                try { model.addAttribute("cartCount", shoppingCartRepository.countByUserId(currentUser.getUserId())); } catch (Exception ignored) {}
                model.addAttribute("user", currentUser);
                model.addAttribute("profileUser", currentUser);
                model.addAttribute("emailError", "Email đã tồn tại, vui lòng chọn email khác.");
                return "customer/profile-edit";
            }
            // Persist the new email immediately using update query to avoid stale entity issues
            usersRepository.updateEmailAndUnverify(currentUser.getUserId(), newEmail);
            // Refresh in-memory user for the same request
            currentUser.setEmail(newEmail);
            currentUser.setIsEmailVerified(false);
            // invalidate previous token
            emailVerificationTokenRepository.findByUserIdAndIsUsedFalse(currentUser.getUserId())
                    .ifPresent(emailVerificationTokenRepository::delete);
            // create new verify token
            try {
                String token = String.format("%06d", new java.util.Random().nextInt(1_000_000));
                EmailVerificationToken evt = new EmailVerificationToken();
                evt.setUserId(currentUser.getUserId());
                evt.setToken(token);
                evt.setExpiresAt(java.time.LocalDateTime.now().plusDays(1));
                evt.setIsUsed(false);
                evt.setCreatedAt(java.time.LocalDateTime.now());
                emailVerificationTokenRepository.save(evt);
                // send email with code
                try {
                    emailService.sendEmail(new Email(currentUser.getEmail(), "Your verification code", "Your code is: " + token + " (valid 24 hours)"));
                } catch (Exception ignored) {}
            } catch (Exception ignored) {
                // tolerate missing token table or other issues in dev
            }
        }
        if (phoneNumber != null) currentUser.setPhoneNumber(phoneNumber.trim());
        if (gender != null) currentUser.setGender(gender.trim());
        if (birthDateStr != null && !birthDateStr.trim().isEmpty()) {
            try {
                currentUser.setBirthDate(java.time.LocalDate.parse(birthDateStr));
            } catch (Exception ignored) {}
        }
        try { usersRepository.saveAndFlush(currentUser); } catch (Exception e) {
            try { model.addAttribute("cartCount", shoppingCartRepository.countByUserId(currentUser.getUserId())); } catch (Exception ignored) {}
            model.addAttribute("user", currentUser);
            model.addAttribute("profileUser", currentUser);
            model.addAttribute("saveError", "Không thể lưu thay đổi. Vui lòng thử lại.");
            return "customer/profile-edit";
        }
        return "redirect:/customer/profile/" + currentUser.getUsername() + "?updated=1";
    }

    @GetMapping("/customer/verify-code")
    public String showVerifyCodeForm(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return "redirect:/login";
        Users currentUser = usersRepository.findByUsername(auth.getName()).orElse(null);
        if (currentUser == null) return "redirect:/login";
        try { model.addAttribute("cartCount", shoppingCartRepository.countByUserId(currentUser.getUserId())); } catch (Exception ignored) {}
        long remaining = 0;
        var existingOpt = emailVerificationTokenRepository.findByUserIdAndIsUsedFalse(currentUser.getUserId());
        if (existingOpt.isPresent()) {
            var evt = existingOpt.get();
            long seconds = java.time.Duration.between(evt.getCreatedAt(), java.time.LocalDateTime.now()).getSeconds();
            if (seconds < VERIFY_CODE_COOLDOWN_SECONDS) remaining = VERIFY_CODE_COOLDOWN_SECONDS - seconds;
        }
        model.addAttribute("remainingSeconds", remaining);
        model.addAttribute("user", currentUser);
        return "customer/verify-email-code";
    }

    @PostMapping("/customer/verify-code")
    public String submitVerifyCode(@RequestParam("code") String code, Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return "redirect:/login";
        Users currentUser = usersRepository.findByUsername(auth.getName()).orElse(null);
        if (currentUser == null) return "redirect:/login";
        var opt = emailVerificationTokenRepository.findByUserIdAndIsUsedFalse(currentUser.getUserId());
        if (opt.isEmpty()) {
            model.addAttribute("error", "Không tìm thấy mã xác thực. Hãy đổi email hoặc yêu cầu gửi lại mã.");
            try { model.addAttribute("cartCount", shoppingCartRepository.countByUserId(currentUser.getUserId())); } catch (Exception ignored) {}
            model.addAttribute("remainingSeconds", 0);
            model.addAttribute("user", currentUser);
            return "customer/verify-email-code";
        }
        EmailVerificationToken evt = opt.get();
        if (evt.getExpiresAt() != null && evt.getExpiresAt().isBefore(java.time.LocalDateTime.now())) {
            model.addAttribute("error", "Mã đã hết hạn. Vui lòng yêu cầu mã mới.");
            try { model.addAttribute("cartCount", shoppingCartRepository.countByUserId(currentUser.getUserId())); } catch (Exception ignored) {}
            model.addAttribute("remainingSeconds", 0);
            model.addAttribute("user", currentUser);
            return "customer/verify-email-code";
        }
        if (!evt.getToken().equals(code.trim())) {
            model.addAttribute("error", "Mã không đúng. Vui lòng thử lại.");
            try { model.addAttribute("cartCount", shoppingCartRepository.countByUserId(currentUser.getUserId())); } catch (Exception ignored) {}
            long remaining = 0;
            long seconds = java.time.Duration.between(evt.getCreatedAt(), java.time.LocalDateTime.now()).getSeconds();
            if (seconds < VERIFY_CODE_COOLDOWN_SECONDS) remaining = VERIFY_CODE_COOLDOWN_SECONDS - seconds;
            model.addAttribute("remainingSeconds", remaining);
            model.addAttribute("user", currentUser);
            return "customer/verify-email-code";
        }
        currentUser.setIsEmailVerified(true);
        usersRepository.saveAndFlush(currentUser);
        evt.setIsUsed(true);
        emailVerificationTokenRepository.save(evt);
        return "redirect:/customer/profile/" + currentUser.getUsername() + "?verified=1";
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
        usersRepository.saveAndFlush(user);
        evt.setIsUsed(true);
        emailVerificationTokenRepository.save(evt);
        return "redirect:/customer/dashboard";
    }

    // Request a new verification code from profile page
    @PostMapping("/customer/profile/verify-email")
    public String sendVerifyCodeFromProfile() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return "redirect:/login";
        Users currentUser = usersRepository.findByUsername(auth.getName()).orElse(null);
        if (currentUser == null) return "redirect:/login";

        // Cooldown: if existing unused token within cooldown, do not send new
        var existingOpt = emailVerificationTokenRepository.findByUserIdAndIsUsedFalse(currentUser.getUserId());
        if (existingOpt.isPresent()) {
            var evt = existingOpt.get();
            long seconds = java.time.Duration.between(evt.getCreatedAt(), java.time.LocalDateTime.now()).getSeconds();
            if (seconds < VERIFY_CODE_COOLDOWN_SECONDS) {
                long remaining = VERIFY_CODE_COOLDOWN_SECONDS - seconds;
                return "redirect:/customer/verify-code?sent=1&remaining=" + remaining;
            }
        }

        // Create or overwrite 6-digit code
        try {
            String token = String.format("%06d", new java.util.Random().nextInt(1_000_000));
            EmailVerificationToken evt = existingOpt.orElseGet(EmailVerificationToken::new);
            evt.setUserId(currentUser.getUserId());
            evt.setToken(token);
            evt.setExpiresAt(java.time.LocalDateTime.now().plusDays(1));
            evt.setIsUsed(false);
            evt.setCreatedAt(java.time.LocalDateTime.now());
            emailVerificationTokenRepository.save(evt);

            try { emailService.sendEmail(new Email(currentUser.getEmail(), "Your verification code", "Your code is: " + token + " (valid 24 hours)")); } catch (Exception ignored) {}
        } catch (Exception ignored) {}

        return "redirect:/customer/verify-code?sent=1&remaining=" + VERIFY_CODE_COOLDOWN_SECONDS;
    }

    // === THÊM METHODS XỬ LÝ ĐỔI MẬT KHẨU ===
    @GetMapping("/customer/profile/{username}/change-password")
    public String showChangePasswordForm(@PathVariable("username") String username, Model model) {
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
        return "customer/change-password";
    }

    @PostMapping("/customer/profile/{username}/change-password")
    public String changePassword(@PathVariable("username") String username,
                                @RequestParam String currentPassword,
                                @RequestParam String newPassword,
                                @RequestParam String confirmPassword,
                                Model model,
                                RedirectAttributes redirectAttributes) {
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

        // Validate passwords
        if (!newPassword.equals(confirmPassword)) {
            model.addAttribute("pwdError", "Mật khẩu mới và xác nhận mật khẩu không khớp");
            return "customer/change-password";
        }

        if (newPassword.length() < 6) {
            model.addAttribute("pwdError", "Mật khẩu mới phải có ít nhất 6 ký tự");
            return "customer/change-password";
        }

        // Verify current password
        if (!passwordEncoder.matches(currentPassword, currentUser.getPassword())) {
            model.addAttribute("pwdError", "Mật khẩu hiện tại không đúng");
            return "customer/change-password";
        }

        // Update password
        try {
            String encryptedPassword = passwordEncoder.encode(newPassword);
            currentUser.setPassword(encryptedPassword);
            usersRepository.save(currentUser);
            redirectAttributes.addFlashAttribute("successMessage", "Đổi mật khẩu thành công!");
            return "redirect:/customer/profile/" + username + "?passwordChanged=1";
        } catch (Exception e) {
            model.addAttribute("pwdError", "Lỗi khi đổi mật khẩu: " + e.getMessage());
            return "customer/change-password";
        }
    }

    // === THÊM METHOD TẠO USER MẶC ĐỊNH ===
    private Users createDefaultUser(String username) {
        try {
            System.out.println("Creating default user with username: " + username);
            
            Users defaultUser = new Users();
            defaultUser.setUsername(username);
            defaultUser.setEmail(username + "@example.com");
            defaultUser.setPassword(passwordEncoder.encode("123456")); // Default password
            defaultUser.setUserType("customer");
            defaultUser.setIsActive(true);
            defaultUser.setIsEmailVerified(false);
            defaultUser.setPhoneNumber("0123456789");
            defaultUser.setGender("other");
            defaultUser.setBalance(java.math.BigDecimal.ZERO);
            defaultUser.setCreatedAt(java.time.LocalDateTime.now());
            defaultUser.setUpdatedAt(java.time.LocalDateTime.now());
            
            Users savedUser = usersRepository.save(defaultUser);
            System.out.println("✅ Default user created successfully: " + savedUser.getUsername());
            System.out.println("User ID: " + savedUser.getUserId());
            System.out.println("Email: " + savedUser.getEmail());
            System.out.println("Phone: " + savedUser.getPhoneNumber());
            return savedUser;
            
        } catch (Exception e) {
            System.out.println("❌ Error creating default user: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
}


