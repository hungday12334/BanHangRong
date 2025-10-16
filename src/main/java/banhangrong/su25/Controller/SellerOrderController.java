package banhangrong.su25.Controller;

import banhangrong.su25.Repository.ProductsRepository;
import banhangrong.su25.Repository.SellerOrderRepository;
import banhangrong.su25.Repository.OrderItemsRepository;
import banhangrong.su25.Entity.OrderItems;
import banhangrong.su25.Entity.Products;
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
    private final OrderItemsRepository orderItemsRepository;
    private final ProductsRepository productsRepository;

    public SellerOrderController(SellerOrderRepository sellerOrderRepository,
                                 OrderItemsRepository orderItemsRepository,
                                 ProductsRepository productsRepository) {
        this.sellerOrderRepository = sellerOrderRepository;
        this.orderItemsRepository = orderItemsRepository;
        this.productsRepository = productsRepository;
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

    @GetMapping("/{orderId}")
    public ResponseEntity<?> getOne(@PathVariable Long sellerId, @PathVariable Long orderId) {
        var summary = sellerOrderRepository.findSellerOrder(sellerId, orderId);
        if (summary == null) return ResponseEntity.notFound().build();
        // Filter items of this order to only items belonging to this seller
        var allItems = orderItemsRepository.findByOrderId(orderId);
        var involvedProductIds = allItems.stream().map(OrderItems::getProductId).filter(java.util.Objects::nonNull).distinct().toList();
        var productList = productsRepository.findAllById(involvedProductIds);
        var productNameMap = productList.stream().collect(java.util.stream.Collectors.toMap(Products::getProductId, Products::getName));
        var sellerProductIdSet = productList.stream().filter(p -> sellerId.equals(p.getSellerId())).map(Products::getProductId).collect(java.util.stream.Collectors.toSet());
        var sellerItems = new java.util.ArrayList<java.util.Map<String,Object>>();
        for (OrderItems it : allItems) {
            var pid = it.getProductId();
            if (pid == null || !sellerProductIdSet.contains(pid)) continue;
            var m = new java.util.LinkedHashMap<String,Object>();
            m.put("productId", pid);
            m.put("productName", productNameMap.get(pid));
            m.put("quantity", it.getQuantity());
            m.put("priceAtTime", it.getPriceAtTime());
            sellerItems.add(m);
        }
        var body = new java.util.LinkedHashMap<String,Object>();
        body.put("order", java.util.Map.of(
                "orderId", summary.getOrderId(),
                "createdAt", summary.getCreatedAt(),
                "sellerAmount", summary.getSellerAmount(),
                "sellerItems", summary.getSellerItems()
        ));
    body.put("user", java.util.Map.of(
        "userId", summary.getBuyerUserId(),
        "username", summary.getBuyerUsername()
        ));
        body.put("items", sellerItems);
        return ResponseEntity.ok(body);
    }
}
