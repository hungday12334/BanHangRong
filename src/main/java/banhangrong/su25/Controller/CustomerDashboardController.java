package banhangrong.su25.Controller;

import banhangrong.su25.Entity.Products;
import banhangrong.su25.Entity.Orders;
import banhangrong.su25.Entity.OrderItems;
import banhangrong.su25.Entity.ProductReviews;
import banhangrong.su25.Repository.ProductsRepository;
import banhangrong.su25.Repository.ProductImagesRepository;
import banhangrong.su25.Repository.UsersRepository;
import banhangrong.su25.Repository.OrdersRepository;
import banhangrong.su25.Repository.OrderItemsRepository;
import banhangrong.su25.Repository.ProductReviewsRepository;
import banhangrong.su25.Entity.Users;
import banhangrong.su25.Repository.ShoppingCartRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.HashMap;
import java.util.Map;

@Controller
public class CustomerDashboardController {

    private final ProductsRepository productsRepository;
    private final ProductImagesRepository productImagesRepository;
    private final ShoppingCartRepository shoppingCartRepository;
    private final UsersRepository usersRepository;
    private final OrdersRepository ordersRepository;
    private final OrderItemsRepository orderItemsRepository;
    private final ProductReviewsRepository productReviewsRepository;

    public CustomerDashboardController(ProductsRepository productsRepository, ProductImagesRepository productImagesRepository, ShoppingCartRepository shoppingCartRepository, UsersRepository usersRepository, OrdersRepository ordersRepository, OrderItemsRepository orderItemsRepository, ProductReviewsRepository productReviewsRepository) {
        this.productsRepository = productsRepository;
        this.productImagesRepository = productImagesRepository;
        this.shoppingCartRepository = shoppingCartRepository;
        this.usersRepository = usersRepository;
        this.ordersRepository = ordersRepository;
        this.orderItemsRepository = orderItemsRepository;
        this.productReviewsRepository = productReviewsRepository;
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

    @GetMapping("/rating-history")
    public String ratingHistory() {
        // Redirect to the unified My Reviews page
        return "redirect:/customer/reviews";
    }

    @GetMapping("/orderhistory")
    public String orderHistory(@RequestParam(name = "page", required = false, defaultValue = "0") int page,
                               @RequestParam(name = "size", required = false, defaultValue = "3") int size,
                               @RequestParam(name = "search", required = false) String search,
                               @RequestParam(name = "status", required = false) String status,
                               Model model) {
        try {
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

        // Lấy danh sách orders của user hiện tại với search và filter
        PageRequest pageable = PageRequest.of(Math.max(page, 0), Math.max(size, 1),
                Sort.by(Sort.Order.desc("createdAt")));
        
        Page<Orders> ordersPage;
        List<Orders> orders;
        
        // Apply search and filter logic
        if (search != null && !search.trim().isEmpty()) {
            // Search by product name or seller ID
            ordersPage = ordersRepository.findByUserIdAndSearchTerm(currentUser.getUserId(), search.trim(), pageable);
        } else if (status != null && !status.trim().isEmpty() && !status.equalsIgnoreCase("all")) {
            // Filter by status
            ordersPage = ordersRepository.findByUserIdAndStatusOrderByCreatedAtDesc(currentUser.getUserId(), status.trim(), pageable);
        } else {
            // Default: get all orders
            ordersPage = ordersRepository.findByUserIdOrderByCreatedAtDesc(currentUser.getUserId(), pageable);
        }
        
        orders = ordersPage.getContent();

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
        model.addAttribute("search", search);
        model.addAttribute("status", status);
        
        // Cart count
        try {
            model.addAttribute("cartCount", shoppingCartRepository.countByUserId(currentUser.getUserId()));
        } catch (Exception ignored) {}

        return "customer/orderhistory";
        } catch (Exception e) {
            e.printStackTrace();
            return "redirect:/customer/dashboard?error=orderhistory_error";
        }
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

    @GetMapping("/customer/seller/{sellerId}")
    public String viewSeller(@PathVariable Long sellerId, Model model) {
        try {
            // Get current user for header
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            Users currentUser = null;
            if (auth != null && auth.isAuthenticated()) {
                String username = auth.getName();
                currentUser = usersRepository.findByUsername(username).orElse(null);
            }
            
            // Get seller information
            Optional<Users> sellerOptional = usersRepository.findById(sellerId);
            if (sellerOptional.isEmpty()) {
                return "redirect:/customer/dashboard?error=seller_not_found";
            }
            
            Users seller = sellerOptional.get();
            
            // Get seller's products
            List<Products> products = productsRepository.findBySellerId(sellerId);
            
            // Get product images
            Map<Long, String> productImages = new HashMap<>();
            for (Products product : products) {
                try {
                    var primary = productImagesRepository.findTop1ByProductIdAndIsPrimaryTrueOrderByImageIdAsc(product.getProductId());
                    if (primary != null && !primary.isEmpty()) {
                        productImages.put(product.getProductId(), primary.get(0).getImageUrl());
                    } else {
                        var any = productImagesRepository.findTop1ByProductIdOrderByImageIdAsc(product.getProductId());
                        if (any != null && !any.isEmpty()) {
                            productImages.put(product.getProductId(), any.get(0).getImageUrl());
                        }
                    }
                } catch (Exception ignored) {}
            }
            
            // Get seller statistics
            Long totalProducts = (long) products.size();
            Long totalSales = productsRepository.totalUnitsSoldBySeller(sellerId);
            
            // Get reviews for seller's products
            List<ProductReviews> reviews = new java.util.ArrayList<>();
            for (Products product : products) {
                List<ProductReviews> productReviews = productReviewsRepository.findByProductIdOrderByCreatedAtDesc(product.getProductId());
                for (ProductReviews review : productReviews) {
                    // Get username for each review
                    usersRepository.findById(review.getUserId()).ifPresent(user -> {
                        review.setUsername(user.getUsername());
                    });
                }
                reviews.addAll(productReviews);
            }
            Long totalReviews = (long) reviews.size();
            
            // Calculate separate averages for product rating and service rating
            BigDecimal averageProductRating = BigDecimal.ZERO;
            BigDecimal averageServiceRating = BigDecimal.ZERO;
            
            if (!reviews.isEmpty()) {
                double productSum = reviews.stream()
                    .filter(r -> r.getRating() != null)
                    .mapToInt(ProductReviews::getRating)
                    .average()
                    .orElse(0.0);
                averageProductRating = BigDecimal.valueOf(productSum);
                
                double serviceSum = reviews.stream()
                    .filter(r -> r.getServiceRating() != null)
                    .mapToInt(ProductReviews::getServiceRating)
                    .average()
                    .orElse(0.0);
                averageServiceRating = BigDecimal.valueOf(serviceSum);
            }
            
            // Sort reviews by created date descending and limit to 10
            reviews.sort((r1, r2) -> r2.getCreatedAt().compareTo(r1.getCreatedAt()));
            if (reviews.size() > 10) {
                reviews = reviews.subList(0, 10);
            }
            
            // Add data to model
            model.addAttribute("seller", seller);
            model.addAttribute("products", products);
            model.addAttribute("productImages", productImages);
            model.addAttribute("totalProducts", totalProducts);
            model.addAttribute("totalSales", totalSales);
            model.addAttribute("averageProductRating", averageProductRating);
            model.addAttribute("averageServiceRating", averageServiceRating);
            model.addAttribute("totalReviews", totalReviews);
            model.addAttribute("reviews", reviews);
            
            // Add current user data for header
            if (currentUser != null) {
                model.addAttribute("user", currentUser);
                try {
                    model.addAttribute("cartCount", shoppingCartRepository.countByUserId(currentUser.getUserId()));
                } catch (Exception ignored) {}
            }
            
            return "customer/seller-profile";
            
        } catch (Exception e) {
            e.printStackTrace();
            return "redirect:/customer/dashboard?error=seller_view_error";
        }
    }
}


