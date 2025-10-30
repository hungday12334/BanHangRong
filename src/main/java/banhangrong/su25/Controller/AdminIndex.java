package banhangrong.su25.Controller;

import banhangrong.su25.Entity.Products;
import banhangrong.su25.Entity.Users;
import banhangrong.su25.service.AdminProductService;
import banhangrong.su25.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/admin")
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
        long publicCount = adminProductService.countByStatus("public");
        long pendingCount = adminProductService.countByStatus("pending");
        long cancelledCount = adminProductService.countByStatus("cancelled");

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
        List<Users> userList;

        // Kiểm tra an toàn: có filter và đúng kiểu không
        Object filterObj = model.getAttribute("filter");
        Boolean isFromFilter = Boolean.TRUE.equals((Boolean) model.getAttribute("isFromFilter"));

        if (isFromFilter && filterObj instanceof List<?> filterList) {
            // Ép kiểu an toàn với pattern matching (Java 14+)
            userList = filterList.stream()
                    .filter(user -> user instanceof Users)
                    .map(object -> (Users) object)
                    .toList();
        } else {
            userList = userService.findAll();
        }

        model.addAttribute("userList", userList);
        return "admin/user-management";
    }


    @GetMapping("/products")
    public String getAllProduct(Model model) {
        List<Products> productsList;
        Object filterObj = model.getAttribute("filter");
        Boolean isFromFilter = Boolean.TRUE.equals((Boolean) model.getAttribute("isFromFilter"));
        if(isFromFilter && filterObj instanceof List<?> filterList){
            productsList = filterList.stream().filter(product -> product instanceof Products).map(object -> (Products) object).toList();
        }else{
            productsList = adminProductService.findAll();
        }
        model.addAttribute("productsList", productsList);
        return "admin/product-management";
    }

//    @GetMapping("/pending-product")
//    public String getPendingProduct(Model model) {
//        List<Products> productsList = adminProductService.findByStatus("pending");
//        model.addAttribute("productsList", productsList);
//        return "admin/pendingproduct-management";
//    }
}
