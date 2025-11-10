package banhangrong.su25.Controller;

import banhangrong.su25.Entity.Products;
import banhangrong.su25.Repository.ProductsRepository;
import banhangrong.su25.Repository.ProductImagesRepository;
import banhangrong.su25.Repository.CategoriesProductsRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.transaction.annotation.Transactional;

@RestController
@RequestMapping("/api/products")
public class ProductController {
    private final ProductsRepository productsRepository;
    private final ProductImagesRepository productImagesRepository;
    private final CategoriesProductsRepository categoriesProductsRepository;

    public ProductController(ProductsRepository productsRepository,
            ProductImagesRepository productImagesRepository,
            CategoriesProductsRepository categoriesProductsRepository) {
        this.productsRepository = productsRepository;
        this.productImagesRepository = productImagesRepository;
        this.categoriesProductsRepository = categoriesProductsRepository;
    }

    // GET /api/products?sellerId=1 → Lấy products (có thể filter theo seller)
    @GetMapping
    public ResponseEntity<?> list(@RequestParam(name = "sellerId", required = false) Long sellerId) {
        if (sellerId != null) {
            List<Products> items = productsRepository.findBySellerId(sellerId)
                    .stream()
                    .map(ProductController::ensureStandardStatus)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(items);
        }
        List<Products> all = productsRepository.findAll()
                .stream()
                .map(ProductController::ensureStandardStatus)
                .collect(Collectors.toList());
        return ResponseEntity.ok(all);
    }

