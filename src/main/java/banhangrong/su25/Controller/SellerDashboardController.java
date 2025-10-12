package banhangrong.su25.Controller;

import banhangrong.su25.Repository.ProductsRepository;
import banhangrong.su25.Repository.SellerOrderRepository;
import banhangrong.su25.Repository.UsersRepository;
import banhangrong.su25.Entity.Users;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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

    @GetMapping("/seller/dashboard")
    public String dashboard(@RequestParam(name = "sellerId", required = false) Long sellerId,
            Model model) {
        // Lấy user hiện tại từ SecurityContext
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Users currentUser = null;
        if (auth != null && auth.getName() != null) {
            currentUser = usersRepository.findByUsername(auth.getName()).orElse(null);
        }

        // Xác định sellerId sử dụng:
        // - Nếu là ADMIN và có truyền sellerId -> dùng sellerId đó (để xem dashboard
        // của seller khác)
        // - Ngược lại: dùng userId của chính user đăng nhập
        Long sellerIdUsed;
        if (currentUser != null && "ADMIN".equalsIgnoreCase(currentUser.getUserType()) && sellerId != null) {
            sellerIdUsed = sellerId;
        } else if (currentUser != null) {
            sellerIdUsed = currentUser.getUserId();
        } else {
            // Fallback an toàn nếu không xác định được user (không nên xảy ra do route đã
            // auth)
            sellerIdUsed = sellerId != null ? sellerId : 0L;
        }

        // KPIs
        BigDecimal totalRevenue = Optional.ofNullable(productsRepository.totalRevenueBySeller(sellerIdUsed))
                .orElse(BigDecimal.ZERO);
        Long totalUnits = Optional.ofNullable(productsRepository.totalUnitsSoldBySeller(sellerIdUsed))
                .orElse(0L);
        Long totalOrders = Optional.ofNullable(productsRepository.totalOrdersBySeller(sellerIdUsed))
                .orElse(0L);
        BigDecimal avgRating = Optional.ofNullable(productsRepository.averageRatingBySeller(sellerIdUsed))
                .orElse(BigDecimal.ZERO);

        // Today/This month
        BigDecimal todayRev = Optional.ofNullable(productsRepository.todayRevenue(sellerIdUsed))
                .orElse(BigDecimal.ZERO);
        BigDecimal monthRev = Optional.ofNullable(productsRepository.thisMonthRevenue(sellerIdUsed))
                .orElse(BigDecimal.ZERO);

        // Daily revenue for last 15 days inclusive (based on calendar date to avoid TZ drift)
        java.time.LocalDate today = java.time.LocalDate.now();
        java.time.LocalDate fromDate = today.minusDays(14); // include today + 14 days back
        List<Object[]> raw = productsRepository.dailyRevenueFrom(sellerIdUsed, fromDate);
        // Build date -> revenue map covering all dates from fromDate..today
        LinkedHashMap<String, BigDecimal> series = new LinkedHashMap<>();
        for (java.time.LocalDate d = fromDate; !d.isAfter(today); d = d.plusDays(1)) {
            series.put(d.toString(), BigDecimal.ZERO);
        }
        for (Object[] row : raw) {
            String date = Objects.toString(row[0]);
            BigDecimal rev = (row[1] instanceof BigDecimal) ? (BigDecimal) row[1] : new BigDecimal(row[1].toString());
            series.put(date, rev);
        }

        // Top products
        List<Map<String, Object>> topProducts = new ArrayList<>();
        for (Object[] row : productsRepository.topProducts(sellerIdUsed)) {
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
        var pageRecent = sellerOrderRepository.findSellerOrders(sellerIdUsed, null, null, null, pageableRecent);
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
        var lowStock = productsRepository
                .findTop10BySellerIdAndStatusAndQuantityLessThanEqualOrderByQuantityAsc(sellerIdUsed, "Public", 5);
        long activeProducts = productsRepository.countBySellerIdAndStatus(sellerIdUsed, "Public");

        // Seller ranking (revenue-based)
        Integer myRank = productsRepository.sellerRevenueRank(sellerIdUsed);
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

        model.addAttribute("sellerId", sellerIdUsed);
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
        Users user = usersRepository.findById(sellerIdUsed).orElse(null);
        model.addAttribute("user", user);
        if (user != null) {
            model.addAttribute("userType", user.getUserType());
        }

        return "pages/seller/seller_dashboard";
    }
}
