package banhangrong.su25.Controller;

import banhangrong.su25.Entity.Products;
import banhangrong.su25.Entity.ProductImages;
import banhangrong.su25.Entity.Users;
import banhangrong.su25.Repository.ProductImagesRepository;
import banhangrong.su25.Repository.ProductsRepository;
import banhangrong.su25.Repository.UsersRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@Controller
public class PageController {

    private static final Logger logger = LoggerFactory.getLogger(PageController.class);

    private final ProductsRepository productsRepository;
    private final ProductImagesRepository productImagesRepository;
    private final UsersRepository usersRepository;

    public PageController(ProductsRepository productsRepository, ProductImagesRepository productImagesRepository, UsersRepository usersRepository) {
        this.productsRepository = productsRepository;
        this.productImagesRepository = productImagesRepository;
        this.usersRepository = usersRepository;
    }

    // Extracted helper: returns redirect view (e.g. "redirect:/admin/dashboard") or null to continue
    private String getRedirectForAuthenticatedUser(Authentication auth) {
        if (auth == null || !auth.isAuthenticated() || (auth.getPrincipal() instanceof String)) return null;
        try {
            String username = auth.getName();
            Users user = usersRepository.findByUsername(username).orElse(null);
            if (user == null) return null;
            switch (user.getUserType()) {
                case "ADMIN": return "redirect:/admin/dashboard";
                case "SELLER": return "redirect:/seller/dashboard";
                case "CUSTOMER":
                    return Boolean.TRUE.equals(user.getIsEmailVerified()) ? "redirect:/customer/dashboard" : "redirect:/verify-email-required";
                default: return null;
            }
        } catch (Exception e) {
            logger.debug("Error checking authenticated user for homepage redirect", e);
            return null;
        }
    }

    // Extracted helper: obtain primary image URL for a product or null
    private String getPrimaryImageUrl(Long productId) {
        if (productId == null) return null;
        try {
            List<ProductImages> primary = productImagesRepository.findTop1ByProductIdAndIsPrimaryTrueOrderByImageIdAsc(productId);
            String url = primary.stream().findFirst().map(ProductImages::getImageUrl).orElse(null);
            if (url == null) {
                List<ProductImages> any = productImagesRepository.findTop1ByProductIdOrderByImageIdAsc(productId);
                url = any.stream().findFirst().map(ProductImages::getImageUrl).orElse(null);
            }
            return url;
        } catch (Exception e) {
            logger.warn("Failed to fetch images for productId={}" , productId, e);
            return null;
        }
    }

    @GetMapping("/")
    public String home(@RequestParam(name = "page", required = false, defaultValue = "0") int page,
                       @RequestParam(name = "size", required = false, defaultValue = "15") int size,
                       @RequestParam(name = "search", required = false) String search,
                       Model model) {
        // Check if user is already authenticated and redirect to appropriate dashboard
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String redirect = getRedirectForAuthenticatedUser(auth);
        if (redirect != null) return redirect;

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
            String url = getPrimaryImageUrl(p.getProductId());
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
