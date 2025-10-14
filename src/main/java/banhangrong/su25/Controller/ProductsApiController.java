package banhangrong.su25.Controller;

import banhangrong.su25.Entity.Products;
import banhangrong.su25.Repository.ProductsRepository;
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

    public ProductsApiController(ProductsRepository productsRepository) {
        this.productsRepository = productsRepository;
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
        public String status;
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
        d.status = p.getStatus();
        return d;
    }

    // GET /api/products?sellerId=123 -> list products by seller
    @GetMapping
    public List<ProductDto> list(@RequestParam(name = "sellerId", required = false) Long sellerId) {
        List<Products> src = (sellerId != null) ? productsRepository.findBySellerId(sellerId) : productsRepository.findAll();
        return src.stream().map(this::toDto).collect(Collectors.toList());
    }

    // GET /api/products/{id} -> product details
    @GetMapping("/{id}")
    public ResponseEntity<ProductDto> getOne(@PathVariable("id") Long id) {
        return productsRepository.findById(id)
                .map(p -> ResponseEntity.ok(toDto(p)))
                .orElse(ResponseEntity.notFound().build());
    }

    // Create product (sellerId must be provided via body or query)
    @PostMapping
    public ResponseEntity<?> create(@RequestBody ProductDto body,
                                    @RequestParam(name = "sellerId", required = false) Long sellerId) {
        Long sid = body.sellerId != null ? body.sellerId : sellerId;
        if (sid == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("sellerId is required");
        }
        Products p = new Products();
        p.setSellerId(sid);
        p.setName(body.name);
        p.setDescription(body.description);
        p.setPrice(body.price);
        p.setSalePrice(body.salePrice);
        p.setQuantity(body.quantity != null ? body.quantity : 0);
        p.setDownloadUrl(body.downloadUrl);
        // status normalized by entity callbacks
        p.setStatus(Objects.toString(body.status, "pending"));
        Products saved = productsRepository.save(p);
        return ResponseEntity.status(HttpStatus.CREATED).body(toDto(saved));
    }

    // Update product
    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody ProductDto body) {
        return productsRepository.findById(id).map(p -> {
            // Detect changes against incoming body
            boolean changed = false;
            if (body.name != null && !Objects.equals(p.getName(), body.name)) changed = true;
            if (!Objects.equals(p.getDescription(), body.description)) changed = true;
            if (!eq(p.getPrice(), body.price)) changed = true;
            if (!eq(p.getSalePrice(), body.salePrice)) changed = true;
            if (body.quantity != null && !Objects.equals(p.getQuantity(), body.quantity)) changed = true;
            if (!Objects.equals(p.getDownloadUrl(), body.downloadUrl)) changed = true;

            // Apply incoming values (allow clearing via nulls to persist user's intent)
            if (body.name != null) p.setName(body.name);
            p.setDescription(body.description);
            p.setPrice(body.price);
            p.setSalePrice(body.salePrice);
            if (body.quantity != null) p.setQuantity(body.quantity);
            p.setDownloadUrl(body.downloadUrl);

            if (!changed) {
                // No change -> do not alter status, do not save
                return ResponseEntity.status(HttpStatus.NOT_MODIFIED).build();
            }

            // Any change via modal forces status to hidden
            p.setStatus("hidden");
            Products saved = productsRepository.save(p);
            return ResponseEntity.ok(toDto(saved));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        if (!productsRepository.existsById(id)) return ResponseEntity.notFound().build();
        productsRepository.deleteById(id);
        return ResponseEntity.noContent().build();
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
