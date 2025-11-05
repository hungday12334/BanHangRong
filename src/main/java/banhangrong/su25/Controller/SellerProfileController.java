package banhangrong.su25.Controller;

import banhangrong.su25.Entity.Users;
import banhangrong.su25.Repository.UsersRepository;
import banhangrong.su25.service.UserProfileService;
import banhangrong.su25.email.EmailService;
import banhangrong.su25.email.Email;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Optional;

@Controller
@RequestMapping("/seller")
public class SellerProfileController {

    @Autowired
    private UserProfileService userProfileService;

    @Autowired
    private UsersRepository usersRepository;

    @Autowired
    private EmailService emailService;

    @Value("${file.upload-dir:uploads}")
    private String uploadDir;

    private Long getCurrentSellerId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();

        System.out.println("=== GETTING CURRENT SELLER ===");
        System.out.println("Authenticated username: " + username);

        Optional<Users> userOptional = usersRepository.findByUsername(username);

        if (userOptional.isEmpty()) {
            System.out.println("❌ User not found: " + username);
            throw new RuntimeException("User not found");
        }

        Users user = userOptional.get();
        System.out.println("✅ Found user ID: " + user.getUserId());
        System.out.println("   Username: " + user.getUsername());
        System.out.println("   Role: " + user.getUserType());

