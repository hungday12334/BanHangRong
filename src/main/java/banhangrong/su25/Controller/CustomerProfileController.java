package banhangrong.su25.Controller;

import banhangrong.su25.Entity.Users;
import banhangrong.su25.Repository.ShoppingCartRepository;
import banhangrong.su25.Repository.UsersRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class CustomerProfileController {

    private final UsersRepository usersRepository;
    private final ShoppingCartRepository shoppingCartRepository;

    public CustomerProfileController(UsersRepository usersRepository, ShoppingCartRepository shoppingCartRepository) {
        this.usersRepository = usersRepository;
        this.shoppingCartRepository = shoppingCartRepository;
    }

    @GetMapping("/customer/profile/{username}")
    public String profile(@PathVariable("username") String username, Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Users currentUser = null;
        if (auth != null && auth.isAuthenticated()) {
            currentUser = usersRepository.findByUsername(auth.getName()).orElse(null);
        }

        Users profileUser = usersRepository.findByUsername(username).orElse(null);
        if (profileUser == null) {
            // Fallback: if not found, redirect to dashboard
            return "redirect:/customer/dashboard";
        }

        // Header data
        if (currentUser != null) {
            try { model.addAttribute("cartCount", shoppingCartRepository.countByUserId(currentUser.getUserId())); } catch (Exception ignored) {}
            model.addAttribute("user", currentUser);
        }

        model.addAttribute("profileUser", profileUser);
        return "customer/profile";
    }

    @GetMapping("/customer/profile/{username}/edit")
    public String editProfileForm(@PathVariable("username") String username, Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Users currentUser = null;
        if (auth != null && auth.isAuthenticated()) {
            currentUser = usersRepository.findByUsername(auth.getName()).orElse(null);
        }
        if (currentUser == null || !username.equalsIgnoreCase(currentUser.getUsername())) {
            return "redirect:/customer/profile/" + username;
        }

        try { model.addAttribute("cartCount", shoppingCartRepository.countByUserId(currentUser.getUserId())); } catch (Exception ignored) {}
        model.addAttribute("user", currentUser);
        model.addAttribute("profileUser", currentUser);
        return "customer/profile-edit";
    }

    @PostMapping("/customer/profile/{username}/edit")
    public String editProfileSubmit(@PathVariable("username") String username,
                                    @RequestParam(name = "avatarUrl", required = false) String avatarUrl,
                                    @RequestParam(name = "email", required = false) String email,
                                    @RequestParam(name = "phoneNumber", required = false) String phoneNumber,
                                    @RequestParam(name = "gender", required = false) String gender,
                                    @RequestParam(name = "birthDate", required = false) java.time.LocalDate birthDate) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Users currentUser = null;
        if (auth != null && auth.isAuthenticated()) {
            currentUser = usersRepository.findByUsername(auth.getName()).orElse(null);
        }
        if (currentUser == null || !username.equalsIgnoreCase(currentUser.getUsername())) {
            return "redirect:/customer/profile/" + username;
        }

        // Update allowed fields only
        if (avatarUrl != null) currentUser.setAvatarUrl(avatarUrl.trim());
        if (email != null) currentUser.setEmail(email.trim());
        if (phoneNumber != null) currentUser.setPhoneNumber(phoneNumber.trim());
        if (gender != null) currentUser.setGender(gender.trim());
        if (birthDate != null) currentUser.setBirthDate(birthDate);
        usersRepository.save(currentUser);
        return "redirect:/customer/profile/" + username + "?updated=1";
    }
}


