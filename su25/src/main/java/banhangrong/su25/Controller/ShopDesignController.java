package banhangrong.su25.Controller;

import banhangrong.su25.Entity.SellerShopSections;
import banhangrong.su25.service.ShopDesignService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/seller")
public class ShopDesignController {

    @Autowired
    private ShopDesignService shopDesignService;

    // Hiển thị trang HTML
    @GetMapping("/shop-designer")
    public String showShopDesignerPage(Model model) {
        // Thêm các attribute cần thiết cho template
        model.addAttribute("pageTitle", "Thiết Kế Cửa Hàng");
        model.addAttribute("activePage", "shop-designer");
        return "seller/shop-designer";
    }

    // API endpoints - phải đổi URL để tránh conflict
    @GetMapping("/api/shop-design/sections")
    @ResponseBody
    public ResponseEntity<List<SellerShopSections>> getShopSections(@RequestParam Long sellerId) {
        List<SellerShopSections> sections = shopDesignService.getShopSections(sellerId);
        return ResponseEntity.ok(sections);
    }

    @PostMapping("/api/shop-design/layout")
    @ResponseBody
    public ResponseEntity<String> saveShopLayout(
            @RequestParam Long sellerId,
            @RequestBody List<SellerShopSections> sections) {
        shopDesignService.saveShopLayout(sellerId, sections);
        return ResponseEntity.ok("Shop layout saved successfully");
    }
}