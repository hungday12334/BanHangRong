package banhangrong.su25.Controller;

import banhangrong.su25.Repository.ProductsRepository;
import banhangrong.su25.Repository.UsersRepository;
import banhangrong.su25.Entity.Users;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Controller
public class SellerDashboardController {

    private final ProductsRepository productsRepository;
    private final UsersRepository usersRepository;

    public SellerDashboardController(ProductsRepository productsRepository, UsersRepository usersRepository) {
        this.productsRepository = productsRepository;
        this.usersRepository = usersRepository;
    }

    // Temporary: sellerId is read from query or default to 1L until auth in place
    @GetMapping("/seller/dashboard")
    public String dashboard(@RequestParam(name = "sellerId", required = false) Long sellerId,
                            Model model) {
        if (sellerId == null) sellerId = 1L; // assumption: demo seller

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
            String date = Objects.toString(row[0]);
            BigDecimal rev = (row[1] instanceof BigDecimal) ? (BigDecimal) row[1] : new BigDecimal(row[1].toString());
            series.put(date, rev);
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

        // Recent orders
        List<Map<String, Object>> recentOrders = new ArrayList<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        for (Object[] row : productsRepository.recentOrders(sellerId)) {
            Map<String, Object> m = new HashMap<>();
            m.put("orderId", row[0]);
            Object ts = row[1];
            String createdAtStr;
            if (ts instanceof LocalDateTime ldt) {
                createdAtStr = ldt.format(fmt);
            } else {
                createdAtStr = Objects.toString(ts);
            }
            m.put("createdAtStr", createdAtStr);
            m.put("amount", row[2]);
            m.put("items", row[3]);
            recentOrders.add(m);
        }

        // Low stock products (<= 5)
        var lowStock = productsRepository.findTop10BySellerIdAndIsActiveTrueAndQuantityLessThanEqualOrderByQuantityAsc(sellerId, 5);
        long activeProducts = productsRepository.countBySellerIdAndIsActiveTrue(sellerId);

    model.addAttribute("sellerId", sellerId);
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

        // Load user profile (assume sellerId == userId for now)
        Users user = usersRepository.findById(sellerId).orElse(null);
        model.addAttribute("user", user);
        model.addAttribute("sellerId", sellerId);
        if (user != null) {
            model.addAttribute("userType", user.getUserType());
        }

        return "pages/seller_dashboard";
    }

    // API riêng cho profile panel
    @GetMapping("/dashboard/profile-panel")
    public String getProfilePanel(@RequestParam(name = "sellerId", required = false) Long sellerId,
                                  Model model) {
        if (sellerId == null) sellerId = 1L;

        Users user = usersRepository.findById(sellerId).orElse(null);
        model.addAttribute("user", user);
        model.addAttribute("sellerId", sellerId);

        return "fragments/panels :: #profilePanel";
    }

    // Chỉnh sửa profile
    @GetMapping("/dashboard/profile/edit")
    public String editProfileForm(@RequestParam(name = "sellerId", required = false) Long sellerId,
                                  Model model) {
        if (sellerId == null) sellerId = 1L;

        Users user = usersRepository.findById(sellerId).orElse(null);
        model.addAttribute("user", user);
        model.addAttribute("sellerId", sellerId);

        return "seller/profile-edit";
    }

    // Cập nhật profile
    @PostMapping("/dashboard/profile/update")
    public String updateProfile(@RequestParam(name = "sellerId", required = false) Long sellerId,
                                @ModelAttribute Users updatedUser,
                                RedirectAttributes redirectAttributes) {
        if (sellerId == null) sellerId = 1L;

        try {
            Users existingUser = usersRepository.findById(sellerId)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Cập nhật các field được phép
            if (updatedUser.getUsername() != null) {
                existingUser.setUsername(updatedUser.getUsername());
            }
            if (updatedUser.getPhoneNumber() != null) {
                existingUser.setPhoneNumber(updatedUser.getPhoneNumber());
            }
            if (updatedUser.getGender() != null) {
                existingUser.setGender(updatedUser.getGender());
            }
            if (updatedUser.getBirthDate() != null) {
                existingUser.setBirthDate(updatedUser.getBirthDate());
            }

            usersRepository.save(existingUser);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật thông tin thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
        }

        return "redirect:/seller/dashboard?sellerId=" + sellerId;
    }


}
