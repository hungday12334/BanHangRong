package banhangrong.su25.Controller;

import banhangrong.su25.Entity.Products;
import banhangrong.su25.Entity.Users;
import banhangrong.su25.service.CustomerDashboardService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Map;

@Controller
public class CustomerDashboardController {

    // Inject Service - Controller CHỈ dùng Service, KHÔNG dùng Repository trực tiếp
    private final CustomerDashboardService customerDashboardService;

    // Constructor injection - Spring tự động inject Service
    public CustomerDashboardController(CustomerDashboardService customerDashboardService) {
        this.customerDashboardService = customerDashboardService;
    }

    @GetMapping("/customer/dashboard")
    public String customerDashboard(
            @RequestParam(name = "page", required = false, defaultValue = "0") int page,
            @RequestParam(name = "size", required = false, defaultValue = "15") int size,
            @RequestParam(name = "search", required = false) String search,
            Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Users currentUser = null;
        if (auth != null && auth.isAuthenticated()) {
            String username = auth.getName();
            currentUser = customerDashboardService.getUserByUsername(username);
            if (currentUser != null) {
                if (!customerDashboardService.isCustomerEmailVerified(currentUser)) {
                    return "redirect:/verify-email-required";
                }
            }
        }
        Page<Products> featuredPage = customerDashboardService.getPublicProducts(page, size, search);
        List<Products> featured = featuredPage.getContent();
        Map<Long, String> primaryImageByProduct = customerDashboardService.getProductImages(featured);
        model.addAttribute("featuredProducts", featured);
        model.addAttribute("page", featuredPage.getNumber());
        model.addAttribute("totalPages", featuredPage.getTotalPages());
        model.addAttribute("size", featuredPage.getSize());
        model.addAttribute("primaryImageByProduct", primaryImageByProduct);
        model.addAttribute("search", search);
        if (currentUser != null) {
            Long cartCount = customerDashboardService.getCartCount(currentUser.getUserId());
            model.addAttribute("cartCount", cartCount);
            model.addAttribute("user", currentUser);
        }
        return "customer/dashboard";
    }

    @GetMapping("/rating-history")
    public String ratingHistory() {
        return "redirect:/customer/reviews";
    }

    @GetMapping("/orderhistory")
    public String orderHistory(@RequestParam(name = "page", required = false, defaultValue = "0") int page,
                               @RequestParam(name = "size", required = false, defaultValue = "3") int size,
                               @RequestParam(name = "search", required = false) String search,
                               @RequestParam(name = "status", required = false) String status,
                               Model model) {
        return "redirect:/customer/dashboard";
    }


    @GetMapping("/notification")
    public String notification(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return "redirect:/login";
        }
        String username = auth.getName();
        Users user = customerDashboardService.getUserByUsername(username);
        if (user == null) {
            return "redirect:/login";
        }
        Long cartCount = customerDashboardService.getCartCount(user.getUserId());
        model.addAttribute("user", user);
        model.addAttribute("cartCount", cartCount);
        return "customer/notification";
    }

    @GetMapping("/customer/seller/{sellerId}")
    public String viewSeller(@PathVariable Long sellerId, Model model) {
        return "redirect:/customer/dashboard";
    }
}


