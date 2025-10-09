package banhangrong.su25.Controller.seller;

import banhangrong.su25.Repository.SellerOrderRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/seller/{sellerId}/orders")
public class SellerOrderController {

    private final SellerOrderRepository sellerOrderRepository;

    public SellerOrderController(SellerOrderRepository sellerOrderRepository) {
        this.sellerOrderRepository = sellerOrderRepository;
    }

    @GetMapping
    public ResponseEntity<?> list(@PathVariable Long sellerId,
                                  @RequestParam(name = "page", defaultValue = "0") int page,
                                  @RequestParam(name = "size", defaultValue = "10") int size,
                                  @RequestParam(name = "from", required = false) String fromStr,
                                  @RequestParam(name = "to", required = false) String toStr,
                                  @RequestParam(name = "search", required = false) String search) {
        if (size > 100) size = 100; // hard cap
        LocalDateTime fromTs = parseNullable(fromStr);
        LocalDateTime toTs = parseNullable(toStr);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<SellerOrderRepository.SellerOrderSummary> p = sellerOrderRepository.findSellerOrders(sellerId, fromTs, toTs, (search != null && !search.isBlank()) ? search.trim() : null, pageable);
        Map<String, Object> body = new HashMap<>();
        body.put("content", p.getContent());
        body.put("page", p.getNumber());
        body.put("size", p.getSize());
        body.put("totalPages", p.getTotalPages());
        body.put("totalElements", p.getTotalElements());
        return ResponseEntity.ok(body);
    }

    private LocalDateTime parseNullable(String s) {
        if (s == null || s.isBlank()) return null;
        try { return LocalDateTime.parse(s); } catch (DateTimeParseException e) { return null; }
    }
}
