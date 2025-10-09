package banhangrong.su25.Controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {

    @GetMapping("/")
    public String home() {
        return "index";
    }

    @GetMapping("/login")
    public String login() {
        return "login/login";
    }

    @GetMapping("/register")
    public String register() {
        return "login/register";
    }

    @GetMapping("/forgot-password")
    public String forgotPassword() {
        return "login/forgot-password";
    }

    @GetMapping("/reset-password")
    public String resetPassword() {
        return "login/reset-password";
    }

    @GetMapping("/find-account")
    public String findAccount() {
        return "login/find-account";
    }

    @GetMapping("/verify-email-required")
    public String verifyEmailRequired() {
        return "login/verify-email-required";
    }
}