        return user.getUserId();
    }

    @GetMapping("/profile")
    public String viewSellerProfile(Model model) {
        try {
            Long sellerId = getCurrentSellerId();
            Users user = userProfileService.getSellerProfile(sellerId);
            model.addAttribute("user", user);
            model.addAttribute("sellerId", sellerId);

            // === THÊM DEBUG LOG ===
            System.out.println("=== PROFILE PAGE DATA ===");
            System.out.println("User ID: " + user.getUserId());
            System.out.println("Username: " + user.getUsername());
            System.out.println("Avatar URL: " + user.getAvatarUrl());
            System.out.println("Email: " + user.getEmail());

        } catch (Exception e) {
            model.addAttribute("error", "Seller information not found");
            System.out.println("Error in viewSellerProfile: " + e.getMessage());
        }
        return "seller/profile";
    }

    // GIỮ NGUYÊN
    @GetMapping("/profile/edit")
    public String showEditProfileForm(Model model) {
        Long sellerId = getCurrentSellerId();
        Users seller = userProfileService.getSellerProfile(sellerId);
        model.addAttribute("seller", seller);
        return "seller/profile-edit";
    }

    @PostMapping("/profile/update")
    public String updateSellerProfile(@RequestParam(required = false) String username,
                                      @RequestParam(required = false) String email,
                                      @RequestParam String phoneNumber,
                                      @RequestParam String gender,
                                      @RequestParam(required = false) String birthDate,
                                      RedirectAttributes redirectAttributes) {
        try {
            System.out.println("=== START PROFILE UPDATE ===");

            Long sellerId = getCurrentSellerId();
            Users currentUser = userProfileService.getSellerProfile(sellerId);

            // ===== SECURITY: NGĂN CHẶN CẬP NHẬT TÊN VÀ EMAIL =====
            // Nếu client cố gắng gửi username hoặc email, REJECT ngay
            if (username != null && !username.equals(currentUser.getUsername())) {
                System.out.println("⚠️ SECURITY ALERT: Attempt to change username detected!");
                redirectAttributes.addFlashAttribute("errorMessage", "Changing username is not allowed!");
                return "redirect:/seller/profile";
            }

            if (email != null && !email.equals(currentUser.getEmail())) {
                System.out.println("⚠️ SECURITY ALERT: Attempt to change email detected!");
                redirectAttributes.addFlashAttribute("errorMessage", "Changing email is not allowed!");
                return "redirect:/seller/profile";
            }

            // ===== VALIDATION: XSS Protection - Sanitize inputs =====
            String sanitizedPhone = sanitizeInput(phoneNumber);
            String sanitizedGender = sanitizeInput(gender);

            // Validate phone number format (Vietnamese phone: 10-11 digits)
            if (!isValidPhoneNumber(sanitizedPhone)) {
                redirectAttributes.addFlashAttribute("errorMessage", "Invalid phone number! Please enter in correct format.");
                return "redirect:/seller/profile";
            }

            // Validate gender
            if (!isValidGender(sanitizedGender)) {
                redirectAttributes.addFlashAttribute("errorMessage", "Invalid gender!");
                return "redirect:/seller/profile";
            }

            System.out.println("Phone: " + sanitizedPhone);
            System.out.println("Gender: " + sanitizedGender);
            System.out.println("BirthDate: " + birthDate);

            // Tạo user object với dữ liệu mới (CHỈ CẬP NHẬT PHONE, GENDER, BIRTHDATE)
            Users updatedUser = new Users();
            updatedUser.setPhoneNumber(sanitizedPhone);
            updatedUser.setGender(sanitizedGender);

            // Validate và convert birthDate
            if (birthDate != null && !birthDate.isEmpty()) {
                try {
                    LocalDate parsedDate = LocalDate.parse(birthDate);

                    // Validate: Ngày sinh không được trong tương lai
                    if (parsedDate.isAfter(LocalDate.now())) {
                        redirectAttributes.addFlashAttribute("errorMessage", "Date of birth cannot be in the future!");
                        return "redirect:/seller/profile";
                    }

                    // Validate: Phải từ 13 tuổi trở lên
                    if (parsedDate.isAfter(LocalDate.now().minusYears(13))) {
                        redirectAttributes.addFlashAttribute("errorMessage", "You must be 13 years or older!");
                        return "redirect:/seller/profile";
                    }

                    // Validate: Không quá 120 tuổi
                    if (parsedDate.isBefore(LocalDate.now().minusYears(120))) {
                        redirectAttributes.addFlashAttribute("errorMessage", "Invalid date of birth!");
                        return "redirect:/seller/profile";
                    }

                    updatedUser.setBirthDate(parsedDate);
                    System.out.println("Parsed birth date: " + parsedDate);
                } catch (Exception e) {
                    redirectAttributes.addFlashAttribute("errorMessage", "Invalid date of birth format!");
                    return "redirect:/seller/profile";
                }
            }

            // Cập nhật thông tin (SERVICE CHỈ CẬP NHẬT PHONE, GENDER, BIRTHDATE)
            Users savedUser = userProfileService.updateSellerProfile(sellerId, updatedUser);

            System.out.println("✅ Profile updated successfully");
            System.out.println("Saved phone: " + savedUser.getPhoneNumber());
            System.out.println("Saved gender: " + savedUser.getGender());
            System.out.println("Saved birth date: " + savedUser.getBirthDate());

            redirectAttributes.addFlashAttribute("successMessage", "Information updated successfully!");

        } catch (Exception e) {
            System.out.println("❌ Error updating profile: " + e.getMessage());
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("errorMessage", "Error updating information. Please try again!");
        }

        return "redirect:/seller/profile";
    }

    // ===== HELPER METHODS: VALIDATION & SANITIZATION =====

    private String sanitizeInput(String input) {
        if (input == null) return "";
        // Remove HTML tags and dangerous characters
        return input.replaceAll("<", "&lt;")
                   .replaceAll(">", "&gt;")
                   .replaceAll("\"", "&quot;")
                   .replaceAll("'", "&#x27;")
                   .replaceAll("/", "&#x2F;")
                   .trim();
    }

    private boolean isValidPhoneNumber(String phone) {
        if (phone == null || phone.isEmpty()) return false;
        // Remove spaces, dashes, parentheses
        String cleanPhone = phone.replaceAll("[\\s\\-\\(\\)\\+]", "");
        // Vietnamese phone: starts with 0, 10-11 digits
        return cleanPhone.matches("^0\\d{9,10}$");
    }

    private boolean isValidGender(String gender) {
        if (gender == null) return false;
        return gender.equals("male") || gender.equals("female") || gender.equals("other");
    }

    // === UPLOAD AVATAR VỚI ĐẦY ĐỦ VALIDATION VÀ XÓA ẢNH CŨ ===
    @PostMapping("/profile/upload-avatar")
    @ResponseBody
    public ResponseEntity<?> uploadAvatar(@RequestParam("avatar") MultipartFile file) {
        try {
            System.out.println("=== START UPLOAD AVATAR ===");
            System.out.println("File name: " + file.getOriginalFilename());
            System.out.println("File size: " + file.getSize());
            System.out.println("Content type: " + file.getContentType());

            // ===== VALIDATION 1: Check empty file =====
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Please select image file"));
            }

            // ===== VALIDATION 2: Check file type by MIME type =====
            String contentType = file.getContentType();
            if (contentType == null || !isValidImageType(contentType)) {
                System.out.println("⚠️ Invalid content type: " + contentType);
                return ResponseEntity.badRequest().body(Map.of("error", "Only image files (JPEG, PNG, GIF) can be uploaded"));
            }

            // ===== VALIDATION 3: Check file size (max 5MB) =====
            long maxSize = 5 * 1024 * 1024; // 5MB
            if (file.getSize() > maxSize) {
                System.out.println("⚠️ File too large: " + file.getSize() + " bytes");
                return ResponseEntity.badRequest().body(Map.of("error", "File size must not exceed 5MB"));
            }

            // ===== VALIDATION 4: Check file extension =====
            String originalFileName = file.getOriginalFilename();
            if (originalFileName == null || !hasValidImageExtension(originalFileName)) {
                System.out.println("⚠️ Invalid file extension: " + originalFileName);
                return ResponseEntity.badRequest().body(Map.of("error", "File must have the extension .jpg, .jpeg, .png or .gif"));
            }

            // ===== SECURITY: Validate actual file content (prevent fake extensions) =====
            byte[] fileBytes = file.getBytes();
            if (!isValidImageFile(fileBytes)) {
                System.out.println("⚠️ SECURITY ALERT: File content does not match image signature!");
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid file! Please upload a real photo."));
            }

            Long sellerId = getCurrentSellerId();
            Users currentUser = userProfileService.getSellerProfile(sellerId);

            // ===== XÓA ẢNH CŨ TRƯỚC KHI UPLOAD ẢNH MỚI =====
            String oldAvatarUrl = currentUser.getAvatarUrl();
            if (oldAvatarUrl != null && !oldAvatarUrl.isEmpty() && !oldAvatarUrl.equals("/img/avatar_default.jpg")) {
                try {
                    // Extract filename from URL (e.g., "/uploads/avatar_1_xyz.jpg" -> "avatar_1_xyz.jpg")
                    String oldFileName = oldAvatarUrl.substring(oldAvatarUrl.lastIndexOf("/") + 1);
                    Path oldFilePath = Paths.get(uploadDir).resolve(oldFileName);

                    if (Files.exists(oldFilePath)) {
                        Files.delete(oldFilePath);
                        System.out.println("🗑️ Deleted old photo: " + oldFilePath.toAbsolutePath());
                    }
                } catch (Exception e) {
                    System.out.println("⚠️ Cannot delete old photos (no impact): " + e.getMessage());
                    // Không throw exception, tiếp tục upload ảnh mới
                }
            }

            // Tạo thư mục uploads nếu chưa tồn tại
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
                System.out.println("📁 Created folder: " + uploadPath.toAbsolutePath());
            }

            // ===== SECURITY: Sanitize filename to prevent path traversal =====
            String safeFileName = sanitizeFileName(originalFileName);
            String fileExtension = safeFileName.substring(safeFileName.lastIndexOf("."));
            String fileName = "avatar_" + sellerId + "_" + System.currentTimeMillis() + fileExtension;

            // Lưu file mới
            Path filePath = uploadPath.resolve(fileName);

            // ===== SECURITY: Prevent path traversal =====
            if (!filePath.normalize().startsWith(uploadPath.normalize())) {
                System.out.println("⚠️ SECURITY ALERT: Path traversal attempt detected!");
                return ResponseEntity.badRequest().body(Map.of("error", "Detect abnormal behavior!"));
            }

            Files.copy(file.getInputStream(), filePath);
            System.out.println("💾 File saved: " + filePath.toAbsolutePath());

            // Tạo URL để truy cập ảnh
            String avatarUrl = "/uploads/" + fileName;

            // Cập nhật avatar URL trong database
            userProfileService.updateAvatar(sellerId, avatarUrl);
            System.out.println("✅ Updated avatar URL: " + avatarUrl);

            // Return JSON response
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Avatar update successful!",
                "avatarUrl", avatarUrl
            ));

        } catch (IOException e) {
            System.out.println("❌ Lỗi IOException: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", "Error saving file. Please try again!"));
        } catch (Exception e) {
            System.out.println("❌ Lỗi Exception: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", "An error occurred. Please try again!"));
        }
    }

    // ===== HELPER METHODS: FILE VALIDATION =====

    private boolean isValidImageType(String contentType) {
        return contentType.equals("image/jpeg") ||
               contentType.equals("image/jpg") ||
               contentType.equals("image/png") ||
               contentType.equals("image/gif");
    }

    private boolean hasValidImageExtension(String filename) {
        String lower = filename.toLowerCase();
        return lower.endsWith(".jpg") ||
               lower.endsWith(".jpeg") ||
               lower.endsWith(".png") ||
               lower.endsWith(".gif");
    }

    private String sanitizeFileName(String filename) {
        // Remove dangerous characters and path traversal attempts
        return filename.replaceAll("[^a-zA-Z0-9._-]", "_")
                      .replaceAll("\\.\\.", "")
                      .replaceAll("/", "")
                      .replaceAll("\\\\", "");
    }

    private boolean isValidImageFile(byte[] fileBytes) {
        if (fileBytes == null || fileBytes.length < 4) return false;

        // Check file signatures (magic numbers)
        // JPEG: FF D8 FF
        if (fileBytes[0] == (byte)0xFF && fileBytes[1] == (byte)0xD8 && fileBytes[2] == (byte)0xFF) {
            return true;
        }
        // PNG: 89 50 4E 47
        if (fileBytes[0] == (byte)0x89 && fileBytes[1] == (byte)0x50 &&
            fileBytes[2] == (byte)0x4E && fileBytes[3] == (byte)0x47) {
            return true;
        }
        // GIF: 47 49 46 38
        if (fileBytes[0] == (byte)0x47 && fileBytes[1] == (byte)0x49 &&
            fileBytes[2] == (byte)0x46 && fileBytes[3] == (byte)0x38) {
            return true;
        }

        return false;
    }

    // === ĐỔI MẬT KHẨU VỚI ĐẦY ĐỦ VALIDATION ===
    @PostMapping("/profile/change-password")
    @ResponseBody
    public ResponseEntity<?> changePassword(@RequestParam String currentPassword,
                                            @RequestParam String newPassword,
                                            @RequestParam String confirmPassword) {
        try {
            System.out.println("=== START PASSWORD CHANGE ===");

            Long sellerId = getCurrentSellerId();
            Users user = userProfileService.getSellerProfile(sellerId);

            // ===== VALIDATION 1: Check empty fields =====
            if (currentPassword == null || currentPassword.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Please enter current password"));
            }

            if (newPassword == null || newPassword.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Please enter new password"));
            }

            if (confirmPassword == null || confirmPassword.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Please confirm new password"));
            }

            // ===== VALIDATION 2: Check current password =====
            if (!userProfileService.verifyPassword(currentPassword, user.getPassword())) {
                System.out.println("⚠️ Incorrect current password attempt for user: " + user.getUsername());
                return ResponseEntity.badRequest().body(Map.of("error", "Current password is incorrect"));
            }

            // ===== VALIDATION 3: Check password length =====
            if (newPassword.length() < 6) {
                return ResponseEntity.badRequest().body(Map.of("error", "New password must be at least 6 characters"));
            }

            // ===== VALIDATION 4: Check password maximum length =====
            if (newPassword.length() > 100) {
                return ResponseEntity.badRequest().body(Map.of("error", "Password must not exceed 100 characters"));
            }

            // ===== VALIDATION 5: Check password does not contain spaces =====
            if (newPassword.contains(" ")) {
                return ResponseEntity.badRequest().body(Map.of("error", "Password cannot contain spaces"));
            }

            // ===== VALIDATION 6: Check password confirmation match =====
            if (!newPassword.equals(confirmPassword)) {
                System.out.println("⚠️ Password confirmation does not match");
                return ResponseEntity.badRequest().body(Map.of("error", "Confirmation password does not match"));
            }

            // ===== VALIDATION 7: Check if new password is same as current =====
            if (userProfileService.verifyPassword(newPassword, user.getPassword())) {
                return ResponseEntity.badRequest().body(Map.of("error", "The new password must be different from the current password."));
            }

            // ===== SECURITY: Sanitize password (prevent XSS in logs) =====
            // Don't log actual passwords, just log that change is happening

            // Đổi mật khẩu
            userProfileService.changePassword(sellerId, newPassword);

            System.out.println("✅ Password changed successfully for user: " + user.getUsername());
            System.out.println("=================================================================");
            System.out.println("=== SENDING PASSWORD CHANGE NOTIFICATION EMAIL ===");
            System.out.println("=================================================================");

            // Gửi email thông báo đổi mật khẩu thành công
            try {
                // Validate email address
                String userEmail = user.getEmail();
                if (userEmail == null || userEmail.isEmpty()) {
                    System.err.println("❌ WARNING: User email is NULL or EMPTY!");
                    System.err.println("   User ID: " + user.getUserId());
                    System.err.println("   Username: " + user.getUsername());
                    System.err.println("   Email cannot be sent without valid email address!");
                } else {
                    System.out.println("📧 User email: " + userEmail);
                    System.out.println("📧 User ID: " + user.getUserId());
                    System.out.println("📧 Username: " + user.getUsername());

                    String emailSubject = "🔒 Thông báo thay đổi mật khẩu - Ban Hang Rong";
                    String timeStamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));

                    // Format email body với line breaks rõ ràng
                    StringBuilder emailBody = new StringBuilder();
                    emailBody.append("Hello ").append(user.getUsername()).append(",\n\n");
                    emailBody.append("Your password has been changed successfully.!\n\n");
                    emailBody.append("📅 Time: ").append(timeStamp).append("\n");
                    emailBody.append("📧 Email: ").append(userEmail).append("\n");
                    emailBody.append("👤 Account: ").append(user.getUsername()).append("\n\n");
                    emailBody.append("⚠️ If you do NOT do this, please contact us IMMEDIATELY to secure your account.!\n\n");
                    emailBody.append("To ensure safety:\n");
                    emailBody.append("- Do not share your password with anyone\n");
                    emailBody.append("- Use strong and unique passwords\n");
                    emailBody.append("- Sign out of devices no longer in use\n\n");
                    emailBody.append("Best regards,\n");
                    emailBody.append("Ban Hang Rong Team\n");
                    emailBody.append("Email: bonhoangncd@gmail.com");

                    Email email = new Email(userEmail, emailSubject, emailBody.toString());

                    System.out.println("📧 Creating email object...");
                    System.out.println("   To: " + userEmail);
                    System.out.println("   Subject: " + emailSubject);
                    System.out.println("📧 Calling emailService.sendEmail()...");

                    emailService.sendEmail(email);

                    System.out.println("✅✅✅ PASSWORD CHANGE EMAIL SENT SUCCESSFULLY! ✅✅✅");
                    System.out.println("=================================================================");
                }
            } catch (Exception emailEx) {
                // Log chi tiết lỗi
                System.err.println("=================================================================");
                System.err.println("❌❌❌ FAILED TO SEND EMAIL NOTIFICATION! ❌❌❌");
                System.err.println("=================================================================");
                System.err.println("Error type: " + emailEx.getClass().getName());
                System.err.println("Error message: " + emailEx.getMessage());
                System.err.println("Stack trace:");
                emailEx.printStackTrace();
                System.err.println("=================================================================");
                // Không fail request - user vẫn đã đổi mật khẩu thành công
            }

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Password changed successfully! You will be logged out and logged in again."
            ));

        } catch (IllegalArgumentException e) {
            System.out.println("❌ Validation error: " + e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            System.out.println("❌ Error changing password: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", "An error occurred while changing your password. Please try again!"));
        }
    }
}
