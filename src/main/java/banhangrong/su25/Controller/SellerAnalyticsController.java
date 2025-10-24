package banhangrong.su25.Controller;

import banhangrong.su25.Repository.ProductsRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@RestController
public class SellerAnalyticsController {

    private final ProductsRepository productsRepository;

    public SellerAnalyticsController(ProductsRepository productsRepository) {
        this.productsRepository = productsRepository;
    }

    /**
     * Return daily revenue series for the given seller for the past `days` days (inclusive).
     * Example: days=15 returns 15 days ending today.
     */
    @GetMapping("/api/seller/{sellerId}/analytics/revenue")
    public Map<String, Object> revenueSeries(@PathVariable Long sellerId,
                                             @RequestParam(name = "days", required = false, defaultValue = "15") Integer days) {
        if (days == null || days <= 0) days = 15;
        // Build inclusive range: from (days-1) days ago at start of day
        LocalDateTime from = LocalDateTime.now().minus(days - 1L, ChronoUnit.DAYS).truncatedTo(ChronoUnit.DAYS);
        List<Object[]> raw = productsRepository.dailyRevenueFrom(sellerId, from);

        LinkedHashMap<String, BigDecimal> series = new LinkedHashMap<>();
        for (int i = days - 1; i >= 0; i--) {
            LocalDateTime d = LocalDateTime.now().minus(i, ChronoUnit.DAYS).truncatedTo(ChronoUnit.DAYS);
            series.put(d.toLocalDate().toString(), BigDecimal.ZERO);
        }

        for (Object[] row : raw) {
            Object dObj = row[0];
            String dateKey = null;
            try {
                if (dObj instanceof java.sql.Date) dateKey = ((java.sql.Date) dObj).toLocalDate().toString();
                else if (dObj instanceof java.sql.Timestamp) dateKey = ((java.sql.Timestamp) dObj).toLocalDateTime().toLocalDate().toString();
                else if (dObj instanceof java.time.LocalDate) dateKey = dObj.toString();
                else if (dObj instanceof java.time.LocalDateTime) dateKey = ((java.time.LocalDateTime) dObj).toLocalDate().toString();
                else {
                    String s = Objects.toString(dObj, "");
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
            if (series.containsKey(dateKey)) series.put(dateKey, rev);
        }

        Map<String, Object> out = new java.util.HashMap<>();
        out.put("labels", series.keySet());
        out.put("data", series.values());
        return out;
    }
}
