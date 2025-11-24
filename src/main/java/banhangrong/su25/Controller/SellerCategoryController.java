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
    public String categoryManagementPage(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String categoryName,
            @RequestParam(required = false) Long productCountFrom,
            @RequestParam(required = false) Long productCountTo,
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate,
            Model model,
            HttpSession session) {
        System.out.println("========================================");
        System.out.println("🔍 SellerCategoryController.categoryManagementPage() CALLED");
        System.out.println("Session ID: " + session.getId());
        System.out.println("Session isNew: " + session.isNew());
        System.out.println("Session MaxInactiveInterval: " + session.getMaxInactiveInterval());

        // Log filter parameters
        System.out.println("🔍 Filters: categoryId=" + categoryId + ", categoryName=" + categoryName +
                          ", productCountFrom=" + productCountFrom + ", productCountTo=" + productCountTo +
                          ", fromDate=" + fromDate + ", toDate=" + toDate);

        try {
            // Get current seller from session
            Users currentUser = (Users) session.getAttribute("user");
            System.out.println("Current user from session: " + (currentUser != null ? currentUser.getUsername() : "NULL"));

            if (currentUser == null) {
                // Session expired or not logged in
                System.out.println("❌ User is NULL - redirecting to login");
                System.out.println("========================================");
                model.addAttribute("error", "Session has expired. Please log in again.");
                return "redirect:/login?expired=true";
            }

            System.out.println("✅ User found: " + currentUser.getUsername() + " (ID: " + currentUser.getUserId() + ")");

            // Check if user is seller or admin
            String userType = currentUser.getUserType();
            if (!"seller".equalsIgnoreCase(userType) && !"admin".equalsIgnoreCase(userType)) {
                model.addAttribute("error", "You do not have permission to access this page.");
                return "redirect:/";
            }

            Long sellerId = currentUser.getUserId();

            // Get ALL categories (shared across sellers)
            List<Categories> allCategories = categoryService.getAllCategories();

            // Calculate statistics - FILTER BY SELLER'S PRODUCTS
            Map<Long, Long> productCountByCategory = new HashMap<>();
            long totalProducts = 0;
            long categoriesWithProducts = 0;

            // Show ALL categories for seller (not filtered by products)
            // Calculate product counts for each category
            for (Categories category : allCategories) {
                // Count seller's products in this category
                Long count = productsRepository.countByCategoryIdAndSellerId(category.getCategoryId(), sellerId);
                productCountByCategory.put(category.getCategoryId(), count);

                totalProducts += count;
                if (count > 0) {
                    categoriesWithProducts++;
                }
            }

            // Count recent categories (last 7 days)
            LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
            long recentCategories = allCategories.stream()
                    .filter(c -> c.getCreatedAt() != null && c.getCreatedAt().isAfter(sevenDaysAgo))
                    .count();

            // ===== APPLY ADVANCED FILTERS =====
            List<Categories> filteredCategories = allCategories;

            // Filter by Category ID
            if (categoryId != null) {
                filteredCategories = filteredCategories.stream()
                    .filter(c -> c.getCategoryId().equals(categoryId))
                    .collect(Collectors.toList());
            }

            // Filter by Category Name (case-insensitive partial match)
            if (categoryName != null && !categoryName.trim().isEmpty()) {
                String searchTerm = categoryName.toLowerCase().trim();
                filteredCategories = filteredCategories.stream()
                    .filter(c -> c.getName() != null && c.getName().toLowerCase().contains(searchTerm))
                    .collect(Collectors.toList());
            }

            // Filter by Product Count Range
            if (productCountFrom != null) {
                filteredCategories = filteredCategories.stream()
                    .filter(c -> {
                        Long count = productCountByCategory.getOrDefault(c.getCategoryId(), 0L);
                        return count >= productCountFrom;
                    })
                    .collect(Collectors.toList());
            }

            if (productCountTo != null) {
                filteredCategories = filteredCategories.stream()
                    .filter(c -> {
                        Long count = productCountByCategory.getOrDefault(c.getCategoryId(), 0L);
                        return count <= productCountTo;
                    })
                    .collect(Collectors.toList());
            }

            // Filter by Date Range
            if (fromDate != null && !fromDate.isEmpty()) {
                try {
                    LocalDateTime fromDateTime = LocalDateTime.parse(fromDate + "T00:00:00");
                    filteredCategories = filteredCategories.stream()
                        .filter(c -> c.getCreatedAt() != null &&
                                    c.getCreatedAt().isAfter(fromDateTime))
                        .collect(Collectors.toList());
                } catch (Exception e) {
                    System.err.println("Error parsing fromDate: " + e.getMessage());
                }
            }

            if (toDate != null && !toDate.isEmpty()) {
                try {
                    LocalDateTime toDateTime = LocalDateTime.parse(toDate + "T23:59:59");
                    filteredCategories = filteredCategories.stream()
                        .filter(c -> c.getCreatedAt() != null &&
                                    c.getCreatedAt().isBefore(toDateTime))
                        .collect(Collectors.toList());
                } catch (Exception e) {
                    System.err.println("Error parsing toDate: " + e.getMessage());
                }
            }

            model.addAttribute("categories", filteredCategories);
            model.addAttribute("productCountByCategory", productCountByCategory);
            model.addAttribute("totalCategories", allCategories.size());
            model.addAttribute("categoriesWithProducts", categoriesWithProducts);
            model.addAttribute("totalProducts", totalProducts);
            model.addAttribute("recentCategories", recentCategories);
            model.addAttribute("newCategory", new Categories());
            model.addAttribute("sellerId", sellerId);
            model.addAttribute("hasNoProducts", totalProducts == 0); // Flag for empty state

            // Pass filter params back to view
            model.addAttribute("filterCategoryId", categoryId);
            model.addAttribute("filterCategoryName", categoryName);
            model.addAttribute("filterProductCountFrom", productCountFrom);
            model.addAttribute("filterProductCountTo", productCountTo);
            model.addAttribute("filterFromDate", fromDate);
            model.addAttribute("filterToDate", toDate);

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
            redirectAttributes.addFlashAttribute("success", "✅ Category '" + category.getName() + "' created successfully");
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
                                 // ===== NEW PARAMETERS FOR ENHANCED FEATURES =====
                                 @RequestParam(value = "slug", required = false) String slug,
                                 @RequestParam(value = "parentId", required = false) Long parentId,
                                 @RequestParam(value = "icon", required = false) String icon,
                                 @RequestParam(value = "imageUrl", required = false) String imageUrl,
                                 @RequestParam(value = "sortOrder", required = false) Integer sortOrder,
                                 @RequestParam(value = "status", required = false) String status,
                                 @RequestParam(value = "featured", required = false) Boolean featured,
                                 RedirectAttributes redirectAttributes) {
        try {
            if (categoryId == null) {
                redirectAttributes.addFlashAttribute("error", "Missing categoryId when updating");
                return "redirect:/seller/categories";
            }

            Categories c = new Categories();
            c.setName(name);
            c.setDescription(description);

            // ===== SET NEW FIELDS =====

            // Auto-generate slug from name if not provided
            if (slug != null && !slug.trim().isEmpty()) {
                c.setSlug(slug.toLowerCase().replaceAll("[^a-z0-9-]", "-"));
            } else if (name != null) {
                // Auto-generate slug from name
                c.setSlug(name.toLowerCase()
                    .replaceAll("[^a-z0-9\\s-]", "")
                    .replaceAll("\\s+", "-")
                    .replaceAll("-+", "-")
                    .replaceAll("^-|-$", ""));
            }

            if (parentId != null && parentId > 0) {
                c.setParentId(parentId);
            }

            if (icon != null && !icon.trim().isEmpty()) {
                c.setIcon(icon);
            }

            if (imageUrl != null && !imageUrl.trim().isEmpty()) {
                c.setImageUrl(imageUrl);
            }

            if (sortOrder != null) {
                c.setSortOrder(sortOrder);
            } else {
                c.setSortOrder(0);  // Default
            }

            if (status != null && !status.trim().isEmpty()) {
                c.setStatus(status.toUpperCase());
            } else {
                c.setStatus("ACTIVE");  // Default
            }

            if (featured != null) {
                c.setFeatured(featured);
            } else {
                c.setFeatured(false);  // Default
            }

            categoryService.updateCategory(categoryId, c);
            redirectAttributes.addFlashAttribute("success", "✅ Category updated successfully");
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
                redirectAttributes.addFlashAttribute("error", "Missing categoryId when deleting");
                return "redirect:/seller/categories";
            }
            Categories category = categoryService.getCategoryById(categoryId)
                    .orElseThrow(() -> new RuntimeException("Category not found"));

            categoryService.deleteCategory(categoryId);
            redirectAttributes.addFlashAttribute("success", "✅ Category '" + category.getName() + "' deleted successfully");
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
                redirectAttributes.addFlashAttribute("error", "Please select at least one category to delete");
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
                        "✅ Successfully deleted " + deletedCount + " categories" +
                                (errors.isEmpty() ? "" : " (with " + errors.size() + " errors)"));
            }

            if (!errors.isEmpty()) {
                redirectAttributes.addFlashAttribute("error",
                        "❌ Some categories could not be deleted: " + String.join("; ", errors));
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
                data.put("sku", "P" + product.getProductId()); // SKU là mã nội bộ dùng để quản lý sản phẩm trong kho.
                                                                //Trong hệ thống của em, SKU được tạo tự động từ productId bằng format ‘P + ID’.
                                                                //SKU giúp admin dễ quản lý tồn kho, phân biệt sản phẩm và thống kê số liệu.
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
                return ResponseEntity.status(401).body(Map.of("success", false, "error", "Not logged in"));
            }
            Long sellerId = currentUser.getUserId();

            // Get all products of seller
            List<Products> products = productsRepository.findBySellerId(sellerId);

            List<Map<String, Object>> productData = products.stream().map(product -> {
                Map<String, Object> data = new HashMap<>();
                data.put("productId", product.getProductId());
                data.put("name", product.getName());
                data.put("sku", "P" + product.getProductId()); //SKU là mã nội bộ dùng để quản lý sản phẩm trong kho.
                                                                //Trong hệ thống của, SKU được tạo tự động từ productId bằng format ‘P + ID’.
                                                                //SKU giúp admin dễ quản lý tồn kho, phân biệt sản phẩm và thống kê số liệu.
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
                return ResponseEntity.status(401).body(Map.of("success", false, "error", "Not logged in")); // 401 Unauthorized
            }
            Long sellerId = currentUser.getUserId();

            // Verify product belongs to seller
            Products product = productsRepository.findById(productId)
                    .orElseThrow(() -> new RuntimeException("Product does not exist"));

            if (!product.getSellerId().equals(sellerId)) {
                return ResponseEntity.status(403).body(Map.of("success", false, "error", "No access permission")); // 403 Forbidden
            }

            // Verify category exists
            Categories category = categoryService.getCategoryById(categoryId)
                    .orElseThrow(() -> new RuntimeException("Category does not exist"));

            // Check if relationship already exists
            CategoriesProductsId id = new CategoriesProductsId(categoryId, productId);
            if (categoriesProductsRepository.existsById(id)) {
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", "Product already exists in this category"
                )); //  200 OK with message
            }

            // Create new relationship
            CategoriesProducts categoryProduct = new CategoriesProducts(category, product);
            categoriesProductsRepository.save(categoryProduct);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Product assigned to category successfully"
            )); // 200 OK
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("success", false, "error", e.getMessage())); // 500 Internal Server Error
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
                return ResponseEntity.status(401).body(Map.of("success", false, "error", "Not logged in")); // 401 Unauthorized
            }
            Long sellerId = currentUser.getUserId(); // Get current seller ID

            // Verify product belongs to seller
            Products product = productsRepository.findById(productId)
                    .orElseThrow(() -> new RuntimeException("Product does not exist"));

            if (!product.getSellerId().equals(sellerId)) {
                return ResponseEntity.status(403).body(Map.of("success", false, "error", "No access permission"));
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
                    "message", "Product removed from category successfully"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    // ========== LICENSE MANAGEMENT ENDPOINTS ==========

    /**
     * Get all licenses for seller with assignment status for a specific category
     */
    // API để lấy tất cả giấy phép cho người bán, tùy chọn với trạng thái chuyển nhượng cho một danh mục cụ thể
    @GetMapping("/api/licenses")
    @ResponseBody
    public ResponseEntity<?> getAllLicenses(
            @RequestParam(required = false) Long categoryId,
            HttpSession session) {
        try {
            Users currentUser = (Users) session.getAttribute("user");
            if (currentUser == null) {
                return ResponseEntity.status(401).body(Map.of("error", "Not logged in"));
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
     * Nhận giấy phép được chỉ định cho một danh mục
     */
    @GetMapping("/api/licenses/assigned")
    @ResponseBody
    public ResponseEntity<?> getAssignedLicenses(
            @RequestParam Long categoryId,
            HttpSession session) {
        try {
            Users currentUser = (Users) session.getAttribute("user");
            if (currentUser == null) {
                return ResponseEntity.status(401).body(Map.of("error", "Not logged in"));
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
                return ResponseEntity.status(401).body(Map.of("error", "Not logged in"));
            }

            license.setSellerId(currentUser.getUserId());
            ShopLicenses created = licenseManagementService.createLicense(license);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "License created successfully",
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
                return ResponseEntity.status(401).body(Map.of("error", "Not logged in"));
            }

            Long sellerId = currentUser.getUserId();
            licenseManagementService.assignLicenseToCategory(categoryId, licenseId, sellerId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "License assigned to category successfully"
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
                return ResponseEntity.status(401).body(Map.of("error", "Not logged in"));
            }

            Long sellerId = currentUser.getUserId();
            licenseManagementService.removeLicenseFromCategory(categoryId, licenseId, sellerId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "License removed from category successfully"
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
                return ResponseEntity.status(401).body(Map.of("error", "Not logged in"));
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