    // GET /api/products/{id} → Lấy product by ID
    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable Long id) {
        return productsRepository.findById(id)
                .<ResponseEntity<?>>map(p -> ResponseEntity.ok(ensureStandardStatus(p)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // GET /api/products/seller/{sellerId}/active → Lấy ACTIVE products của seller
    // (CHO SHOP DESIGNER)
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
        if (req.getQuantity() == null || req.getQuantity() <= 0) {
            return ResponseEntity.badRequest().body("quantity must be > 0");
        }
        if (req.getDownloadUrl() == null || req.getDownloadUrl().trim().isEmpty()) {
            return ResponseEntity.badRequest().body("downloadUrl is required");
        }
        // Unique name per seller (case-insensitive)
        String nm = req.getName().trim();
        if (productsRepository.existsBySellerIdAndNameIgnoreCase(req.getSellerId(), nm)) {
            return ResponseEntity.status(409).body(Map.of(
                    "error", "duplicate_name",
                    "message", "Product name already exists"));
        }
        req.setName(nm);
        req.setStatus("pending"); // pending approval
        req.setCreatedAt(LocalDateTime.now());
        req.setUpdatedAt(LocalDateTime.now());
        try {
            Products saved = productsRepository.save(req);
            return ResponseEntity.created(URI.create("/api/products/" + saved.getProductId())).body(saved);
        } catch (Exception ex) {
            return ResponseEntity.status(500).body(Map.of(
                    "error", "save_failed",
                    "message", ex.getMessage() != null ? ex.getMessage() : "Failed to save product"));
        }
    }

    // PUT /api/products/{id} → Seller cập nhật product
    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody Products req) {
        Optional<Products> opt = productsRepository.findById(id);
        if (opt.isEmpty())
            return ResponseEntity.notFound().build();
        Products p = opt.get();
        boolean changed = false;
        boolean nameChanged = false;
        boolean descriptionChanged = false;
        // numeric-only changes (price, salePrice, quantity) are considered safe for
        // public status
        boolean downloadUrlChanged = false;
        if (req.getName() != null && !req.getName().equals(p.getName())) {
            String nextName = req.getName().trim();
            // Prevent name clash with other products of same seller
            if (p.getSellerId() != null
                    && productsRepository.existsBySellerIdAndNameIgnoreCase(p.getSellerId(), nextName)
                    && !nextName.equalsIgnoreCase(p.getName())) {
                return ResponseEntity.status(409).body(Map.of(
                        "error", "duplicate_name",
                        "message", "Product name already exists"));
            }
            p.setName(nextName);
            changed = true;
            nameChanged = true;
        }
        if (req.getDescription() != null && !req.getDescription().equals(p.getDescription())) {
            p.setDescription(req.getDescription());
            changed = true;
            descriptionChanged = true;
        }
        if (req.getPrice() != null && (p.getPrice() == null || req.getPrice().compareTo(p.getPrice()) != 0)) {
            p.setPrice(req.getPrice());
            changed = true;
        }
        if (req.getSalePrice() != null
                && (p.getSalePrice() == null || req.getSalePrice().compareTo(p.getSalePrice()) != 0)) {
            p.setSalePrice(req.getSalePrice());
            changed = true;
        }
        if (req.getQuantity() != null && !req.getQuantity().equals(p.getQuantity())) {
            if (req.getQuantity() <= 0)
                return ResponseEntity.badRequest().body("quantity must be > 0");
            p.setQuantity(req.getQuantity());
            changed = true;
        }
        if (req.getDownloadUrl() != null && !req.getDownloadUrl().equals(p.getDownloadUrl())) {
            if (req.getDownloadUrl().trim().isEmpty())
                return ResponseEntity.badRequest().body("downloadUrl is required");
            p.setDownloadUrl(req.getDownloadUrl().trim());
            changed = true;
            downloadUrlChanged = true;
        }
        // Selective status change logic (mirror of ProductsApiController)
        if (changed) {
            String currentStatus = p.getStatus() == null ? null : p.getStatus().trim().toLowerCase();
            boolean sensitiveChanged = nameChanged || descriptionChanged || downloadUrlChanged;
            if ("public".equals(currentStatus)) {
                if (sensitiveChanged) {
                    p.setStatus("hidden");
                } // else retain public
            } else if (!"hidden".equals(currentStatus)) {
                // For pending (or other non-hidden/non-public) states, keep existing behavior
                // of forcing hidden
                p.setStatus("hidden");
            }
        }
        if (changed) {
            p.setUpdatedAt(LocalDateTime.now());
        }
        try {
            Products saved = productsRepository.save(p);
            return ResponseEntity.ok(saved);
        } catch (Exception ex) {
            return ResponseEntity.status(500).body(Map.of(
                    "error", "save_failed",
                    "message", ex.getMessage() != null ? ex.getMessage() : "Failed to save product"));
        }
    }

    // DELETE /api/products/{id} → Xóa product
    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<?> delete(@PathVariable Long id) {
        Optional<Products> opt = productsRepository.findById(id);
        if (opt.isEmpty())
            return ResponseEntity.notFound().build();
        Products p = opt.get();
        // Rule: only allow delete if product has NEVER been public
        Boolean everPublic = p.getWasPublic();
        String currentStatus = p.getStatus() != null ? p.getStatus().toLowerCase() : null;
        if (Boolean.TRUE.equals(everPublic) || "public".equals(currentStatus)) {
            return ResponseEntity.status(409).body(Map.of(
                    "error", "cannot_delete_public_product",
                    "message", "Product that has been public cannot be deleted. Please hide it instead."));
        }
        // Cleanup dependencies to satisfy FK constraints
        try {
            categoriesProductsRepository.deleteByProductId(id);
        } catch (Exception ignore) {
        }
        try {
            productImagesRepository.deleteByProductId(id);
        } catch (Exception ignore) {
        }
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
        if (opt.isEmpty())
            return ResponseEntity.notFound().build();
        Products p = opt.get();
        if (userType != null && "ADMIN".equalsIgnoreCase(userType)) {
            if (publish) {
                if ("public".equals(p.getStatus())) {
                    return ResponseEntity.ok(p); // no-op
                }
                p.setStatus("public");
                p.setWasPublic(Boolean.TRUE);
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

    // --- Helpers ---
    private static Products ensureStandardStatus(Products p) {
        String st = p.getStatus();
        st = (st == null) ? null : st.trim().toLowerCase();
        if (st == null || st.isBlank()) {
            // Derive from isActive/wasPublic when status is missing
            Boolean active = p.getIsActive();
            Boolean wasPub = p.getWasPublic();
            if (Boolean.TRUE.equals(active))
                st = "public";
            else if (Boolean.TRUE.equals(wasPub))
                st = "hidden"; // was public before but currently not active
            else
                st = "pending";
        } else {
            // Normalize synonyms that might exist in legacy data
            if ("active".equals(st))
                st = "public";
            if ("inactive".equals(st))
                st = "hidden";
            if ("canceled".equals(st))
                st = "cancelled"; // unify spelling
        }
        p.setStatus(st);
        return p;
    }
}