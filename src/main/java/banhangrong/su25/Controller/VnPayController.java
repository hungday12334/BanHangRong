package banhangrong.su25.Controller;

import banhangrong.su25.Entity.ShoppingCart;
import banhangrong.su25.Entity.Orders;
import banhangrong.su25.Entity.OrderItems;
import banhangrong.su25.Repository.ShoppingCartRepository;
import banhangrong.su25.Repository.ProductsRepository;
import banhangrong.su25.Repository.UsersRepository;
import banhangrong.su25.Repository.OrdersRepository;
import banhangrong.su25.Repository.OrderItemsRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import banhangrong.su25.Entity.Products;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.text.SimpleDateFormat;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.Calendar;
import java.util.TimeZone;
import banhangrong.su25.Util.VnPayConfig;

/**
 * Minimal VNPay sandbox integration for demo checkout from cart
 */
@Controller
public class VnPayController {

    private final ShoppingCartRepository cartRepository;
    private final ProductsRepository productsRepository;
    private final UsersRepository usersRepository;
    private final OrdersRepository ordersRepository;
    private final OrderItemsRepository orderItemsRepository;

    // Deprecated hardcoded values; kept for reference. Use VnPayConfig instead.
    // legacy constants removed; use values from VnPayConfig

    public VnPayController(ShoppingCartRepository cartRepository, ProductsRepository productsRepository, UsersRepository usersRepository, OrdersRepository ordersRepository, OrderItemsRepository orderItemsRepository) {
        this.cartRepository = cartRepository;
        this.productsRepository = productsRepository;
        this.usersRepository = usersRepository;
        this.ordersRepository = ordersRepository;
        this.orderItemsRepository = orderItemsRepository;
    }

