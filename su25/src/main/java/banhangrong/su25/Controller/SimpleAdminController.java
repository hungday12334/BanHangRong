package banhangrong.su25.Controller;

import banhangrong.su25.Entity.Users;
import banhangrong.su25.Entity.Products;
import banhangrong.su25.service.UserService;
import banhangrong.su25.service.AdminProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/admin")
public class SimpleAdminController {

    @Autowired
    private UserService userService;
    
    @Autowired
    private AdminProductService adminProductService;

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
    
    @GetMapping("/user")
    public String userManagement(Model model) {
        List<Users> userList = userService.findAll();
        model.addAttribute("userList", userList);
        return "admin/user-management";
    }
    
    @GetMapping("/product")
    public String productManagement(Model model) {
        List<Products> productsList = adminProductService.findAll();
        List<Products> productsListPending = adminProductService.findByStatus("Pending");
        List<Products> productsListPublic = adminProductService.findByStatus("Public");
        List<Products> productsListCancelled = adminProductService.findByStatus("Cancelled");
        
        model.addAttribute("productsList", productsList);
        model.addAttribute("productsListPublic", productsListPublic);
        model.addAttribute("productsListCancelled", productsListCancelled);
        model.addAttribute("productsListPending", productsListPending);
        
        return "admin/product-management";
    }
}
