package banhangrong.su25.Controller;

import banhangrong.su25.Entity.Products;
import banhangrong.su25.Entity.Users;
import banhangrong.su25.Repository.ProductsRepository;
import banhangrong.su25.Repository.ProductImagesRepository;
import banhangrong.su25.Repository.ProductReviewsRepository;
import banhangrong.su25.Repository.UsersRepository;
import banhangrong.su25.Repository.ShoppingCartRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class ProductDetailController {

    private final ProductsRepository productsRepository;
    private final ProductImagesRepository productImagesRepository;
    private final ProductReviewsRepository productReviewsRepository;
    private final UsersRepository usersRepository;
    private final ShoppingCartRepository shoppingCartRepository;

    public ProductDetailController(ProductsRepository productsRepository,
                                   ProductImagesRepository productImagesRepository,
                                   ProductReviewsRepository productReviewsRepository,
                                   UsersRepository usersRepository,
                                   ShoppingCartRepository shoppingCartRepository) {
        this.productsRepository = productsRepository;
        this.productImagesRepository = productImagesRepository;
        this.productReviewsRepository = productReviewsRepository;
        this.usersRepository = usersRepository;
        this.shoppingCartRepository = shoppingCartRepository;
    }

    @GetMapping("/product/{id}")
    public String productDetail(@PathVariable("id") Long id, Model model) {
        // Get current user for header
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Users currentUser = null;
        if (auth != null && auth.isAuthenticated()) {
            String username = auth.getName();
            currentUser = usersRepository.findByUsername(username).orElse(null);
        }
        
        // Get product details
        Products p = productsRepository.findById(id).orElse(null);
        if (p == null) return "redirect:/customer/dashboard";
        
        model.addAttribute("product", p);
        model.addAttribute("images", productImagesRepository.findTop1ByProductIdAndIsPrimaryTrueOrderByImageIdAsc(id));
        model.addAttribute("reviews", productReviewsRepository.findByProductIdOrderByCreatedAtDesc(id));
        
        // Add user data for header
        if (currentUser != null) {
            model.addAttribute("user", currentUser);
            try {
                model.addAttribute("cartCount", shoppingCartRepository.countByUserId(currentUser.getUserId()));
            } catch (Exception ignored) {}
        }
        
        return "customer/product_detail";
    }
}


