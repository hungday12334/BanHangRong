package banhangrong.su25.Controller;

import banhangrong.su25.Entity.OrderItems;
import banhangrong.su25.Entity.Users;
import banhangrong.su25.Entity.Products;
import banhangrong.su25.Repository.OrderItemsRepository;
import banhangrong.su25.Repository.ProductsRepository;
import banhangrong.su25.Repository.SellerOrderRepository;
import banhangrong.su25.Repository.UsersRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/seller/orders")
public class OrdersApiController {
    private final SellerOrderRepository sellerOrderRepository;
    private final OrderItemsRepository orderItemsRepository;
    private final ProductsRepository productsRepository;
    private final UsersRepository usersRepository;

    public OrdersApiController(SellerOrderRepository sellerOrderRepository,
                               OrderItemsRepository orderItemsRepository,
                               ProductsRepository productsRepository,
                               UsersRepository usersRepository) {
        this.sellerOrderRepository = sellerOrderRepository;
        this.orderItemsRepository = orderItemsRepository;
        this.productsRepository = productsRepository;
        this.usersRepository = usersRepository;
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<?> getOrderForSeller(@PathVariable Long orderId,
                                               @RequestParam(name = "sellerId", required = false) Long sellerId) {
        Long sellerIdResolved = sellerId;
        if (sellerIdResolved == null) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getName() != null) {
                Users u = usersRepository.findByUsername(auth.getName()).orElse(null);
                if (u != null) sellerIdResolved = u.getUserId();
            }
            if (sellerIdResolved == null) return ResponseEntity.status(401).body(Map.of("error", "unauthenticated"));
        }
        final Long sid = sellerIdResolved;
        var summary = sellerOrderRepository.findSellerOrder(sid, orderId);
        if (summary == null) return ResponseEntity.notFound().build();

        List<OrderItems> allItems = orderItemsRepository.findByOrderId(orderId);
        var involvedProductIds = allItems.stream().map(OrderItems::getProductId).filter(Objects::nonNull).distinct().toList();
        var productList = productsRepository.findAllById(involvedProductIds);
    var productNameMap = productList.stream().collect(Collectors.toMap(Products::getProductId, Products::getName));
    var sellerProductIdSet = productList.stream().filter(p -> sid.equals(p.getSellerId())).map(Products::getProductId).collect(Collectors.toSet());
        var sellerItems = new ArrayList<Map<String,Object>>();
        for (OrderItems it : allItems) {
            var pid = it.getProductId();
            if (pid == null || !sellerProductIdSet.contains(pid)) continue;
            var m = new LinkedHashMap<String,Object>();
            m.put("productId", pid);
            m.put("productName", productNameMap.get(pid));
            m.put("quantity", it.getQuantity());
            m.put("priceAtTime", it.getPriceAtTime());
            sellerItems.add(m);
        }
        var body = new LinkedHashMap<String,Object>();
    body.put("order", Map.of(
        "orderId", summary.getOrderId(),
        "createdAt", summary.getCreatedAt(),
        "sellerAmount", summary.getSellerAmount(),
        "sellerItems", summary.getSellerItems(),
        // Include totalAmount for client compatibility (use sellerAmount scoped to this seller)
        "totalAmount", summary.getSellerAmount()
    ));
        body.put("user", Map.of(
                "userId", summary.getBuyerUserId(),
                "username", summary.getBuyerUsername()
        ));
        body.put("items", sellerItems);
        return ResponseEntity.ok(body);
    }
}
