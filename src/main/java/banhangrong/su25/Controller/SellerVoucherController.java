package banhangrong.su25.Controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
class SellerPageController {

    @GetMapping("/seller/voucher")
    public String voucherPage() {
        return "seller/voucher";
    }
}

