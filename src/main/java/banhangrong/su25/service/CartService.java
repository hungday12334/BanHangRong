package banhangrong.su25.service;

import banhangrong.su25.Entity.*;
import banhangrong.su25.Repository.*;
import banhangrong.su25.email.EmailService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class CartService {

    private final ShoppingCartRepository cartRepository;
    private final ProductsRepository productsRepository;
    private final ProductImagesRepository productImagesRepository;
    private final UsersRepository usersRepository;
    private final OrdersRepository ordersRepository;
    private final OrderItemsRepository orderItemsRepository;
    private final VouchersRepository vouchersRepository;
    private final VoucherRedemptionsRepository voucherRedemptionsRepository;
    private final NotificationService notificationService;
    private final EmailService emailService;

    public CartService(ShoppingCartRepository cartRepository,
                       ProductsRepository productsRepository,
                       ProductImagesRepository productImagesRepository,
                       UsersRepository usersRepository,
                       OrdersRepository ordersRepository,
                       OrderItemsRepository orderItemsRepository,
                       VouchersRepository vouchersRepository,
                       VoucherRedemptionsRepository voucherRedemptionsRepository,
                       NotificationService notificationService,
                       EmailService emailService) {
        this.cartRepository = cartRepository;
        this.productsRepository = productsRepository;
        this.productImagesRepository = productImagesRepository;
        this.usersRepository = usersRepository;
        this.ordersRepository = ordersRepository;
        this.orderItemsRepository = orderItemsRepository;
        this.vouchersRepository = vouchersRepository;
        this.voucherRedemptionsRepository = voucherRedemptionsRepository;
        this.notificationService = notificationService;
        this.emailService = emailService;
    }

    public Users getCurrentUserOrNull() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getName() == null) return null;
        return usersRepository.findByUsername(auth.getName()).orElse(null);
    }

    public Long getCurrentUserIdOrFallback() {
        try {
            Users u = getCurrentUserOrNull();
            if (u != null) return u.getUserId();
        } catch (Exception ignored) {}
        return 2L;
    }

    public Long getCartCount(Long userId) {
        if (userId == null) {
            return 0L;
        }
        return cartRepository.countByUserId(userId);
    }

    public String getPrimaryOrAnyImageUrl(Long productId) {
        String img = null;
        List<ProductImages> primary = productImagesRepository.findTop1ByProductIdAndIsPrimaryTrueOrderByImageIdAsc(productId);
        if (primary != null && !primary.isEmpty()) img = primary.get(0).getImageUrl();
        if (img == null) {
            List<ProductImages> any = productImagesRepository.findTop1ByProductIdOrderByImageIdAsc(productId);
            if (any != null && !any.isEmpty()) img = any.get(0).getImageUrl();
        }
        return img;
    }

    public Map<String, Object> buildCartView(Users user, Map<Long, String> appliedVouchers) {
        if (appliedVouchers == null) {
            appliedVouchers = new HashMap<>();
        }

        List<ShoppingCart> items = cartRepository.findByUserId(user.getUserId());

        List<Map<String, Object>> viewItems = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        for (ShoppingCart it : items) {
            Optional<Products> productOpt = productsRepository.findById(it.getProductId());
            if (productOpt.isEmpty()) continue;

            Products p = productOpt.get();
            if (p.getStatus() == null || !"Public".equalsIgnoreCase(p.getStatus())) {
                continue;
            }

            String img = getPrimaryOrAnyImageUrl(p.getProductId());
            BigDecimal unit = p.getSalePrice() != null ? p.getSalePrice() : p.getPrice();
            BigDecimal line = unit.multiply(BigDecimal.valueOf(it.getQuantity() != null ? it.getQuantity() : 1));

            BigDecimal discount = BigDecimal.ZERO;
            String appliedVoucherCode = appliedVouchers.get(p.getProductId());
            if (appliedVoucherCode != null) {
                List<Vouchers> vouchers = vouchersRepository.findByProductIdAndStatusIgnoreCase(p.getProductId(), "active");
                for (Vouchers v : vouchers) {
                    if (v.getCode().equalsIgnoreCase(appliedVoucherCode)) {
                        if ((v.getStartAt() == null || !LocalDateTime.now().isBefore(v.getStartAt())) &&
                            (v.getEndAt() == null || !LocalDateTime.now().isAfter(v.getEndAt()))) {
                            if (v.getMinOrder() == null || line.compareTo(v.getMinOrder()) >= 0) {
                                if ("PERCENT".equalsIgnoreCase(v.getDiscountType())) {
                                    discount = line.multiply(v.getDiscountValue().divide(new BigDecimal("100")));
                                } else {
                                    discount = v.getDiscountValue();
                                }
                                if (discount.compareTo(line) > 0) discount = line;
                                appliedVoucherCode = v.getCode();
                                break;
                            }
                        }
                    }
                }
            }

            BigDecimal finalPrice = line.subtract(discount);
            total = total.add(finalPrice);

            Map<String, Object> m = new HashMap<>();
            m.put("product", p);
            m.put("quantity", it.getQuantity());
            m.put("image", img);
            m.put("unitPrice", unit);
            m.put("lineTotal", line);
            m.put("discount", discount);
            m.put("finalPrice", finalPrice);
            m.put("appliedVoucher", appliedVoucherCode);
            m.put("stock", p.getQuantity() != null ? p.getQuantity() : 0);
            viewItems.add(m);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("items", viewItems);
        result.put("total", total);
        result.put("cartCount", items.size());
        return result;
    }

    public Map<String, Object> applyVoucherForProduct(Long productId, String code) {
        Map<String, Object> res = new HashMap<>();

        if (code == null || code.trim().isEmpty()) {
            res.put("ok", false);
            res.put("error", "Voucher code is required");
            return res;
        }

        Optional<Products> productOpt = productsRepository.findById(productId);
        if (productOpt.isEmpty()) {
            res.put("ok", false);
            res.put("error", "Product not found");
            return res;
        }

        List<Vouchers> vouchers = vouchersRepository.findByProductIdAndStatusIgnoreCase(productId, "active");
        Vouchers voucher = null;
        for (Vouchers v : vouchers) {
            if (v.getCode().equalsIgnoreCase(code.trim())) {
                voucher = v;
                break;
            }
        }

        if (voucher == null) {
            res.put("ok", false);
            res.put("error", "Voucher not found or invalid");
            return res;
        }

        LocalDateTime now = LocalDateTime.now();
        if ((voucher.getStartAt() != null && now.isBefore(voucher.getStartAt())) ||
            (voucher.getEndAt() != null && now.isAfter(voucher.getEndAt()))) {
            res.put("ok", false);
            res.put("error", "Voucher is expired or not yet active");
            return res;
        }

        res.put("ok", true);
        res.put("code", voucher.getCode());
        return res;
    }

    public void addToCart(Long productId, Integer quantity) {
        Optional<Products> productOpt = productsRepository.findById(productId);
        if (productOpt.isEmpty()) {
            return;
        }

        Products product = productOpt.get();
        // Chỉ cho phép thêm sản phẩm có status là "Public"
        if (product.getStatus() == null || !"Public".equalsIgnoreCase(product.getStatus())) {
            return;
        }

        int qty = (quantity != null && quantity > 0) ? quantity : 1;
        int stock = product.getQuantity() != null ? product.getQuantity() : 0;
        Optional<ShoppingCart> existing = cartRepository.findByUserIdAndProductId(getCurrentUserIdOrFallback(), productId);
        
        if (existing.isPresent()) {
            ShoppingCart it = existing.get();
            int current = it.getQuantity() != null ? it.getQuantity() : 0;
            int applied = Math.min(current + qty, stock);
            it.setQuantity(applied);
            cartRepository.save(it);
        } else {
            ShoppingCart item = new ShoppingCart();
            item.setUserId(getCurrentUserIdOrFallback());
            item.setProductId(productId);
            int applied = Math.min(qty, stock);
            item.setQuantity(applied);
            cartRepository.save(item);
        }
    }

    public Map<String, Object> updateQuantity(Long productId, Integer quantity) {
        Map<String, Object> res = new HashMap<>();
        Optional<Products> productOpt = productsRepository.findById(productId);
        if (productOpt.isEmpty()) {
            res.put("ok", false);
            res.put("error", "Product not found");
            return res;
        }

        Products product = productOpt.get();
        // Chỉ cho phép update quantity nếu product có status là "Public"
        if (product.getStatus() == null || !"Public".equalsIgnoreCase(product.getStatus())) {
            res.put("ok", false);
            res.put("error", "Product is not available");
            return res;
        }

        int requested = quantity != null && quantity > 0 ? quantity : 1;
        int stock = product.getQuantity() != null ? product.getQuantity() : 0;
        int applied = Math.min(requested, stock);
        Optional<ShoppingCart> existing = cartRepository.findByUserIdAndProductId(getCurrentUserIdOrFallback(), productId);

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

    public void removeFromCart(Long productId) {
        Optional<ShoppingCart> ex = cartRepository.findByUserIdAndProductId(getCurrentUserIdOrFallback(), productId);
        ex.ifPresent(cartRepository::delete);
    }

    @Transactional
    public String checkoutDemoAndReturnRedirect(jakarta.servlet.http.HttpSession session) {
        Long uid = getCurrentUserIdOrFallback();
        List<ShoppingCart> items = cartRepository.findByUserId(uid);

        // Chỉ lấy các sản phẩm có status là "Public"
        List<ShoppingCart> validItems = new ArrayList<>();
        for (ShoppingCart it : items) {
            Optional<Products> productOpt = productsRepository.findById(it.getProductId());
            if (productOpt.isPresent()) {
                Products p = productOpt.get();
                if (p.getStatus() != null && "Public".equalsIgnoreCase(p.getStatus())) {
                    validItems.add(it);
                }
            }
        }

        if (validItems.isEmpty()) {
            return "redirect:/cart?pay=empty";
        }

        BigDecimal totalAmount = validItems.stream()
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

        Users user = usersRepository.findById(uid).orElse(null);
        if (user == null) return "redirect:/cart?pay=empty";

        BigDecimal currentBalance = user.getBalance() != null ? user.getBalance() : BigDecimal.ZERO;
        if (currentBalance.compareTo(totalAmount) < 0) {
            return "redirect:/cart?pay=insufficient";
        }

        user.setBalance(currentBalance.subtract(totalAmount));
        usersRepository.save(user);

        Orders order = new Orders();
        order.setUserId(uid);
        order.setTotalAmount(totalAmount);
        order.setStatus("completed");
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());
        order.setSellerId(1L);
        Orders savedOrder = ordersRepository.save(order);

        for (ShoppingCart it : validItems) {
            Products product = productsRepository.findById(it.getProductId()).orElse(null);
            if (product != null) {
                OrderItems orderItem = new OrderItems();
                orderItem.setOrderId(savedOrder.getOrderId());
                orderItem.setProductId(it.getProductId());
                orderItem.setQuantity(it.getQuantity());
                orderItem.setPriceAtTime(product.getSalePrice() != null ? product.getSalePrice() : product.getPrice());
                orderItem.setCreatedAt(LocalDateTime.now());
                orderItemsRepository.save(orderItem);
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

        // Xóa tất cả items trong cart (cả Public và không Public)
        for (ShoppingCart it : items) {
            cartRepository.delete(it);
        }

        return "redirect:/cart?pay=success";
    }
}


