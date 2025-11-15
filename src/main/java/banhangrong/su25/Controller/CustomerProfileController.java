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
    public String profile(@PathVariable("username") String username, Model model) {
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
                                    @RequestParam(name = "avatarUrl", required = false) String avatarUrl,
                                    @RequestParam(name = "email", required = false) String email,
                                    @RequestParam(name = "phoneNumber", required = false) String phoneNumber,
                                    @RequestParam(name = "gender", required = false) String gender,
                                    @RequestParam(name = "birthDate", required = false) String birthDateStr,
                                    Model model) {
        Users currentUser = profileService.getCurrentUserOrNull();
        if (currentUser == null || !username.equalsIgnoreCase(currentUser.getUsername())) {
            return "redirect:/customer/profile/" + username;
        }

        // Update allowed fields only
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
            profileService.updateEmailAndUnverify(currentUser.getUserId(), newEmail);
            currentUser.setEmail(newEmail);
            currentUser.setIsEmailVerified(false);
            profileService.findUnusedTokenByUser(currentUser.getUserId())
                    .ifPresent(profileService::deleteToken);
            // create new verify token
            try {
                String token = String.format("%06d", new java.util.Random().nextInt(1_000_000));
                EmailVerificationToken evt = profileService.createAndSaveToken(currentUser.getUserId(), token);
                // send email with code
                profileService.sendEmailSafe(currentUser.getEmail(), "Your verification code", "Your code is: " + token + " (valid 24 hours)");
            } catch (Exception ignored) {
                // tolerate missing token table or other issues in dev
            }
        }
        if (phoneNumber != null) currentUser.setPhoneNumber(phoneNumber.trim());
        if (gender != null) currentUser.setGender(gender.trim());
        if (birthDateStr != null && !birthDateStr.trim().isEmpty()) {
            java.time.LocalDate bd = profileService.parseDateOrNull(birthDateStr);
            if (bd != null) currentUser.setBirthDate(bd);
        }
        try { profileService.saveAndFlushUser(currentUser); } catch (Exception e) {
            addHeader(model, currentUser);
            model.addAttribute("profileUser", currentUser);
            model.addAttribute("saveError", "Không thể lưu thay đổi. Vui lòng thử lại.");
            return "customer/profile-edit";
        }
        return "redirect:/customer/profile/" + currentUser.getUsername() + "?updated=1";
    }

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


