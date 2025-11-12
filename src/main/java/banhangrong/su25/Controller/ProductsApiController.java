package banhangrong.su25.Controller;

import banhangrong.su25.Entity.Products;
import banhangrong.su25.Repository.ProductsRepository;
import banhangrong.su25.Repository.ProductImagesRepository;
import banhangrong.su25.Repository.VouchersRepository;
import banhangrong.su25.Repository.ProductLicensesRepository;
import banhangrong.su25.Entity.Vouchers;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/products-lite")
public class ProductsApiController {

    private final ProductsRepository productsRepository;
    private final ProductImagesRepository productImagesRepository;
    private final VouchersRepository vouchersRepository;
    private final ProductLicensesRepository productLicensesRepository;

    public ProductsApiController(ProductsRepository productsRepository,
                                 ProductImagesRepository productImagesRepository,
                                 VouchersRepository vouchersRepository,
                                 ProductLicensesRepository productLicensesRepository) {
        this.productsRepository = productsRepository;
        this.productImagesRepository = productImagesRepository;
        this.vouchersRepository = vouchersRepository;
        this.productLicensesRepository = productLicensesRepository;
    }

    // Lightweight DTO to avoid lazy recursion and reduce payload
    public static class ProductDto {
        public Long productId;
        public Long sellerId;
        public String name;
        public String description;
        public BigDecimal price;
        public BigDecimal salePrice;
    public Integer quantity;
    public String downloadUrl;
    public String primaryImage;
    public String status;
    }

    // Lightweight remaining endpoint for dynamic low-stock refresh
    @GetMapping("/{id}/remaining")
    public ResponseEntity<?> getRemaining(@PathVariable("id") Long id) {
        return productsRepository.findById(id).map(p -> {
            Integer qty = p.getQuantity();
            int capacity = qty != null ? qty : 0;
            long sold = 0L;
            long pre = 0L;
            try { sold = productLicensesRepository.countByProductViaOrders(p.getProductId()); } catch (Exception ignored) {}
            try { pre = productLicensesRepository.countPreGeneratedForProduct(p.getProductId()); } catch (Exception ignored) {}
            long remaining = Math.max(0L, (long) capacity - sold - pre);
            return ResponseEntity.ok(java.util.Map.of("productId", p.getProductId(), "remaining", remaining, "status", p.getStatus()));
        }).orElse(ResponseEntity.notFound().build());
    }

    // Public vouchers for product
    @GetMapping("/{id}/vouchers")
    public ResponseEntity<java.util.List<java.util.Map<String, Object>>> getVouchers(@PathVariable("id") Long id) {
        return productsRepository.findById(id).map(p -> {
            java.util.List<Vouchers> list = vouchersRepository.findBySellerIdAndProductIdOrderByUpdatedAtDesc(p.getSellerId(), p.getProductId());
            java.time.LocalDateTime now = java.time.LocalDateTime.now();
            java.util.List<java.util.Map<String,Object>> out = new java.util.ArrayList<>();
            for (Vouchers v : list) {
                if (!"active".equalsIgnoreCase(v.getStatus())) continue;
                if (v.getStartAt() != null && now.isBefore(v.getStartAt())) continue;
                if (v.getEndAt() != null && now.isAfter(v.getEndAt())) continue;
                java.util.Map<String,Object> m = new java.util.LinkedHashMap<>();
                m.put("code", v.getCode());
                m.put("type", v.getDiscountType());
                m.put("value", v.getDiscountValue());
                m.put("minOrder", v.getMinOrder());
                m.put("endAt", v.getEndAt());
                out.add(m);
            }
            return ResponseEntity.ok(out);
        }).orElse(ResponseEntity.notFound().build());
    }

    private ProductDto toDto(Products p) {
        ProductDto d = new ProductDto();
        d.productId = p.getProductId();
        d.sellerId = p.getSellerId();
        d.name = p.getName();
        d.description = p.getDescription();
        d.price = p.getPrice();
        d.salePrice = p.getSalePrice();
        d.quantity = p.getQuantity();
        d.downloadUrl = p.getDownloadUrl();
        // attempt to populate primaryImage from product_images if available
        try {
            if (this.productImagesRepository != null && p.getProductId() != null) {
                var imgs = this.productImagesRepository.findTop1ByProductIdAndIsPrimaryTrueOrderByImageIdAsc(p.getProductId());
                if (imgs != null && !imgs.isEmpty()) {
                    d.primaryImage = imgs.get(0).getImageUrl();
                } else {
                    var any = this.productImagesRepository.findTop1ByProductIdOrderByImageIdAsc(p.getProductId());
                    if (any != null && !any.isEmpty()) d.primaryImage = any.get(0).getImageUrl();
                }
            }
        } catch (Exception ignored) {}
        d.status = p.getStatus();
        return d;
    }