    private Long getCurrentUserId() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
                return usersRepository.findByUsername(auth.getName()).map(u -> u.getUserId()).orElse(0L);
            }
        } catch (Exception ignored) {}
        return 0L;
    }

    @PostMapping(value = "/wallet/vnpay/create")
    public String createTopupPayment(HttpServletRequest req) {
        Long uid = getCurrentUserId();
        if (uid == 0L) {
            return "redirect:/login";
        }
        
        String vnp_Version = "2.1.0";
        String vnp_Command = "pay";
        String vnp_OrderInfo = "TOPUP:" + uid; // Special prefix for topup
        String orderType = "other";
        String vnp_TxnRef = VnPayConfig.getRandomNumber(8);
        String vnp_IpAddr = VnPayConfig.getIpAddress(req);
        String vnp_TmnCode = VnPayConfig.vnp_TmnCode;

        long amountVnd = normalizeAmountVnd(req.getParameter("amount"));
        // Enforce business minimum topup of 20,000 VND (higher than VNPay's 5,000)
        if (amountVnd < 20000L) amountVnd = 20000L;

        // VNPay expects smallest unit: VND * 100
        long amount = amountVnd * 100L;

        Map<String, String> vnp_Params = new HashMap<>();
        vnp_Params.put("vnp_Version", vnp_Version);
        vnp_Params.put("vnp_Command", vnp_Command);
        vnp_Params.put("vnp_TmnCode", vnp_TmnCode);
        vnp_Params.put("vnp_Amount", String.valueOf(amount));
        vnp_Params.put("vnp_CurrCode", "VND");
        vnp_Params.put("vnp_TxnRef", vnp_TxnRef);
        vnp_Params.put("vnp_OrderInfo", vnp_OrderInfo);
        vnp_Params.put("vnp_OrderType", orderType);
        vnp_Params.put("vnp_Locale", "vn");
        vnp_Params.put("vnp_ReturnUrl", VnPayConfig.vnp_ReturnUrl);
        vnp_Params.put("vnp_IpAddr", vnp_IpAddr);

        Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        String vnp_CreateDate = formatter.format(cld.getTime());
        vnp_Params.put("vnp_CreateDate", vnp_CreateDate);

        cld.add(Calendar.MINUTE, 15);
        String vnp_ExpireDate = formatter.format(cld.getTime());
        vnp_Params.put("vnp_ExpireDate", vnp_ExpireDate);

        List<String> fieldNames = new ArrayList<>(vnp_Params.keySet());
        Collections.sort(fieldNames);
        StringBuilder query = new StringBuilder();
        Iterator<String> itr = fieldNames.iterator();
        boolean first = true;
        while (itr.hasNext()) {
            String fieldName = itr.next();
            String fieldValue = vnp_Params.get(fieldName);
            if ((fieldValue != null) && (fieldValue.length() > 0)) {
                if (!first) query.append('&');
                first = false;
                try {
                    query.append(URLEncoder.encode(fieldName, StandardCharsets.UTF_8.toString()));
                    query.append('=');
                    query.append(URLEncoder.encode(fieldValue, StandardCharsets.UTF_8.toString()));
                } catch (Exception e) {
                    query.append(fieldName);
                    query.append('=');
                    query.append(fieldValue);
                }
            }
        }
        String queryUrl = query.toString();
        // Sign exactly the URL-encoded query string that will be sent (without vnp_SecureHash*)
        String vnp_SecureHash = VnPayConfig.hmacSHA512(VnPayConfig.vnp_HashSecret, queryUrl);
        queryUrl += "&vnp_SecureHash=" + vnp_SecureHash + "&vnp_SecureHashType=HmacSHA512";
        String paymentUrl = VnPayConfig.vnp_PayUrl + "?" + queryUrl;
        return "redirect:" + paymentUrl;
    }

    @PostMapping(value = "/payment/vnpay/create")
    public String createPayment(HttpServletRequest req) {
        String vnp_Version = "2.1.0";
        String vnp_Command = "pay";
        String vnp_OrderInfo = req.getParameter("vnp_OrderInfo");
        String orderType = req.getParameter("ordertype");
        String vnp_TxnRef = VnPayConfig.getRandomNumber(8);
        String vnp_IpAddr = VnPayConfig.getIpAddress(req);
        String vnp_TmnCode = VnPayConfig.vnp_TmnCode;

        long amountVnd = normalizeAmountVnd(req.getParameter("amount"));
        if (amountVnd == 0L) {
            // Fallback: calculate from cart
        BigDecimal total = cartRepository.findByUserId(getCurrentUserId()).stream()
                .map(it -> {
                    Products p = productsRepository.findById(it.getProductId()).orElse(null);
                    if (p == null) return BigDecimal.ZERO;
                    BigDecimal unit = p.getSalePrice() != null ? p.getSalePrice() : p.getPrice();
                    int qty = it.getQuantity() != null ? it.getQuantity() : 1;
                    return unit.multiply(BigDecimal.valueOf(qty));
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            amountVnd = total.longValue();
            if (amountVnd < 5000L) amountVnd = 5000L; // ensure VNPay min
        }
        // VNPay expects smallest unit: VND * 100
        long amount = amountVnd * 100L;

        Map<String, String> vnp_Params = new HashMap<>();
        vnp_Params.put("vnp_Version", vnp_Version);
        vnp_Params.put("vnp_Command", vnp_Command);
        vnp_Params.put("vnp_TmnCode", vnp_TmnCode);
        vnp_Params.put("vnp_Amount", String.valueOf(amount));
        vnp_Params.put("vnp_CurrCode", "VND");
        String bank_code = req.getParameter("bankcode");
        if (bank_code != null && !bank_code.isEmpty()) {
            vnp_Params.put("vnp_BankCode", bank_code);
        }
        vnp_Params.put("vnp_TxnRef", vnp_TxnRef);
        vnp_Params.put("vnp_OrderInfo", vnp_OrderInfo);
        vnp_Params.put("vnp_OrderType", orderType);
        vnp_Params.put("vnp_Locale", "vn");
        vnp_Params.put("vnp_ReturnUrl", VnPayConfig.vnp_ReturnUrl);
        vnp_Params.put("vnp_IpAddr", vnp_IpAddr);

        Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        String vnp_CreateDate = formatter.format(cld.getTime());
        vnp_Params.put("vnp_CreateDate", vnp_CreateDate);

        cld.add(Calendar.MINUTE, 15);
        String vnp_ExpireDate = formatter.format(cld.getTime());
        vnp_Params.put("vnp_ExpireDate", vnp_ExpireDate);

        List<String> fieldNames = new ArrayList<>(vnp_Params.keySet());
        Collections.sort(fieldNames);
        StringBuilder query = new StringBuilder();
        Iterator<String> itr = fieldNames.iterator();
        boolean first = true;
        while (itr.hasNext()) {
            String fieldName = itr.next();
            String fieldValue = vnp_Params.get(fieldName);
            if ((fieldValue != null) && (fieldValue.length() > 0)) {
                if (!first) query.append('&');
                first = false;
                try {
                    query.append(URLEncoder.encode(fieldName, StandardCharsets.UTF_8.toString()));
                    query.append('=');
                    query.append(URLEncoder.encode(fieldValue, StandardCharsets.UTF_8.toString()));
                } catch (Exception e) {
                    query.append(fieldName);
                    query.append('=');
                    query.append(fieldValue);
                }
            }
        }
        String queryUrl = query.toString();
        // Sign exactly the URL-encoded query string that will be sent (without vnp_SecureHash*)
        String vnp_SecureHash = VnPayConfig.hmacSHA512(VnPayConfig.vnp_HashSecret, queryUrl);
        queryUrl += "&vnp_SecureHash=" + vnp_SecureHash + "&vnp_SecureHashType=HmacSHA512";
        String paymentUrl = VnPayConfig.vnp_PayUrl + "?" + queryUrl;
        return "redirect:" + paymentUrl;
    }

    @GetMapping("/payment/vnpay/return")
    @Transactional
    public String paymentReturn(HttpServletRequest request, Model model) {
        try {
            Map<String,String[]> fields = request.getParameterMap();
            Map<String,String> vnp = new HashMap<>();
            for (Map.Entry<String,String[]> e : fields.entrySet()) {
                if (e.getKey().startsWith("vnp_")) vnp.put(e.getKey(), e.getValue()[0]);
            }
            String receivedHash = vnp.remove("vnp_SecureHash");
            vnp.remove("vnp_SecureHashType");
            String signData = VnPayConfig.buildSignData(new TreeMap<>(vnp));
            String calcHash = VnPayConfig.hmacSHA512(VnPayConfig.vnp_HashSecret, signData);
            boolean valid = calcHash.equals(receivedHash);
            String respCode = vnp.getOrDefault("vnp_ResponseCode", "99");
            boolean success = "00".equals(respCode) && valid;

            // Detect top-up flow by OrderInfo prefix
            String orderInfo = vnp.getOrDefault("vnp_OrderInfo", "");
            if (orderInfo.startsWith("TOPUP:")) {
                if (success) {
                    try {
                        Long uid = Long.parseLong(orderInfo.substring("TOPUP:".length()));
                        String amountStr = vnp.getOrDefault("vnp_Amount", "0");
                        // vnp_Amount is VND*100, convert back
                        final long creditedAmountVnd;
                        try { creditedAmountVnd = Long.parseLong(amountStr) / 100L; } catch (Exception ex) { return "redirect:/customer/dashboard?topup=failure&code=99"; }
                        System.out.println("[VNPay] TOPUP success for uid=" + uid + ", rawAmount=" + amountStr);
                        usersRepository.findById(uid).ifPresent(u -> {
                            java.math.BigDecimal current = u.getBalance() != null ? u.getBalance() : java.math.BigDecimal.ZERO;
                            java.math.BigDecimal inc = java.math.BigDecimal.valueOf(creditedAmountVnd);
                            java.math.BigDecimal next = current.add(inc);
                            u.setBalance(next);
                            try { usersRepository.saveAndFlush(u); } catch (Exception ignored) { usersRepository.save(u); }
                            System.out.println("[VNPay] Balance updated: " + current + " -> " + next);
                        });
                        return "redirect:/customer/dashboard?topup=success&amount=" + creditedAmountVnd;
                    } catch (Exception e) {
                        return "redirect:/customer/dashboard?topup=failure&code=99";
                    }
                } else {
                    return "redirect:/customer/dashboard?topup=failure&code=" + respCode;
                }
            } else {
                // Handle regular product purchase
                if (success) {
                    // on success: subtract stock, deduct money from wallet, and clear cart
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
                    banhangrong.su25.Entity.Users user = usersRepository.findById(uid).orElse(null);
                    if (user == null) {
                        System.out.println("[VNPay] Payment failed: user not found");
                        return "redirect:/customer/dashboard?purchase=failure&reason=user_not_found";
                    }
                    
                    BigDecimal currentBalance = user.getBalance() != null ? user.getBalance() : BigDecimal.ZERO;
                    if (currentBalance.compareTo(totalAmount) < 0) {
                        System.out.println("[VNPay] Payment failed: insufficient balance for user " + uid);
                        return "redirect:/customer/dashboard?purchase=failure&reason=insufficient_balance";
                    }
                    
                    BigDecimal newBalance = currentBalance.subtract(totalAmount);
                    user.setBalance(newBalance);
                    usersRepository.save(user);
                    System.out.println("[VNPay] Payment success: deducted " + totalAmount + " from user " + uid + ", new balance: " + newBalance);
                    
                    // Create order
                    Orders order = new Orders();
                    order.setUserId(uid);
                    order.setTotalAmount(totalAmount);
                    order.setStatus("completed"); // VNPay payment is completed
                    order.setCreatedAt(LocalDateTime.now());
                    order.setUpdatedAt(LocalDateTime.now());
                    
                    // For demo: assume all products are from the same seller (sellerId = 1)
                    order.setSellerId(1L);
                    
                    Orders savedOrder = ordersRepository.save(order);
                    System.out.println("[VNPay] Created order: " + savedOrder.getOrderId());
                    
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
                            System.out.println("[VNPay] Created order item: " + orderItem.getOrderItemId());
                        }
                    }
                    
                    // Update product stock and sales
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
                    
                    // Clear cart
                    for (ShoppingCart it : items) { 
                        try { cartRepository.delete(it); } catch (Exception ignored) {} 
                    }
                    
                    return "redirect:/customer/dashboard?purchase=success";
                } else {
                    return "redirect:/customer/dashboard?purchase=failure&code=" + respCode;
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            return "redirect:/customer/dashboard?topup=failure&code=err";
        }
    }

    // Unused helpers below kept previously for redirect flow have been removed.

    private static long normalizeAmountVnd(String raw) {
        if (raw == null) return 0L;
        // Strip all non-digits (handle inputs like "2.000.000" or "2,000,000")
        String digits = raw.replaceAll("[^0-9]", "");
        try { return Long.parseLong(digits); } catch (NumberFormatException ex) { return 0L; }
    }
}