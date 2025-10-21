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
public class CustomerReviewsController {

    @Autowired
    private UsersRepository usersRepository;

    @Autowired
    private ShoppingCartRepository shoppingCartRepository;

    @GetMapping("/customer/reviews")
    public String reviews(Model model) {
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
        
        return "customer/reviews";
    }

    @PostMapping("/customer/reviews/submit")
    public String submitReview(@RequestParam Long productId,
                              @RequestParam Integer rating,
                              @RequestParam String comment) {
        // TODO: Implement review submission
        return "redirect:/customer/reviews?submitted=true";
    }

    @PostMapping("/customer/reviews/edit")
    public String editReview(@RequestParam Long reviewId,
                            @RequestParam Integer rating,
                            @RequestParam String comment) {
        // TODO: Implement review editing
        return "redirect:/customer/reviews?edited=true";
    }
}
