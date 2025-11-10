package banhangrong.su25.Controller;

import banhangrong.su25.Entity.Users;
import banhangrong.su25.Entity.EmailVerificationToken;
import banhangrong.su25.service.CustomerProfileService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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

    private final CustomerProfileService profileService;

    public CustomerProfileController(CustomerProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping("/customer/profile/{username}")
    public String profile(@PathVariable("username") String username, Model model, RedirectAttributes redirectAttributes) {
        Users currentUser = profileService.getCurrentUserOrNull();

        Users profileUser = profileService.findByUsernameOrNull(username);
        if (profileUser == null) {
            System.out.println("❌ Profile user not found: " + username);
            // Try to create a default user if none exists
            if (profileService.countUsers() == 0) {
                System.out.println("No users in database, creating default user...");
                profileUser = profileService.createDefaultUser(username);
                if (profileUser == null) {
                    System.out.println("❌ Failed to create default user");
                    return "redirect:/customer/dashboard";
                }
            } else {
                // Fallback: if not found, redirect to dashboard
                return "redirect:/customer/dashboard";
            }
        }

        // Header data
        if (currentUser != null) addHeader(model, currentUser); else model.addAttribute("user", profileUser);

        model.addAttribute("profileUser", profileUser);
        return "customer/profile";
    }

    @GetMapping("/customer/profile/{username}/edit")
    public String editProfileForm(@PathVariable("username") String username, Model model) {
        Users currentUser = profileService.getCurrentUserOrNull();
        if (currentUser == null || !username.equalsIgnoreCase(currentUser.getUsername())) {
            return "redirect:/customer/profile/" + username;
        }

        addHeader(model, currentUser);
        model.addAttribute("profileUser", currentUser);
        return "customer/profile-edit";
    }

    @PostMapping("/customer/profile/{username}/edit")
    public String editProfileSubmit(@PathVariable("username") String username,
                                    @RequestParam(name = "fullName", required = false) String fullName,
                                    @RequestParam(name = "avatarUrl", required = false) String avatarUrl,
                                    @RequestParam(name = "email", required = false) String email,
                                    @RequestParam(name = "phoneNumber", required = false) String phoneNumber,
                                    @RequestParam(name = "gender", required = false) String gender,
                                    @RequestParam(name = "birthDate", required = false) String birthDateStr,
                                    Model model,
                                    RedirectAttributes redirectAttributes) {
        Users currentUser = profileService.getCurrentUserOrNull();
        if (currentUser == null || !username.equalsIgnoreCase(currentUser.getUsername())) {
            return "redirect:/customer/profile/" + username;
        }
        
        // Lưu email cũ để gửi thông báo nếu có thay đổi
        String oldEmail = currentUser.getEmail();
        boolean emailChanged = false;

        // Update allowed fields only
        if (fullName != null && !fullName.trim().isEmpty()) {
            currentUser.setFullName(fullName.trim());
        }
        if (avatarUrl != null) currentUser.setAvatarUrl(avatarUrl.trim());
        if (email != null && !email.trim().isEmpty() && !email.trim().equalsIgnoreCase(currentUser.getEmail())) {
            String newEmail = email.trim();
            // Validate duplicate email (belongs to another user)
            var existing = profileService.findByEmail(newEmail).orElse(null);
            if (existing != null && !existing.getUserId().equals(currentUser.getUserId())) {
                // stay on edit page with inline error
                addHeader(model, currentUser);
                model.addAttribute("profileUser", currentUser);
                model.addAttribute("emailError", "Email đã tồn tại, vui lòng chọn email khác.");
                return "customer/profile-edit";
            }
            
            emailChanged = true;
            
            // Gửi email thông báo về email CŨ
            if (oldEmail != null && !oldEmail.trim().isEmpty()) {
                String userName = currentUser.getFullName() != null && !currentUser.getFullName().trim().isEmpty() 
                    ? currentUser.getFullName() 
                    : currentUser.getUsername();
                String oldEmailSubject = "Thông báo thay đổi email";
                String oldEmailBody = String.format(
                    "Xin chào %s,\n\n" +
                    "Email của bạn đã được thay đổi từ %s sang %s.\n\n" +
                    "Nếu đây không phải là bạn, vui lòng liên hệ ngay với chúng tôi.\n\n" +
                    "Trân trọng,\nBán Hàng Rong Team",
                    userName, oldEmail, newEmail
                );
                try {
                    profileService.sendEmailSafe(oldEmail, oldEmailSubject, oldEmailBody);
                } catch (Exception e) {
                    System.err.println("Failed to send notification to old email: " + e.getMessage());
                }
            }
            
            // Persist the new email immediately using update query to avoid stale entity issues
            profileService.updateEmailAndUnverify(currentUser.getUserId(), newEmail);
            // Refresh in-memory user for the same request
            currentUser.setEmail(newEmail);
            currentUser.setIsEmailVerified(false);
            // invalidate previous token
            profileService.findUnusedTokenByUser(currentUser.getUserId())
                    .ifPresent(profileService::deleteToken);
            // create new verify token
            try {
                String token = String.format("%06d", new java.util.Random().nextInt(1_000_000));
                EmailVerificationToken evt = profileService.createAndSaveToken(currentUser.getUserId(), token);
                
                // Gửi email xác thực về email MỚI
                String userName = currentUser.getFullName() != null && !currentUser.getFullName().trim().isEmpty() 
                    ? currentUser.getFullName() 
                    : currentUser.getUsername();
                String newEmailSubject = "Xác thực email mới của bạn";
                String newEmailBody = String.format(
                    "Xin chào %s,\n\n" +
                    "Email của bạn đã được thay đổi từ %s sang %s.\n\n" +
                    "Mã xác thực của bạn là: %s (có hiệu lực trong 24 giờ)\n\n" +
                    "Trân trọng,\nBán Hàng Rong Team",
                    userName, oldEmail, newEmail, token
                );
                profileService.sendEmailSafe(currentUser.getEmail(), newEmailSubject, newEmailBody);
            } catch (Exception e) {
                System.err.println("Failed to send verification code to new email: " + e.getMessage());
                // tolerate missing token table or other issues in dev
            }
        }
        if (phoneNumber != null) currentUser.setPhoneNumber(phoneNumber.trim());
        if (gender != null) currentUser.setGender(gender.trim());
        if (birthDateStr != null && !birthDateStr.trim().isEmpty()) {
            java.time.LocalDate bd = profileService.parseDateOrNull(birthDateStr);
            if (bd != null) currentUser.setBirthDate(bd);
        }
        
        try { 
            profileService.saveAndFlushUser(currentUser);
            
            // Thêm thông báo thành công
            if (emailChanged) {
                redirectAttributes.addFlashAttribute("successMessage", 
                    "Cập nhật thông tin thành công! Email đã được thay đổi, vui lòng nhập mã xác thực đã được gửi về email mới.");
                // Redirect đến trang verify-code để nhập mã
                return "redirect:/customer/verify-code?emailChanged=1";
            } else {
                redirectAttributes.addFlashAttribute("successMessage", "Cập nhật thông tin thành công!");
            }
            
        } catch (Exception e) {
            // Thông báo lỗi
            redirectAttributes.addFlashAttribute("errorMessage", 
                "Không thể lưu thay đổi. Vui lòng thử lại. Lỗi: " + e.getMessage());
            return "redirect:/customer/profile/" + currentUser.getUsername() + "/edit";
        }
        
        return "redirect:/customer/profile/" + currentUser.getUsername() + "?updated=1";
    }

    @GetMapping("/customer/verify-code")
    public String showVerifyCodeForm(Model model) {
        Users currentUser = profileService.getCurrentUserOrNull();
        if (currentUser == null) return "redirect:/login";
        addHeader(model, currentUser);
        long remaining = 0;
        var existingOpt = profileService.findUnusedTokenByUser(currentUser.getUserId());
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
        Users currentUser = profileService.getCurrentUserOrNull();
        if (currentUser == null) return "redirect:/login";
        var opt = profileService.findUnusedTokenByUser(currentUser.getUserId());
        if (opt.isEmpty()) {
            model.addAttribute("error", "Không tìm thấy mã xác thực. Hãy đổi email hoặc yêu cầu gửi lại mã.");
            addHeader(model, currentUser);
            model.addAttribute("remainingSeconds", 0);
            model.addAttribute("user", currentUser);
            return "customer/verify-email-code";
        }
        EmailVerificationToken evt = opt.get();
        if (evt.getExpiresAt() != null && evt.getExpiresAt().isBefore(java.time.LocalDateTime.now())) {
            model.addAttribute("error", "Mã đã hết hạn. Vui lòng yêu cầu mã mới.");
            addHeader(model, currentUser);
            model.addAttribute("remainingSeconds", 0);
            model.addAttribute("user", currentUser);
            return "customer/verify-email-code";
        }
        if (!evt.getToken().equals(code.trim())) {
            model.addAttribute("error", "Mã không đúng. Vui lòng thử lại.");
            addHeader(model, currentUser);
            long remaining = 0;
            long seconds = java.time.Duration.between(evt.getCreatedAt(), java.time.LocalDateTime.now()).getSeconds();
            if (seconds < VERIFY_CODE_COOLDOWN_SECONDS) remaining = VERIFY_CODE_COOLDOWN_SECONDS - seconds;
            model.addAttribute("remainingSeconds", remaining);
            model.addAttribute("user", currentUser);
            return "customer/verify-email-code";
        }
        currentUser.setIsEmailVerified(true);
        profileService.saveAndFlushUser(currentUser);
        evt.setIsUsed(true);
        // reuse saveUser as simple persist
        profileService.createAndSaveToken(evt.getUserId(), evt.getToken()); // not ideal, but keep persistence simple
        return "redirect:/customer/profile/" + currentUser.getUsername() + "?verified=1";
    }

    @GetMapping("/customer/verify-email")
    public String verifyEmail(@RequestParam("token") String token) {
        EmailVerificationToken evt = profileService.findByToken(token).orElse(null);
        if (evt == null || Boolean.TRUE.equals(evt.getIsUsed()) || evt.getExpiresAt() == null || evt.getExpiresAt().isBefore(java.time.LocalDateTime.now())) {
            return "redirect:/verify-email-required"; // simple fallback
        }
        Users user = profileService.findByIdOrNull(evt.getUserId());
        if (user == null) return "redirect:/verify-email-required";
        user.setIsEmailVerified(true);
        profileService.saveAndFlushUser(user);
        evt.setIsUsed(true);
        profileService.createAndSaveToken(evt.getUserId(), evt.getToken());
        return "redirect:/customer/dashboard";
    }

    // Request a new verification code from profile page
    @PostMapping("/customer/profile/verify-email")
    public String sendVerifyCodeFromProfile() {
        Users currentUser = profileService.getCurrentUserOrNull();
        if (currentUser == null) return "redirect:/login";

        // Cooldown: if existing unused token within cooldown, do not send new
        var existingOpt = profileService.findUnusedTokenByUser(currentUser.getUserId());
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
            EmailVerificationToken evt = existingOpt.orElseGet(() -> profileService.createAndSaveToken(currentUser.getUserId(), token));
            if (existingOpt.isPresent()) {
                evt.setToken(token);
                evt.setExpiresAt(java.time.LocalDateTime.now().plusDays(1));
                evt.setIsUsed(false);
                evt.setCreatedAt(java.time.LocalDateTime.now());
                profileService.createAndSaveToken(evt.getUserId(), evt.getToken());
            }
            profileService.sendEmailSafe(currentUser.getEmail(), "Your verification code", "Your code is: " + token + " (valid 24 hours)");
        } catch (Exception ignored) {}

        return "redirect:/customer/verify-code?sent=1&remaining=" + VERIFY_CODE_COOLDOWN_SECONDS;
    }

    // === THÊM METHODS XỬ LÝ ĐỔI MẬT KHẨU ===
    @GetMapping("/customer/profile/{username}/change-password")
    public String showChangePasswordForm(@PathVariable("username") String username, Model model) {
        Users currentUser = profileService.getCurrentUserOrNull();
        if (currentUser == null || !username.equalsIgnoreCase(currentUser.getUsername())) {
            return "redirect:/customer/profile/" + username;
        }

        addHeader(model, currentUser);
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
        Users currentUser = profileService.getCurrentUserOrNull();
        if (currentUser == null || !username.equalsIgnoreCase(currentUser.getUsername())) {
            return "redirect:/customer/profile/" + username;
        }

        try { model.addAttribute("cartCount", profileService.getCartCount(currentUser.getUserId())); } catch (Exception ignored) {}
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
        if (!profileService.checkPassword(currentPassword, currentUser.getPassword())) {
            model.addAttribute("pwdError", "Mật khẩu hiện tại không đúng");
            return "customer/change-password";
        }

        // Update password
        try {
            String encryptedPassword = profileService.encodePassword(newPassword);
            currentUser.setPassword(encryptedPassword);
            profileService.saveUser(currentUser);
            redirectAttributes.addFlashAttribute("successMessage", "Đổi mật khẩu thành công!");
            return "redirect:/customer/profile/" + username + "?passwordChanged=1";
        } catch (Exception e) {
            model.addAttribute("pwdError", "Lỗi khi đổi mật khẩu: " + e.getMessage());
            return "customer/change-password";
        }
    }
    // ===== Helpers =====
    private void addHeader(Model model, Users user) {
        try { model.addAttribute("cartCount", profileService.getCartCount(user.getUserId())); } catch (Exception ignored) {}
        model.addAttribute("user", user);
    }
}


