package banhangrong.su25.Config;

import banhangrong.su25.Entity.Categories;
import banhangrong.su25.Entity.Users;
import banhangrong.su25.service.CartService;
import banhangrong.su25.service.CategoryViewService;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.List;

@ControllerAdvice
@Component
public class CustomerModelAttributes {

    private final CartService cartService;
    private final CategoryViewService categoryViewService;

    public CustomerModelAttributes(CartService cartService, CategoryViewService categoryViewService) {
        this.cartService = cartService;
        this.categoryViewService = categoryViewService;
    }

    @ModelAttribute
    public void addCustomerAttributes(Model model) {
        Users currentUser = categoryViewService.getCurrentUserOrNull();
        
        if (currentUser != null) {
            model.addAttribute("user", currentUser);
            
            try {
                Long cartCount = cartService.getCartCount(currentUser.getUserId());
                model.addAttribute("cartItemCount", cartCount);
            } catch (Exception e) {
                model.addAttribute("cartItemCount", 0L);
            }
        } else {
            model.addAttribute("cartItemCount", 0L);
        }
        
        try {
            List<Categories> categories = categoryViewService.listCategoriesWithPublicProducts();
            model.addAttribute("categories", categories);
        } catch (Exception e) {
            model.addAttribute("categories", java.util.Collections.emptyList());
        }
    }
}
