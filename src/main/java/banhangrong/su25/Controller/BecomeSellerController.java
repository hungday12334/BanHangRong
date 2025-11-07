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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
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
    @Transactional
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

        // Kiểm tra checkbox đồng ý điều khoản
        String agreeTerms = request.getParameter("agreeTerms");
        if (agreeTerms == null || !"on".equals(agreeTerms)) {
            redirectAttributes.addFlashAttribute("error", "Vui lòng đồng ý với điều khoản để tiếp tục");
            return "redirect:/become-seller";
        }

        // Kiểm tra số dư - cần 200,000 VNĐ
        BigDecimal requiredAmount = new BigDecimal("200000");
        BigDecimal currentBalance = user.getBalance() != null ? user.getBalance() : BigDecimal.ZERO;
        
        if (currentBalance.compareTo(requiredAmount) < 0) {
            // Không đủ tiền, trả về trang với thông báo
            redirectAttributes.addFlashAttribute("insufficientBalance", true);
            redirectAttributes.addFlashAttribute("currentBalance", currentBalance);
            redirectAttributes.addFlashAttribute("requiredAmount", requiredAmount);
            return "redirect:/become-seller";
        }

        try {
            // Reload user từ database trong transaction để đảm bảo có dữ liệu mới nhất
            Users userToUpdate = usersRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found: " + username));
            
            // QUAN TRỌNG: Lưu password hiện tại để đảm bảo không bị mất khi save
            String currentPassword = userToUpdate.getPassword();
            if (currentPassword == null || currentPassword.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Lỗi: Password không hợp lệ. Vui lòng liên hệ admin.");
                return "redirect:/customer/dashboard";
            }
            
            // Lấy số dư mới nhất từ database
            BigDecimal latestBalance = userToUpdate.getBalance() != null ? userToUpdate.getBalance() : BigDecimal.ZERO;
            
            // Trừ tiền từ số dư
            BigDecimal newBalance = latestBalance.subtract(requiredAmount);
            userToUpdate.setBalance(newBalance);
            
            // Nâng cấp role lên SELLER
            userToUpdate.setUserType("SELLER");
            userToUpdate.setUpdatedAt(LocalDateTime.now());
            
            // Đảm bảo password không bị mất
            userToUpdate.setPassword(currentPassword);
            
            // Save và flush ngay để đảm bảo balance được cập nhật vào database
            usersRepository.saveAndFlush(userToUpdate);

            // Lấy tên user để hiển thị trong thông báo
            String displayName = userToUpdate.getFullName() != null && !userToUpdate.getFullName().trim().isEmpty() 
                ? userToUpdate.getFullName() 
                : userToUpdate.getUsername();

            // Logout để Spring Security refresh authorities
            new SecurityContextLogoutHandler().logout(request, response, auth);

            // Thông báo chúc mừng với tên user
            String successMessage = String.format("Chúc mừng %s đã trở thành seller thành công! 🎉 Vui lòng đăng nhập lại để sử dụng các tính năng seller.", displayName);
            redirectAttributes.addFlashAttribute("upgradeSuccess", successMessage);
            return "redirect:/login?upgrade=success";

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Có lỗi xảy ra: " + e.getMessage());
            return "redirect:/customer/dashboard";
        }
    }
}


