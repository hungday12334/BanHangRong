package banhangrong.su25.Controller;

import banhangrong.su25.Entity.ShoppingCart;
import banhangrong.su25.Entity.Products;
import banhangrong.su25.Repository.ShoppingCartRepository;
import banhangrong.su25.Repository.ProductsRepository;
import banhangrong.su25.Repository.UsersRepository;
import banhangrong.su25.Repository.OrdersRepository;
import banhangrong.su25.Repository.OrderItemsRepository;
import banhangrong.su25.Entity.Users;
import banhangrong.su25.Entity.Orders;
import banhangrong.su25.Entity.OrderItems;
import banhangrong.su25.Repository.ProductImagesRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

@Controller
public class CartController {

    private final ShoppingCartRepository cartRepository;
    private final ProductsRepository productsRepository;
    private final ProductImagesRepository productImagesRepository;
    private final UsersRepository usersRepository;
    private final OrdersRepository ordersRepository;
    private final OrderItemsRepository orderItemsRepository;

    public CartController(ShoppingCartRepository cartRepository,
                          ProductsRepository productsRepository,
                          ProductImagesRepository productImagesRepository,
                          UsersRepository usersRepository,
                          OrdersRepository ordersRepository,
                          OrderItemsRepository orderItemsRepository) {
        this.cartRepository = cartRepository;
        this.productsRepository = productsRepository;
        this.productImagesRepository = productImagesRepository;
        this.usersRepository = usersRepository;
        this.ordersRepository = ordersRepository;
        this.orderItemsRepository = orderItemsRepository;
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
            m.put("stock", p.getQuantity() != null ? p.getQuantity() : 0);
            viewItems.add(m);
        }
        model.addAttribute("items", viewItems);
        model.addAttribute("total", total);
        model.addAttribute("cartCount", items.size());
        return "customer/cart";
    }

    // Wallet checkout using user balance, with full DB consistency
    @PostMapping("/cart/checkout-wallet")
    @Transactional
    public String checkoutWithWallet() {
        Long uid = getCurrentUserId();
        List<ShoppingCart> items = cartRepository.findByUserId(uid);
        if (items.isEmpty()) return "redirect:/cart?pay=empty";

        // Calculate total and lock/validate stock
        BigDecimal total = BigDecimal.ZERO;
        for (ShoppingCart it : items) {
            Products p = productsRepository.findById(it.getProductId()).orElse(null);
            if (p == null) continue;
            BigDecimal unit = p.getSalePrice() != null ? p.getSalePrice() : p.getPrice();
            int want = it.getQuantity() != null ? it.getQuantity() : 1;
            int stock = p.getQuantity() != null ? p.getQuantity() : 0;
            if (want <= 0 || stock <= 0) continue;
            int buy = Math.min(stock, want);
            total = total.add(unit.multiply(BigDecimal.valueOf(buy)));
        }
        if (total.compareTo(BigDecimal.ZERO) <= 0) return "redirect:/cart?pay=empty";

        // Check balance
        Users user = usersRepository.findById(uid).orElse(null);
        if (user == null) return "redirect:/cart?pay=auth";
        BigDecimal balance = user.getBalance() != null ? user.getBalance() : BigDecimal.ZERO;
        if (balance.compareTo(total) < 0) {
            return "redirect:/cart?pay=insufficient"; // not enough balance
        }

        // Deduct balance
        user.setBalance(balance.subtract(total));
        usersRepository.save(user);

        // Create order
        Orders order = new Orders();
        order.setUserId(uid);
        order.setTotalAmount(total);
        order.setCreatedAt(java.time.LocalDateTime.now());
        order.setUpdatedAt(java.time.LocalDateTime.now());
        Orders saved = ordersRepository.save(order);

        // Create items, update stock/sales
        for (ShoppingCart it : items) {
            Products p = productsRepository.findById(it.getProductId()).orElse(null);
            if (p == null) continue;
            BigDecimal unit = p.getSalePrice() != null ? p.getSalePrice() : p.getPrice();
            int want = it.getQuantity() != null ? it.getQuantity() : 1;
            int stock = p.getQuantity() != null ? p.getQuantity() : 0;
            int buy = Math.min(stock, want);
            if (buy <= 0) continue;

            OrderItems oi = new OrderItems();
            oi.setOrderId(saved.getOrderId());
            oi.setProductId(p.getProductId());
            oi.setQuantity(buy);
            oi.setPriceAtTime(unit);
            orderItemsRepository.save(oi);

            p.setQuantity(stock - buy);
            Integer sold = p.getTotalSales();
            p.setTotalSales((sold != null ? sold : 0) + buy);
            productsRepository.save(p);
        }

        // Clear cart
        for (ShoppingCart it : items) { try { cartRepository.delete(it); } catch (Exception ignored) {} }

        return "redirect:/cart?pay=success";
    }

    @PostMapping("/cart/add")
    public String addToCart(@RequestParam("productId") Long productId,
                            @RequestParam(name = "quantity", required = false, defaultValue = "1") Integer quantity) {
        int qty = (quantity != null && quantity > 0) ? quantity : 1;
        // clamp with stock
        int stock = productsRepository.findById(productId)
                .map(p -> p.getQuantity() != null ? p.getQuantity() : 0).orElse(0);
        // merge quantity if item already exists
        var existing = cartRepository.findByUserIdAndProductId(getCurrentUserId(), productId);
        if (existing.isPresent()) {
            ShoppingCart it = existing.get();
            int current = it.getQuantity() == null ? 0 : it.getQuantity();
            int applied = Math.min(current + qty, stock);
            it.setQuantity(applied);
            cartRepository.save(it);
        } else {
            ShoppingCart item = new ShoppingCart();
            item.setUserId(getCurrentUserId());
            item.setProductId(productId);
            int applied = Math.min(qty, stock);
            item.setQuantity(applied);
            cartRepository.save(item);
        }
        return "redirect:/cart";
    }

    // Ajax update for quantity with stock validation
    @PostMapping("/cart/update")
    @ResponseBody
    public Map<String, Object> updateQty(@RequestParam("productId") Long productId,
                                         @RequestParam("quantity") Integer quantity) {
        Map<String, Object> res = new HashMap<>();
        int requested = quantity != null && quantity > 0 ? quantity : 1;
        // get product stock
        int stock = productsRepository.findById(productId)
                .map(p -> p.getQuantity() != null ? p.getQuantity() : 0).orElse(0);
        int applied = Math.min(requested, stock);
        var existing = cartRepository.findByUserIdAndProductId(getCurrentUserId(), productId);
        if (existing.isPresent()) {
            ShoppingCart it = existing.get();
            it.setQuantity(applied);
            cartRepository.save(it);
        }
        res.put("ok", true);
        res.put("appliedQty", applied);
        res.put("stock", stock);
        res.put("over", requested > stock);
        return res;
    }

    @PostMapping("/cart/remove")
    @ResponseBody
    public Map<String, Object> removeFromCart(@RequestParam("productId") Long productId) {
        Map<String,Object> res = new HashMap<>();
        try {
            var ex = cartRepository.findByUserIdAndProductId(getCurrentUserId(), productId);
            ex.ifPresent(cartRepository::delete);
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
            var ex = cartRepository.findByUserIdAndProductId(getCurrentUserId(), productId);
            ex.ifPresent(cartRepository::delete);
        } catch (Exception ignored) {}
        return "redirect:/cart";
    }

    // Demo checkout: subtract stock and clear cart
    @PostMapping("/cart/checkout-demo")
    @Transactional
    public String checkoutDemo() {
        Long uid = getCurrentUserId();
        List<ShoppingCart> items = cartRepository.findByUserId(uid);
        for (ShoppingCart it : items) {
            productsRepository.findById(it.getProductId()).ifPresent(p -> {
                int stock = p.getQuantity() != null ? p.getQuantity() : 0;
                int want = it.getQuantity() != null ? it.getQuantity() : 0;
                int buy = Math.min(stock, want);
                if (buy > 0) {
                    p.setQuantity(stock - buy);
                    Integer sold = p.getTotalSales();
                    p.setTotalSales((sold != null ? sold : 0) + buy);
                    productsRepository.save(p);
                }
            });
        }
        // clear cart after demo checkout
        for (ShoppingCart it : items) {
            try { cartRepository.delete(it); } catch (Exception ignored) {}
        }
        return "redirect:/cart";
    }
}


