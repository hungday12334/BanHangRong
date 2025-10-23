package banhangrong.su25.Controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("/seller")
public class ShopDesignerViewController {

    @GetMapping("/shop-designer")
    public String showShopDesignerPage() {
        // vì file nằm trong templates/seller/shop-designer.html
        return "seller/shop-designer";
    }

    // test controller
    @GetMapping("/test")
    @ResponseBody
    public String test() {
        return "✅ Controller loaded!";
    }
}
