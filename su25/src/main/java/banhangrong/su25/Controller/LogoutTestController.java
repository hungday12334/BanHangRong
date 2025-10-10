package banhangrong.su25.Controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/test-logout")
public class LogoutTestController {

    @GetMapping
    public String testLogout(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        
        if (auth == null || !auth.isAuthenticated()) {
            model.addAttribute("message", "No user authenticated");
            model.addAttribute("authenticated", false);
        } else {
            model.addAttribute("message", "User authenticated: " + auth.getName());
            model.addAttribute("authenticated", true);
            model.addAttribute("authorities", auth.getAuthorities());
        }
        
        return "test-logout";
    }
}
