package banhangrong.su25.Controller;

import banhangrong.su25.Entity.Orders;
import banhangrong.su25.Repository.OrdersRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
    private final OrdersRepository ordersRepository;
    private final banhangrong.su25.Repository.OrderItemsRepository orderItemsRepository;

    public OrderController(OrdersRepository ordersRepository,
                           banhangrong.su25.Repository.OrderItemsRepository orderItemsRepository) {
        this.ordersRepository = ordersRepository;
        this.orderItemsRepository = orderItemsRepository;
    }

    @GetMapping
    public List<Orders> list() { return ordersRepository.findAll(); }

    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable Long id) {
    return ordersRepository.findById(id)
        .<ResponseEntity<?>>map(o -> ResponseEntity.ok(java.util.Map.of(
            "order", o,
            "items", orderItemsRepository.findByOrderId(o.getOrderId())
        )))
        .orElseGet(() -> ResponseEntity.notFound().build());
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
