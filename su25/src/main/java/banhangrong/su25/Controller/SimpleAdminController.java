package banhangrong.su25.Controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin")
public class SimpleAdminController {

    @GetMapping({"/", "/dashboard", "/index"})
    public String adminDashboard(Model model) {
        // Simple data for testing
        model.addAttribute("totalUsers", 10);
        model.addAttribute("customerCount", 5);
        model.addAttribute("sellerCount", 3);
        model.addAttribute("adminCount", 2);
        model.addAttribute("totalProducts", 25);
        model.addAttribute("publicCount", 15);
        model.addAttribute("pendingCount", 8);
        model.addAttribute("cancelledCount", 2);
        
        return "admin/index";
    }
}
