package banhangrong.su25.Controller;

import banhangrong.su25.Entity.Categories;
import banhangrong.su25.Entity.Products;
import banhangrong.su25.Entity.Users;
import banhangrong.su25.service.CategoryViewService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Map;

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

        List<Categories> categories = categoryViewService.listCategoriesWithPublicProducts();
        Map<Long, Long> productCountByCategory = new java.util.HashMap<>();
        for (Categories category : categories) {
            productCountByCategory.put(category.getCategoryId(), categoryViewService.countPublicProductsInCategory(category.getCategoryId()));
        }

        Users currentUser = categoryViewService.getCurrentUserOrNull();
        Long cartCount = currentUser != null ? categoryViewService.getCartCount(currentUser.getUserId()) : 0L;

        model.addAttribute("categories", categories);
        model.addAttribute("productCountByCategory", productCountByCategory);
        model.addAttribute("cartCount", cartCount);
        model.addAttribute("user", currentUser);

        return "customer/categories";
    }

    @GetMapping("/category/{categoryId}")
    public String categoryProducts(@PathVariable Long categoryId,
                                @RequestParam(name = "page", required = false, defaultValue = "0") int page,
                                @RequestParam(name = "size", required = false, defaultValue = "15") int size,
                                @RequestParam(name = "search", required = false) String search,
                                Model model) {
        if (categoryViewService.shouldRedirectVerifyForCustomer()) {
            return "redirect:/verify-email-required";
        }

        Categories category = categoryViewService.getCategoryById(categoryId);
        if (category == null) {
            return "redirect:/categories";
        }

        Page<Products> productsPage = categoryViewService.getProductsPage(categoryId, page, size, search);
        List<Products> products = productsPage.getContent();
        Map<Long, String> primaryImageByProduct = categoryViewService.buildPrimaryImageMap(products);

        Users currentUser = categoryViewService.getCurrentUserOrNull();
        Long cartCount = currentUser != null ? categoryViewService.getCartCount(currentUser.getUserId()) : 0L;

        model.addAttribute("category", category);
        model.addAttribute("products", products);
        model.addAttribute("primaryImageByProduct", primaryImageByProduct);
        model.addAttribute("page", page);
        model.addAttribute("size", size);
        model.addAttribute("search", search);
        model.addAttribute("totalPages", productsPage.getTotalPages());
        model.addAttribute("totalElements", productsPage.getTotalElements());
        model.addAttribute("cartCount", cartCount);
        model.addAttribute("user", currentUser);

        return "customer/category-products";
    }
}
