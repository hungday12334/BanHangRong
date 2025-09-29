package banhangrong.su25.Controller;

import banhangrong.su25.Entity.Products;
import banhangrong.su25.Repository.ProductsRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.Optional;

@RestController
@RequestMapping("/api/products")
public class ProductAdminController {
    private final ProductsRepository productsRepository;

    public ProductAdminController(ProductsRepository productsRepository) {
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

    // Seller creates product: default not public (isActive = false) waiting for admin approval
    @PostMapping
    public ResponseEntity<?> create(@RequestBody Products req) {
        if (req.getSellerId() == null || req.getName() == null || req.getPrice() == null) {
            return ResponseEntity.badRequest().body("sellerId, name, price are required");
        }
        req.setIsActive(false); // pending approval
        req.setCreatedAt(LocalDateTime.now());
        req.setUpdatedAt(LocalDateTime.now());
        Products saved = productsRepository.save(req);
        return ResponseEntity.created(URI.create("/api/products/" + saved.getProductId())).body(saved);
    }

    // Seller updates product: if currently public, auto set to hidden (isActive=false)
    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody Products req) {
        Optional<Products> opt = productsRepository.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        Products p = opt.get();
        // Basic fields update
        if (req.getName() != null) p.setName(req.getName());
        if (req.getDescription() != null) p.setDescription(req.getDescription());
        if (req.getPrice() != null) p.setPrice(req.getPrice());
        if (req.getSalePrice() != null) p.setSalePrice(req.getSalePrice());
        if (req.getQuantity() != null) p.setQuantity(req.getQuantity());
        if (req.getDownloadUrl() != null) p.setDownloadUrl(req.getDownloadUrl());
        p.setUpdatedAt(LocalDateTime.now());
        // If currently active/public -> hide until admin re-approves
        if (Boolean.TRUE.equals(p.getIsActive())) {
            p.setIsActive(false);
        }
        Products saved = productsRepository.save(p);
        return ResponseEntity.ok(saved);
    }

    // Seller deletes product freely
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        if (!productsRepository.existsById(id)) return ResponseEntity.notFound().build();
        productsRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // Admin approves or unpublishes product
    @PostMapping("/{id}/approval")
    public ResponseEntity<?> approve(@PathVariable Long id, @RequestParam("publish") boolean publish) {
        Optional<Products> opt = productsRepository.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        Products p = opt.get();
        p.setIsActive(publish);
        p.setUpdatedAt(LocalDateTime.now());
        Products saved = productsRepository.save(p);
        return ResponseEntity.ok(saved);
    }
}
