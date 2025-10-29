package banhangrong.su25.Controller;

import banhangrong.su25.Entity.Users;
import banhangrong.su25.Repository.UsersRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.Optional;

@Controller
public class BecomeSellerController {

    @Autowired
    private UsersRepository usersRepository;

    /**
     * Trang xác nhận nâng cấp lên seller
     */
    @GetMapping("/become-seller")
    public String becomeSellerPage(Model model, HttpSession session) {
        // Lấy thông tin user hiện tại
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return "redirect:/login?error=Please login first";
        }

        String username = auth.getName();
        Optional<Users> userOpt = usersRepository.findByUsername(username);
        
        if (userOpt.isEmpty()) {
            return "redirect:/login?error=User not found";
        }

        Users user = userOpt.get();
        
        // Kiểm tra nếu đã là seller rồi
        if ("SELLER".equalsIgnoreCase(user.getUserType())) {
            return "redirect:/seller/dashboard";
        }

        model.addAttribute("user", user);
        return "customer/become-seller";
    }

    /**
     * Xử lý nâng cấp lên seller
     */
    @PostMapping("/become-seller/upgrade")
    public String upgradeToSeller(RedirectAttributes redirectAttributes, HttpSession session) {
        // Lấy thông tin user hiện tại
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            redirectAttributes.addFlashAttribute("error", "Vui lòng đăng nhập trước");
            return "redirect:/login";
        }

        String username = auth.getName();
        Optional<Users> userOpt = usersRepository.findByUsername(username);
        
        if (userOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Không tìm thấy người dùng");
            return "redirect:/customer/dashboard";
        }

        Users user = userOpt.get();
        
        // Kiểm tra nếu đã là seller rồi
        if ("SELLER".equalsIgnoreCase(user.getUserType())) {
            redirectAttributes.addFlashAttribute("info", "Bạn đã là seller rồi!");
            return "redirect:/seller/dashboard";
        }

        // Kiểm tra email verification
        if (!Boolean.TRUE.equals(user.getIsEmailVerified())) {
            redirectAttributes.addFlashAttribute("error", "Vui lòng xác thực email trước khi trở thành seller");
            return "redirect:/customer/dashboard";
        }

        try {
            // Nâng cấp role lên SELLER
            user.setUserType("SELLER");
            user.setUpdatedAt(LocalDateTime.now());
            usersRepository.save(user);

            // Cập nhật session
            session.setAttribute("userRole", "SELLER");
            
            redirectAttributes.addFlashAttribute("success", "Chúc mừng! Bạn đã trở thành seller thành công! 🎉");
            return "redirect:/seller/dashboard";
            
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Có lỗi xảy ra: " + e.getMessage());
            return "redirect:/customer/dashboard";
        }
    }
}

