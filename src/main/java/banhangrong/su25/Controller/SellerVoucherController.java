package banhangrong.su25.Controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
public class SellerVoucherController {

    @GetMapping("/seller/voucher")
    public String voucherPage() {
        return "seller/voucher";
    }
}

