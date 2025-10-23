package banhangrong.su25.Controller;

import banhangrong.su25.Entity.Products;
import banhangrong.su25.Repository.ProductImagesRepository;
import banhangrong.su25.Repository.ProductsRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.List;

@Controller
public class PageController {

    private final ProductsRepository productsRepository;
    private final ProductImagesRepository productImagesRepository;
    private final UsersRepository usersRepository;

    public PageController(ProductsRepository productsRepository, ProductImagesRepository productImagesRepository, UsersRepository usersRepository) {
        this.productsRepository = productsRepository;
        this.productImagesRepository = productImagesRepository;
        this.usersRepository = usersRepository;
    }

    @GetMapping("/")
    public String home(@RequestParam(name = "page", required = false, defaultValue = "0") int page,
                       @RequestParam(name = "size", required = false, defaultValue = "15") int size,
                       @RequestParam(name = "search", required = false) String search,
                       Model model) {
        
        // Check if user is already authenticated and redirect to appropriate dashboard
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !(auth.getPrincipal() instanceof String)) {
            try {
                String username = auth.getName();
                Users user = usersRepository.findByUsername(username).orElse(null);
                if (user != null) {
                    if ("ADMIN".equals(user.getUserType())) {
                        return "redirect:/admin/dashboard";
                    } else if ("SELLER".equals(user.getUserType())) {
                        return "redirect:/seller/dashboard";
                    } else if ("CUSTOMER".equals(user.getUserType())) {
                        if (Boolean.TRUE.equals(user.getIsEmailVerified())) {
                            return "redirect:/customer/dashboard";
                        } else {
                            return "redirect:/verify-email-required";
                        }
                    }
                }
            } catch (Exception e) {
                // If there's any error, continue to show homepage
            }
        }
        
        // Lấy sản phẩm có status = "Public" với phân trang và search
        PageRequest pageable = PageRequest.of(Math.max(page,0), Math.max(size,1),
                Sort.by(Sort.Order.desc("totalSales"), Sort.Order.desc("createdAt")));
        
        Page<Products> featuredPage;
        if (search != null && !search.trim().isEmpty()) {
            // Search mode
            featuredPage = productsRepository.findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCaseAndStatus(
                search.trim(), search.trim(), "Public", pageable);
        } else {
            // Normal mode
            featuredPage = productsRepository.findByStatus("Public", pageable);
        }
        List<Products> featured = featuredPage.getContent();
        
        // Lấy ảnh chính cho từng sản phẩm
        java.util.Map<Long, String> primaryImageByProduct = new java.util.HashMap<>();
        for (Products p : featured) {
            String url = null;
            // Prefer repository lookups to avoid lazy-loading / type issues with p.getImages()
            try {
                var primary = productImagesRepository.findTop1ByProductIdAndIsPrimaryTrueOrderByImageIdAsc(p.getProductId());
                if (primary != null && !primary.isEmpty()) {
                    url = primary.get(0).getImageUrl();
                } else {
                    var any = productImagesRepository.findTop1ByProductIdOrderByImageIdAsc(p.getProductId());
                    if (any != null && !any.isEmpty()) {
                        url = any.get(0).getImageUrl();
                    }
                }
            } catch (Exception ignored) {}
            if (url != null && !url.isBlank()) primaryImageByProduct.put(p.getProductId(), url);
        }
        
        model.addAttribute("featuredProducts", featured);
        model.addAttribute("page", featuredPage.getNumber());
        model.addAttribute("totalPages", featuredPage.getTotalPages());
        model.addAttribute("size", featuredPage.getSize());
        model.addAttribute("primaryImageByProduct", primaryImageByProduct);
        model.addAttribute("search", search);
        
        return "homepage";
    }

    @GetMapping("/login")
    public String login() {
        return "login/login";
    }

    @GetMapping("/register")
    public String register() {
        return "login/register";
    }

    @GetMapping("/forgot-password")
    public String forgotPassword() {
        return "login/forgot-password";
    }

    @GetMapping("/reset-password")
    public String resetPassword() {
        return "login/reset-password";
    }

    @GetMapping("/find-account")
    public String findAccount() {
        return "login/find-account";
    }

    @GetMapping("/verify-email-required")
    public String verifyEmailRequired() {
        return "login/verify-email-required";
    }
}
