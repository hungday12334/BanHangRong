package banhangrong.su25.Controller;

import banhangrong.su25.Entity.Categories;
import banhangrong.su25.service.CategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/seller/categories")
public class SellerCategoryController {

    @Autowired
    private CategoryService categoryService;

    // ========== MAIN ENDPOINTS ==========

    // Hiển thị trang quản lý danh mục
    @GetMapping
    public String categoryManagementPage(Model model) {
        try {
            model.addAttribute("categories", categoryService.getAllCategories());
            model.addAttribute("newCategory", new Categories());
            return "seller/category-management";
        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("error", e.getMessage());
            // Avoid Whitelabel by returning the same view with safe defaults
            model.addAttribute("categories", java.util.List.of());
            model.addAttribute("newCategory", new Categories());
            return "seller/category-management";
        }
    }

    // Tạo danh mục mới
    @PostMapping
    public String createCategory(@ModelAttribute Categories category,
                                 RedirectAttributes redirectAttributes) {
        try {
            categoryService.createCategory(category);
            redirectAttributes.addFlashAttribute("success", "✅ Đã tạo danh mục '" + category.getName() + "' thành công");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "❌ " + e.getMessage());
        }
        return "redirect:/seller/categories";
    }

    // Tránh 400 khi truy cập trực tiếp bằng GET
    @GetMapping("/actions/update")
    public String redirectUpdateGet() { return "redirect:/seller/categories"; }
    @GetMapping("/actions/delete")
    public String redirectDeleteGet() { return "redirect:/seller/categories"; }

    // Cập nhật danh mục (endpoint cố định, đường dẫn không thể nhầm với {categoryId})
    @PostMapping("/actions/update")
    public String updateCategory(@RequestParam(value = "categoryId", required = false) Long categoryId,
                                 @RequestParam(value = "name", required = false) String name,
                                 @RequestParam(value = "description", required = false) String description,
                                 RedirectAttributes redirectAttributes) {
        try {
            if (categoryId == null) {
                redirectAttributes.addFlashAttribute("error", "Thiếu categoryId khi cập nhật");
                return "redirect:/seller/categories";
            }
            Categories c = new Categories();
            c.setName(name);
            c.setDescription(description);
            categoryService.updateCategory(categoryId, c);
            redirectAttributes.addFlashAttribute("success", "✅ Đã cập nhật danh mục thành công");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "❌ " + e.getMessage());
        }
        return "redirect:/seller/categories";
    }

    // Xóa danh mục (endpoint cố định, đường dẫn không thể nhầm với {categoryId})
    @RequestMapping(value = "/actions/delete", method = {RequestMethod.POST, RequestMethod.GET})
    public String deleteCategory(@RequestParam(value = "categoryId", required = false) Long categoryId,
                                 RedirectAttributes redirectAttributes) {
        try {
            if (categoryId == null) {
                redirectAttributes.addFlashAttribute("error", "Thiếu categoryId khi xóa");
                return "redirect:/seller/categories";
            }
            Categories category = categoryService.getCategoryById(categoryId)
                    .orElseThrow(() -> new RuntimeException("Category not found"));

            categoryService.deleteCategory(categoryId);
            redirectAttributes.addFlashAttribute("success", "✅ Đã xóa danh mục '" + category.getName() + "' thành công");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "❌ " + e.getMessage());
        }
        return "redirect:/seller/categories";
    }
}
