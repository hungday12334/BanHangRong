package banhangrong.su25.Controller;

import banhangrong.su25.Entity.Users;
import banhangrong.su25.Repository.UsersRepository;
import banhangrong.su25.Repository.ShoppingCartRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Optional;

@Controller
public class CustomerWishlistController {

    @Autowired
    private UsersRepository usersRepository;

    @Autowired
    private ShoppingCartRepository shoppingCartRepository;

    @GetMapping("/customer/wishlist")
    public String wishlist(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        Optional<Users> userOptional = usersRepository.findByUsername(username);
        
        if (userOptional.isEmpty()) {
            return "redirect:/login";
        }
        
        Users user = userOptional.get();
        
        // Get cart count
        Long cartCount = shoppingCartRepository.countByUserId(user.getUserId());
        
        model.addAttribute("user", user);
        model.addAttribute("cartCount", cartCount);
        
        return "customer/wishlist";
    }

    @PostMapping("/customer/wishlist/add")
    public String addToWishlist(@RequestParam Long productId) {
        // TODO: Implement add to wishlist
        return "redirect:/customer/wishlist";
    }

    @PostMapping("/customer/wishlist/remove")
    public String removeFromWishlist(@RequestParam Long productId) {
        // TODO: Implement remove from wishlist
        return "redirect:/customer/wishlist";
    }
}