    // GET /api/products?sellerId=123 -> list products by seller
    @GetMapping
    public List<ProductDto> list(@RequestParam(name = "sellerId", required = false) Long sellerId) {
        List<Products> src = (sellerId != null) ? productsRepository.findBySellerId(sellerId) : productsRepository.findAll();
        List<ProductDto> dtos = src.stream().map(this::toDto).collect(Collectors.toList());
        // Return DTOs with quantity as stored in DB (do not override with remaining keys)
        return dtos;
    }

    // GET /api/products/{id} -> product details
    @GetMapping("/{id}")
    public ResponseEntity<ProductDto> getOne(@PathVariable("id") Long id) {
        return productsRepository.findById(id)
                .map(p -> ResponseEntity.ok(toDto(p)))
                .orElse(ResponseEntity.notFound().build());
    }

    // GET /api/products/{id}/images -> list of image URLs (primary first)
    @GetMapping("/{id}/images")
    public ResponseEntity<java.util.List<String>> getImages(@PathVariable("id") Long id) {
        try {
            if (this.productImagesRepository == null) return ResponseEntity.ok(java.util.List.of());
            var imgs = this.productImagesRepository.findTop1ByProductIdAndIsPrimaryTrueOrderByImageIdAsc(id);
            java.util.List<String> out = new java.util.ArrayList<>();
            if (imgs != null && !imgs.isEmpty()) {
                out.add(imgs.get(0).getImageUrl());
            }
            // also append the first image if primary wasn't present or to provide fallback
            var any = this.productImagesRepository.findTop1ByProductIdOrderByImageIdAsc(id);
            if (any != null && !any.isEmpty()) {
                String u = any.get(0).getImageUrl();
                if (out.isEmpty() || !out.get(0).equals(u)) out.add(u);
            }
            return ResponseEntity.ok(out);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(java.util.List.of());
        }
    }

