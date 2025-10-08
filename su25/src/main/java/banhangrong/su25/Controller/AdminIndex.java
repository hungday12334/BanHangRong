package banhangrong.su25.Controller;

import banhangrong.su25.Entity.Products;
import banhangrong.su25.Entity.Users;
import banhangrong.su25.service.AdminProductService;
import banhangrong.su25.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/admin")
public class AdminIndex {
    @Autowired
    UserService userService;
    @Autowired
    AdminProductService adminProductService;
    @GetMapping("/index")
    public String showAdminIndex() {
        return "admin/index";
    }

    @GetMapping("/user")
    public String manageUser(Model model) {
        List<Users> userList = userService.findAll();
        model.addAttribute("userList", userList);
        return "admin/user-management"; // trả về file admin/user-management.html
    }
    @GetMapping("/product")
    public String manageProduct(Model model){
        List<Products> productsList = adminProductService.findAll();
        model.addAttribute("productsList", productsList);
        return "admin/product-management";
    }
}
