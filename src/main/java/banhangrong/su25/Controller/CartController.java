package banhangrong.su25.Controller;

import banhangrong.su25.Entity.ShoppingCart;
import banhangrong.su25.Entity.Products;
import banhangrong.su25.Entity.Users;
import banhangrong.su25.Entity.Orders;
import banhangrong.su25.Entity.OrderItems;
import banhangrong.su25.Repository.ShoppingCartRepository;
import banhangrong.su25.Repository.ProductsRepository;
import banhangrong.su25.Repository.ProductImagesRepository;
import banhangrong.su25.Repository.UsersRepository;
import banhangrong.su25.Repository.OrdersRepository;
import banhangrong.su25.Repository.OrderItemsRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import jakarta.servlet.http.HttpSession;
import banhangrong.su25.Repository.VouchersRepository;
import banhangrong.su25.Entity.Vouchers;
import java.time.LocalDateTime;

@Controller
public class CartController {

    private final ShoppingCartRepository cartRepository;
    private final ProductsRepository productsRepository;
    private final ProductImagesRepository productImagesRepository;
    private final UsersRepository usersRepository;
    private final OrdersRepository ordersRepository;
    private final OrderItemsRepository orderItemsRepository;
    private final VouchersRepository vouchersRepository;

    public CartController(ShoppingCartRepository cartRepository,
                          ProductsRepository productsRepository,
                          ProductImagesRepository productImagesRepository,
                          UsersRepository usersRepository,
                          OrdersRepository ordersRepository,
                          OrderItemsRepository orderItemsRepository,
                          VouchersRepository vouchersRepository) {
        this.cartRepository = cartRepository;
        this.productsRepository = productsRepository;
        this.productImagesRepository = productImagesRepository;
        this.usersRepository = usersRepository;
        this.ordersRepository = ordersRepository;
        this.orderItemsRepository = orderItemsRepository;
        this.vouchersRepository = vouchersRepository;
    }

