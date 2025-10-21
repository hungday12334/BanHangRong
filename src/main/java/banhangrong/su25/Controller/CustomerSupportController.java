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
public class CustomerSupportController {

    @Autowired
    private UsersRepository usersRepository;

    @Autowired
    private ShoppingCartRepository shoppingCartRepository;

    @GetMapping("/customer/support")
    public String support(Model model) {
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
        
        return "customer/support";
    }

    @PostMapping("/customer/support/contact")
    public String contactSupport(@RequestParam String subject,
                                @RequestParam String message,
                                @RequestParam String category) {
        // TODO: Implement contact support functionality
        return "redirect:/customer/support?sent=true";
    }

    @PostMapping("/customer/support/feedback")
    public String submitFeedback(@RequestParam String rating,
                                @RequestParam String feedback) {
        // TODO: Implement feedback submission
        return "redirect:/customer/support?feedback=true";
    }
}
