package banhangrong.su25.Controller;

import banhangrong.su25.Entity.Products;
import banhangrong.su25.Repository.ProductsRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Controller xử lý CRUD sản phẩm cho Seller và luồng duyệt/publish cho Admin.
 * (Tên cũ: ProductAdminController – đổi thành ProductController vì lớp này phục vụ cả seller lẫn admin.)
 */
@RestController
@RequestMapping("/api/products")
public class ProductController {
    private final ProductsRepository productsRepository;

    public ProductController(ProductsRepository productsRepository) {
        this.productsRepository = productsRepository;
    }

    @GetMapping
    public ResponseEntity<?> list(@RequestParam(name = "sellerId", required = false) Long sellerId) {
        if (sellerId != null) return ResponseEntity.ok(productsRepository.findBySellerId(sellerId));
        return ResponseEntity.ok(productsRepository.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable Long id) {
        return productsRepository.findById(id)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // Seller creates product: default status = pending (requires admin approval to become public)
    @PostMapping
    public ResponseEntity<?> create(@RequestBody Products req) {
        if (req.getSellerId() == null || req.getName() == null || req.getPrice() == null) {
            return ResponseEntity.badRequest().body("sellerId, name, price are required");
        }
        req.setStatus("pending"); // pending approval
        req.setCreatedAt(LocalDateTime.now());
        req.setUpdatedAt(LocalDateTime.now());
        Products saved = productsRepository.save(req);
        return ResponseEntity.created(URI.create("/api/products/" + saved.getProductId())).body(saved);
    }

    // Seller updates product: nếu có thay đổi ở các trường cơ bản -> chuyển hidden (để chờ duyệt lại)
    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody Products req) {
        Optional<Products> opt = productsRepository.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        Products p = opt.get();
        boolean changed = false;
        if (req.getName() != null && !req.getName().equals(p.getName())) { p.setName(req.getName()); changed = true; }
        if (req.getDescription() != null && !req.getDescription().equals(p.getDescription())) { p.setDescription(req.getDescription()); changed = true; }
        if (req.getPrice() != null && (p.getPrice()==null || req.getPrice().compareTo(p.getPrice())!=0)) { p.setPrice(req.getPrice()); changed = true; }
        if (req.getSalePrice() != null && (p.getSalePrice()==null || req.getSalePrice().compareTo(p.getSalePrice())!=0)) { p.setSalePrice(req.getSalePrice()); changed = true; }
        if (req.getQuantity() != null && !req.getQuantity().equals(p.getQuantity())) { p.setQuantity(req.getQuantity()); changed = true; }
        if (req.getDownloadUrl() != null && !req.getDownloadUrl().equals(p.getDownloadUrl())) { p.setDownloadUrl(req.getDownloadUrl()); changed = true; }

        if (changed && !"hidden".equals(p.getStatus())) {
            p.setStatus("hidden");
        }
        if (changed) {
            p.setUpdatedAt(LocalDateTime.now());
        }
        Products saved = productsRepository.save(p);
        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        if (!productsRepository.existsById(id)) return ResponseEntity.notFound().build();
        productsRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // Admin phê duyệt / huỷ publish; Seller bấm publish để gửi duyệt -> pending
    @PostMapping("/{id}/approval")
    public ResponseEntity<?> approve(
            @PathVariable Long id,
            @RequestParam("publish") boolean publish,
            @RequestHeader(value = "X-User-Type", required = false) String userType) {
        Optional<Products> opt = productsRepository.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        Products p = opt.get();
        if (userType != null && "ADMIN".equalsIgnoreCase(userType)) {
            if (publish) {
                if ("public".equals(p.getStatus())) {
                    return ResponseEntity.ok(p); // no-op
                }
                p.setStatus("public");
            } else {
                p.setStatus("hidden");
            }
        } else { // Seller flow
            if ("public".equals(p.getStatus())) {
                // Seller không chỉnh sửa gì nhưng bấm publish -> giữ nguyên public, no-op
                return ResponseEntity.ok(p);
            }
            // Các trạng thái khác (hidden / pending) khi seller bấm publish -> gửi lại duyệt (pending)
            p.setStatus("pending");
        }
        p.setUpdatedAt(LocalDateTime.now());
        Products saved = productsRepository.save(p);
        return ResponseEntity.ok(saved);
    }
}