    // Get current user ID from authentication
    private Long getCurrentUserId() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && auth.getName() != null) {
                return usersRepository.findByUsername(auth.getName()).map(Users::getUserId).orElse(2L);
            }
        } catch (Exception ignored) {}
        return 2L; // fallback for demo
    }

    @GetMapping("/cart")
    public String viewCart(Model model, HttpSession session) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth != null ? auth.getName() : null;
        Optional<Users> userOptional = username != null ? usersRepository.findByUsername(username) : Optional.empty();
        
        if (userOptional.isEmpty()) {
            return "redirect:/login";
        }
        
        Users user = userOptional.get();
        List<ShoppingCart> items = cartRepository.findByUserId(user.getUserId());

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
        // Voucher from session if present and valid
        Map<String,Object> applied = (Map<String,Object>) session.getAttribute("appliedVoucher");
        BigDecimal discount = BigDecimal.ZERO;
        if (applied != null) {
            try {
                String code = Objects.toString(applied.get("code"), null);
                if (code != null) {
                    var candidates = vouchersRepository.findByCodeIgnoreCaseOrderByUpdatedAtDesc(code);
                    Vouchers v = candidates.isEmpty() ? null : candidates.get(0);
                    if (v != null && Objects.equals(v.getStatus(), "active") &&
                        (v.getStartAt() == null || !LocalDateTime.now().isBefore(v.getStartAt())) &&
                        (v.getEndAt() == null || !LocalDateTime.now().isAfter(v.getEndAt()))) {
                        // eligible total for that product id
                        BigDecimal eligible = viewItems.stream()
                                .filter(m -> Objects.equals(((Products)m.get("product")).getProductId(), v.getProductId()))
                                .map(m -> (BigDecimal) m.get("lineTotal"))
                                .reduce(BigDecimal.ZERO, BigDecimal::add);
                        if (v.getMinOrder() == null || eligible.compareTo(v.getMinOrder()) >= 0) {
                            if ("PERCENT".equalsIgnoreCase(v.getDiscountType())) {
                                discount = eligible.multiply(v.getDiscountValue().divide(new BigDecimal("100")));
                            } else {
                                discount = v.getDiscountValue();
                            }
                            if (discount.compareTo(eligible) > 0) discount = eligible;
                            model.addAttribute("appliedVoucher", v.getCode());
                        }
                    }
                }
            } catch (Exception ignored) {}
        }

        model.addAttribute("user", user);
        model.addAttribute("items", viewItems);
        model.addAttribute("discount", discount);
        model.addAttribute("total", total.subtract(discount));
        model.addAttribute("cartCount", items.size());
        return "customer/cart";
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

    @PostMapping("/cart/apply-voucher")
    public String applyVoucher(@RequestParam("code") String code, HttpSession session) {
        if (code == null || code.trim().isEmpty()) return "redirect:/cart?voucher=invalid";
        // Simply store code to session; validation and discount computed in viewCart
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

    // Demo checkout: subtract stock, deduct money from wallet, and clear cart
    @PostMapping("/cart/checkout-demo")
    @Transactional
    public String checkoutDemo() {
        Long uid = getCurrentUserId();
        List<ShoppingCart> items = cartRepository.findByUserId(uid);
        
        // Calculate total amount to deduct from wallet
        final BigDecimal totalAmount = items.stream()
            .map(it -> {
                Products p = productsRepository.findById(it.getProductId()).orElse(null);
                if (p != null) {
                    BigDecimal unitPrice = p.getSalePrice() != null ? p.getSalePrice() : p.getPrice();
                    int qty = it.getQuantity() != null ? it.getQuantity() : 1;
                    return unitPrice.multiply(BigDecimal.valueOf(qty));
                }
                return BigDecimal.ZERO;
            })
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // Deduct money from user's wallet
        System.out.println("[Demo Checkout] Total amount: " + totalAmount);
        Users user = usersRepository.findById(uid).orElse(null);
        if (user == null) {
            return "redirect:/cart?error=user_not_found";
        }
        
        BigDecimal currentBalance = user.getBalance() != null ? user.getBalance() : BigDecimal.ZERO;
        if (currentBalance.compareTo(totalAmount) < 0) {
            System.out.println("[Demo Checkout] Payment failed: insufficient balance for user " + uid + ", current: " + currentBalance + ", required: " + totalAmount);
            return "redirect:/cart?error=insufficient_balance";
        }
        
        BigDecimal newBalance = currentBalance.subtract(totalAmount);
        user.setBalance(newBalance);
        usersRepository.save(user);
        System.out.println("[Demo Checkout] Payment success: deducted " + totalAmount + " from user " + uid + ", new balance: " + newBalance);
        
        // Create order
        Orders order = new Orders();
        order.setUserId(uid);
        order.setTotalAmount(totalAmount);
        order.setStatus("completed"); // Demo checkout is immediately completed
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());
        
        // For demo: assume all products are from the same seller (sellerId = 1)
        // In real app, you might need to group by seller
        order.setSellerId(1L);
        
        Orders savedOrder = ordersRepository.save(order);
        System.out.println("[Demo Checkout] Created order: " + savedOrder.getOrderId());
        
        // Create order items
        for (ShoppingCart it : items) {
            Products product = productsRepository.findById(it.getProductId()).orElse(null);
            if (product != null) {
                OrderItems orderItem = new OrderItems();
                orderItem.setOrderId(savedOrder.getOrderId());
                orderItem.setProductId(it.getProductId());
                orderItem.setQuantity(it.getQuantity());
                orderItem.setPriceAtTime(product.getSalePrice() != null ? product.getSalePrice() : product.getPrice());
                orderItem.setCreatedAt(LocalDateTime.now());
                orderItemsRepository.save(orderItem);
                System.out.println("[Demo Checkout] Created order item: " + orderItem.getOrderItemId());
            }
        }
        
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
        return "redirect:/customer/dashboard?purchase=success";
    }
}


