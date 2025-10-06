package banhangrong.su25.Controller;

import banhangrong.su25.Repository.ProductLicensesRepository;
import banhangrong.su25.Repository.ProductsRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
public class ProductLicenseController {

    private final ProductLicensesRepository licensesRepository;
    private final ProductsRepository productsRepository;

    public ProductLicenseController(ProductLicensesRepository licensesRepository,
                                    ProductsRepository productsRepository) {
        this.licensesRepository = licensesRepository;
        this.productsRepository = productsRepository;
    }

    @GetMapping("/api/seller/{sellerId}/licenses")
    public ResponseEntity<?> list(@PathVariable Long sellerId,
                                  @RequestParam(name = "page", defaultValue = "0") int page,
                                  @RequestParam(name = "size", defaultValue = "10") int size,
                                  @RequestParam(name = "active", required = false) Boolean active,
                                  @RequestParam(name = "productId", required = false) Long productId,
                                  @RequestParam(name = "search", required = false) String search) {
        if (size > 100) size = 100;
        // Validate product belongs to seller (if provided)
        if (productId != null) {
            boolean ok = productsRepository.findById(productId)
                    .map(p -> p.getSellerId() != null && p.getSellerId().equals(sellerId))
                    .orElse(false);
            if (!ok) return ResponseEntity.badRequest().body("productId không thuộc seller");
        }
        Pageable pageable = PageRequest.of(page, size);
        Page<ProductLicensesRepository.LicenseView> p = licensesRepository.findSellerLicenses(sellerId, active, productId, (search != null && !search.isBlank()) ? search.trim() : null, pageable);
        Map<String, Object> body = new HashMap<>();
        body.put("content", p.getContent());
        body.put("page", p.getNumber());
        body.put("size", p.getSize());
        body.put("totalPages", p.getTotalPages());
        body.put("totalElements", p.getTotalElements());
        return ResponseEntity.ok(body);
    }

    /** Toggle active / update device identifier */
    @PatchMapping("/api/licenses/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody Map<String, Object> payload) {
        Optional<banhangrong.su25.Entity.ProductLicenses> opt = licensesRepository.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        var lic = opt.get();
        if (payload.containsKey("isActive")) {
            Object v = payload.get("isActive");
            if (v instanceof Boolean b) lic.setIsActive(b);
        }
        if (payload.containsKey("deviceIdentifier")) {
            Object v = payload.get("deviceIdentifier");
            lic.setDeviceIdentifier(v != null ? v.toString() : null);
        }
        // We intentionally do NOT change order_item_id, user_id, license_key here for integrity.
        licensesRepository.save(lic);
        return ResponseEntity.ok(lic);
    }
}
