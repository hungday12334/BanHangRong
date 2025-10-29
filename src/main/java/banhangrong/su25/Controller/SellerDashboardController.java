package banhangrong.su25.Controller;

import banhangrong.su25.Repository.ProductsRepository;
import banhangrong.su25.Repository.SellerOrderRepository;
import banhangrong.su25.Repository.UsersRepository;
import banhangrong.su25.Entity.Users;
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

    public SellerDashboardController(ProductsRepository productsRepository,
                                     UsersRepository usersRepository,
                                     SellerOrderRepository sellerOrderRepository) {
        this.productsRepository = productsRepository;
        this.usersRepository = usersRepository;
        this.sellerOrderRepository = sellerOrderRepository;
    }

    // Temporary: sellerId is read from query or default to 1L until auth in place
    @GetMapping("/seller/dashboard")
    public String dashboard(@RequestParam(name = "sellerId", required = false) Long sellerId,
                            Model model,
                            Principal principal,
                            HttpSession session) {
        // Resolve sellerId from authenticated principal or session when available.
        // Order of preference:
        // 1) explicit request param (useful for testing / admin overrides)
        // 2) Principal - if Principal.getName() is numeric we parse it as userId; otherwise lookup by username
        // 3) HttpSession attribute "userId" (Long) or "user" (Users object)
        // 4) fallback demo id 56L (existing behaviour)

        if (sellerId == null) {
            // Try principal
            if (principal != null && principal.getName() != null) {
                String name = principal.getName();
                try {
                    sellerId = Long.parseLong(name);
                } catch (NumberFormatException e) {
                    // Not a numeric principal name, try lookup by username
                    var opt = usersRepository.findByUsername(name);
                    if (opt.isPresent()) sellerId = opt.get().getUserId();
                }
            }

            // Try session attributes
            if (sellerId == null && session != null) {
                Object uid = session.getAttribute("userId");
                if (uid instanceof Long) sellerId = (Long) uid;
                else if (uid instanceof Integer) sellerId = ((Integer) uid).longValue();
                else {
                    Object userObj = session.getAttribute("user");
                    if (userObj instanceof Users) sellerId = ((Users) userObj).getUserId();
                }
            }

            // final fallback
            if (sellerId == null) sellerId = 6L; // assumption: demo seller
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

        // Daily revenue for last 14 days
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
                    if (s.length() >= 10) dateKey = s.substring(0, 10);
                    else dateKey = s;
                }
            } catch (Exception ex) {
                dateKey = Objects.toString(dObj, "");
            }
            if (dateKey == null) continue;
            BigDecimal rev = BigDecimal.ZERO;
            if (row.length > 1 && row[1] != null) {
                if (row[1] instanceof BigDecimal) rev = (BigDecimal) row[1];
                else {
                    try { rev = new BigDecimal(row[1].toString()); } catch (Exception e) { rev = BigDecimal.ZERO; }
                }
            }
            // Only put into the series if the dateKey exists (guards against formatting mismatches)
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
        var pageableRecent = org.springframework.data.domain.PageRequest.of(0, 8, org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt"));
        var pageRecent = sellerOrderRepository.findSellerOrders(sellerId, null, null, null, pageableRecent);
        for (SellerOrderRepository.SellerOrderSummary s : pageRecent.getContent()) {
            Map<String, Object> m = new HashMap<>();
            m.put("orderId", s.getOrderId());
            LocalDateTime ldt = s.getCreatedAt();
            String createdAtStr = ldt != null ? ldt.format(fmt) : "";
            m.put("createdAtStr", createdAtStr);
            m.put("amount", s.getSellerAmount());
            m.put("items", s.getSellerItems());
            // Optional: could include buyer info if needed for UI in future
            // m.put("buyerUsername", s.getBuyerUsername());
            // m.put("buyerUserId", s.getBuyerUserId());
            recentOrders.add(m);
        }

        // Low stock products (<= 5)
    var lowStock = productsRepository.findTop10BySellerIdAndStatusAndQuantityLessThanEqualOrderByQuantityAsc(sellerId, "public", 5);
    long activeProducts = productsRepository.countBySellerIdAndStatus(sellerId, "public");

        // Seller ranking (revenue-based)
        Integer myRank = productsRepository.sellerRevenueRank(sellerId);
        Long totalSellers = Optional.ofNullable(productsRepository.totalSellers()).orElse(0L);
        double percentile = (myRank != null && totalSellers > 0) ? (100.0 * (totalSellers - myRank + 1) / totalSellers) : 0.0;
        List<Map<String,Object>> topSellers = new ArrayList<>();
        for (Object[] row : productsRepository.topSellers()) {
            Map<String,Object> m = new HashMap<>();
            m.put("sellerId", row[0]);
            m.put("username", row[1]);
            m.put("revenue", row[2]);
            m.put("units", row[3]);
            topSellers.add(m);
        }

    model.addAttribute("sellerId", sellerId);
        // Also expose userId for clarity: the sellerId is the same as the logged-in user's id
        model.addAttribute("userId", sellerId);
        model.addAttribute("totalRevenue", totalRevenue);
        model.addAttribute("totalUnits", totalUnits);
        model.addAttribute("totalOrders", totalOrders);
        model.addAttribute("avgRating", avgRating);
        model.addAttribute("todayRevenue", todayRev);
        model.addAttribute("monthRevenue", monthRev);
        model.addAttribute("dailyRevenueLabels", String.join(",", series.keySet()));
        model.addAttribute("dailyRevenueData", String.join(",", series.values().stream().map(BigDecimal::toPlainString).toList()));
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

        return "pages/seller/seller_dashboard";
    }
}
