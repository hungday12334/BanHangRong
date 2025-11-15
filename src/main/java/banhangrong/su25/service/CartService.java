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
    private final ProductLicensesRepository productLicensesRepository;
    private final LicenseUsageLogService licenseUsageLogService;

    public CartService(ShoppingCartRepository cartRepository,
                       ProductsRepository productsRepository,
                       ProductImagesRepository productImagesRepository,
                       UsersRepository usersRepository,
                       OrdersRepository ordersRepository,
                       OrderItemsRepository orderItemsRepository,
                       VouchersRepository vouchersRepository,
                       VoucherRedemptionsRepository voucherRedemptionsRepository,
                       NotificationService notificationService,
                       EmailService emailService, ProductLicensesRepository productLicensesRepository, LicenseUsageLogService licenseUsageLogService) {
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
        this.productLicensesRepository = productLicensesRepository;
        this.licenseUsageLogService = licenseUsageLogService;
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
                                    // Voucher phần trăm chỉ áp dụng cho 1 license
                                    BigDecimal oneLicensePrice = unit;
                                    discount = oneLicensePrice.multiply(v.getDiscountValue().divide(new BigDecimal("100")));
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

    /**
     * Add to cart with detailed response for API
     * Returns Map with success status, product info, quantity, and max quantity
     */
    public Map<String, Object> addToCartWithResponse(Long productId, Integer quantity) {
        Map<String, Object> res = new HashMap<>();
        
        Optional<Products> productOpt = productsRepository.findById(productId);
        if (productOpt.isEmpty()) {
            res.put("success", false);
            res.put("error", "Product not found");
            return res;
        }

        Products product = productOpt.get();
        // Chỉ cho phép thêm sản phẩm có status là "Public"
        if (product.getStatus() == null || !"Public".equalsIgnoreCase(product.getStatus())) {
            res.put("success", false);
            res.put("error", "Product is not available");
            return res;
        }

        int qty = (quantity != null && quantity > 0) ? quantity : 1;
        int stock = product.getQuantity() != null ? product.getQuantity() : 0;
        Optional<ShoppingCart> existing = cartRepository.findByUserIdAndProductId(getCurrentUserIdOrFallback(), productId);
        
        int currentCartQty = 0;
        if (existing.isPresent()) {
            ShoppingCart it = existing.get();
            currentCartQty = it.getQuantity() != null ? it.getQuantity() : 0;
        }
        
        int totalRequested = currentCartQty + qty;
        int maxQuantity = stock;
        
        // Check if total quantity exceeds max
        if (totalRequested > maxQuantity) {
            res.put("success", false);
            res.put("error", "Max quantity");
            res.put("errorCode", "MAX_QUANTITY");
            res.put("maxQuantity", maxQuantity);
            res.put("productName", product.getName());
            res.put("currentCartQty", currentCartQty);
            return res;
        }
        
        // Add to cart
        int finalQuantity = 0;
        if (existing.isPresent()) {
            ShoppingCart it = existing.get();
            finalQuantity = Math.min(currentCartQty + qty, stock);
            it.setQuantity(finalQuantity);
            cartRepository.save(it);
        } else {
            ShoppingCart item = new ShoppingCart();
            item.setUserId(getCurrentUserIdOrFallback());
            item.setProductId(productId);
            finalQuantity = Math.min(qty, stock);
            item.setQuantity(finalQuantity);
            cartRepository.save(item);
        }
        
        res.put("success", true);
        res.put("productName", product.getName());
        res.put("quantity", qty); // Quantity that was requested to add
        res.put("finalQuantity", finalQuantity); // Final quantity in cart
        res.put("maxQuantity", maxQuantity);
        
        return res;
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

        // Validate quantity > 0
        if (quantity == null || quantity <= 0) {
            res.put("ok", false);
            res.put("error", "Quantity must be greater than 0");
            return res;
        }

        int requested = quantity;
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

        // 🔹 Lấy applied vouchers từ session
        @SuppressWarnings("unchecked")
        Map<Long, String> appliedVouchers = (Map<Long, String>) (session != null ? session.getAttribute("appliedVouchers") : null);
        if (appliedVouchers == null) {
            appliedVouchers = new HashMap<>();
        }

        // 🔹 Chỉ lấy các sản phẩm có status là "Public" và quantity > 0
        List<ShoppingCart> validItems = new ArrayList<>();
        for (ShoppingCart it : items) {
            Optional<Products> productOpt = productsRepository.findById(it.getProductId());
            if (productOpt.isPresent()) {
                Products p = productOpt.get();
                if (p.getStatus() != null && "Public".equalsIgnoreCase(p.getStatus())) {
                    // Kiểm tra quantity > 0
                    int qty = it.getQuantity() != null ? it.getQuantity() : 0;
                    if (qty > 0) {
                        validItems.add(it);
                    }
                }
            }
        }

        if (validItems.isEmpty()) {
            return "redirect:/cart?pay=empty";
        }

        // 🔹 Tính tổng tiền với voucher discount
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (ShoppingCart it : validItems) {
            Products p = productsRepository.findById(it.getProductId()).orElse(null);
            if (p != null) {
                BigDecimal unitPrice = p.getSalePrice() != null ? p.getSalePrice() : p.getPrice();
                int qty = it.getQuantity() != null ? it.getQuantity() : 1;
                BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(qty));
                
                // Tính discount nếu có voucher
                BigDecimal discount = BigDecimal.ZERO;
                String appliedVoucherCode = appliedVouchers.get(p.getProductId());
                if (appliedVoucherCode != null) {
                    List<Vouchers> vouchers = vouchersRepository.findByProductIdAndStatusIgnoreCase(p.getProductId(), "active");
                    for (Vouchers v : vouchers) {
                        if (v.getCode().equalsIgnoreCase(appliedVoucherCode)) {
                            LocalDateTime now = LocalDateTime.now();
                            if ((v.getStartAt() == null || !now.isBefore(v.getStartAt())) &&
                                (v.getEndAt() == null || !now.isAfter(v.getEndAt()))) {
                                if (v.getMinOrder() == null || lineTotal.compareTo(v.getMinOrder()) >= 0) {
                                    if ("PERCENT".equalsIgnoreCase(v.getDiscountType())) {
                                        // Voucher phần trăm chỉ áp dụng cho 1 license
                                        BigDecimal oneLicensePrice = unitPrice;
                                        discount = oneLicensePrice.multiply(v.getDiscountValue().divide(new BigDecimal("100"), 2, java.math.RoundingMode.HALF_UP));
                                    } else {
                                        discount = v.getDiscountValue();
                                    }
                                    if (discount.compareTo(lineTotal) > 0) discount = lineTotal;
                                    break;
                                }
                            }
                        }
                    }
                }
                
                totalAmount = totalAmount.add(lineTotal.subtract(discount));
            }
        }

        Users user = usersRepository.findById(uid).orElse(null);
        if (user == null) return "redirect:/cart?error=user_not_found";

        BigDecimal currentBalance = user.getBalance() != null ? user.getBalance() : BigDecimal.ZERO;
        if (currentBalance.compareTo(totalAmount) < 0) {
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

        // 🔹 Save order items (chỉ Public) và map để tạo license
        java.util.Map<Long, OrderItems> savedItemsByProduct = new java.util.HashMap<>();
        for (ShoppingCart it : validItems) {
            Products product = productsRepository.findById(it.getProductId()).orElse(null);
            if (product != null) {
                OrderItems orderItem = new OrderItems();
                orderItem.setOrderId(savedOrder.getOrderId());
                orderItem.setProductId(it.getProductId());
                orderItem.setQuantity(it.getQuantity());
                orderItem.setPriceAtTime(product.getSalePrice() != null ? product.getSalePrice() : product.getPrice());
                orderItem.setCreatedAt(LocalDateTime.now());
                OrderItems savedItem = orderItemsRepository.save(orderItem);
                savedItemsByProduct.put(savedItem.getProductId(), savedItem);
            }
        }

        // 🔹 Update stock & generate licenses
        java.util.List<ProductLicenses> toInsert = new java.util.ArrayList<>();
        for (ShoppingCart it : validItems) {
            productsRepository.findById(it.getProductId()).ifPresent(p -> {
                int stock = p.getQuantity() != null ? p.getQuantity() : 0;
                int want = it.getQuantity() != null ? it.getQuantity() : 0;
                int buy = Math.min(stock, want);
                if (buy > 0) {
                    p.setQuantity(stock - buy);
                    Integer sold = p.getTotalSales();
                    p.setTotalSales((sold != null ? sold : 0) + buy);
                    productsRepository.save(p);

                    // Create licenses for each purchased unit
                    OrderItems savedItem = savedItemsByProduct.get(p.getProductId());
                    if (savedItem != null) {
                        LocalDateTime nowTs = LocalDateTime.now();
                        LocalDateTime time= LocalDateTime.now();
//                        List<ProductLicenses> listProductLicense = productLicensesRepository.findByUserIdAndProductId(it.getUserId(), p.getProductId());
//                        System.out.println("helloooooo"+listProductLicense.size());
//                        if(!listProductLicense.isEmpty()){
//                            String key = listProductLicense.get(0).getLicenseKey();
//                            //Lấy thơi gian ở giữa.
//                            String[] listS = key.split("-");
//                            if(listS.length >= 2){
//                                //Thoi gian ma key nay het han
//                                String lastTime = listS[1];
//                                System.out.println(lastTime);
//                                try{
//                                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
//                                    LocalDate date = LocalDate.parse(lastTime, formatter);
//                                    time = date.atStartOfDay();
//                                }catch (Exception e){
//
//                                }
//                            }
//                        }
                        String expStr = time.plusDays(30).format(java.time.format.DateTimeFormatter.BASIC_ISO_DATE);
                        for (int i = 0; i < buy; i++) {
                            String random = java.util.UUID.randomUUID().toString().replaceAll("-", "").substring(0, 12).toUpperCase();
                            String key = "PRD" + p.getProductId() + '-' + expStr + '-' + random;
                            ProductLicenses lic = new ProductLicenses();
                            lic.setOrderItemId(savedItem.getOrderItemId());
                            lic.setUserId(uid);
                            lic.setLicenseKey(key);
                            lic.setIsActive(true);
                            lic.setActivationDate(null);
                            lic.setLastUsedDate(null);
                            lic.setDeviceIdentifier(null);
                            lic.setCreatedAt(nowTs);
                            lic.setUpdatedAt(nowTs);
                            toInsert.add(lic);
                        }
                    }
                }
            });
        }

        if (!toInsert.isEmpty()) {
            var savedList = productLicensesRepository.saveAll(toInsert);
            try {
                for (ProductLicenses lic : savedList) {
                    try { licenseUsageLogService.append(lic.getLicenseId(), "generated", lic.getUserId(), null, null, null); } catch (Exception ignored) {}
                }
            } catch (Exception ignored) {}
        }

        // 🔹 Xóa tất cả cart (Public & non-Public)
        for (ShoppingCart it : items) {
            try { cartRepository.delete(it); } catch (Exception ignored) {}
        }

        // 🔹 Gửi thông báo đặt hàng thành công
        try {
            String orderCode = "ORD" + savedOrder.getOrderId();
            notificationService.createOrderNotification(uid, savedOrder.getOrderId(), orderCode);
        } catch (Exception e) {
            System.err.println("[CartService] Failed to send notification: " + e.getMessage());
        }

        // 🔹 Gửi email xác nhận đơn hàng + license keys
        try {
            String orderCode = "ORD" + savedOrder.getOrderId();
            String customerName = user.getFullName() != null && !user.getFullName().isEmpty()
                    ? user.getFullName() : user.getUsername();
            String customerEmail = user.getEmail();

            if (customerEmail != null && !customerEmail.isEmpty()) {
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

                java.util.List<EmailService.LicenseKeyInfo> emailLicenseKeys = new ArrayList<>();
                try {
                    org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(0, 1000);
                    var licensesPage = productLicensesRepository.findByOrderId(savedOrder.getOrderId(), pageable);
                    for (var licenseView : licensesPage.getContent()) {
                        String productName = null;
                        if (licenseView.getOrderItemId() != null) {
                            var orderItemOpt = orderItemsRepository.findById(licenseView.getOrderItemId());
                            if (orderItemOpt.isPresent()) {
                                var productOpt = productsRepository.findById(orderItemOpt.get().getProductId());
                                if (productOpt.isPresent()) {
                                    productName = productOpt.get().getName();
                                }
                            }
                        }
                        emailLicenseKeys.add(new EmailService.LicenseKeyInfo(
                                licenseView.getLicenseKey(),
                                productName != null ? productName : (licenseView.getProductName() != null ? licenseView.getProductName() : "Product"),
                                licenseView.getIsActive()
                        ));
                    }
                } catch (Exception e) {
                    System.err.println("[CartService] Failed to load license keys for email: " + e.getMessage());
                }

                emailService.sendOrderConfirmationEmail(
                        customerEmail,
                        customerName,
                        orderCode,
                        savedOrder.getOrderId(),
                        savedOrder.getTotalAmount(),
                        savedOrder.getCreatedAt(),
                        emailOrderItems,
                        emailLicenseKeys
                );
                System.out.println("[CartService] Order confirmation email sent to: " + customerEmail);
            }
        } catch (Exception e) {
            System.err.println("[CartService] Failed to send order confirmation email: " + e.getMessage());
            e.printStackTrace();
        }

        // 🔹 Xử lý voucher nếu có và lưu redemption records
        try {
            if (session != null && appliedVouchers != null && !appliedVouchers.isEmpty()) {
                for (Map.Entry<Long, String> entry : appliedVouchers.entrySet()) {
                    Long productId = entry.getKey();
                    String code = entry.getValue();
                    if (code != null) {
                        var candidates = vouchersRepository.findByCodeIgnoreCaseOrderByUpdatedAtDesc(code);
                        if (!candidates.isEmpty()) {
                            Vouchers v = candidates.get(0);
                            // Calculate actual discount for this product
                            BigDecimal discountAmount = BigDecimal.ZERO;
                            for (ShoppingCart it : validItems) {
                                if (it.getProductId().equals(productId)) {
                                    Products p = productsRepository.findById(productId).orElse(null);
                                    if (p != null) {
                                        BigDecimal unitPrice = p.getSalePrice() != null ? p.getSalePrice() : p.getPrice();
                                        int qty = it.getQuantity() != null ? it.getQuantity() : 1;
                                        BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(qty));
                                        
                                        if ("PERCENT".equalsIgnoreCase(v.getDiscountType())) {
                                            BigDecimal oneLicensePrice = unitPrice;
                                            discountAmount = oneLicensePrice.multiply(v.getDiscountValue().divide(new BigDecimal("100"), 2, java.math.RoundingMode.HALF_UP));
                                        } else {
                                            discountAmount = v.getDiscountValue();
                                        }
                                        if (discountAmount.compareTo(lineTotal) > 0) discountAmount = lineTotal;
                                    }
                                    break;
                                }
                            }
                            
                            VoucherRedemptions rec = new VoucherRedemptions();
                            rec.setVoucherId(v.getVoucherId());
                            rec.setOrderId(savedOrder.getOrderId());
                            rec.setUserId(uid);
                            rec.setDiscountAmount(discountAmount);
                            voucherRedemptionsRepository.save(rec);
                            v.setUsedCount((v.getUsedCount() == null ? 0 : v.getUsedCount()) + 1);
                            vouchersRepository.save(v);
                        }
                    }
                }
                // Clear applied vouchers after successful checkout
                session.removeAttribute("appliedVouchers");
            }
        } catch (Exception ignored) {}

        return "redirect:/customer/dashboard?purchase=success";
    }
}


