package banhangrong.su25.Controller;

import banhangrong.su25.Entity.Users;
import banhangrong.su25.service.CartService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.*;
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
        
        Map<String,Object> applied = (Map<String,Object>) session.getAttribute("appliedVoucher");
        
        Map<String,Object> view = cartService.buildCartView(user, applied);
        
        model.addAttribute("user", user);
        model.addAttribute("items", view.get("items"));
        model.addAttribute("discount", view.get("discount"));
        model.addAttribute("total", view.get("total"));
        model.addAttribute("cartCount", view.get("cartCount"));
        
        if (view.get("appliedVoucher") != null) {
            model.addAttribute("appliedVoucher", view.get("appliedVoucher"));
        }
        
        return "customer/cart";
    }

    @PostMapping("/cart/add")
    public String addToCart(@RequestParam("productId") Long productId,
                            @RequestParam(name = "quantity", required = false, defaultValue = "1") Integer quantity) {
        cartService.addToCart(productId, quantity);
        
        return "redirect:/cart";
    }

    @PostMapping("/cart/apply-voucher")
    public String applyVoucher(@RequestParam("code") String code, HttpSession session) {
        if (code == null || code.trim().isEmpty()) return "redirect:/cart?voucher=invalid";
        Map<String,Object> m = new HashMap<>();
        m.put("code", code.trim());
        session.setAttribute("appliedVoucher", m);
        return "redirect:/cart?voucher=applied";
    }

    @PostMapping("/cart/remove-voucher")
    public String removeVoucher(HttpSession session) {
        session.removeAttribute("appliedVoucher");
        return "redirect:/cart?voucher=removed";
    }

    @PostMapping("/cart/update")
    @ResponseBody
    public Map<String, Object> updateQty(@RequestParam("productId") Long productId,
                                         @RequestParam("quantity") Integer quantity) {
        return cartService.updateQuantity(productId, quantity);
    }

    @PostMapping("/cart/remove")
    @ResponseBody
    public Map<String, Object> removeFromCart(@RequestParam("productId") Long productId) {
        Map<String,Object> res = new HashMap<>();
        try {
            cartService.removeFromCart(productId);
            res.put("ok", true);
        } catch (Exception e) {
            res.put("ok", false);
            res.put("error", e.getMessage());
        }
        return res;
    }

    @GetMapping("/cart/remove")
    public String removeFromCartGet(@RequestParam("productId") Long productId) {
        try { 
            cartService.removeFromCart(productId); 
        } catch (Exception ignored) {}
        return "redirect:/cart";
    }

    @PostMapping("/cart/checkout-demo")
    public String checkoutDemo() {
        jakarta.servlet.http.HttpSession session = ((org.springframework.web.context.request.ServletRequestAttributes) org.springframework.web.context.request.RequestContextHolder.currentRequestAttributes()).getRequest().getSession(false);
        return cartService.checkoutDemoAndReturnRedirect(session);
    }
}
