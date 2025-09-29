package banhangrong.su25.Controller;

import banhangrong.su25.Entity.OrderItems;
import banhangrong.su25.Entity.Orders;
import banhangrong.su25.Entity.Products;
import banhangrong.su25.Entity.Users;
import banhangrong.su25.Repository.OrderItemsRepository;
import banhangrong.su25.Repository.OrdersRepository;
import banhangrong.su25.Repository.ProductsRepository;
import banhangrong.su25.Repository.UsersRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
    private final OrdersRepository ordersRepository;
    private final OrderItemsRepository orderItemsRepository;
    private final UsersRepository usersRepository;
    private final ProductsRepository productsRepository;

    public OrderController(OrdersRepository ordersRepository,
                           OrderItemsRepository orderItemsRepository,
                           UsersRepository usersRepository,
                           ProductsRepository productsRepository) {
        this.ordersRepository = ordersRepository;
        this.orderItemsRepository = orderItemsRepository;
        this.usersRepository = usersRepository;
        this.productsRepository = productsRepository;
    }

    @GetMapping
    public List<Orders> list() { return ordersRepository.findAll(); }

    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable Long id) {
        Optional<Orders> opt = ordersRepository.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        Orders o = opt.get();

        // Fetch items and enrich with product names (batch to avoid N+1)
        List<OrderItems> items = orderItemsRepository.findByOrderId(o.getOrderId());
        Set<Long> productIds = items.stream()
                .map(OrderItems::getProductId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, String> productNameMap = productsRepository.findAllById(productIds).stream()
                .collect(Collectors.toMap(Products::getProductId, Products::getName));
        List<Map<String, Object>> enrichedItems = items.stream().map(it -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("productId", it.getProductId());
            m.put("productName", productNameMap.get(it.getProductId()));
            m.put("quantity", it.getQuantity());
            m.put("priceAtTime", it.getPriceAtTime());
            return m;
        }).collect(Collectors.toList());

        // Fetch user for username
        Map<String, Object> userInfo = new LinkedHashMap<>();
        userInfo.put("userId", o.getUserId());
        if (o.getUserId() != null) {
            usersRepository.findById(o.getUserId()).ifPresent(u -> {
                userInfo.put("username", u.getUsername());
                userInfo.put("email", u.getEmail());
            });
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("order", o);
        body.put("user", userInfo);
        body.put("items", enrichedItems);
        return ResponseEntity.ok(body);
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Orders req) {
        // Minimal validation
        if (req.getUserId() == null) return ResponseEntity.badRequest().body("userId is required");
        req.setCreatedAt(LocalDateTime.now());
        req.setUpdatedAt(LocalDateTime.now());
        Orders saved = ordersRepository.save(req);
        return ResponseEntity.created(URI.create("/api/orders/" + saved.getOrderId())).body(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody Orders req) {
        Optional<Orders> opt = ordersRepository.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        Orders o = opt.get();
        if (req.getUserId() != null) o.setUserId(req.getUserId());
        if (req.getTotalAmount() != null) o.setTotalAmount(req.getTotalAmount());
        o.setUpdatedAt(LocalDateTime.now());
        Orders saved = ordersRepository.save(o);
        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        if (!ordersRepository.existsById(id)) return ResponseEntity.notFound().build();
        ordersRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
