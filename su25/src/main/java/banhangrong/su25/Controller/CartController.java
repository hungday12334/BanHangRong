package banhangrong.su25.Controller;

import banhangrong.su25.Entity.ShoppingCart;
import banhangrong.su25.Entity.Products;
import banhangrong.su25.Repository.ShoppingCartRepository;
import banhangrong.su25.Repository.ProductsRepository;
import banhangrong.su25.Repository.ProductImagesRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.*;

@Controller
public class CartController {

    private final ShoppingCartRepository cartRepository;
    private final ProductsRepository productsRepository;
    private final ProductImagesRepository productImagesRepository;

    public CartController(ShoppingCartRepository cartRepository,
                          ProductsRepository productsRepository,
                          ProductImagesRepository productImagesRepository) {
        this.cartRepository = cartRepository;
        this.productsRepository = productsRepository;
        this.productImagesRepository = productImagesRepository;
    }

    // For demo: use fixed userId=2 (alice). In real app, read from session/auth
    private Long getCurrentUserId() { return 2L; }

    @GetMapping("/cart")
    public String viewCart(Model model) {
        List<ShoppingCart> items = cartRepository.findByUserId(getCurrentUserId());

        List<Map<String,Object>> viewItems = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        for (ShoppingCart it : items) {
            Optional<Products> op = productsRepository.findById(it.getProductId());
            if (op.isEmpty()) continue;
            Products p = op.get();
            String img = null;
            var primary = productImagesRepository.findTop1ByProductIdAndIsPrimaryTrueOrderByImageIdAsc(p.getProductId());
            if (primary != null && !primary.isEmpty()) img = primary.get(0).getImageUrl();
            if (img == null) {
                var any = productImagesRepository.findTop1ByProductIdOrderByImageIdAsc(p.getProductId());
                if (any != null && !any.isEmpty()) img = any.get(0).getImageUrl();
            }
            BigDecimal unit = p.getSalePrice() != null ? p.getSalePrice() : p.getPrice();
            BigDecimal line = unit.multiply(BigDecimal.valueOf(it.getQuantity() == null ? 1 : it.getQuantity()));
            total = total.add(line);
            Map<String,Object> m = new HashMap<>();
            m.put("product", p);
            m.put("quantity", it.getQuantity());
            m.put("image", img);
            m.put("unitPrice", unit);
            m.put("lineTotal", line);
            viewItems.add(m);
        }
        model.addAttribute("items", viewItems);
        model.addAttribute("total", total);
        model.addAttribute("cartCount", items.size());
        return "pages/cart";
    }

    @PostMapping("/cart/add")
    public String addToCart(@RequestParam("productId") Long productId,
                            @RequestParam(name = "quantity", required = false, defaultValue = "1") Integer quantity) {
        ShoppingCart item = new ShoppingCart();
        item.setUserId(getCurrentUserId());
        item.setProductId(productId);
        item.setQuantity(quantity != null && quantity > 0 ? quantity : 1);
        cartRepository.save(item);
        return "redirect:/cart";
    }

    @PostMapping("/cart/update")
    public String updateQty(@RequestParam("productId") Long productId,
                            @RequestParam("quantity") Integer quantity) {
        // naive update: remove and re-add
        cartRepository.deleteByUserIdAndProductId(getCurrentUserId(), productId);
        if (quantity != null && quantity > 0) {
            ShoppingCart item = new ShoppingCart();
            item.setUserId(getCurrentUserId());
            item.setProductId(productId);
            item.setQuantity(quantity);
            cartRepository.save(item);
        }
        return "redirect:/cart";
    }

    @PostMapping("/cart/remove")
    public String removeFromCart(@RequestParam("productId") Long productId) {
        cartRepository.deleteByUserIdAndProductId(getCurrentUserId(), productId);
        return "redirect:/cart";
    }
}


