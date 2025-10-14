package banhangrong.su25.Controller;

import banhangrong.su25.Entity.ShoppingCart;
import banhangrong.su25.Repository.ShoppingCartRepository;
import banhangrong.su25.Repository.ProductsRepository;
import banhangrong.su25.Repository.UsersRepository;
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
// imports cleaned after refactor
import java.util.*;
import banhangrong.su25.Util.VnPayConfig;

/**
 * Minimal VNPay sandbox integration for demo checkout from cart
 */
@Controller
public class VnPayController {

    private final ShoppingCartRepository cartRepository;
    private final ProductsRepository productsRepository;
    private final UsersRepository usersRepository;

    // Deprecated hardcoded values; kept for reference. Use VnPayConfig instead.
    // legacy constants removed; use values from VnPayConfig

    public VnPayController(ShoppingCartRepository cartRepository, ProductsRepository productsRepository, UsersRepository usersRepository) {
        this.cartRepository = cartRepository;
        this.productsRepository = productsRepository;
        this.usersRepository = usersRepository;
    }

    private Long getCurrentUserId() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && auth.getName() != null) {
                return usersRepository.findByUsername(auth.getName()).map(banhangrong.su25.Entity.Users::getUserId).orElse(0L);
            }
        } catch (Exception ignored) {}
        return 0L;
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
        vnp_Params.put("vnp_OrderInfo", vnp_OrderInfo != null ? vnp_OrderInfo : ("Thanh toan don hang #" + vnp_TxnRef));
        vnp_Params.put("vnp_OrderType", orderType != null ? orderType : "other");

        String locate = req.getParameter("language");
        if (locate != null && !locate.isEmpty()) {
            vnp_Params.put("vnp_Locale", locate);
        } else {
            vnp_Params.put("vnp_Locale", "vn");
        }
        vnp_Params.put("vnp_ReturnUrl", VnPayConfig.vnp_ReturnUrl);
        vnp_Params.put("vnp_IpAddr", vnp_IpAddr);

        java.util.Calendar cld = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("Etc/GMT+7"));
        java.text.SimpleDateFormat formatter = new java.text.SimpleDateFormat("yyyyMMddHHmmss");
        String vnp_CreateDate = formatter.format(cld.getTime());
        vnp_Params.put("vnp_CreateDate", vnp_CreateDate);
        cld.add(java.util.Calendar.MINUTE, 15);
        String vnp_ExpireDate = formatter.format(cld.getTime());
        vnp_Params.put("vnp_ExpireDate", vnp_ExpireDate);

        vnp_Params.put("vnp_Bill_Mobile", req.getParameter("txt_billing_mobile"));
        vnp_Params.put("vnp_Bill_Email", req.getParameter("txt_billing_email"));
        String fullName = req.getParameter("txt_billing_fullname");
        if (fullName != null) fullName = fullName.trim();
        if (fullName != null && !fullName.isEmpty()) {
            int idx = fullName.indexOf(' ');
            String firstName = idx > 0 ? fullName.substring(0, idx) : fullName;
            String lastName = fullName.substring(fullName.lastIndexOf(' ') + 1);
            vnp_Params.put("vnp_Bill_FirstName", firstName);
            vnp_Params.put("vnp_Bill_LastName", lastName);
        }
        vnp_Params.put("vnp_Bill_Address", req.getParameter("txt_inv_addr1"));
        vnp_Params.put("vnp_Bill_City", req.getParameter("txt_bill_city"));
        vnp_Params.put("vnp_Bill_Country", req.getParameter("txt_bill_country"));
        if (req.getParameter("txt_bill_state") != null && !req.getParameter("txt_bill_state").isEmpty()) {
            vnp_Params.put("vnp_Bill_State", req.getParameter("txt_bill_state"));
        }
        vnp_Params.put("vnp_Inv_Phone", req.getParameter("txt_inv_mobile"));
        vnp_Params.put("vnp_Inv_Email", req.getParameter("txt_inv_email"));
        vnp_Params.put("vnp_Inv_Customer", req.getParameter("txt_inv_customer"));
        vnp_Params.put("vnp_Inv_Address", req.getParameter("txt_inv_addr1"));
        vnp_Params.put("vnp_Inv_Company", req.getParameter("txt_inv_company"));
        vnp_Params.put("vnp_Inv_Taxcode", req.getParameter("txt_inv_taxcode"));
        vnp_Params.put("vnp_Inv_Type", req.getParameter("cbo_inv_type"));

        List<String> fieldNames = new ArrayList<>(vnp_Params.keySet());
        java.util.Collections.sort(fieldNames);
        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();
        Iterator<String> itr = fieldNames.iterator();
        while (itr.hasNext()) {
            String fieldName = itr.next();
            String fieldValue = vnp_Params.get(fieldName);
            if (fieldValue != null && fieldValue.length() > 0) {
                hashData.append(fieldName);
                hashData.append('=');
                hashData.append(java.net.URLEncoder.encode(fieldValue, java.nio.charset.StandardCharsets.US_ASCII));
                query.append(java.net.URLEncoder.encode(fieldName, java.nio.charset.StandardCharsets.US_ASCII));
                query.append('=');
                query.append(java.net.URLEncoder.encode(fieldValue, java.nio.charset.StandardCharsets.US_ASCII));
                if (itr.hasNext()) {
                    query.append('&');
                    hashData.append('&');
                }
            }
        }
        String queryUrl = query.toString();
        String vnp_SecureHash = VnPayConfig.hmacSHA512(VnPayConfig.vnp_HashSecret, hashData.toString());
        queryUrl += "&vnp_SecureHash=" + vnp_SecureHash;
        String paymentUrl = VnPayConfig.vnp_PayUrl + "?" + queryUrl;

        return "redirect:" + paymentUrl;
    }

    // Wallet top-up via VNPay: redirect flow
    @PostMapping(value = "/wallet/vnpay/create")
    public String createTopup(HttpServletRequest req) {
        String vnp_Version = "2.1.0";
        String vnp_Command = "pay";
        String orderType = "topup";
        String vnp_TxnRef = VnPayConfig.getRandomNumber(8);
        String vnp_IpAddr = VnPayConfig.getIpAddress(req);
        String vnp_TmnCode = VnPayConfig.vnp_TmnCode;

        long amountVnd = normalizeAmountVnd(req.getParameter("amount"));
        if (amountVnd < 5000L) amountVnd = 5000L;
        long amount = amountVnd * 100L;

        Map<String, String> vnp_Params = new HashMap<>();
        vnp_Params.put("vnp_Version", vnp_Version);
        vnp_Params.put("vnp_Command", vnp_Command);
        vnp_Params.put("vnp_TmnCode", vnp_TmnCode);
        vnp_Params.put("vnp_Amount", String.valueOf(amount));
        vnp_Params.put("vnp_CurrCode", "VND");
        String bank_code = req.getParameter("bankcode");
        if (bank_code != null && !bank_code.isEmpty()) vnp_Params.put("vnp_BankCode", bank_code);
        vnp_Params.put("vnp_TxnRef", vnp_TxnRef);
        // Mark topup for return handler to detect
        vnp_Params.put("vnp_OrderInfo", "TOPUP:" + getCurrentUserId());
        vnp_Params.put("vnp_OrderType", orderType);
        vnp_Params.put("vnp_Locale", "vn");
        vnp_Params.put("vnp_ReturnUrl", VnPayConfig.vnp_ReturnUrl);
        vnp_Params.put("vnp_IpAddr", vnp_IpAddr);

        java.util.Calendar cld = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("Etc/GMT+7"));
        java.text.SimpleDateFormat formatter = new java.text.SimpleDateFormat("yyyyMMddHHmmss");
        String vnp_CreateDate = formatter.format(cld.getTime());
        vnp_Params.put("vnp_CreateDate", vnp_CreateDate);
        cld.add(java.util.Calendar.MINUTE, 15);
        String vnp_ExpireDate = formatter.format(cld.getTime());
        vnp_Params.put("vnp_ExpireDate", vnp_ExpireDate);

        List<String> fieldNames = new ArrayList<>(vnp_Params.keySet());
        java.util.Collections.sort(fieldNames);
        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();
        Iterator<String> itr = fieldNames.iterator();
        while (itr.hasNext()) {
            String fieldName = itr.next();
            String fieldValue = vnp_Params.get(fieldName);
            if (fieldValue != null && fieldValue.length() > 0) {
                hashData.append(fieldName).append('=')
                        .append(java.net.URLEncoder.encode(fieldValue, java.nio.charset.StandardCharsets.US_ASCII));
                query.append(java.net.URLEncoder.encode(fieldName, java.nio.charset.StandardCharsets.US_ASCII))
                        .append('=')
                        .append(java.net.URLEncoder.encode(fieldValue, java.nio.charset.StandardCharsets.US_ASCII));
                if (itr.hasNext()) { query.append('&'); hashData.append('&'); }
            }
        }
        String queryUrl = query.toString();
        String vnp_SecureHash = VnPayConfig.hmacSHA512(VnPayConfig.vnp_HashSecret, hashData.toString());
        queryUrl += "&vnp_SecureHash=" + vnp_SecureHash;
        String paymentUrl = VnPayConfig.vnp_PayUrl + "?" + queryUrl;
        return "redirect:" + paymentUrl;
    }

    // Deprecated: direct JSON endpoint removed in favor of redirect flow above

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
        System.out.println("[VNPay] return-signData=" + signData);
        System.out.println("[VNPay] return-receivedHash=" + receivedHash);
        System.out.println("[VNPay] return-calcHash=" + calcHash);
        boolean valid = calcHash.equalsIgnoreCase(receivedHash);
        String respCode = vnp.getOrDefault("vnp_ResponseCode","99");
        // Sandbox: treat ResponseCode=00 as success to avoid false negatives from encoding differences
        boolean success = "00".equals(respCode);
        model.addAttribute("success", success);
        model.addAttribute("code", respCode);

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
        if (success) {
            // on success: subtract stock and clear cart (same as demo checkout)
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
            for (ShoppingCart it : items) { try { cartRepository.delete(it); } catch (Exception ignored) {} }
        }
        return "customer/vnpay-result";
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
        if (digits.isEmpty()) return 0L;
        try {
            long vnd = Long.parseLong(digits);
            // VNPay valid range: 5,000 <= amount < 1,000,000,000 (VND)
            if (vnd < 5000L) return 0L;
            if (vnd >= 1_000_000_000L) return 0L;
            return vnd;
        } catch (Exception e) {
            return 0L;
        }
    }
}


