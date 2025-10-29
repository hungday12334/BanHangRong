package banhangrong.su25.Controller;

import banhangrong.su25.Entity.Users;
import banhangrong.su25.service.UserProfileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
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
import java.util.Map;
import java.util.UUID;

@Controller
@RequestMapping("/seller")
public class SellerProfileController {

    @Autowired
    private UserProfileService userProfileService;

    @Value("${file.upload-dir:uploads}")
    private String uploadDir;

    private Long getCurrentSellerId() {
        return 1L;
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
            model.addAttribute("error", "Không tìm thấy thông tin seller");
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
                redirectAttributes.addFlashAttribute("errorMessage", "Không được phép thay đổi tên người dùng!");
                return "redirect:/seller/profile";
            }

            if (email != null && !email.equals(currentUser.getEmail())) {
                System.out.println("⚠️ SECURITY ALERT: Attempt to change email detected!");
                redirectAttributes.addFlashAttribute("errorMessage", "Không được phép thay đổi email!");
                return "redirect:/seller/profile";
            }

            // ===== VALIDATION: XSS Protection - Sanitize inputs =====
            String sanitizedPhone = sanitizeInput(phoneNumber);
            String sanitizedGender = sanitizeInput(gender);

            // Validate phone number format (Vietnamese phone: 10-11 digits)
            if (!isValidPhoneNumber(sanitizedPhone)) {
                redirectAttributes.addFlashAttribute("errorMessage", "Số điện thoại không hợp lệ! Vui lòng nhập đúng định dạng.");
                return "redirect:/seller/profile";
            }

            // Validate gender
            if (!isValidGender(sanitizedGender)) {
                redirectAttributes.addFlashAttribute("errorMessage", "Giới tính không hợp lệ!");
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
                        redirectAttributes.addFlashAttribute("errorMessage", "Ngày sinh không được ở tương lai!");
                        return "redirect:/seller/profile";
                    }

                    // Validate: Phải từ 13 tuổi trở lên
                    if (parsedDate.isAfter(LocalDate.now().minusYears(13))) {
                        redirectAttributes.addFlashAttribute("errorMessage", "Bạn phải từ 13 tuổi trở lên!");
                        return "redirect:/seller/profile";
                    }

                    // Validate: Không quá 120 tuổi
                    if (parsedDate.isBefore(LocalDate.now().minusYears(120))) {
                        redirectAttributes.addFlashAttribute("errorMessage", "Ngày sinh không hợp lệ!");
                        return "redirect:/seller/profile";
                    }

