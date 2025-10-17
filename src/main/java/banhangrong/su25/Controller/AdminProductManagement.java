package banhangrong.su25.Controller;

import banhangrong.su25.Entity.Products;
import banhangrong.su25.service.AdminProductService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/product")
public class AdminProductManagement {
    @Autowired
    AdminProductService adminProductService;

    @GetMapping("/update")
    public String showUpdateForm(HttpServletRequest request,
                                 Model model,
                                 RedirectAttributes redirectAttributes) {
        String sId = request.getParameter("id");
        if (sId == null || sId.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Product not found");
            return "redirect:/admin/product";
        }

        Long id = Long.parseLong(sId);
        Products product = adminProductService.findById(id);

        if (product == null) {
            redirectAttributes.addFlashAttribute("error", "Product not found");
            return "redirect:/admin/product";
        }

        model.addAttribute("product", product);
        return "admin/product-update";
    }

    @PostMapping("/update")
    public String updateProduct(HttpServletRequest request,
                                RedirectAttributes redirectAttributes,
                                Model model) {
        String sId = request.getParameter("productId");
        if (sId == null || sId.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Product not found");
            return "redirect:/admin/product";
        }
        Long id = Long.parseLong(sId);
        Products product = adminProductService.findById(id);
        if (product == null || product.getStatus() == null || !product.getStatus().equalsIgnoreCase("pending")) {
            redirectAttributes.addFlashAttribute("error", "Product not found");
            return "redirect:/admin/product";
        }
        product.setStatus("public");
        adminProductService.save(product);
        redirectAttributes.addFlashAttribute("success", "Product updated successfully");
        return "redirect:/admin/product";
    }

    @PostMapping("/cancel")
    public String cancelProduct(HttpServletRequest request,
                                RedirectAttributes redirectAttributes,
                                Model model) {
        String sId = request.getParameter("productId");
        if (sId == null || sId.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Product not found");
            return "redirect:/admin/product";
        }
        Long id = Long.parseLong(sId);
        Products product = adminProductService.findById(id);
        if (product == null || product.getStatus() == null || !product.getStatus().equalsIgnoreCase("pending")) {
            redirectAttributes.addFlashAttribute("error", "Product not found");
            return "redirect:/admin/product";
        }
        product.setStatus("cancelled");
        adminProductService.save(product);
        redirectAttributes.addFlashAttribute("success", "Cancelled successfully");
        return "redirect:/admin/product";
    }
}
