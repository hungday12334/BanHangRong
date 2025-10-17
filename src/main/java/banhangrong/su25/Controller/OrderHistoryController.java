package banhangrong.su25.Controller;

import banhangrong.su25.Entity.Orders;
import banhangrong.su25.Entity.OrderItems;
import banhangrong.su25.Entity.Products;
import banhangrong.su25.Repository.OrdersRepository;
import banhangrong.su25.Repository.OrderItemsRepository;
import banhangrong.su25.Repository.ProductsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class OrderHistoryController {

    @Autowired
    private OrdersRepository ordersRepository;
    
    @Autowired
    private OrderItemsRepository orderItemsRepository;
    
    @Autowired
    private ProductsRepository productsRepository;

    private Long getCurrentUserId() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && auth.getName() != null) {
                // For demo: return hardcoded user ID
                // In real app, get from user repository
                return 2L;
            }
        } catch (Exception ignored) {}
        return 2L; // fallback for demo
    }

    @GetMapping("/orders")
    public String orderHistory(@RequestParam(name = "page", required = false, defaultValue = "0") int page,
                              @RequestParam(name = "size", required = false, defaultValue = "10") int size,
                              Model model) {
        
        Long userId = getCurrentUserId();
        Pageable pageable = PageRequest.of(page, size);
        Page<Orders> ordersPage = ordersRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        List<Orders> orders = ordersPage.getContent();
        
        // Get order items for each order
        Map<Long, List<OrderItems>> orderItemsMap = new HashMap<>();
        Map<Long, String> productNamesMap = new HashMap<>();
        Map<Long, Products> productsMap = new HashMap<>();
        
        for (Orders order : orders) {
            List<OrderItems> orderItems = orderItemsRepository.findByOrderId(order.getOrderId());
            orderItemsMap.put(order.getOrderId(), orderItems);
            
            // Get product names and details for order items
            for (OrderItems item : orderItems) {
                Products product = productsRepository.findById(item.getProductId()).orElse(null);
                if (product != null) {
                    productNamesMap.put(item.getProductId(), product.getName());
                    productsMap.put(item.getProductId(), product);
                }
            }
        }
        
        model.addAttribute("orders", orders);
        model.addAttribute("orderItemsMap", orderItemsMap);
        model.addAttribute("productNamesMap", productNamesMap);
        model.addAttribute("productsMap", productsMap);
        model.addAttribute("page", ordersPage.getNumber());
        model.addAttribute("totalPages", ordersPage.getTotalPages());
        model.addAttribute("size", ordersPage.getSize());
        model.addAttribute("cartCount", 0); // You can calculate this if needed
        
        return "customer/orderhistory";
    }
}
