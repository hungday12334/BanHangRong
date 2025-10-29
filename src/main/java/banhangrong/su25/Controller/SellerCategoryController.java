package banhangrong.su25.Controller;

import banhangrong.su25.Entity.Categories;
import banhangrong.su25.Entity.Products;
import banhangrong.su25.Repository.ProductsRepository;
import banhangrong.su25.Repository.ProductImagesRepository;
import banhangrong.su25.service.CategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/seller/categories")
public class SellerCategoryController {

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private ProductsRepository productsRepository;

    @Autowired
    private ProductImagesRepository productImagesRepository;

    // ========== MAIN ENDPOINTS ==========

    // Hiển thị trang quản lý danh mục
    @GetMapping
    public String categoryManagementPage(Model model) {
        try {
            List<Categories> categories = categoryService.getAllCategories();

            // Calculate statistics
            Map<Long, Long> productCountByCategory = new HashMap<>();
            long totalProducts = 0;
            long categoriesWithProducts = 0;

            for (Categories category : categories) {
                Long count = productsRepository.countByCategoryId(category.getCategoryId());
                productCountByCategory.put(category.getCategoryId(), count);
                totalProducts += count;
                if (count > 0) {
                    categoriesWithProducts++;
                }
            }

            // Count recent categories (last 7 days)
            LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
            long recentCategories = categories.stream()
                .filter(c -> c.getCreatedAt() != null && c.getCreatedAt().isAfter(sevenDaysAgo))
                .count();

            model.addAttribute("categories", categories);
            model.addAttribute("productCountByCategory", productCountByCategory);
            model.addAttribute("totalCategories", categories.size());
            model.addAttribute("categoriesWithProducts", categoriesWithProducts);
            model.addAttribute("totalProducts", totalProducts);
            model.addAttribute("recentCategories", recentCategories);
            model.addAttribute("newCategory", new Categories());

            return "seller/category-management";
        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("error", e.getMessage());
            model.addAttribute("categories", java.util.List.of());
            model.addAttribute("productCountByCategory", new HashMap<>());
            model.addAttribute("totalCategories", 0);
            model.addAttribute("categoriesWithProducts", 0);
            model.addAttribute("totalProducts", 0);
            model.addAttribute("recentCategories", 0);
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

    // Xóa nhiều danh mục cùng lúc
    @PostMapping("/actions/bulk-delete")
    public String bulkDeleteCategories(@RequestParam(value = "categoryIds", required = false) List<Long> categoryIds,
                                      RedirectAttributes redirectAttributes) {
        try {
            if (categoryIds == null || categoryIds.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Vui lòng chọn ít nhất một danh mục để xóa");
                return "redirect:/seller/categories";
            }

            int deletedCount = 0;
            List<String> errors = new ArrayList<>();

            for (Long categoryId : categoryIds) {
                try {
                    categoryService.deleteCategory(categoryId);
                    deletedCount++;
                } catch (Exception e) {
                    errors.add("ID " + categoryId + ": " + e.getMessage());
                }
            }

            if (deletedCount > 0) {
                redirectAttributes.addFlashAttribute("success",
                    "✅ Đã xóa " + deletedCount + " danh mục thành công" +
                    (errors.isEmpty() ? "" : " (có " + errors.size() + " lỗi)"));
            }

            if (!errors.isEmpty()) {
                redirectAttributes.addFlashAttribute("error",
                    "❌ Một số danh mục không thể xóa: " + String.join("; ", errors));
            }

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "❌ " + e.getMessage());
        }
        return "redirect:/seller/categories";
    }

    // API để xem sản phẩm trong danh mục
    @GetMapping("/{categoryId}/products")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getCategoryProducts(@PathVariable Long categoryId) {
        try {
            List<Products> products = productsRepository.findByCategoryId(categoryId);

            List<Map<String, Object>> productData = products.stream().map(product -> {
                Map<String, Object> data = new HashMap<>();
                data.put("productId", product.getProductId());
                data.put("name", product.getName());
                data.put("sku", "P" + product.getProductId()); // Generate SKU from ID
                data.put("price", product.getPrice());
                data.put("stockQuantity", product.getQuantity()); // Use quantity field
                data.put("totalSales", product.getTotalSales() != null ? product.getTotalSales() : 0);

                // Get primary image
                try {
                    var images = productImagesRepository.findTop1ByProductIdAndIsPrimaryTrueOrderByImageIdAsc(product.getProductId());
                    if (!images.isEmpty()) {
                        data.put("imageUrl", images.get(0).getImageUrl());
                    } else {
                        data.put("imageUrl", null);
                    }
                } catch (Exception e) {
                    data.put("imageUrl", null);
                }

                return data;
            }).toList();

            return ResponseEntity.ok(productData);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ArrayList<>());
        }
    }
}