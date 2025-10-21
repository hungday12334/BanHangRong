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
public class CustomerSettingsController {

    @Autowired
    private UsersRepository usersRepository;

    @Autowired
    private ShoppingCartRepository shoppingCartRepository;

    @GetMapping("/customer/settings")
    public String settings(Model model) {
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
        
        return "customer/settings";
    }

    @PostMapping("/customer/settings/update")
    public String updateSettings(@RequestParam String email,
                                @RequestParam String phoneNumber,
                                @RequestParam String language,
                                @RequestParam String timezone) {
        // TODO: Implement settings update
        return "redirect:/customer/settings?updated=true";
    }

    @PostMapping("/customer/settings/notifications")
    public String updateNotificationSettings(@RequestParam(required = false) Boolean emailNotifications,
                                           @RequestParam(required = false) Boolean smsNotifications,
                                           @RequestParam(required = false) Boolean pushNotifications) {
        // TODO: Implement notification settings update
        return "redirect:/customer/settings?notifications=true";
    }
}
