package banhangrong.su25.Controller;

import banhangrong.su25.Entity.Products;
import banhangrong.su25.Repository.ProductsRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/products")
public class ProductController {
    private final ProductsRepository productsRepository;

    public ProductController(ProductsRepository productsRepository) {
        this.productsRepository = productsRepository;
    }

    // GET /api/products?sellerId=1 → Lấy products (có thể filter theo seller)
    @GetMapping
    public ResponseEntity<?> list(@RequestParam(name = "sellerId", required = false) Long sellerId) {
        if (sellerId != null) {
            return ResponseEntity.ok(productsRepository.findBySellerId(sellerId));
        }
        return ResponseEntity.ok(productsRepository.findAll());
    }

    // GET /api/products/{id} → Lấy product by ID
    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable Long id) {
        return productsRepository.findById(id)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // GET /api/products/seller/{sellerId}/active → Lấy ACTIVE products của seller (CHO SHOP DESIGNER)
    @GetMapping("/seller/{sellerId}/active")
    public ResponseEntity<?> getSellerActiveProducts(@PathVariable Long sellerId) {
        try {
            List<Products> products = productsRepository.findBySellerIdAndIsActiveTrue(sellerId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("products", products);
            response.put("count", products.size());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Lỗi khi lấy sản phẩm: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    // GET /api/products/seller/{sellerId}/active/simple → Simple format
    @GetMapping("/seller/{sellerId}/active/simple")
    public ResponseEntity<List<Products>> getSellerActiveProductsSimple(@PathVariable Long sellerId) {
        try {
            List<Products> products = productsRepository.findBySellerIdAndIsActiveTrue(sellerId);
            return ResponseEntity.ok(products);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // POST /api/products → Seller tạo product mới
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

    // PUT /api/products/{id} → Seller cập nhật product
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

    // DELETE /api/products/{id} → Xóa product
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        if (!productsRepository.existsById(id)) return ResponseEntity.notFound().build();
        productsRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // POST /api/products/{id}/approval → Admin duyệt/ Seller publish
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
                return ResponseEntity.ok(p);
            }
            p.setStatus("pending");
        }
        p.setUpdatedAt(LocalDateTime.now());
        Products saved = productsRepository.save(p);
        return ResponseEntity.ok(saved);
    }

    // THÊM: Lấy products theo status (cho admin)
    @GetMapping("/status/{status}")
    public ResponseEntity<List<Products>> getProductsByStatus(@PathVariable String status) {
        try {
            // Cần thêm method này trong Repository
            // List<Products> products = productsRepository.findByStatus(status);
            // Tạm thời filter manual
            List<Products> allProducts = productsRepository.findAll();
            List<Products> filtered = allProducts.stream()
                    .filter(p -> status.equals(p.getStatus()))
                    .toList();
            return ResponseEntity.ok(filtered);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}