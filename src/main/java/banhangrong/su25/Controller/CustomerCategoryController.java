package banhangrong.su25.Controller;

import banhangrong.su25.Entity.Categories;
import banhangrong.su25.Entity.Products;
import banhangrong.su25.service.CategoryService;
import banhangrong.su25.service.CategoryViewService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;
import java.util.Map;

@Controller
public class CustomerCategoryController {
    private final CategoryService categoryService;
    private final CategoryViewService categoryViewService;
    public CustomerCategoryController(CategoryService categoryService, CategoryViewService categoryViewService) {
        this.categoryService = categoryService;
        this.categoryViewService = categoryViewService;
    }
    @GetMapping("/customer/categories")
    public String category(Model model){
        List<Categories> categoryList = categoryService.getAllCategories();
        Map<String, Long> numberProductInCate = categoryViewService.countPublicProductsInCategories(categoryList);
        model.addAttribute("categories", categoryList);
        model.addAttribute("numberProductInCate", numberProductInCate);
        return "customer/test/test-product";
    }
}
