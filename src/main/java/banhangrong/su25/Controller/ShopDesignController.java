// ShopDesignController.java - SỬA LẠI
package banhangrong.su25.Controller;

import banhangrong.su25.Entity.SellerShopSections;
import banhangrong.su25.service.ShopDesignService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/seller/shop-design")
public class ShopDesignController {

    private final ShopDesignService shopDesignService;

    public ShopDesignController(ShopDesignService shopDesignService) {
        this.shopDesignService = shopDesignService;
    }

    @GetMapping("/sections")
    public ResponseEntity<List<SellerShopSections>> getShopSections(@RequestParam Long sellerId) {
        try {
            List<SellerShopSections> sections = shopDesignService.getShopSections(sellerId);
            return ResponseEntity.ok(sections);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/layout")
    public ResponseEntity<String> saveShopLayout(
            @RequestParam Long sellerId,
            @RequestBody List<SellerShopSections> sections) {
        try {
            shopDesignService.saveShopLayout(sellerId, sections);
            return ResponseEntity.ok("Shop layout saved successfully");
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("Error saving layout: " + e.getMessage());
        }
    }

    @PostMapping("/featured-products/{sectionId}")
    public ResponseEntity<String> updateFeaturedProducts(
            @PathVariable Long sectionId,
            @RequestBody List<Long> productIds) {
        try {
            shopDesignService.updateFeaturedProducts(sectionId, productIds);
            return ResponseEntity.ok("Featured products updated successfully");
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("Error updating featured products: " + e.getMessage());
        }
    }

    @GetMapping("/preview/{sellerId}")
    public ResponseEntity<Object> getShopPreview(@PathVariable Long sellerId) {
        try {
            List<SellerShopSections> sections = shopDesignService.getShopSections(sellerId);
            // Tạo response object chứa section info và products
            // (có thể tạo DTO cho việc này)
            return ResponseEntity.ok(sections);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}