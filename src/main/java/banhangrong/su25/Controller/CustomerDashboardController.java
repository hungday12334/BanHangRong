package banhangrong.su25.Controller;

import banhangrong.su25.Entity.Products;
import banhangrong.su25.Repository.ProductImagesRepository;
import banhangrong.su25.Repository.ProductsRepository;
import banhangrong.su25.Repository.UsersRepository;
import banhangrong.su25.Entity.Users;
import banhangrong.su25.Repository.ShoppingCartRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.List;

@Controller
public class CustomerDashboardController {

    private final ProductsRepository productsRepository;
    private final ProductImagesRepository productImagesRepository;
    private final ShoppingCartRepository shoppingCartRepository;
    private final UsersRepository usersRepository;

    public CustomerDashboardController(ProductsRepository productsRepository, ProductImagesRepository productImagesRepository, ShoppingCartRepository shoppingCartRepository, UsersRepository usersRepository) {
        this.productsRepository = productsRepository;
        this.productImagesRepository = productImagesRepository;
        this.shoppingCartRepository = shoppingCartRepository;
        this.usersRepository = usersRepository;
    }

    @GetMapping("/customer/dashboard")
    public String customerDashboard(@RequestParam(name = "page", required = false, defaultValue = "0") int page,
                                    @RequestParam(name = "size", required = false, defaultValue = "15") int size,
                                    @RequestParam(name = "search", required = false) String search,
                                    Model model) {
        // Kiểm tra email verified cho CUSTOMER
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Users currentUser = null;
        if (auth != null && auth.isAuthenticated()) {
            String username = auth.getName();
            currentUser = usersRepository.findByUsername(username).orElse(null);
            if (currentUser != null && "CUSTOMER".equals(currentUser.getUserType())) {
                if (!Boolean.TRUE.equals(currentUser.getIsEmailVerified())) {
                    return "redirect:/verify-email-required";
                }
            }
        }
        
        // ORM: paginated public products ordered by total sales desc then created_at desc
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
        // Derive primary image directly from entity relations
        java.util.Map<Long, String> primaryImageByProduct = new java.util.HashMap<>();
        for (Products p : featured) {
            String url = null;
            // Try repository lookups for primary image first, then any image
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
        // Use logged-in user info for header
        try {
            if (currentUser != null) {
                model.addAttribute("cartCount", shoppingCartRepository.countByUserId(currentUser.getUserId()));
                model.addAttribute("user", currentUser);
            }
        } catch (Exception ignored) {}
        return "customer/dashboard";
    }
}


