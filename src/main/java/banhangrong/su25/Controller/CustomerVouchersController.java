package banhangrong.su25.Controller;

import banhangrong.su25.Entity.Users;
import banhangrong.su25.Repository.ShoppingCartRepository;
import banhangrong.su25.Repository.UsersRepository;
import banhangrong.su25.Repository.VouchersRepository;
import banhangrong.su25.Repository.ProductsRepository;
import banhangrong.su25.Repository.ProductImagesRepository;
import banhangrong.su25.Entity.Vouchers;
import banhangrong.su25.Entity.Products;
import banhangrong.su25.Entity.VoucherRedemptions;
import banhangrong.su25.Repository.VoucherRedemptionsRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import jakarta.servlet.http.HttpSession;

@Controller
public class CustomerVouchersController {

    private final UsersRepository usersRepository;
    private final ShoppingCartRepository shoppingCartRepository;
    private final VouchersRepository vouchersRepository;
    private final ProductsRepository productsRepository;
    private final ProductImagesRepository productImagesRepository;
    private final VoucherRedemptionsRepository voucherRedemptionsRepository;

    public CustomerVouchersController(UsersRepository usersRepository,
                                      ShoppingCartRepository shoppingCartRepository,
                                      VouchersRepository vouchersRepository,
                                      ProductsRepository productsRepository,
                                      ProductImagesRepository productImagesRepository,
                                      VoucherRedemptionsRepository voucherRedemptionsRepository) {
        this.usersRepository = usersRepository;
        this.shoppingCartRepository = shoppingCartRepository;
        this.vouchersRepository = vouchersRepository;
        this.productsRepository = productsRepository;
        this.productImagesRepository = productImagesRepository;
        this.voucherRedemptionsRepository = voucherRedemptionsRepository;
    }

    @GetMapping("/vouchers")
    public String vouchers(Model model,
                           @org.springframework.web.bind.annotation.RequestParam(name = "q", required = false) String q,
                           @org.springframework.web.bind.annotation.RequestParam(name = "productId", required = false) Long productIdFilter,
                           @org.springframework.web.bind.annotation.RequestParam(name = "type", required = false) String discountType) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Users user = null;
        if (auth != null && auth.isAuthenticated()) {
            user = usersRepository.findByUsername(auth.getName()).orElse(null);
        }
        if (user != null) {
            model.addAttribute("user", user);
            try { model.addAttribute("cartCount", shoppingCartRepository.countByUserId(user.getUserId())); } catch (Exception ignored) {}
        }
        // Build available vouchers
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        java.util.List<java.util.Map<String,Object>> list = new java.util.ArrayList<>();
        try {
            for (Vouchers v : vouchersRepository.findAll()) {
                if (!"active".equalsIgnoreCase(v.getStatus())) continue;
                if (v.getStartAt() != null && now.isBefore(v.getStartAt())) continue;
                if (v.getEndAt() != null && now.isAfter(v.getEndAt())) continue;
                Products p = productsRepository.findById(v.getProductId()).orElse(null);
                if (p == null || p.getStatus() == null || !p.getStatus().equalsIgnoreCase("Public")) continue;
                java.util.Map<String,Object> m = new java.util.LinkedHashMap<>();
                m.put("code", v.getCode());
                m.put("type", v.getDiscountType());
                m.put("value", v.getDiscountValue());
                m.put("minOrder", v.getMinOrder());
                m.put("endAt", v.getEndAt());
                m.put("productId", p.getProductId());
                m.put("productName", p.getName());
                try {
                    var imgs = productImagesRepository.findTop1ByProductIdAndIsPrimaryTrueOrderByImageIdAsc(p.getProductId());
                    if (imgs != null && !imgs.isEmpty()) m.put("image", imgs.get(0).getImageUrl());
                    else {
                        var any = productImagesRepository.findTop1ByProductIdOrderByImageIdAsc(p.getProductId());
                        if (any != null && !any.isEmpty()) m.put("image", any.get(0).getImageUrl());
                    }
                } catch (Exception ignored) {}
                list.add(m);
            }
        } catch (Exception ignored) {}
        model.addAttribute("vouchers", list);

        // Build product options for searchable dropdown
        try {
            java.util.List<java.util.Map<String,Object>> productOptions = new java.util.ArrayList<>();
            for (Products p : productsRepository.findAll()) {
                if (p == null) continue;
                if (p.getStatus() != null && !"Public".equalsIgnoreCase(p.getStatus())) continue;
                java.util.Map<String,Object> m = new java.util.LinkedHashMap<>();
                m.put("id", p.getProductId());
                m.put("name", p.getName());
                productOptions.add(m);
            }
            if (productOptions.size() > 500) productOptions = productOptions.subList(0, 500);
            model.addAttribute("productOptions", productOptions);
        } catch (Exception ignored) {}

