package banhangrong.su25.Controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin")
public class AdminIndex {
    @GetMapping("/index")
    public String showAdminIndex() {
        return "admin/index";
    }
}
