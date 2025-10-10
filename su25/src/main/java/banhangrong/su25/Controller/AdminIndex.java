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

// @Controller
// @RequestMapping("/admin")
public class AdminIndex {
    @Autowired
    UserService userService;
    @Autowired
    AdminProductService adminProductService;

    @GetMapping({"/", "/dashboard", "/index"})
    public String showAdminIndex(Model model) {
        long totalUsers = userService.count();
        long customerCount = userService.countByUserType("CUSTOMER");
        long sellerCount = userService.countByUserType("SELLER");
        long adminCount = userService.countByUserType("ADMIN");
        long totalProducts = adminProductService.count();
        long publicCount = adminProductService.countByStatus("Public");
        long pendingCount = adminProductService.countByStatus("Pending");
        long cancelledCount = adminProductService.countByStatus("Cancelled");

        model.addAttribute("totalUsers", totalUsers);
        model.addAttribute("customerCount", customerCount);
        model.addAttribute("sellerCount", sellerCount);
        model.addAttribute("adminCount", adminCount);

        model.addAttribute("totalProducts", totalProducts);
        model.addAttribute("publicCount", publicCount);
        model.addAttribute("pendingCount", pendingCount);
        model.addAttribute("cancelledCount", cancelledCount);
        return "admin/index";
    }

    @GetMapping("/user")
    public String manageUser(Model model) {
        List<Users> userList = userService.findAll();
        model.addAttribute("userList", userList);
        return "admin/user-management"; // trả về file admin/user-management.html
    }

    @GetMapping("/product")
    public String manageProduct(Model model) {
        List<Products> productsList = adminProductService.findAll();
        List<Products> productsListPening = adminProductService.findByStatus("Pending");
        List<Products> productsListPublic = adminProductService.findByStatus("Public");
        List<Products> productsListCancelled = adminProductService.findByStatus("Cancelled");
        model.addAttribute("productsList", productsList);
        model.addAttribute("productsListPublic", productsListPublic);
        model.addAttribute("productsListCancelled", productsListCancelled);
        model.addAttribute("productsListPending", productsListPening);
        return "admin/product-management";
    }
}
