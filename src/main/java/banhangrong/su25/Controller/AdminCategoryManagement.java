package banhangrong.su25.Controller;

import banhangrong.su25.DTO.CategoryFilter;
import banhangrong.su25.Entity.Categories;
import banhangrong.su25.Repository.CategoriesRepository;
import banhangrong.su25.service.AdminCategoryService;
import banhangrong.su25.service.CategoryService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequestMapping("/admin/category")
public class AdminCategoryManagement {

    @Autowired private AdminCategoryService adminCategoryService;

    @GetMapping("/filter")
    public String filterCategory(Model model, @ModelAttribute("filter") CategoryFilter categoryFilter, RedirectAttributes redirectAttributes){
        List<Categories> listFilterCategory = adminCategoryService.filter(categoryFilter);
        redirectAttributes.addFlashAttribute("filter", listFilterCategory);
        redirectAttributes.addFlashAttribute("isFromFilter", true);
        return "redirect:/admin/category";
    }

    @PostMapping("/save")
    public String saveCategory(Model model, HttpServletRequest request, RedirectAttributes redirectAttributes) {
        String actionType = request.getParameter("actionType"); // ✅ create hoặc update
        String sId = request.getParameter("categoryId");
        String sName = request.getParameter("name");
        String sDescription = request.getParameter("description");

        if (sName == null || sName.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Category name must not be empty");
            return "redirect:/admin/category";
        }

        if ("create".equalsIgnoreCase(actionType)) {
            // Check trùng tên
            if (adminCategoryService.countByName(sName.trim()) > 0) {
                redirectAttributes.addFlashAttribute("error", "Category name already exists");
                return "redirect:/admin/category";
            }

            try {
                Categories newCat = new Categories();
                newCat.setName(sName.trim());
                newCat.setDescription(sDescription);
                newCat.setCreatedAt(LocalDateTime.now());
                newCat.setUpdatedAt(LocalDateTime.now());
                adminCategoryService.save(newCat);

                redirectAttributes.addFlashAttribute("success", "Category created successfully!");
                return "redirect:/admin/category";
            } catch (Exception e) {
                redirectAttributes.addFlashAttribute("error", "Error creating category: " + e.getMessage());
                return "redirect:/admin/category";
            }
        }

        else if ("update".equalsIgnoreCase(actionType)) {
            if (sId == null || sId.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Category ID is required for update");
                return "redirect:/admin/category";
            }

            try {
                Long id = Long.parseLong(sId);
                Categories category = adminCategoryService.findById(id);

                if (category == null) {
                    redirectAttributes.addFlashAttribute("error", "Category not found");
                    return "redirect:/admin/category";
                }

                if (adminCategoryService.countByName(sName.trim()) > 0 &&
                        !category.getName().equalsIgnoreCase(sName.trim())) {
                    redirectAttributes.addFlashAttribute("error", "Category name already exists");
                    return "redirect:/admin/category";
                }

                category.setName(sName.trim());
                category.setDescription(sDescription);
                category.setUpdatedAt(LocalDateTime.now());

                adminCategoryService.save(category);

                redirectAttributes.addFlashAttribute("success", "Category updated successfully!");
                return "redirect:/admin/category";

            } catch (NumberFormatException e) {
                redirectAttributes.addFlashAttribute("error", "Invalid category ID");
                return "redirect:/admin/category";
            } catch (Exception e) {
                redirectAttributes.addFlashAttribute("error", "Error updating category: " + e.getMessage());
                return "redirect:/admin/category";
            }
        }
        //Ngoai 2 cai kia, tranh payload....
        redirectAttributes.addFlashAttribute("error", "Invalid action type");
        return "redirect:/admin/category";
    }
    @PostMapping("/delete")
    public String deleteCategory(HttpServletRequest request, RedirectAttributes redirectAttributes) {
        try {
            // Lấy categoryId từ form
            String idParam = request.getParameter("categoryId");
            Long categoryId = Long.parseLong(idParam);

            // Gọi service xóa
            adminCategoryService.removeCategoryById(categoryId);

            // Gửi thông báo thành công
            redirectAttributes.addFlashAttribute("success", "Category deleted successfully!");

        } catch (Exception e) {
            // Nếu lỗi, thêm thông báo lỗi
            redirectAttributes.addFlashAttribute("error", "Failed to delete category. It may be in use.");
        }

        // Chuyển hướng lại trang danh sách
        return "redirect:/admin/category";
    }

}
