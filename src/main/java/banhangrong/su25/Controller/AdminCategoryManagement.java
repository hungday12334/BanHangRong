package banhangrong.su25.Controller;

import banhangrong.su25.DTO.CategoryFilter;
import banhangrong.su25.Entity.Categories;
import banhangrong.su25.service.AdminCategoryService;
import banhangrong.su25.service.CategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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
}
