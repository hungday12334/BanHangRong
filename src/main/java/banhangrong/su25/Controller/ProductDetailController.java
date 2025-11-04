package banhangrong.su25.Controller;

import banhangrong.su25.Entity.Products;
import banhangrong.su25.Entity.Users;
import banhangrong.su25.service.ProductDetailService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class ProductDetailController {

    private final ProductDetailService productDetailService;

    public ProductDetailController(ProductDetailService productDetailService) {
        this.productDetailService = productDetailService;
    }

    @GetMapping("/product/{id}")
    public String productDetail(@PathVariable("id") Long id, Model model) {
        // Get current user for header (via Service)
        Users currentUser = productDetailService.getCurrentUserOrNull();
        
        // Get product details
        Products p = productDetailService.getProductById(id);
        if (p == null) return "redirect:/customer/dashboard";
        
        model.addAttribute("product", p);
        model.addAttribute("images", productDetailService.getPrimaryImages(id));
        model.addAttribute("reviews", productDetailService.getReviewsByProduct(id));
        
        // Add user data for header
        if (currentUser != null) {
            model.addAttribute("user", currentUser);
            try {
                model.addAttribute("cartCount", productDetailService.getCartCount(currentUser.getUserId()));
            } catch (Exception ignored) {}
        }
        
        return "customer/product_detail";
    }
}


