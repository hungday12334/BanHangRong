package banhangrong.su25.Controller;

import banhangrong.su25.Entity.Users;
import banhangrong.su25.Repository.UsersRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
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
        System.out.println("=== BECOME SELLER PAGE LOADING ===");

        // Lấy thông tin user hiện tại
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            System.out.println("⚠ User not authenticated");
            return "redirect:/login?error=Please login first";
        }

        String username = auth.getName();
        System.out.println("ℹ Username: " + username);

        Optional<Users> userOpt = usersRepository.findByUsername(username);

        if (userOpt.isEmpty()) {
            System.out.println("⚠ User not found in database");
            return "redirect:/login?error=User not found";
        }

        Users user = userOpt.get();
        System.out.println("ℹ User found: " + user.getUsername() + ", Type: " + user.getUserType());

        // Kiểm tra nếu đã là seller rồi
        if ("SELLER".equalsIgnoreCase(user.getUserType())) {
            System.out.println("✅ User is already a seller, redirecting to seller dashboard");
            return "redirect:/seller/dashboard";
        }

        model.addAttribute("user", user);
        System.out.println("✅ Returning become-seller view");
        return "customer/become-seller";
    }

    /**
     * Xử lý nâng cấp lên seller
     */
    @PostMapping("/become-seller/upgrade")
    public String upgradeToSeller(RedirectAttributes redirectAttributes,
                                  HttpServletRequest request,
                                  HttpServletResponse response,
                                  HttpSession session) {
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
            return "redirect:/login?message=Please login again as seller";
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

            System.out.println("ℹ User upgraded to SELLER: " + username);

            // Logout để Spring Security refresh authorities
            new SecurityContextLogoutHandler().logout(request, response, auth);

            redirectAttributes.addFlashAttribute("upgradeSuccess", "Chúc mừng! Bạn đã trở thành seller thành công! 🎉 Vui lòng đăng nhập lại để sử dụng các tính năng seller.");
            return "redirect:/login?upgrade=success";

        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Có lỗi xảy ra: " + e.getMessage());
            return "redirect:/customer/dashboard";
        }
    }
}


