package banhangrong.su25.Controller;

import banhangrong.su25.Entity.Users;
import banhangrong.su25.Repository.UsersRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Optional;

@Controller
public class AuthController {

    private final UsersRepository usersRepository;

    public AuthController(UsersRepository usersRepository) {
        this.usersRepository = usersRepository;
    }

    @GetMapping("/login")
    public String loginForm(@RequestParam(name = "next", required = false) String next, Model model) {
        model.addAttribute("next", next == null ? "/seller/dashboard" : next);
        return "login";
    }

    @PostMapping("/login")
    public String doLogin(@RequestParam String username,
                          @RequestParam String password,
                          @RequestParam(name = "next", required = false) String next,
                          HttpSession session,
                          Model model) {
        Optional<Users> opt = usersRepository.findAll().stream()
                .filter(u -> u.getUsername() != null && u.getUsername().equals(username))
                .filter(u -> u.getPassword() != null && u.getPassword().equals(password))
                .findFirst();
        if (opt.isEmpty()) {
            model.addAttribute("error", "Tên đăng nhập hoặc mật khẩu sai");
            model.addAttribute("next", next == null ? "/seller/dashboard" : next);
            return "login";
        }
        Users u = opt.get();
        // Set session attributes used by controllers
        session.setAttribute("userId", u.getUserId());
        session.setAttribute("user", u);
        // Redirect to next or dashboard
        return "redirect:" + (next == null || next.isBlank() ? "/seller/dashboard" : next);
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        try { session.invalidate(); } catch (Exception ignored) {}
        return "redirect:/login";
    }
}
