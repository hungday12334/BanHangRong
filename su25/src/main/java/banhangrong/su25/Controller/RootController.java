package banhangrong.su25.Controller;

import banhangrong.su25.Entity.Users;
import banhangrong.su25.Repository.UsersRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Optional;

@Controller
public class RootController {

    @Autowired
    private UsersRepository usersRepository;

    @GetMapping("/")
    public String home() {
        // Kiểm tra xem user đã login chưa
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
            // User đã login, redirect về dashboard tương ứng
            try {
                Optional<Users> userOpt = usersRepository.findByUsername(auth.getName());
                if (userOpt.isPresent()) {
                    Users user = userOpt.get();
                    String userType = user.getUserType();
                    
                    if ("ADMIN".equals(userType)) {
                        return "redirect:/admin/dashboard";
                    } else if ("SELLER".equals(userType)) {
                        return "redirect:/seller/dashboard";
                    } else if ("CUSTOMER".equals(userType)) {
                        return "redirect:/customer/dashboard";
                    }
                }
            } catch (Exception e) {
                // Nếu có lỗi, fallback về login
                return "redirect:/login";
            }
        }
        
        // User chưa login hoặc có lỗi, redirect về login
        return "redirect:/login";
    }
}
