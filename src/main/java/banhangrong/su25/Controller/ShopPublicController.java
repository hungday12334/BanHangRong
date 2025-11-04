// ShopPublicController.java - FIX LẠI
package banhangrong.su25.Controller;

import banhangrong.su25.Entity.SellerShopSections;
import banhangrong.su25.Entity.Products;
import banhangrong.su25.service.ShopDesignService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class ShopPublicController {

    private static final Logger logger = LoggerFactory.getLogger(ShopPublicController.class);

    private final ShopDesignService shopDesignService;

    public ShopPublicController(ShopDesignService shopDesignService) {
        this.shopDesignService = shopDesignService;
    }

    // Xem cửa hàng công khai - FIX: Thêm try-catch và default values
    @GetMapping("/shop/{sellerId}")
    public String viewShop(@PathVariable Long sellerId, Model model) {
        logger.info("🚀 START: Loading shop for seller: {}", sellerId);

        try {
            // TEST: Kiểm tra service có null không
            if (shopDesignService == null) {
                logger.error("❌ shopDesignService is NULL!");
                throw new RuntimeException("ShopDesignService not initialized");
            }

            // 1. Lấy sections
            logger.info("📦 Step 1: Getting shop sections...");
            List<SellerShopSections> sections = shopDesignService.getShopSections(sellerId);
            logger.info("✅ Found {} sections", sections.size());

            // 2. Lấy products cho từng section
            logger.info("🎯 Step 2: Getting products for each section...");
            Map<Long, List<Products>> sectionProducts = new HashMap<>();

            for (SellerShopSections section : sections) {
                if (section.getIsActive()) {
                    logger.info("   Processing section: {} (ID: {})",
                            section.getSectionTitle(), section.getSectionId());

                    List<Products> products = shopDesignService.getProductsForSection(section);
                    sectionProducts.put(section.getSectionId(), products);

                    logger.info("   ✅ Section has {} products", products.size());
                }
            }

            // 3. Add data to model
            logger.info("🎨 Step 3: Adding data to model...");
            model.addAttribute("sections", sections);
            model.addAttribute("sectionProducts", sectionProducts);
            model.addAttribute("sellerId", sellerId);

            logger.info("🎉 SUCCESS: Shop data loaded successfully!");
            return "shop-public";

        } catch (Exception e) {
            logger.error("💥 ERROR: Failed to load shop", e);

            // Add empty data to avoid Thymeleaf errors
            model.addAttribute("sections", new ArrayList<>());
            model.addAttribute("sectionProducts", new HashMap<>());
            model.addAttribute("sellerId", sellerId);
            model.addAttribute("error", e.getMessage());

            return "shop-public";
        }
    }

    // Preview cửa hàng (cho seller) - FIX: Thêm error handling
    @GetMapping("/shop/preview/{sellerId}")
    public String previewShop(@PathVariable Long sellerId, Model model) {
        logger.info("👁️ PREVIEW: Loading preview for seller: {}", sellerId);
        return viewShop(sellerId, model);
    }

    // API lấy shop data cho AJAX - FIX: Thêm response body
    @GetMapping("/api/shop/{sellerId}/data")
    @ResponseBody
    public Map<String, Object> getShopData(@PathVariable Long sellerId) {
        Map<String, Object> response = new HashMap<>();
        try {
            List<SellerShopSections> sections = shopDesignService.getShopSections(sellerId);
            Map<String, List<Products>> productsBySection = new HashMap<>();

            for (SellerShopSections section : sections) {
                if (section.getIsActive()) {
                    List<Products> products = shopDesignService.getProductsForSection(section);
                    productsBySection.put(section.getSectionId().toString(), products);
                }
            }

            response.put("success", true);
            response.put("sections", sections);
            response.put("products", productsBySection);
            response.put("sellerId", sellerId);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
        }
        return response;
    }

    // 🧪 TEST ENDPOINT - Simple test without database
    @GetMapping("/shop/test/{sellerId}")
    public String testShop(@PathVariable Long sellerId, Model model) {
        logger.info("🧪 TEST: Loading test shop page");

        // Tạo dữ liệu test thủ công (không cần database)
        List<Map<String, Object>> testSections = new ArrayList<>();

        Map<String, Object> section1 = new HashMap<>();
        section1.put("sectionId", 1L);
        section1.put("sectionTitle", "Sản phẩm nổi bật - TEST");
        section1.put("sectionDescription", "Đây là dữ liệu test");
        section1.put("isActive", true);
        testSections.add(section1);

        Map<String, Object> section2 = new HashMap<>();
        section2.put("sectionId", 2L);
        section2.put("sectionTitle", "Bán chạy nhất - TEST");
        section2.put("sectionDescription", "Dữ liệu test không cần database");
        section2.put("isActive", true);
        testSections.add(section2);

        model.addAttribute("sections", testSections);
        model.addAttribute("sectionProducts", new HashMap<>());
        model.addAttribute("sellerId", sellerId);
        model.addAttribute("isTest", true);

        return "shop-public";
    }
}