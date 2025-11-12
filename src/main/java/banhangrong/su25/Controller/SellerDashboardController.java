package banhangrong.su25.Controller;

import banhangrong.su25.Repository.ProductsRepository;
import banhangrong.su25.Repository.ProductLicensesRepository;
import banhangrong.su25.Repository.SellerOrderRepository;
import banhangrong.su25.Repository.UsersRepository;
import banhangrong.su25.Entity.Users;
import banhangrong.su25.Entity.Products;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import jakarta.servlet.http.HttpSession;
import java.security.Principal;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Controller
public class SellerDashboardController {

    private final ProductsRepository productsRepository;
    private final UsersRepository usersRepository;
    private final SellerOrderRepository sellerOrderRepository;
    private final ProductLicensesRepository productLicensesRepository;

    public SellerDashboardController(ProductsRepository productsRepository,
            UsersRepository usersRepository,
            SellerOrderRepository sellerOrderRepository,
            ProductLicensesRepository productLicensesRepository) {
        this.productsRepository = productsRepository;
        this.usersRepository = usersRepository;
        this.sellerOrderRepository = sellerOrderRepository;
        this.productLicensesRepository = productLicensesRepository;
    }

    @GetMapping("/seller/dashboard")
    public String dashboard(@RequestParam(name = "sellerId", required = false) Long sellerId,
            Model model,
            Principal principal,
            HttpSession session) {

        // ⭐ SECURITY CHECK: Kiểm tra user có phải SELLER không
        Users currentUser = null;
        if (principal != null && principal.getName() != null) {
            var opt = usersRepository.findByUsername(principal.getName());
            if (opt.isPresent()) {
                currentUser = opt.get();
            }
        }
        
        // Nếu không tìm được từ principal, thử từ session
        if (currentUser == null && session != null) {
            Object userObj = session.getAttribute("user");
            if (userObj instanceof Users) {
                currentUser = (Users) userObj;
            } else {
                Object userIdObj = session.getAttribute("userId");
                if (userIdObj != null) {
                    Long userId = userIdObj instanceof Long ? (Long) userIdObj : 
                                 userIdObj instanceof Integer ? ((Integer) userIdObj).longValue() : null;
                    if (userId != null) {
                        var opt = usersRepository.findById(userId);
                        if (opt.isPresent()) {
                            currentUser = opt.get();
                        }
                    }
                }
            }
        }
        
        // Kiểm tra userType
        if (currentUser != null) {
            String userType = currentUser.getUserType();
            if (userType != null) {
                userType = userType.trim().toUpperCase();
            }
            
            if (!"SELLER".equals(userType) && !"ADMIN".equals(userType)) {
                // Nếu không phải SELLER hoặc ADMIN, redirect về customer dashboard
                System.out.println("⚠ Access denied: User " + currentUser.getUsername() + 
                                 " (userType: " + currentUser.getUserType() + ") tried to access seller dashboard");
                return "redirect:/customer/dashboard?error=access_denied";
            }
        } else {
            // Nếu không tìm được user, redirect về login
            System.out.println("⚠ No user found, redirecting to login");
            return "redirect:/login?error=session_expired";
        }

        if (sellerId == null) {
            // Sử dụng userId của user hiện tại
            sellerId = currentUser.getUserId();
            
            // Try principal (fallback)
            if (sellerId == null && principal != null && principal.getName() != null) {
                String name = principal.getName();
                try {
                    sellerId = Long.parseLong(name);
                } catch (NumberFormatException e) {
                    // Not a numeric principal name, try lookup by username
                    var opt = usersRepository.findByUsername(name);
                    if (opt.isPresent())
                        sellerId = opt.get().getUserId();
                }
            }

            // Try session attributes (fallback)
            if (sellerId == null && session != null) {
                Object uid = session.getAttribute("userId");
                if (uid instanceof Long)
                    sellerId = (Long) uid;
                else if (uid instanceof Integer)
                    sellerId = ((Integer) uid).longValue();
            }
        }
        
        // Đảm bảo sellerId không null
        if (sellerId == null) {
            System.out.println("⚠ SellerId is null, redirecting to customer dashboard");
            return "redirect:/customer/dashboard?error=invalid_seller";
        }

        // KPIs
        BigDecimal totalRevenue = Optional.ofNullable(productsRepository.totalRevenueBySeller(sellerId))
                .orElse(BigDecimal.ZERO);
        Long totalUnits = Optional.ofNullable(productsRepository.totalUnitsSoldBySeller(sellerId))
                .orElse(0L);
        Long totalOrders = Optional.ofNullable(productsRepository.totalOrdersBySeller(sellerId))
                .orElse(0L);
        BigDecimal avgRating = Optional.ofNullable(productsRepository.averageRatingBySeller(sellerId))
                .orElse(BigDecimal.ZERO);

        // Today/This month
        BigDecimal todayRev = Optional.ofNullable(productsRepository.todayRevenue(sellerId))
                .orElse(BigDecimal.ZERO);
        BigDecimal monthRev = Optional.ofNullable(productsRepository.thisMonthRevenue(sellerId))
                .orElse(BigDecimal.ZERO);

        // Daily revenue for default range: last 15 days (inclusive today)
        LocalDateTime from = LocalDateTime.now().minus(14, ChronoUnit.DAYS).truncatedTo(ChronoUnit.DAYS);
        List<Object[]> raw = productsRepository.dailyRevenueFrom(sellerId, from);
        // Build date -> revenue map covering all days
        LinkedHashMap<String, BigDecimal> series = new LinkedHashMap<>();
        for (int i = 14; i >= 0; i--) {
            LocalDateTime d = LocalDateTime.now().minus(i, ChronoUnit.DAYS).truncatedTo(ChronoUnit.DAYS);
            series.put(d.toLocalDate().toString(), BigDecimal.ZERO);
        }
        for (Object[] row : raw) {
            // Normalize various date types returned by native query into yyyy-MM-dd strings
            Object dObj = row[0];
            String dateKey = null;
            try {
                if (dObj instanceof java.sql.Date) {
                    dateKey = ((java.sql.Date) dObj).toLocalDate().toString();
                } else if (dObj instanceof java.sql.Timestamp) {
                    dateKey = ((java.sql.Timestamp) dObj).toLocalDateTime().toLocalDate().toString();
                } else if (dObj instanceof java.time.LocalDate) {
                    dateKey = dObj.toString();
                } else if (dObj instanceof java.time.LocalDateTime) {
                    dateKey = ((java.time.LocalDateTime) dObj).toLocalDate().toString();
                } else {
                    String s = Objects.toString(dObj, "");
                    // If the DB driver returns a datetime string like "2025-09-28 00:00:00",
                    // take the first 10 chars which correspond to yyyy-MM-dd
                    if (s.length() >= 10)
                        dateKey = s.substring(0, 10);
                    else
                        dateKey = s;
                }
            } catch (Exception ex) {
                dateKey = Objects.toString(dObj, "");
            }
            if (dateKey == null)
                continue;
            BigDecimal rev = BigDecimal.ZERO;
            if (row.length > 1 && row[1] != null) {
                if (row[1] instanceof BigDecimal)
                    rev = (BigDecimal) row[1];
                else {
                    try {
                        rev = new BigDecimal(row[1].toString());
                    } catch (Exception e) {
                        rev = BigDecimal.ZERO;
                    }
                }
            }
            // Only put into the series if the dateKey exists (guards against formatting
            // mismatches)
            if (series.containsKey(dateKey)) {
                series.put(dateKey, rev);
            }
        }

        // Top products
        List<Map<String, Object>> topProducts = new ArrayList<>();
        for (Object[] row : productsRepository.topProducts(sellerId)) {
            Map<String, Object> m = new HashMap<>();
            m.put("productId", row[0]);
            m.put("name", row[1]);
            m.put("units", row[2]);
            m.put("revenue", row[3]);
            m.put("rating", row[4]);
            topProducts.add(m);
        }

        // Recent orders (strictly scoped to this seller)
        List<Map<String, Object>> recentOrders = new ArrayList<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        var pageableRecent = org.springframework.data.domain.PageRequest.of(0, 8, org.springframework.data.domain.Sort
                .by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt"));
        var pageRecent = sellerOrderRepository.findSellerOrders(sellerId, null, null, null, pageableRecent);
        for (SellerOrderRepository.SellerOrderSummary s : pageRecent.getContent()) {
            Map<String, Object> m = new HashMap<>();
            m.put("orderId", s.getOrderId());
            LocalDateTime ldt = s.getCreatedAt();
            String createdAtStr = ldt != null ? ldt.format(fmt) : "";
            m.put("createdAtStr", createdAtStr);
            m.put("amount", s.getSellerAmount());
            m.put("items", s.getSellerItems());
            recentOrders.add(m);
        }
        // Low stock products based on product quantity directly (quantity <= 5)
        List<Products> sellerProducts = productsRepository.findBySellerId(sellerId);
        List<Map<String, Object>> lowStock = new ArrayList<>();
        for (var prod : sellerProducts) {
            if (prod == null) continue;
            String st = prod.getStatus();
            if (st == null || !"public".equalsIgnoreCase(st.trim())) continue;
            Long pid = prod.getProductId();
            int quantity = prod.getQuantity() != null ? prod.getQuantity() : 0;
            if (quantity <= 5) {
                Map<String, Object> m = new HashMap<>();
                m.put("productId", pid);
                m.put("name", prod.getName());
                // Show quantity directly in the Remaining column
                m.put("remaining", quantity);
                m.put("status", prod.getStatus());
                lowStock.add(m);
            }
        }
        // sort by ascending remaining and limit to 10
        lowStock.sort(Comparator.comparingLong(m -> ((Number) m.getOrDefault("remaining", 0)).longValue()));
        if (lowStock.size() > 10)
            lowStock = lowStock.subList(0, 10);
        long activeProducts = productsRepository.countBySellerIdAndStatus(sellerId, "public");

        // "My products" for initial server-side render: include basic fields (id, name,
        // price, quantity, status)
        List<Map<String, Object>> myProducts = new ArrayList<>();
        for (Products prod : sellerProducts) {
            if (prod == null)
                continue;
            Map<String, Object> m = new HashMap<>();
            m.put("productId", prod.getProductId());
            m.put("name", prod.getName());
            m.put("price", prod.getPrice());
            m.put("quantity", prod.getQuantity());
            m.put("status", prod.getStatus());
            myProducts.add(m);
        }

        // Seller ranking (revenue-based)
        Integer myRank = productsRepository.sellerRevenueRank(sellerId);
        Long totalSellers = Optional.ofNullable(productsRepository.totalSellers()).orElse(0L);
        double percentile = (myRank != null && totalSellers > 0) ? (100.0 * (totalSellers - myRank + 1) / totalSellers)
                : 0.0;
        List<Map<String, Object>> topSellers = new ArrayList<>();
        for (Object[] row : productsRepository.topSellers()) {
            Map<String, Object> m = new HashMap<>();
            m.put("sellerId", row[0]);
            m.put("username", row[1]);
            m.put("revenue", row[2]);
            m.put("units", row[3]);
            topSellers.add(m);
        }

        model.addAttribute("sellerId", sellerId);
        // Also expose userId for clarity: the sellerId is the same as the logged-in
        // user's id
        model.addAttribute("userId", sellerId);
        model.addAttribute("totalRevenue", totalRevenue);
        model.addAttribute("totalUnits", totalUnits);
        model.addAttribute("totalOrders", totalOrders);
        model.addAttribute("avgRating", avgRating);
        model.addAttribute("todayRevenue", todayRev);
        model.addAttribute("monthRevenue", monthRev);
        model.addAttribute("dailyRevenueLabels", String.join(",", series.keySet()));
        model.addAttribute("dailyRevenueData",
                String.join(",", series.values().stream().map(BigDecimal::toPlainString).toList()));
        model.addAttribute("topProducts", topProducts);
        model.addAttribute("recentOrders", recentOrders);
        model.addAttribute("lowStock", lowStock);
        model.addAttribute("activeProducts", activeProducts);
        model.addAttribute("myRank", myRank == null ? 0 : myRank);
        model.addAttribute("totalSellers", totalSellers);
        model.addAttribute("rankPercentile", percentile);
        model.addAttribute("topSellers", topSellers);

        // Load user profile (assume sellerId == userId for now)
        Users user = usersRepository.findById(sellerId).orElse(null);
        model.addAttribute("user", user);
        if (user != null) {
            model.addAttribute("userType", user.getUserType());
        }
        // Provide server-side product list so the dashboard can render statuses
        // immediately
        model.addAttribute("myProducts", myProducts);

        return "pages/seller/seller_dashboard";
    }

    // Revenue series API for dynamic ranges (defaults to 15 days)
    @GetMapping("/api/seller/{sellerId}/revenue-series")
    public org.springframework.http.ResponseEntity<?> revenueSeries(
            @org.springframework.web.bind.annotation.PathVariable Long sellerId,
            @RequestParam(name = "days", required = false, defaultValue = "15") Integer days) {
        int safeDays = (days == null || days < 1) ? 15 : Math.min(days, 365);
        LocalDateTime from = LocalDateTime.now().truncatedTo(ChronoUnit.DAYS).minus(safeDays - 1L, ChronoUnit.DAYS);
        List<Object[]> raw = productsRepository.dailyRevenueFrom(sellerId, from);
        LinkedHashMap<String, BigDecimal> series = new LinkedHashMap<>();
        for (int i = safeDays - 1; i >= 0; i--) {
            LocalDateTime d = LocalDateTime.now().truncatedTo(ChronoUnit.DAYS).minus(i, ChronoUnit.DAYS);
            series.put(d.toLocalDate().toString(), BigDecimal.ZERO);
        }
        for (Object[] row : raw) {
            Object dObj = row[0];
            String dateKey;
            try {
                if (dObj instanceof java.sql.Date)
                    dateKey = ((java.sql.Date) dObj).toLocalDate().toString();
                else if (dObj instanceof java.sql.Timestamp)
                    dateKey = ((java.sql.Timestamp) dObj).toLocalDateTime().toLocalDate().toString();
                else if (dObj instanceof java.time.LocalDate)
                    dateKey = dObj.toString();
                else if (dObj instanceof java.time.LocalDateTime)
                    dateKey = ((java.time.LocalDateTime) dObj).toLocalDate().toString();
                else {
                    String s = java.util.Objects.toString(dObj, "");
                    dateKey = s.length() >= 10 ? s.substring(0, 10) : s;
                }
            } catch (Exception ex) {
                dateKey = java.util.Objects.toString(dObj, "");
            }
            BigDecimal rev = BigDecimal.ZERO;
            if (row.length > 1 && row[1] != null) {
                if (row[1] instanceof BigDecimal)
                    rev = (BigDecimal) row[1];
                else {
                    try {
                        rev = new BigDecimal(row[1].toString());
                    } catch (Exception ignore) {
                    }
                }
            }
            if (series.containsKey(dateKey))
                series.put(dateKey, rev);
        }
        var labels = new java.util.ArrayList<>(series.keySet());
        var data = series.values().stream().map(BigDecimal::toPlainString).toList();
        return org.springframework.http.ResponseEntity.ok(java.util.Map.of("labels", labels, "data", data));
    }
}
