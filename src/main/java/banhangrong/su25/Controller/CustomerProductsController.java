package banhangrong.su25.Controller;

import banhangrong.su25.Entity.Categories;
import banhangrong.su25.Entity.Products;
import banhangrong.su25.Entity.Users;
import banhangrong.su25.service.CategoryViewService;
import banhangrong.su25.service.CustomerDashboardService;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Controller
public class CustomerProductsController {

    private final CustomerDashboardService customerDashboardService;
    private final CategoryViewService categoryViewService;

    public CustomerProductsController(CustomerDashboardService customerDashboardService,
                                      CategoryViewService categoryViewService) {
        this.customerDashboardService = customerDashboardService;
        this.categoryViewService = categoryViewService;
    }

    @GetMapping("/products")
    public String productsPage(
            @RequestParam(name = "page", required = false, defaultValue = "0") int page,
            @RequestParam(name = "size", required = false, defaultValue = "20") int size,
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "categoryId", required = false) Long categoryId,
            @RequestParam(name = "minPrice", required = false) BigDecimal minPrice,
            @RequestParam(name = "maxPrice", required = false) BigDecimal maxPrice,
            @RequestParam(name = "minRating", required = false) BigDecimal minRating,
            @RequestParam(name = "sortBy", required = false) String sortBy,
            Model model) {

        Users currentUser = customerDashboardService.getCurrentUserOrNull();
        
        if (currentUser != null) {
            if (!customerDashboardService.isCustomerEmailVerified(currentUser)) {
                return "redirect:/verify-email-required";
            }
        }

        Page<Products> productsPage = customerDashboardService.getFilteredProducts(
                page, size, search, categoryId, minPrice, maxPrice, minRating, sortBy);
        List<Products> products = productsPage.getContent();

        Map<Long, String> primaryImageByProduct = customerDashboardService.getProductImages(products);
        List<Categories> categories = categoryViewService.listCategoriesWithPublicProducts();

        model.addAttribute("products", products);
        model.addAttribute("page", productsPage.getNumber());
        model.addAttribute("totalPages", productsPage.getTotalPages());
        model.addAttribute("size", productsPage.getSize());
        model.addAttribute("totalElements", productsPage.getTotalElements());
        model.addAttribute("primaryImageByProduct", primaryImageByProduct);
        model.addAttribute("categories", categories);
        model.addAttribute("search", search);
        model.addAttribute("categoryId", categoryId);
        model.addAttribute("minPrice", minPrice);
        model.addAttribute("maxPrice", maxPrice);
        model.addAttribute("minRating", minRating);
        model.addAttribute("sortBy", sortBy);

        if (currentUser != null) {
            Long cartCount = customerDashboardService.getCartCount(currentUser.getUserId());
            model.addAttribute("cartCount", cartCount);
            model.addAttribute("user", currentUser);
        }

        return "customer/products";
    }
}

