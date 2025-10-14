package banhangrong.su25.Controller;

import banhangrong.su25.Entity.Users;
import banhangrong.su25.service.UserProfileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/seller")
public class SellerProfileController {

    @Autowired
    private UserProfileService userProfileService;

    private Long getCurrentSellerId() {
        return 1L;
    }

    @GetMapping("/profile")
    public String viewSellerProfile(Model model) {
        try {
            Long sellerId = getCurrentSellerId();
            Users user = userProfileService.getSellerProfile(sellerId); // Đổi tên biến
            model.addAttribute("user", user); // SỬA: "user" thay vì "seller"
            model.addAttribute("sellerId", sellerId);
        } catch (Exception e) {
            model.addAttribute("error", "Không tìm thấy thông tin seller");
        }
        return "seller/profile"; // Đảm bảo template tồn tại
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
    public String updateSellerProfile(@ModelAttribute("seller") Users updatedSeller,
                                      RedirectAttributes redirectAttributes) {
        try {
            Long sellerId = getCurrentSellerId();
            userProfileService.updateSellerProfile(sellerId, updatedSeller);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật thông tin thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
        }
        return "redirect:/seller/profile";
    }
}