                    updatedUser.setBirthDate(parsedDate);
                    System.out.println("Parsed birth date: " + parsedDate);
                } catch (Exception e) {
                    redirectAttributes.addFlashAttribute("errorMessage", "Định dạng ngày sinh không hợp lệ!");
                    return "redirect:/seller/profile";
                }
            }

            // Cập nhật thông tin (SERVICE CHỈ CẬP NHẬT PHONE, GENDER, BIRTHDATE)
            Users savedUser = userProfileService.updateSellerProfile(sellerId, updatedUser);

            System.out.println("✅ Profile updated successfully");
            System.out.println("Saved phone: " + savedUser.getPhoneNumber());
            System.out.println("Saved gender: " + savedUser.getGender());
            System.out.println("Saved birth date: " + savedUser.getBirthDate());

            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật thông tin thành công!");

        } catch (Exception e) {
            System.out.println("❌ Error updating profile: " + e.getMessage());
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi cập nhật thông tin. Vui lòng thử lại!");
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
            System.out.println("=== BẮT ĐẦU UPLOAD AVATAR ===");
            System.out.println("File name: " + file.getOriginalFilename());
            System.out.println("File size: " + file.getSize());
            System.out.println("Content type: " + file.getContentType());

            // ===== VALIDATION 1: Check empty file =====
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Vui lòng chọn file ảnh"));
            }

            // ===== VALIDATION 2: Check file type by MIME type =====
            String contentType = file.getContentType();
            if (contentType == null || !isValidImageType(contentType)) {
                System.out.println("⚠️ Invalid content type: " + contentType);
                return ResponseEntity.badRequest().body(Map.of("error", "Chỉ được upload file ảnh (JPEG, PNG, GIF)"));
            }

            // ===== VALIDATION 3: Check file size (max 5MB) =====
            long maxSize = 5 * 1024 * 1024; // 5MB
            if (file.getSize() > maxSize) {
                System.out.println("⚠️ File too large: " + file.getSize() + " bytes");
                return ResponseEntity.badRequest().body(Map.of("error", "Kích thước file không được vượt quá 5MB"));
            }

            // ===== VALIDATION 4: Check file extension =====
            String originalFileName = file.getOriginalFilename();
            if (originalFileName == null || !hasValidImageExtension(originalFileName)) {
                System.out.println("⚠️ Invalid file extension: " + originalFileName);
                return ResponseEntity.badRequest().body(Map.of("error", "File phải có đuôi .jpg, .jpeg, .png hoặc .gif"));
            }

            // ===== SECURITY: Validate actual file content (prevent fake extensions) =====
            byte[] fileBytes = file.getBytes();
            if (!isValidImageFile(fileBytes)) {
                System.out.println("⚠️ SECURITY ALERT: File content does not match image signature!");
                return ResponseEntity.badRequest().body(Map.of("error", "File không hợp lệ! Vui lòng upload ảnh thật."));
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
                        System.out.println("🗑️ Đã xóa ảnh cũ: " + oldFilePath.toAbsolutePath());
                    }
                } catch (Exception e) {
                    System.out.println("⚠️ Không thể xóa ảnh cũ (không ảnh hưởng): " + e.getMessage());
                    // Không throw exception, tiếp tục upload ảnh mới
                }
            }

            // Tạo thư mục uploads nếu chưa tồn tại
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
                System.out.println("📁 Đã tạo thư mục: " + uploadPath.toAbsolutePath());
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
                return ResponseEntity.badRequest().body(Map.of("error", "Phát hiện hành vi bất thường!"));
            }

            Files.copy(file.getInputStream(), filePath);
            System.out.println("💾 Đã lưu file: " + filePath.toAbsolutePath());

            // Tạo URL để truy cập ảnh
            String avatarUrl = "/uploads/" + fileName;

            // Cập nhật avatar URL trong database
            userProfileService.updateAvatar(sellerId, avatarUrl);
            System.out.println("✅ Đã cập nhật avatar URL: " + avatarUrl);

            // Return JSON response
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Cập nhật avatar thành công!",
                "avatarUrl", avatarUrl
            ));

        } catch (IOException e) {
            System.out.println("❌ Lỗi IOException: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", "Lỗi khi lưu file. Vui lòng thử lại!"));
        } catch (Exception e) {
            System.out.println("❌ Lỗi Exception: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", "Có lỗi xảy ra. Vui lòng thử lại!"));
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
    public String changePassword(@RequestParam String currentPassword,
                                 @RequestParam String newPassword,
                                 @RequestParam String confirmPassword,
                                 RedirectAttributes redirectAttributes) {
        try {
            System.out.println("=== START PASSWORD CHANGE ===");

            Long sellerId = getCurrentSellerId();
            Users user = userProfileService.getSellerProfile(sellerId);

            // ===== VALIDATION 1: Check empty fields =====
            if (currentPassword == null || currentPassword.isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage", "Vui lòng nhập mật khẩu hiện tại");
                return "redirect:/seller/profile";
            }

            if (newPassword == null || newPassword.isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage", "Vui lòng nhập mật khẩu mới");
                return "redirect:/seller/profile";
            }

            if (confirmPassword == null || confirmPassword.isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage", "Vui lòng xác nhận mật khẩu mới");
                return "redirect:/seller/profile";
            }

            // ===== VALIDATION 2: Check current password =====
            if (!userProfileService.verifyPassword(currentPassword, user.getPassword())) {
                System.out.println("⚠️ Incorrect current password attempt for user: " + user.getUsername());
                redirectAttributes.addFlashAttribute("errorMessage", "Mật khẩu hiện tại không đúng");
                return "redirect:/seller/profile";
            }

            // ===== VALIDATION 3: Check password length =====
            if (newPassword.length() < 6) {
                redirectAttributes.addFlashAttribute("errorMessage", "Mật khẩu mới phải có ít nhất 6 ký tự");
                return "redirect:/seller/profile";
            }

            // ===== VALIDATION 4: Check password maximum length =====
            if (newPassword.length() > 100) {
                redirectAttributes.addFlashAttribute("errorMessage", "Mật khẩu không được vượt quá 100 ký tự");
                return "redirect:/seller/profile";
            }

            // ===== VALIDATION 5: Check password confirmation match =====
            if (!newPassword.equals(confirmPassword)) {
                System.out.println("⚠️ Password confirmation does not match");
                redirectAttributes.addFlashAttribute("errorMessage", "Mật khẩu xác nhận không khớp");
                return "redirect:/seller/profile";
            }

            // ===== VALIDATION 6: Check if new password is same as current =====
            if (userProfileService.verifyPassword(newPassword, user.getPassword())) {
                redirectAttributes.addFlashAttribute("errorMessage", "Mật khẩu mới phải khác mật khẩu hiện tại");
                return "redirect:/seller/profile";
            }

            // ===== SECURITY: Sanitize password (prevent XSS in logs) =====
            // Don't log actual passwords, just log that change is happening

            // Đổi mật khẩu
            userProfileService.changePassword(sellerId, newPassword);

            System.out.println("✅ Password changed successfully for user: " + user.getUsername());
            redirectAttributes.addFlashAttribute("successMessage", "Đổi mật khẩu thành công!");

        } catch (IllegalArgumentException e) {
            System.out.println("❌ Validation error: " + e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            System.out.println("❌ Error changing password: " + e.getMessage());
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("errorMessage", "Có lỗi xảy ra khi đổi mật khẩu. Vui lòng thử lại!");
        }

        return "redirect:/seller/profile";
    }
}
