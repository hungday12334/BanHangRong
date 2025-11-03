package banhangrong.su25.Controller;

import banhangrong.su25.DTO.LicenseDTO;
import banhangrong.su25.Entity.Categories;
import banhangrong.su25.Entity.Products;
import banhangrong.su25.Entity.ShopLicenses;
import banhangrong.su25.Entity.Users;
import banhangrong.su25.Entity.CategoriesProducts;
import banhangrong.su25.Entity.CategoriesProductsId;
import banhangrong.su25.Repository.ProductsRepository;
import banhangrong.su25.Repository.ProductImagesRepository;
import banhangrong.su25.Repository.CategoriesProductsRepository;
import banhangrong.su25.service.CategoryService;
import banhangrong.su25.service.LicenseManagementService;
import jakarta.servlet.http.HttpSession;
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

    @Autowired
    private LicenseManagementService licenseManagementService;

    @Autowired
    private CategoriesProductsRepository categoriesProductsRepository;

    // ========== DEBUG/TEST ENDPOINT ==========

    @GetMapping("/test")
    @ResponseBody
    public String testEndpoint(HttpSession session) {
        System.out.println("🧪 TEST ENDPOINT CALLED");
        Users user = (Users) session.getAttribute("user");
        return "TEST OK - Session ID: " + session.getId() +
               ", User: " + (user != null ? user.getUsername() : "NULL") +
               ", Attributes: " + java.util.Collections.list(session.getAttributeNames());
    }

    // ========== MAIN ENDPOINTS ==========

    // Hiển thị trang quản lý danh mục
    @GetMapping
    public String categoryManagementPage(Model model, HttpSession session) {
        System.out.println("========================================");
        System.out.println("🔍 SellerCategoryController.categoryManagementPage() CALLED");
        System.out.println("Session ID: " + session.getId());
        System.out.println("Session isNew: " + session.isNew());
        System.out.println("Session MaxInactiveInterval: " + session.getMaxInactiveInterval());

        try {
            // Get current seller from session
            Users currentUser = (Users) session.getAttribute("user");
            System.out.println("Current user from session: " + (currentUser != null ? currentUser.getUsername() : "NULL"));

            if (currentUser == null) {
                // Session expired or not logged in
                System.out.println("❌ User is NULL - redirecting to login");
                System.out.println("========================================");
                model.addAttribute("error", "Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.");
                return "redirect:/login?expired=true";
            }

            System.out.println("✅ User found: " + currentUser.getUsername() + " (ID: " + currentUser.getUserId() + ")");

            // Check if user is seller or admin
            String userType = currentUser.getUserType();
            if (!"seller".equalsIgnoreCase(userType) && !"admin".equalsIgnoreCase(userType)) {
                model.addAttribute("error", "Bạn không có quyền truy cập trang này.");
                return "redirect:/";
            }

            Long sellerId = currentUser.getUserId();

            // Get ALL categories (shared across sellers)
            List<Categories> allCategories = categoryService.getAllCategories();

            // Calculate statistics - FILTER BY SELLER'S PRODUCTS
            Map<Long, Long> productCountByCategory = new HashMap<>();
            long totalProducts = 0;
            long categoriesWithProducts = 0;

            // Filter categories: only show if seller has products OR if seller created it
            List<Categories> relevantCategories = new ArrayList<>();

            for (Categories category : allCategories) {
                // Count seller's products in this category
                Long count = productsRepository.countByCategoryIdAndSellerId(category.getCategoryId(), sellerId);
                productCountByCategory.put(category.getCategoryId(), count);

                // Only include category if seller has products in it
                // OR if it's a recently created category (last 7 days) to allow sellers to add products
                LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
                boolean isRecentCategory = category.getCreatedAt() != null && category.getCreatedAt().isAfter(sevenDaysAgo);

                if (count > 0 || isRecentCategory) {
                    relevantCategories.add(category);
                    totalProducts += count;
                    if (count > 0) {
                        categoriesWithProducts++;
                    }
                }
            }

            // Count recent categories that seller is using
            LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
            long recentCategories = relevantCategories.stream()
                .filter(c -> c.getCreatedAt() != null && c.getCreatedAt().isAfter(sevenDaysAgo))
                .count();

            // If seller has no products at all, show empty state with all categories
            // so they can start adding products
            if (totalProducts == 0) {
                relevantCategories = allCategories; // Show all to let them choose
            }

            model.addAttribute("categories", relevantCategories);
            model.addAttribute("productCountByCategory", productCountByCategory);
            model.addAttribute("totalCategories", relevantCategories.size());
            model.addAttribute("categoriesWithProducts", categoriesWithProducts);
            model.addAttribute("totalProducts", totalProducts);
            model.addAttribute("recentCategories", recentCategories);
            model.addAttribute("newCategory", new Categories());
            model.addAttribute("sellerId", sellerId);
            model.addAttribute("hasNoProducts", totalProducts == 0); // Flag for empty state

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
            model.addAttribute("hasNoProducts", true);
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
    public ResponseEntity<List<Map<String, Object>>> getCategoryProducts(
            @PathVariable Long categoryId,
            HttpSession session) {
        try {
            // Get current seller from session
            Users currentUser = (Users) session.getAttribute("user");
            if (currentUser == null) {
                return ResponseEntity.status(401).body(new ArrayList<>());
            }
            Long sellerId = currentUser.getUserId();

            // Get products filtered by category AND seller
            List<Products> products = productsRepository.findByCategoryIdAndSellerId(categoryId, sellerId);

            List<Map<String, Object>> productData = products.stream().map(product -> {
                Map<String, Object> data = new HashMap<>();
                data.put("productId", product.getProductId());
                data.put("name", product.getName());
                data.put("sku", "P" + product.getProductId()); // Generate SKU from ID
                data.put("price", product.getPrice());
                data.put("salePrice", product.getSalePrice());
                data.put("stockQuantity", product.getQuantity()); // Use quantity field
                data.put("totalSales", product.getTotalSales() != null ? product.getTotalSales() : 0);
                data.put("status", product.getStatus());
                data.put("averageRating", product.getAverageRating());

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

    // API để lấy tất cả sản phẩm của seller
    @GetMapping("/api/products/all")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getAllSellerProducts(HttpSession session) {
        try {
            // Get current seller from session
            Users currentUser = (Users) session.getAttribute("user");
            if (currentUser == null) {
                return ResponseEntity.status(401).body(Map.of("success", false, "error", "Chưa đăng nhập"));
            }
            Long sellerId = currentUser.getUserId();

            // Get all products of seller
            List<Products> products = productsRepository.findBySellerId(sellerId);

            List<Map<String, Object>> productData = products.stream().map(product -> {
                Map<String, Object> data = new HashMap<>();
                data.put("productId", product.getProductId());
                data.put("name", product.getName());
                data.put("sku", "P" + product.getProductId());
                data.put("price", product.getPrice());
                data.put("salePrice", product.getSalePrice());
                data.put("stockQuantity", product.getQuantity());
                data.put("totalSales", product.getTotalSales() != null ? product.getTotalSales() : 0);
                data.put("status", product.getStatus());

                // Get categories for this product (many-to-many relationship)
                try {
                    List<Categories> categories = categoriesProductsRepository.findCategoriesByProductId(product.getProductId());
                    if (!categories.isEmpty()) {
                        // If product has categories, use the first one
                        Categories firstCategory = categories.get(0);
                        data.put("categoryId", firstCategory.getCategoryId());
                        data.put("categoryName", firstCategory.getName());
                    } else {
                        data.put("categoryId", null);
                        data.put("categoryName", null);
                    }
                } catch (Exception e) {
                    data.put("categoryId", null);
                    data.put("categoryName", null);
                }

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

            return ResponseEntity.ok(Map.of(
                "success", true,
                "products", productData,
                "count", productData.size()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    // API để gán sản phẩm vào danh mục
    @PostMapping("/api/products/assign")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> assignProductToCategory(
            @RequestParam Long productId,
            @RequestParam Long categoryId,
            HttpSession session) {
        try {
            Users currentUser = (Users) session.getAttribute("user");
            if (currentUser == null) {
                return ResponseEntity.status(401).body(Map.of("success", false, "error", "Chưa đăng nhập"));
            }
            Long sellerId = currentUser.getUserId();

            // Verify product belongs to seller
            Products product = productsRepository.findById(productId)
                    .orElseThrow(() -> new RuntimeException("Sản phẩm không tồn tại"));

            if (!product.getSellerId().equals(sellerId)) {
                return ResponseEntity.status(403).body(Map.of("success", false, "error", "Không có quyền truy cập"));
            }

            // Verify category exists
            Categories category = categoryService.getCategoryById(categoryId)
                    .orElseThrow(() -> new RuntimeException("Danh mục không tồn tại"));

            // Check if relationship already exists
            CategoriesProductsId id = new CategoriesProductsId(categoryId, productId);
            if (categoriesProductsRepository.existsById(id)) {
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Sản phẩm đã có trong danh mục này"
                ));
            }

            // Create new relationship
            CategoriesProducts categoryProduct = new CategoriesProducts(category, product);
            categoriesProductsRepository.save(categoryProduct);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Đã gán sản phẩm vào danh mục thành công"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    // API để xóa sản phẩm khỏi danh mục
    @PostMapping("/api/products/remove")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> removeProductFromCategory(
            @RequestParam Long productId,
            @RequestParam(required = false) Long categoryId,
            HttpSession session) {
        try {
            Users currentUser = (Users) session.getAttribute("user");
            if (currentUser == null) {
                return ResponseEntity.status(401).body(Map.of("success", false, "error", "Chưa đăng nhập"));
            }
            Long sellerId = currentUser.getUserId();

            // Verify product belongs to seller
            Products product = productsRepository.findById(productId)
                    .orElseThrow(() -> new RuntimeException("Sản phẩm không tồn tại"));

            if (!product.getSellerId().equals(sellerId)) {
                return ResponseEntity.status(403).body(Map.of("success", false, "error", "Không có quyền truy cập"));
            }

            if (categoryId != null) {
                // Remove from specific category
                CategoriesProductsId id = new CategoriesProductsId(categoryId, productId);
                categoriesProductsRepository.deleteById(id);
            } else {
                // Remove from all categories
                List<Categories> categories = categoriesProductsRepository.findCategoriesByProductId(productId);
                for (Categories category : categories) {
                    CategoriesProductsId id = new CategoriesProductsId(category.getCategoryId(), productId);
                    categoriesProductsRepository.deleteById(id);
                }
            }

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Đã xóa sản phẩm khỏi danh mục"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    // ========== LICENSE MANAGEMENT ENDPOINTS ==========

    /**
     * Get all licenses for seller with assignment status for a specific category
     */
    @GetMapping("/api/licenses")
    @ResponseBody
    public ResponseEntity<?> getAllLicenses(
            @RequestParam(required = false) Long categoryId,
            HttpSession session) {
        try {
            Users currentUser = (Users) session.getAttribute("user");
            if (currentUser == null) {
                return ResponseEntity.status(401).body(Map.of("error", "Chưa đăng nhập"));
            }

            Long sellerId = currentUser.getUserId();
            List<LicenseDTO> licenses;

            if (categoryId != null) {
                // Get licenses with assignment status for this category
                licenses = licenseManagementService.getAllLicensesWithAssignmentStatus(sellerId, categoryId);
            } else {
                // Get all licenses
                licenses = licenseManagementService.getAllLicensesBySeller(sellerId);
            }

            return ResponseEntity.ok(Map.of(
                "success", true,
                "licenses", licenses
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get licenses assigned to a category
     */
    @GetMapping("/api/licenses/assigned")
    @ResponseBody
    public ResponseEntity<?> getAssignedLicenses(
            @RequestParam Long categoryId,
            HttpSession session) {
        try {
            Users currentUser = (Users) session.getAttribute("user");
            if (currentUser == null) {
                return ResponseEntity.status(401).body(Map.of("error", "Chưa đăng nhập"));
            }

            Long sellerId = currentUser.getUserId();
            List<LicenseDTO> licenses = licenseManagementService.getAssignedLicensesForCategory(categoryId, sellerId);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "licenses", licenses
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Create a new license
     */
    @PostMapping("/api/licenses")
    @ResponseBody
    public ResponseEntity<?> createLicense(
            @RequestBody ShopLicenses license,
            HttpSession session) {
        try {
            Users currentUser = (Users) session.getAttribute("user");
            if (currentUser == null) {
                return ResponseEntity.status(401).body(Map.of("error", "Chưa đăng nhập"));
            }

            license.setSellerId(currentUser.getUserId());
            ShopLicenses created = licenseManagementService.createLicense(license);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Đã tạo license thành công",
                "license", created
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }

    /**
     * Assign a license to a category
     */
    @PostMapping("/api/licenses/assign")
    @ResponseBody
    public ResponseEntity<?> assignLicenseToCategory(
            @RequestParam Long categoryId,
            @RequestParam Long licenseId,
            HttpSession session) {
        try {
            Users currentUser = (Users) session.getAttribute("user");
            if (currentUser == null) {
                return ResponseEntity.status(401).body(Map.of("error", "Chưa đăng nhập"));
            }

            Long sellerId = currentUser.getUserId();
            licenseManagementService.assignLicenseToCategory(categoryId, licenseId, sellerId);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Đã thêm license vào danh mục"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(400).body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }

    /**
     * Remove a license from a category
     */
    @DeleteMapping("/api/licenses/remove")
    @ResponseBody
    public ResponseEntity<?> removeLicenseFromCategory(
            @RequestParam Long categoryId,
            @RequestParam Long licenseId,
            HttpSession session) {
        try {
            Users currentUser = (Users) session.getAttribute("user");
            if (currentUser == null) {
                return ResponseEntity.status(401).body(Map.of("error", "Chưa đăng nhập"));
            }

            Long sellerId = currentUser.getUserId();
            licenseManagementService.removeLicenseFromCategory(categoryId, licenseId, sellerId);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Đã xóa license khỏi danh mục"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(400).body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }

    /**
     * Get count of products per category including license count - SELLER SPECIFIC
     */
    @GetMapping("/api/stats")
    @ResponseBody
    public ResponseEntity<?> getCategoryStats(HttpSession session) {
        try {
            Users currentUser = (Users) session.getAttribute("user");
            if (currentUser == null) {
                return ResponseEntity.status(401).body(Map.of("error", "Chưa đăng nhập"));
            }

            Long sellerId = currentUser.getUserId();
            List<Categories> categories = categoryService.getAllCategories();

            List<Map<String, Object>> stats = categories.stream().map(category -> {
                Map<String, Object> stat = new HashMap<>();
                stat.put("categoryId", category.getCategoryId());
                stat.put("categoryName", category.getName());
                // Use seller-specific count
                stat.put("productCount", productsRepository.countByCategoryIdAndSellerId(category.getCategoryId(), sellerId));
                stat.put("licenseCount", licenseManagementService.getLicenseCountForCategory(category.getCategoryId(), sellerId));
                return stat;
            }).collect(Collectors.toList());

            return ResponseEntity.ok(Map.of(
                "success", true,
                "stats", stats
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
}