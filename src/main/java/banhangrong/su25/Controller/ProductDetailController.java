package banhangrong.su25.Controller;

import banhangrong.su25.Entity.Products;
import banhangrong.su25.Repository.ProductsRepository;
import banhangrong.su25.Repository.ProductImagesRepository;
import banhangrong.su25.Repository.ProductReviewsRepository;
import banhangrong.su25.Repository.UsersRepository;
import banhangrong.su25.Repository.ShoppingCartRepository;
import banhangrong.su25.Entity.Users;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Collections;
import java.util.List;

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
    public String productDetail(@PathVariable("id") String idStr, Model model) {
        try {
            if (idStr == null || idStr.trim().isEmpty()) {
                return "redirect:/customer/dashboard";
            }

            Long id;
            try {
                id = Long.parseLong(idStr);
            } catch (NumberFormatException e) {
                return "redirect:/customer/dashboard";
            }

            if (id == null || id <= 0) {
                return "redirect:/customer/dashboard";
            }

            Products p = productsRepository.findById(id).orElse(null);
            if (p == null) {
                return "redirect:/customer/dashboard";
            }

            model.addAttribute("product", p);

            // Header data from DB (match dashboard)
            try {
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                Users currentUser = null;
                if (auth != null && auth.isAuthenticated()) {
                    currentUser = usersRepository.findByUsername(auth.getName()).orElse(null);
                }
                if (currentUser != null) {
                    model.addAttribute("user", currentUser);
                    try { model.addAttribute("cartCount", shoppingCartRepository.countByUserId(currentUser.getUserId())); } catch (Exception ignored) {}
                }
            } catch (Exception ignored) {}

            try {
                List<?> images = productImagesRepository.findTop1ByProductIdAndIsPrimaryTrueOrderByImageIdAsc(id);
                model.addAttribute("images", images != null ? images : Collections.emptyList());
            } catch (Exception e) {
                model.addAttribute("images", Collections.emptyList());
            }

            try {
                List<?> reviews = productReviewsRepository.findByProductIdOrderByCreatedAtDesc(id);
                model.addAttribute("reviews", reviews != null ? reviews : Collections.emptyList());
            } catch (Exception e) {
                model.addAttribute("reviews", Collections.emptyList());
            }

            return "customer/product_detail";
        } catch (Exception e) {
            e.printStackTrace();
            return "redirect:/customer/dashboard";
        }
    }
}


