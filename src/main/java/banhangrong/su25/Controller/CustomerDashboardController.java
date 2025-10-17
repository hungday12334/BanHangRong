package banhangrong.su25.Controller;

import banhangrong.su25.Entity.Products;
import banhangrong.su25.Entity.Orders;
import banhangrong.su25.Entity.OrderItems;
import banhangrong.su25.Repository.ProductsRepository;
import banhangrong.su25.Repository.ProductImagesRepository;
import banhangrong.su25.Repository.UsersRepository;
import banhangrong.su25.Repository.OrdersRepository;
import banhangrong.su25.Repository.OrderItemsRepository;
import banhangrong.su25.Entity.Users;
import banhangrong.su25.Repository.ShoppingCartRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Optional;

@Controller
public class CustomerDashboardController {

    private final ProductsRepository productsRepository;
    private final ProductImagesRepository productImagesRepository;
    private final ShoppingCartRepository shoppingCartRepository;
    private final UsersRepository usersRepository;
    private final OrdersRepository ordersRepository;
    private final OrderItemsRepository orderItemsRepository;

    public CustomerDashboardController(ProductsRepository productsRepository, ProductImagesRepository productImagesRepository, ShoppingCartRepository shoppingCartRepository, UsersRepository usersRepository, OrdersRepository ordersRepository, OrderItemsRepository orderItemsRepository) {
        this.productsRepository = productsRepository;
        this.productImagesRepository = productImagesRepository;
        this.shoppingCartRepository = shoppingCartRepository;
        this.usersRepository = usersRepository;
        this.ordersRepository = ordersRepository;
        this.orderItemsRepository = orderItemsRepository;
    }

    @GetMapping("/customer/dashboard")
    public String customerDashboard(@RequestParam(name = "page", required = false, defaultValue = "0") int page,
                                    @RequestParam(name = "size", required = false, defaultValue = "15") int size,
                                    @RequestParam(name = "search", required = false) String search,
                                    Model model) {
        // Kiểm tra email verified cho CUSTOMER
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Users currentUser = null;
        if (auth != null && auth.isAuthenticated()) {
            String username = auth.getName();
            currentUser = usersRepository.findByUsername(username).orElse(null);
            if (currentUser != null && "CUSTOMER".equals(currentUser.getUserType())) {
                if (!Boolean.TRUE.equals(currentUser.getIsEmailVerified())) {
                    return "redirect:/verify-email-required";
                }
            }
        }
        
        // ORM: paginated public products ordered by total sales desc then created_at desc
        PageRequest pageable = PageRequest.of(Math.max(page,0), Math.max(size,1),
                Sort.by(Sort.Order.desc("totalSales"), Sort.Order.desc("createdAt")));
        
        Page<Products> featuredPage;
        if (search != null && !search.trim().isEmpty()) {
            // Search mode
            featuredPage = productsRepository.findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCaseAndStatus(
                search.trim(), search.trim(), "Public", pageable);
        } else {
            // Normal mode
            featuredPage = productsRepository.findByStatus("Public", pageable);
        }
        List<Products> featured = featuredPage.getContent();
        // Derive primary image directly from entity relations
        java.util.Map<Long, String> primaryImageByProduct = new java.util.HashMap<>();
        for (Products p : featured) {
            String url = null;
            // Try repository lookups for primary image first, then any image
            try {
                var primary = productImagesRepository.findTop1ByProductIdAndIsPrimaryTrueOrderByImageIdAsc(p.getProductId());
                if (primary != null && !primary.isEmpty()) {
                    url = primary.get(0).getImageUrl();
                } else {
                    var any = productImagesRepository.findTop1ByProductIdOrderByImageIdAsc(p.getProductId());
                    if (any != null && !any.isEmpty()) {
                        url = any.get(0).getImageUrl();
                    }
                }
            } catch (Exception ignored) {}
            if (url != null && !url.isBlank()) primaryImageByProduct.put(p.getProductId(), url);
        }
        model.addAttribute("featuredProducts", featured);
        model.addAttribute("page", featuredPage.getNumber());
        model.addAttribute("totalPages", featuredPage.getTotalPages());
        model.addAttribute("size", featuredPage.getSize());
        model.addAttribute("primaryImageByProduct", primaryImageByProduct);
        model.addAttribute("search", search);
        // Use logged-in user info for header
        try {
            if (currentUser != null) {
                model.addAttribute("cartCount", shoppingCartRepository.countByUserId(currentUser.getUserId()));
                model.addAttribute("user", currentUser);
            }
        } catch (Exception ignored) {}
        return "customer/dashboard";
    }

    @GetMapping("/orderhistory")
    public String orderHistory(@RequestParam(name = "page", required = false, defaultValue = "0") int page,
                              @RequestParam(name = "size", required = false, defaultValue = "10") int size,
                              Model model) {
        // Kiểm tra authentication
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Users currentUser = null;
        if (auth != null && auth.isAuthenticated()) {
            String username = auth.getName();
            currentUser = usersRepository.findByUsername(username).orElse(null);
            if (currentUser == null) {
                return "redirect:/login";
            }
        } else {
            return "redirect:/login";
        }

        // Lấy danh sách orders của user hiện tại
        PageRequest pageable = PageRequest.of(Math.max(page, 0), Math.max(size, 1),
                Sort.by(Sort.Order.desc("createdAt")));
        
        Page<Orders> ordersPage = ordersRepository.findByUserIdOrderByCreatedAtDesc(currentUser.getUserId(), pageable);
        List<Orders> orders = ordersPage.getContent();

        // Lấy order items cho mỗi order
        java.util.Map<Long, List<OrderItems>> orderItemsMap = new java.util.HashMap<>();
        java.util.Map<Long, String> productNamesMap = new java.util.HashMap<>();
        java.util.Map<Long, Products> productsMap = new java.util.HashMap<>();
        
        for (Orders order : orders) {
            List<OrderItems> items = orderItemsRepository.findByOrderId(order.getOrderId());
            orderItemsMap.put(order.getOrderId(), items);
            
            // Lấy tên sản phẩm và thông tin sản phẩm
            for (OrderItems item : items) {
                if (item.getProductId() != null) {
                    productsRepository.findById(item.getProductId()).ifPresent(product -> {
                        productNamesMap.put(item.getProductId(), product.getName());
                        productsMap.put(item.getProductId(), product);
                    });
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
        model.addAttribute("user", currentUser);
        
        // Cart count
        try {
            model.addAttribute("cartCount", shoppingCartRepository.countByUserId(currentUser.getUserId()));
        } catch (Exception ignored) {}

        return "customer/orderhistory";
    }

    @GetMapping("/support")
    public String support(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        Optional<Users> userOptional = usersRepository.findByUsername(username);
        
        if (userOptional.isEmpty()) {
            return "redirect:/login";
        }
        
        Users user = userOptional.get();
        
        // Get cart count
        Long cartCount = shoppingCartRepository.countByUserId(user.getUserId());
        
        model.addAttribute("user", user);
        model.addAttribute("cartCount", cartCount);
        
        return "customer/support";
    }

    @GetMapping("/notification")
    public String notification(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        Optional<Users> userOptional = usersRepository.findByUsername(username);
        
        if (userOptional.isEmpty()) {
            return "redirect:/login";
        }
        
        Users user = userOptional.get();
        
        // Get cart count
        Long cartCount = shoppingCartRepository.countByUserId(user.getUserId());
        
        model.addAttribute("user", user);
        model.addAttribute("cartCount", cartCount);
        
        return "customer/notification";
    }
}


