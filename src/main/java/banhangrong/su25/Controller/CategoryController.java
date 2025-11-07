package banhangrong.su25.Controller;

import banhangrong.su25.Entity.Categories;
import banhangrong.su25.service.CategoryViewService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class CategoryController {

    private final CategoryViewService categoryViewService;

    public CategoryController(CategoryViewService categoryViewService) {
        this.categoryViewService = categoryViewService;
    }

    @GetMapping("/categories")
    public String categoriesPage(Model model) {
        if (categoryViewService.shouldRedirectVerifyForCustomer()) {
            return "redirect:/verify-email-required";
        }

        var categories = categoryViewService.listCategoriesWithPublicProducts();
        var currentUser = categoryViewService.getCurrentUserOrNull();

        model.addAttribute("categories", categories);
        model.addAttribute("productCountByCategory", categoryViewService.getProductCountByCategory(categories));
        model.addAttribute("cartCount", currentUser != null ? categoryViewService.getCartCount(currentUser.getUserId()) : 0L);
        model.addAttribute("user", currentUser);

        return "customer/categories";
    }

    @GetMapping("/category/{categoryId}")
    public String categoryProducts(@PathVariable Long categoryId,
                                @RequestParam(name = "search", required = false) String search,
                                Model model) {
        if (categoryViewService.shouldRedirectVerifyForCustomer()) {
            return "redirect:/verify-email-required";
        }

        Categories category = categoryViewService.getCategoryById(categoryId);
        if (category == null) {
            return "redirect:/categories";
        }

        var products = categoryViewService.getProducts(categoryId, search);
        var currentUser = categoryViewService.getCurrentUserOrNull();

        model.addAttribute("category", category);
        model.addAttribute("products", products);
        model.addAttribute("productImages", categoryViewService.getProductImagesForProducts(products));
        model.addAttribute("search", search);
        model.addAttribute("cartCount", currentUser != null ? categoryViewService.getCartCount(currentUser.getUserId()) : 0L);
        model.addAttribute("user", currentUser);

        return "customer/category-products";
    }
}