    // Create product (sellerId must be provided via body or query)
    @PostMapping
    public ResponseEntity<?> create(@RequestBody ProductDto body,
                                    @RequestParam(name = "sellerId", required = false) Long sellerId) {
        Long sid = body.sellerId != null ? body.sellerId : sellerId;
        if (sid == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("sellerId is required");
        }
        // server-side unique name check (case-insensitive per seller)
        if (body.name != null && productsRepository.existsBySellerIdAndNameIgnoreCase(sid, body.name.trim())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(java.util.Map.of(
                    "error", "duplicate_name",
                    "message", "Product name already exists"
            ));
        }
        Products p = new Products();
        p.setSellerId(sid);
        p.setName(body.name);
        p.setDescription(body.description);
        p.setPrice(body.price);
        p.setSalePrice(body.salePrice);
    // Server side required enforcement
    if (body.quantity == null || body.quantity <= 0) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(java.util.Map.of(
            "error","invalid_quantity",
            "message","Quantity must be > 0"
        ));
    }
    p.setQuantity(body.quantity);
    if (body.downloadUrl == null || body.downloadUrl.trim().isEmpty()) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(java.util.Map.of(
            "error","download_url_required",
            "message","Download URL is required"
        ));
    }
    p.setDownloadUrl(body.downloadUrl.trim());
        // status normalized by entity callbacks
        p.setStatus(Objects.toString(body.status, "pending"));
        try {
            Products saved = productsRepository.save(p);
            return ResponseEntity.status(HttpStatus.CREATED).body(toDto(saved));
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(java.util.Map.of(
                            "error", "save_failed",
                            "message", ex.getMessage() != null ? ex.getMessage() : "Failed to save product"
                    ));
        }
    }

    // Update product
    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody ProductDto body) {
        return productsRepository.findById(id).map(p -> {
            // Unique name check when changing name
            if (body.name != null) {
                String nextName = body.name.trim();
                if (!nextName.equalsIgnoreCase(Objects.toString(p.getName(), "")) &&
                        p.getSellerId() != null &&
                        productsRepository.existsBySellerIdAndNameIgnoreCase(p.getSellerId(), nextName)) {
                    return ResponseEntity.status(HttpStatus.CONFLICT).body(java.util.Map.of(
                            "error", "duplicate_name",
                            "message", "Product name already exists"
                    ));
                }
            }
            // Detect changes against incoming body. We also track WHICH fields changed so we can
            // decide whether status should be forced to hidden. Rule:
            //  - If current status is public and ONLY price, salePrice, quantity change -> keep public.
            //  - If name OR description OR downloadUrl changes while public -> hide (becomes hidden for re‑review).
            //  - For non-public statuses (pending, etc.), retain existing behavior (force hidden on any change).
            boolean changed = false;
            boolean nameChanged = false;
            boolean descriptionChanged = false;
            // numeric-only changes (price, salePrice, quantity) are considered safe for public status
            boolean downloadUrlChanged = false;

            if (body.name != null && !Objects.equals(p.getName(), body.name)) { changed = true; nameChanged = true; }
            if (!Objects.equals(p.getDescription(), body.description)) { changed = true; descriptionChanged = true; }
            if (!eq(p.getPrice(), body.price)) { changed = true; }
            if (!eq(p.getSalePrice(), body.salePrice)) { changed = true; }
            if (body.quantity != null && !Objects.equals(p.getQuantity(), body.quantity)) { changed = true; }
            if (!Objects.equals(p.getDownloadUrl(), body.downloadUrl)) { changed = true; downloadUrlChanged = true; }

            // Apply incoming values (allow clearing via nulls to persist user's intent)
            if (body.name != null) p.setName(body.name);
            p.setDescription(body.description);
            p.setPrice(body.price);
            p.setSalePrice(body.salePrice);
            if (body.quantity != null) {
                if (body.quantity <= 0) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(java.util.Map.of(
                            "error","invalid_quantity",
                            "message","Quantity must be > 0"
                    ));
                }
                p.setQuantity(body.quantity);
            }
            if (body.downloadUrl != null) {
                if (body.downloadUrl.trim().isEmpty()) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(java.util.Map.of(
                            "error","download_url_required",
                            "message","Download URL is required"
                    ));
                }
                p.setDownloadUrl(body.downloadUrl.trim());
            }

            if (!changed) {
                // No change -> do not alter status, do not save
                return ResponseEntity.status(HttpStatus.NOT_MODIFIED).build();
            }

            // Selective status update logic
            String currentStatus = p.getStatus() == null ? null : p.getStatus().trim().toLowerCase();
            boolean sensitiveChanged = nameChanged || descriptionChanged || downloadUrlChanged; // changes requiring re-review
            if ("public".equals(currentStatus)) {
                // If only numeric/price related changes, keep public
                if (sensitiveChanged) {
                    p.setStatus("hidden");
                } // else keep as public
            } else if (!"hidden".equals(currentStatus)) {
                // Preserve prior behavior: any change on non-hidden & non-public -> hidden
                p.setStatus("hidden");
            }
            try {
                Products saved = productsRepository.save(p);
                return ResponseEntity.ok(toDto(saved));
            } catch (Exception ex) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(java.util.Map.of(
                                "error", "save_failed",
                                "message", ex.getMessage() != null ? ex.getMessage() : "Failed to save product"
                        ));
            }
        }).orElse(ResponseEntity.notFound().build());
    }

    // Optional: client-side async check endpoint
    @GetMapping("/validate-name")
    public ResponseEntity<?> validateName(@RequestParam("sellerId") Long sellerId, @RequestParam("name") String name,
                                          @RequestParam(value = "excludeId", required = false) Long excludeId) {
        String n = name == null ? "" : name.trim();
        if (n.isEmpty()) return ResponseEntity.ok(java.util.Map.of("valid", false, "message", "Name is required"));
        boolean exists = productsRepository.existsBySellerIdAndNameIgnoreCase(sellerId, n);
        if (exists && excludeId != null) {
            var p = productsRepository.findById(excludeId).orElse(null);
            if (p != null && p.getSellerId() != null && p.getSellerId().equals(sellerId) &&
                    p.getName() != null && p.getName().equalsIgnoreCase(n)) {
                exists = false; // same product, allow
            }
        }
        return ResponseEntity.ok(java.util.Map.of("valid", !exists));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        return productsRepository.findById(id).map(p -> {
            Boolean everPublic = p.getWasPublic();
            String status = p.getStatus() != null ? p.getStatus().toLowerCase() : null;
            if (Boolean.TRUE.equals(everPublic) || "public".equals(status)) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(java.util.Map.of(
                        "error", "cannot_delete_public_product",
                        "message", "Product that has been public cannot be deleted. Please hide it instead."
                ));
            }
            productsRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        }).orElse(ResponseEntity.notFound().build());
    }

    // Admin approval: publish/unpublish
    @PostMapping("/{id}/approval")
    public ResponseEntity<?> approve(@PathVariable Long id,
                                     @RequestParam(name = "publish") boolean publish,
                                     @RequestHeader(name = "X-User-Type", required = false) String userType) {
        // Only ADMIN can approve according to frontend contract
        if (userType == null || !userType.equalsIgnoreCase("ADMIN")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Forbidden");
        }
        return productsRepository.findById(id).map(p -> {
            // When publishing, normalize status to public; otherwise hidden
            p.setStatus(publish ? "public" : "hidden");
            if (publish) p.setWasPublic(Boolean.TRUE);
            Products saved = productsRepository.save(p);
            return ResponseEntity.ok(toDto(saved));
        }).orElse(ResponseEntity.notFound().build());
    }

    // Seller request approval: set status to pending
    @PostMapping("/{id}/pending")
    public ResponseEntity<?> markPending(@PathVariable Long id) {
        return productsRepository.findById(id).map(p -> {
            String current = Objects.toString(p.getStatus(), "").toLowerCase();
            if ("hidden".equals(current)) {
                p.setStatus("pending");
                Products saved = productsRepository.save(p);
                return ResponseEntity.ok(toDto(saved));
            } else {
                // leave as-is for public/pending/cancelled
                return ResponseEntity.ok(toDto(p));
            }
        }).orElse(ResponseEntity.notFound().build());
    }

    private static boolean eq(BigDecimal a, BigDecimal b) {
        if (a == b) return true;
        if (a == null || b == null) return false;
        return a.compareTo(b) == 0;
    }
}