        // expose wallet vouchers from session
        try {
            java.util.Set<String> wallet = (java.util.Set<String>) ((org.springframework.web.context.request.ServletRequestAttributes) org.springframework.web.context.request.RequestContextHolder.currentRequestAttributes()).getRequest().getSession().getAttribute("walletVouchers");
            model.addAttribute("walletVouchers", wallet);
        } catch (Exception ignored) {}

        // Build customer-facing vouchers list with simple filters
        java.util.List<java.util.Map<String,Object>> tx = new java.util.ArrayList<>();
        try {
            java.util.List<Vouchers> allV = vouchersRepository.findAll();
            for (Vouchers v : allV) {
                if (v == null) continue;
                if (v.getStatus() != null && !"active".equalsIgnoreCase(v.getStatus())) continue;
                if (v.getEndAt() != null && v.getEndAt().isBefore(java.time.LocalDateTime.now())) continue;
                
                // Simple filters
                if (q != null && !q.isBlank()) {
                    String code = v.getCode() == null ? "" : v.getCode();
                    if (!code.toLowerCase().contains(q.trim().toLowerCase())) continue;
                }
                if (productIdFilter != null && !java.util.Objects.equals(v.getProductId(), productIdFilter)) continue;
                if (discountType != null && !discountType.isBlank() && v.getDiscountType() != null && !v.getDiscountType().equalsIgnoreCase(discountType.trim())) continue;

                int remaining = -1;
                try {
                    Integer max = v.getMaxUses();
                    int used = v.getUsedCount() != null ? v.getUsedCount() : 0;
                    remaining = (max != null) ? Math.max(0, max - used) : -1;
                } catch (Exception ignored) {}

                Products prod = null; 
                try { 
                    prod = productsRepository.findById(v.getProductId()).orElse(null); 
                } catch (Exception ignored) {}
                
                java.util.Map<String,Object> row = new java.util.LinkedHashMap<>();
                row.put("createdAt", v.getUpdatedAt() != null ? v.getUpdatedAt() : v.getCreatedAt());
                row.put("code", v.getCode());
                row.put("productId", v.getProductId());
                row.put("productName", prod != null ? prod.getName() : null);
                row.put("discountType", v.getDiscountType());
                row.put("voucherValue", v.getDiscountValue());
                row.put("minOrder", v.getMinOrder());
                row.put("endAt", v.getEndAt());
                row.put("remaining", remaining >= 0 ? remaining : null);
                tx.add(row);
            }
            tx.sort((a,b) -> {
                java.time.LocalDateTime da = (java.time.LocalDateTime) a.get("createdAt");
                java.time.LocalDateTime db = (java.time.LocalDateTime) b.get("createdAt");
                if (da == null && db == null) return 0; 
                if (da == null) return 1; 
                if (db == null) return -1;
                return db.compareTo(da);
            });
        } catch (Exception ignored) {}

        // Limit to 200 for UI
        if (tx.size() > 200) tx = tx.subList(0, 200);
        model.addAttribute("transactions", tx);
        java.util.Map<String,Object> filtersMap = new java.util.LinkedHashMap<>();
        if (q != null) filtersMap.put("q", q);
        if (productIdFilter != null) filtersMap.put("productId", productIdFilter);
        if (discountType != null) filtersMap.put("type", discountType);
        model.addAttribute("filters", filtersMap);
        return "customer/vouchers";
    }

    @PostMapping("/vouchers/save")
    public String saveVoucher(@org.springframework.web.bind.annotation.RequestParam("code") String code, HttpSession session) {
        if (code != null && !code.trim().isEmpty()) {
            java.util.Set<String> wallet = (java.util.Set<String>) session.getAttribute("walletVouchers");
            if (wallet == null) { wallet = new java.util.LinkedHashSet<>(); }
            wallet.add(code.trim());
            session.setAttribute("walletVouchers", wallet);
        }
        return "redirect:/vouchers?saved=1";
    }

    @PostMapping("/vouchers/remove-saved")
    public String removeSaved(@org.springframework.web.bind.annotation.RequestParam("code") String code, HttpSession session) {
        if (code != null && !code.trim().isEmpty()) {
            java.util.Set<String> wallet = (java.util.Set<String>) session.getAttribute("walletVouchers");
            if (wallet != null) { wallet.remove(code.trim()); session.setAttribute("walletVouchers", wallet); }
        }
        return "redirect:/vouchers?removed=1";
    }
}


