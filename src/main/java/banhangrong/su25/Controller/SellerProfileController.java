package banhangrong.su25.Controller;

import banhangrong.su25.Entity.Users;
import banhangrong.su25.service.UserProfileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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

    // GIỮ NGUYÊN
    @PostMapping("/profile/update")
    public String updateSellerProfile(@RequestParam String username,
                                      @RequestParam String phoneNumber,
                                      @RequestParam String gender,
                                      @RequestParam(required = false) String birthDate,
                                      RedirectAttributes redirectAttributes) {
        try {
            System.out.println("=== START PROFILE UPDATE ===");
            System.out.println("Username: " + username);
            System.out.println("Phone: " + phoneNumber);
            System.out.println("Gender: " + gender);
            System.out.println("BirthDate: " + birthDate);

            Long sellerId = getCurrentSellerId();

            // Tạo user object với dữ liệu mới
            Users updatedUser = new Users();
            updatedUser.setUsername(username);
            updatedUser.setPhoneNumber(phoneNumber);
            updatedUser.setGender(gender);

            // Convert String to LocalDate nếu có
            if (birthDate != null && !birthDate.isEmpty()) {
                updatedUser.setBirthDate(LocalDate.parse(birthDate));
                System.out.println("Parsed birth date: " + updatedUser.getBirthDate());
            }

            // Cập nhật thông tin
            Users savedUser = userProfileService.updateSellerProfile(sellerId, updatedUser);

            System.out.println("✅ Profile updated successfully");
            System.out.println("Saved username: " + savedUser.getUsername());
            System.out.println("Saved phone: " + savedUser.getPhoneNumber());
            System.out.println("Saved gender: " + savedUser.getGender());
            System.out.println("Saved birth date: " + savedUser.getBirthDate());

            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật thông tin thành công!");

        } catch (Exception e) {
            System.out.println("❌ Error updating profile: " + e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
        }

        return "redirect:/seller/profile";
    }

    // === THÊM METHOD NÀY VÀO CONTROLLER ===
    @PostMapping("/profile/upload-avatar")
    public String uploadAvatar(@RequestParam("avatarFile") MultipartFile file,
                               RedirectAttributes redirectAttributes) {
        try {
            System.out.println("=== BẮT ĐẦU UPLOAD AVATAR ===");
            System.out.println("File name: " + file.getOriginalFilename());
            System.out.println("File size: " + file.getSize());
            System.out.println("Content type: " + file.getContentType());

            if (file.isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage", "Vui lòng chọn file ảnh");
                return "redirect:/seller/profile";
            }

            // Validate file type
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                redirectAttributes.addFlashAttribute("errorMessage", "Chỉ được upload file ảnh (JPEG, PNG, GIF)");
                return "redirect:/seller/profile";
            }

            // Validate file size (max 5MB)
            if (file.getSize() > 5 * 1024 * 1024) {
                redirectAttributes.addFlashAttribute("errorMessage", "Kích thước file không được vượt quá 5MB");
                return "redirect:/seller/profile";
            }

            Long sellerId = getCurrentSellerId();

            // Tạo thư mục uploads nếu chưa tồn tại
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
                System.out.println("Đã tạo thư mục: " + uploadPath.toAbsolutePath());
            }

            // Tạo tên file unique
            String originalFileName = file.getOriginalFilename();
            String fileExtension = "";
            if (originalFileName != null && originalFileName.contains(".")) {
                fileExtension = originalFileName.substring(originalFileName.lastIndexOf("."));
            }
            String fileName = "avatar_" + sellerId + "_" + UUID.randomUUID() + fileExtension;

            // Lưu file
            Path filePath = uploadPath.resolve(fileName);
            Files.copy(file.getInputStream(), filePath);
            System.out.println("Đã lưu file: " + filePath.toAbsolutePath());

            // Tạo URL để truy cập ảnh
            String avatarUrl = "/uploads/" + fileName;

            // Cập nhật avatar URL trong database
            userProfileService.updateAvatar(sellerId, avatarUrl);
            System.out.println("Đã cập nhật avatar URL: " + avatarUrl);

            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật avatar thành công!");

        } catch (IOException e) {
            System.out.println("Lỗi IOException: " + e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi upload file: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("Lỗi Exception: " + e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
        }

        return "redirect:/seller/profile";
    }
}
