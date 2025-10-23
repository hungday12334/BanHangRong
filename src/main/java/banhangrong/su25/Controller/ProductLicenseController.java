package banhangrong.su25.Controller;

import banhangrong.su25.Repository.ProductLicensesRepository;
import banhangrong.su25.Repository.ProductsRepository;
import banhangrong.su25.Repository.OrderItemsRepository;
import banhangrong.su25.Repository.OrdersRepository;
import banhangrong.su25.Entity.ProductLicenses;
import banhangrong.su25.Entity.OrderItems;
import banhangrong.su25.Entity.Orders;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@RestController
public class ProductLicenseController {

    private final ProductLicensesRepository licensesRepository;
    private final ProductsRepository productsRepository;
    private final OrderItemsRepository orderItemsRepository;
    private final OrdersRepository ordersRepository;

    public ProductLicenseController(ProductLicensesRepository licensesRepository,
                                    ProductsRepository productsRepository,
                                    OrderItemsRepository orderItemsRepository,
                                    OrdersRepository ordersRepository) {
        this.licensesRepository = licensesRepository;
        this.productsRepository = productsRepository;
        this.orderItemsRepository = orderItemsRepository;
        this.ordersRepository = ordersRepository;
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
        Optional<ProductLicenses> opt = licensesRepository.findById(id);
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

    /** Seller-scoped: only allow toggling licenses that belong to the seller's products */
    @PatchMapping("/api/seller/{sellerId}/licenses/{id}")
    public ResponseEntity<?> updateForSeller(@PathVariable Long sellerId,
                                             @PathVariable Long id,
                                             @RequestBody Map<String, Object> payload) {
        Optional<ProductLicenses> opt = licensesRepository.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        var lic = opt.get();
        Long orderItemId = lic.getOrderItemId();
        if (orderItemId == null) return ResponseEntity.badRequest().body("license không gắn với order_item");
        Optional<OrderItems> oiOpt = orderItemsRepository.findById(orderItemId);
        if (oiOpt.isEmpty()) return ResponseEntity.badRequest().body("order item không tồn tại");
        Long productId = oiOpt.get().getProductId();
        var prod = productsRepository.findById(productId).orElse(null);
        if (prod == null || prod.getSellerId() == null || !prod.getSellerId().equals(sellerId)) {
            return ResponseEntity.status(403).body("Không có quyền cập nhật license này");
        }
        // Perform updates
        if (payload.containsKey("isActive")) {
            Object v = payload.get("isActive");
            if (v instanceof Boolean b) lic.setIsActive(b);
        }
        if (payload.containsKey("deviceIdentifier")) {
            Object v = payload.get("deviceIdentifier");
            lic.setDeviceIdentifier(v != null ? v.toString() : null);
        }
        licensesRepository.save(lic);
        return ResponseEntity.ok(lic);
    }

    /**
     * Generate license keys for a PUBLIC product owned by the seller.
     * Request body: { productId: number, expireDate: 'yyyyMMdd' (optional), quantity: number }
     * Rules:
     *  - Only for products with status 'public'.
     *  - Total keys (generated + existing sold) cannot exceed product.quantity.
     *  - Keys are saved immediately with order_item_id = null, is_active = true by default.
     *  - Key format: PRD<productId>-<expire>-<random>
     */
    @PostMapping("/api/seller/{sellerId}/licenses/generate")
    public ResponseEntity<?> generateKeys(@PathVariable Long sellerId, @RequestBody Map<String, Object> body) {
        Object pidObj = body.get("productId");
        Object qtyObj = body.get("quantity");
        Object expObj = body.get("expireDate"); // yyyyMMdd or null
        Object userObj = body.get("userId"); // optional
        Object orderItemObj = body.get("orderItemId"); // optional
        if (pidObj == null || qtyObj == null) return ResponseEntity.badRequest().body("productId and quantity are required");
        long productId;
        int requestQty;
        try { productId = Long.parseLong(pidObj.toString()); } catch (Exception e) { return ResponseEntity.badRequest().body("invalid productId"); }
        try { requestQty = Integer.parseInt(qtyObj.toString()); } catch (Exception e) { return ResponseEntity.badRequest().body("invalid quantity"); }
        if (requestQty <= 0) return ResponseEntity.badRequest().body("quantity must be > 0");

        var productOpt = productsRepository.findById(productId);
        if (productOpt.isEmpty()) return ResponseEntity.badRequest().body("product not found");
        var p = productOpt.get();
        if (p.getSellerId() == null || !p.getSellerId().equals(sellerId)) return ResponseEntity.status(403).body("product does not belong to seller");
        if (p.getStatus() == null || !"public".equalsIgnoreCase(p.getStatus().trim())) return ResponseEntity.badRequest().body("product must be PUBLIC");
        int capacity = p.getQuantity() != null ? p.getQuantity() : 0;

        // If orderItemId is provided, validate that it belongs to this product and this seller
        Long orderItemId = null;
        if (orderItemObj != null && !orderItemObj.toString().isBlank()) {
            try { orderItemId = Long.parseLong(orderItemObj.toString()); } catch (Exception e) { return ResponseEntity.badRequest().body("invalid orderItemId"); }
            var oiOpt = orderItemsRepository.findById(orderItemId);
            if (oiOpt.isEmpty()) return ResponseEntity.badRequest().body("order item not found");
            var oi = oiOpt.get();
            if (oi.getProductId() == null || !oi.getProductId().equals(productId)) {
                return ResponseEntity.badRequest().body("orderItem does not belong to the specified product");
            }
            // product already checked belongs to seller
        }

        // Parse/derive userId if provided; if not and orderItem present, try to resolve via order
        Long userId = null;
        if (userObj != null && !userObj.toString().isBlank()) {
            try { userId = Long.parseLong(userObj.toString()); } catch (Exception e) { return ResponseEntity.badRequest().body("invalid userId"); }
        } else if (orderItemId != null) {
            // derive user from order
            var oiOpt = orderItemsRepository.findById(orderItemId);
            if (oiOpt.isPresent()) {
                Long orderId = oiOpt.get().getOrderId();
                if (orderId != null) {
                    var ordOpt = ordersRepository.findById(orderId);
                    if (ordOpt.isPresent()) userId = ordOpt.get().getUserId();
                }
            }
        }

        // If no orderItemId provided, auto-create order and item to ensure keys are visible in management
        if (orderItemId == null) {
            if (userId == null) {
                return ResponseEntity.badRequest().body("orderItemId trống: cần userId để tự tạo order/item");
            }
            // Create minimal order
            Orders order = new Orders();
            order.setUserId(userId);
            // sellerId field may or may not exist in DB; entity has it, but DB script might not. We set if possible.
            try { order.setSellerId(sellerId); } catch (Exception ignored) {}
            try { order.setStatus("completed"); } catch (Exception ignored) {}
            order.setTotalAmount(java.math.BigDecimal.ZERO);
            order.setCreatedAt(LocalDateTime.now());
            order.setUpdatedAt(LocalDateTime.now());
            Orders savedOrder = ordersRepository.save(order);

            // Create order item for selected product
            OrderItems oi = new OrderItems();
            oi.setOrderId(savedOrder.getOrderId());
            oi.setProductId(productId);
            oi.setQuantity(requestQty);
            oi.setPriceAtTime(java.math.BigDecimal.ZERO);
            oi.setCreatedAt(LocalDateTime.now());
            OrderItems savedItem = orderItemsRepository.save(oi);
            orderItemId = savedItem.getOrderItemId();
        }
        // existing = sold licenses tied to orders + pre-generated stock for this product
        long sold = licensesRepository.countByProductViaOrders(productId);
        long pre = licensesRepository.countPreGeneratedForProduct(productId);
        long remaining = Math.max(0, (long)capacity - sold - pre);
        if (remaining <= 0) return ResponseEntity.badRequest().body("no capacity left to generate more keys");
        if (requestQty > remaining) requestQty = (int) remaining;

        String expStr = null;
        if (expObj != null && !expObj.toString().isBlank()) {
            try {
                // accept yyyy-MM-dd or yyyyMMdd
                String raw = expObj.toString().trim();
                LocalDate d = raw.contains("-") ? LocalDate.parse(raw) : LocalDate.parse(raw, DateTimeFormatter.ofPattern("yyyyMMdd"));
                expStr = d.format(DateTimeFormatter.BASIC_ISO_DATE); // yyyyMMdd
            } catch (Exception e) {
                return ResponseEntity.badRequest().body("invalid expireDate");
            }
        } else {
            expStr = "N/A";
        }

        LocalDateTime now = LocalDateTime.now();
        for (int i = 0; i < requestQty; i++) {
            String random = UUID.randomUUID().toString().replaceAll("-", "").substring(0, 12).toUpperCase();
            String key = "PRD" + productId + '-' + expStr + '-' + random;
            ProductLicenses lic = new ProductLicenses();
            lic.setOrderItemId(orderItemId); // optional linkage
            lic.setUserId(userId);
            lic.setLicenseKey(key);
            lic.setIsActive(true);
            lic.setActivationDate(null);
            lic.setLastUsedDate(null);
            lic.setDeviceIdentifier(null);
            lic.setCreatedAt(now);
            lic.setUpdatedAt(now);
            licensesRepository.save(lic);
        }
        Map<String, Object> resp = new HashMap<>();
        resp.put("generated", requestQty);
        resp.put("remaining", Math.max(0, remaining - requestQty));
        return ResponseEntity.ok(resp);
    }
}
