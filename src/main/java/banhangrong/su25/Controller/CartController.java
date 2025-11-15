package banhangrong.su25.Controller;

import banhangrong.su25.Entity.Users;
import banhangrong.su25.service.CartService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

import jakarta.servlet.http.HttpSession;

@Controller
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping("/cart")
    public String viewCart(Model model, HttpSession session) {
        Users user = cartService.getCurrentUserOrNull();
        if (user == null) {
            return "redirect:/login";
        }

        @SuppressWarnings("unchecked")
        Map<Long, String> appliedVouchers = (Map<Long, String>) session.getAttribute("appliedVouchers");
        if (appliedVouchers == null) {
            appliedVouchers = new HashMap<>();
        }

        Map<String, Object> view = cartService.buildCartView(user, appliedVouchers);

        model.addAttribute("user", user);
        model.addAttribute("items", view.get("items"));
        model.addAttribute("total", view.get("total"));
        model.addAttribute("cartCount", view.get("cartCount"));

        return "customer/cart";
    }

    @PostMapping("/cart/add")
    public String addToCart(@RequestParam("productId") Long productId,
                            @RequestParam(name = "quantity", required = false, defaultValue = "1") Integer quantity,
                            @RequestHeader(value = "Referer", required = false) String referer) {
        Users user = cartService.getCurrentUserOrNull();
        if (user == null) {
            return "redirect:/login?redirect=/product/" + productId;
        }

        cartService.addToCart(productId, quantity);

        // Nếu đến từ product detail page thì quay lại đó, không thì về cart
        if (referer != null && referer.contains("/product/")) {
            return "redirect:/product/" + productId;
        }
        return "redirect:/cart";
    }

    @PostMapping("/api/cart/add")
    @ResponseBody
    public Map<String, Object> addToCartApi(@RequestParam("productId") Long productId,
                                            @RequestParam(name = "quantity", required = false, defaultValue = "1") Integer quantity) {
        Map<String, Object> result = new HashMap<>();
        Users user = cartService.getCurrentUserOrNull();
        if (user == null) {
            result.put("success", false);
            result.put("error", "Please login to add items to cart");
            return result;
        }

        return cartService.addToCartWithResponse(productId, quantity);
    }


    @PostMapping("/cart/update")
    @ResponseBody
    public Map<String, Object> updateQty(@RequestParam("productId") Long productId,
                                         @RequestParam("quantity") Integer quantity) {
        return cartService.updateQuantity(productId, quantity);
    }

    @PostMapping("/cart/remove")
    @ResponseBody
    public Map<String, Object> removeFromCart(@RequestParam("productId") Long productId, HttpSession session) {
        Map<String, Object> res = new HashMap<>();
        try {
            cartService.removeFromCart(productId);
            // Remove voucher for this product when product is removed from cart
            @SuppressWarnings("unchecked")
            Map<Long, String> appliedVouchers = (Map<Long, String>) session.getAttribute("appliedVouchers");
            if (appliedVouchers != null) {
                appliedVouchers.remove(productId);
                session.setAttribute("appliedVouchers", appliedVouchers);
            }
            res.put("ok", true);
        } catch (Exception e) {
            res.put("ok", false);
            res.put("error", e.getMessage());
        }
        return res;
    }

    @GetMapping("/cart/remove")
    public String removeFromCartGet(@RequestParam("productId") Long productId, HttpSession session) {
        try {
            cartService.removeFromCart(productId);
            // Remove voucher for this product when product is removed from cart
            @SuppressWarnings("unchecked")
            Map<Long, String> appliedVouchers = (Map<Long, String>) session.getAttribute("appliedVouchers");
            if (appliedVouchers != null) {
                appliedVouchers.remove(productId);
                session.setAttribute("appliedVouchers", appliedVouchers);
            }
        } catch (Exception ignored) {
        }
        return "redirect:/cart";
    }

    @PostMapping("/cart/apply-voucher")
    @ResponseBody
    public Map<String, Object> applyVoucher(@RequestParam("productId") Long productId,
                                            @RequestParam("code") String code,
                                            HttpSession session) {
        Map<String, Object> result = cartService.applyVoucherForProduct(productId, code);
        if (Boolean.TRUE.equals(result.get("ok"))) {
//            @SuppressWarnings("unchecked")
            @SuppressWarnings("unchecked")
            Map<Long, String> appliedVouchers = (Map<Long, String>) session.getAttribute("appliedVouchers");
            if (appliedVouchers == null) {
                appliedVouchers = new HashMap<>();
            }
            appliedVouchers.put(productId, code);
            session.setAttribute("appliedVouchers", appliedVouchers);
        }

        return result;
    }

    @PostMapping("/cart/remove-voucher")
    @ResponseBody
    public Map<String, Object> removeVoucher(@RequestParam("productId") Long productId,
                                             HttpSession session) {
        Map<String, Object> res = new HashMap<>();
        @SuppressWarnings("unchecked")
        Map<Long, String> appliedVouchers = (Map<Long, String>) session.getAttribute("appliedVouchers");
        if (appliedVouchers != null) {
            appliedVouchers.remove(productId);
            session.setAttribute("appliedVouchers", appliedVouchers);
        }
        res.put("ok", true);
        return res;
    }

    @PostMapping("/cart/checkout-demo")
    public String checkoutDemo(HttpSession session) {
        return cartService.checkoutDemoAndReturnRedirect(session);
    }
}
