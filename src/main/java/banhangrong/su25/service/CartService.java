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
        var primary = productImagesRepository.findTop1ByProductIdAndIsPrimaryTrueOrderByImageIdAsc(productId);
        if (primary != null && !primary.isEmpty()) img = primary.get(0).getImageUrl();
        if (img == null) {
            var any = productImagesRepository.findTop1ByProductIdOrderByImageIdAsc(productId);
            if (any != null && !any.isEmpty()) img = any.get(0).getImageUrl();
        }
        return img;
    }

    public Map<String, Object> buildCartView(Users user, Map<String, Object> sessionAppliedVoucher) {
        List<ShoppingCart> items = cartRepository.findByUserId(user.getUserId());

        List<Map<String, Object>> viewItems = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        for (ShoppingCart it : items) {
            Optional<Products> op = productsRepository.findById(it.getProductId());
            if (op.isEmpty()) continue;
            Products p = op.get();
            String img = getPrimaryOrAnyImageUrl(p.getProductId());
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

        BigDecimal discount = BigDecimal.ZERO;
        String appliedCode = null;
        if (sessionAppliedVoucher != null) {
            try {
                String code = Objects.toString(sessionAppliedVoucher.get("code"), null);
                if (code != null) {
                    var candidates = vouchersRepository.findByCodeIgnoreCaseOrderByUpdatedAtDesc(code);
                    Vouchers v = candidates.isEmpty() ? null : candidates.get(0);
                    if (v != null && Objects.equals(v.getStatus(), "active") &&
                        (v.getStartAt() == null || !LocalDateTime.now().isBefore(v.getStartAt())) &&
                        (v.getEndAt() == null || !LocalDateTime.now().isAfter(v.getEndAt()))) {
                        try {
                            if (v.getMaxUses() != null && voucherRedemptionsRepository.countByVoucherId(v.getVoucherId()) >= v.getMaxUses()) {
                                throw new IllegalStateException("max_uses reached");
                            }
                            if (v.getMaxUsesPerUser() != null && voucherRedemptionsRepository.countByVoucherIdAndUserId(v.getVoucherId(), user.getUserId()) >= v.getMaxUsesPerUser()) {
                                throw new IllegalStateException("max_uses_per_user reached");
                            }
                        } catch (Exception ignored) {}
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
                            appliedCode = v.getCode();
                        }
                    }
                }
            } catch (Exception ignored) {}
        }

        Map<String, Object> result = new HashMap<>();
        result.put("items", viewItems);
        result.put("total", total.subtract(discount));
        result.put("discount", discount);
        result.put("appliedVoucher", appliedCode);
        result.put("cartCount", items.size());
        return result;
    }

    public void addToCart(Long productId, Integer quantity) {
        int qty = (quantity != null && quantity > 0) ? quantity : 1;
        int stock = productsRepository.findById(productId)
                .map(p -> p.getQuantity() != null ? p.getQuantity() : 0).orElse(0);
        var existing = cartRepository.findByUserIdAndProductId(getCurrentUserIdOrFallback(), productId);
        Long userId = getCurrentUserIdOrFallback();
        boolean isNewItem = false;
        
        if (existing.isPresent()) {
            ShoppingCart it = existing.get();
            int current = it.getQuantity() == null ? 0 : it.getQuantity();
            int applied = Math.min(current + qty, stock);
            it.setQuantity(applied);
            cartRepository.save(it);
        } else {
            ShoppingCart item = new ShoppingCart();
            item.setUserId(userId);
            item.setProductId(productId);
            int applied = Math.min(qty, stock);
            item.setQuantity(applied);
            cartRepository.save(item);
            isNewItem = true;
        }
        
        // Gửi thông báo khi thêm vào giỏ hàng (chỉ khi thêm mới, không phải cập nhật số lượng)
        if (isNewItem) {
            try {
                Products product = productsRepository.findById(productId).orElse(null);
                if (product != null) {
                    String productName = product.getName() != null ? product.getName() : "Product";
                    notificationService.createCartNotification(userId, productId, productName, qty);
                }
            } catch (Exception e) {
                System.err.println("[CartService] Failed to send add to cart notification: " + e.getMessage());
            }
        }
    }

    public Map<String, Object> updateQuantity(Long productId, Integer quantity) {
        Map<String, Object> res = new HashMap<>();
        int requested = quantity != null && quantity > 0 ? quantity : 1;
        int stock = productsRepository.findById(productId)
                .map(p -> p.getQuantity() != null ? p.getQuantity() : 0).orElse(0);
        int applied = Math.min(requested, stock);
        var existing = cartRepository.findByUserIdAndProductId(getCurrentUserIdOrFallback(), productId);
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
        var ex = cartRepository.findByUserIdAndProductId(getCurrentUserIdOrFallback(), productId);
        ex.ifPresent(cartRepository::delete);
    }

    @Transactional
    public String checkoutDemoAndReturnRedirect(jakarta.servlet.http.HttpSession session) {
        Long uid = getCurrentUserIdOrFallback();
        List<ShoppingCart> items = cartRepository.findByUserId(uid);

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

        Users user = usersRepository.findById(uid).orElse(null);
        if (user == null) return "redirect:/cart?error=user_not_found";

        BigDecimal currentBalance = user.getBalance() != null ? user.getBalance() : BigDecimal.ZERO;
        if (currentBalance.compareTo(totalAmount) < 0) {
            // Lưu thông tin vào session để hiển thị popup
            if (session != null) {
                session.setAttribute("insufficientBalance", true);
                session.setAttribute("currentBalance", currentBalance);
                session.setAttribute("requiredAmount", totalAmount);
            }
            return "redirect:/cart?error=insufficient_balance";
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

        // Gửi thông báo đặt hàng thành công
        try {
            String orderCode = "ORD" + savedOrder.getOrderId();
            notificationService.createOrderNotification(uid, savedOrder.getOrderId(), orderCode);
        } catch (Exception e) {
            System.err.println("[CartService] Failed to send notification: " + e.getMessage());
        }

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

        for (ShoppingCart it : items) {
            try { cartRepository.delete(it); } catch (Exception ignored) {}
        }

        // Gửi email xác nhận đơn hàng
        try {
            String orderCode = "ORD" + savedOrder.getOrderId();
            String customerName = user.getFullName() != null && !user.getFullName().isEmpty() 
                ? user.getFullName() : user.getUsername();
            String customerEmail = user.getEmail();
            
            if (customerEmail != null && !customerEmail.isEmpty()) {
                // Lấy danh sách order items để gửi email
                List<OrderItems> orderItemsList = orderItemsRepository.findByOrderId(savedOrder.getOrderId());
                List<EmailService.OrderItemInfo> emailOrderItems = new ArrayList<>();
                
                for (OrderItems orderItem : orderItemsList) {
                    Products product = productsRepository.findById(orderItem.getProductId()).orElse(null);
                    if (product != null) {
                        emailOrderItems.add(new EmailService.OrderItemInfo(
                            product.getName(),
                            orderItem.getQuantity(),
                            orderItem.getPriceAtTime()
                        ));
                    }
                }
                
                emailService.sendOrderConfirmationEmail(
                    customerEmail,
                    customerName,
                    orderCode,
                    savedOrder.getOrderId(),
                    savedOrder.getTotalAmount(),
                    savedOrder.getCreatedAt(),
                    emailOrderItems
                );
                System.out.println("[CartService] Order confirmation email sent to: " + customerEmail);
            }
        } catch (Exception e) {
            System.err.println("[CartService] Failed to send order confirmation email: " + e.getMessage());
            e.printStackTrace();
            // Không fail transaction nếu email không gửi được
        }

        try {
            if (session != null) {
                Map<String,Object> applied = (Map<String,Object>) session.getAttribute("appliedVoucher");
                if (applied != null) {
                    String code = Objects.toString(applied.get("code"), null);
                    if (code != null) {
                        var candidates = vouchersRepository.findByCodeIgnoreCaseOrderByUpdatedAtDesc(code);
                        Vouchers v = candidates.isEmpty() ? null : candidates.get(0);
                        if (v != null) {
                            VoucherRedemptions rec = new VoucherRedemptions();
                            rec.setVoucherId(v.getVoucherId());
                            rec.setOrderId(savedOrder.getOrderId());
                            rec.setUserId(uid);
                            rec.setDiscountAmount(java.math.BigDecimal.ZERO);
                            voucherRedemptionsRepository.save(rec);
                            v.setUsedCount((v.getUsedCount() == null ? 0 : v.getUsedCount()) + 1);
                            vouchersRepository.save(v);
                            session.removeAttribute("appliedVoucher");
                        }
                    }
                }
            }
        } catch (Exception ignored) {}

        return "redirect:/customer/dashboard?purchase=success";
    }
}


