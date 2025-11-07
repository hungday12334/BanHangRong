package banhangrong.su25.Controller;

import banhangrong.su25.Entity.*;
import banhangrong.su25.Repository.ProductsRepository;
import banhangrong.su25.Repository.ProductImagesRepository;
import banhangrong.su25.Repository.ShoppingCartRepository;
import banhangrong.su25.Repository.UsersRepository;
import banhangrong.su25.Repository.OrdersRepository;
import banhangrong.su25.Repository.OrderItemsRepository;
import banhangrong.su25.Repository.ProductLicensesRepository;
import banhangrong.su25.Repository.ProductReviewsRepository;
import banhangrong.su25.service.CustomerDashboardService;
import banhangrong.su25.service.ProductImageService;
import banhangrong.su25.service.ProductService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
public class CustomerDashboardController {

    private final CustomerDashboardService customerDashboardService;
    private final ProductsRepository productsRepository;
    private final ProductImagesRepository productImagesRepository;
    private final ShoppingCartRepository shoppingCartRepository;
    private final UsersRepository usersRepository;
    private final OrdersRepository ordersRepository;
    private final OrderItemsRepository orderItemsRepository;
    private final ProductReviewsRepository productReviewsRepository;
    private final ProductService productService;
    private final ProductImageService productImageService;
    private final ProductLicensesRepository productLicensesRepository;

    public CustomerDashboardController(CustomerDashboardService customerDashboardService,
                                       ProductsRepository productsRepository,
                                       ProductImagesRepository productImagesRepository,
                                       ShoppingCartRepository shoppingCartRepository,
                                       UsersRepository usersRepository,
                                       OrdersRepository ordersRepository,
                                       OrderItemsRepository orderItemsRepository,
                                       ProductReviewsRepository productReviewsRepository,
                                       ProductService productService,
                                       ProductImageService productImageService,
                                       ProductLicensesRepository productLicensesRepository) {
        this.customerDashboardService = customerDashboardService;
        this.productsRepository = productsRepository;
        this.productImagesRepository = productImagesRepository;
        this.shoppingCartRepository = shoppingCartRepository;
        this.usersRepository = usersRepository;
        this.ordersRepository = ordersRepository;
        this.orderItemsRepository = orderItemsRepository;
        this.productReviewsRepository = productReviewsRepository;
        this.productService = productService;
        this.productImageService = productImageService;
        this.productLicensesRepository = productLicensesRepository;
    }


    @GetMapping("/customer/dashboard")
    public String customerDashboard(Model model){
        List<Products> productsList = productService.getAllProducts();
        List<ProductImages> productImagesList = productImageService.getAllProductImages();
        //send product to view
        model.addAttribute("products", productsList);
        //send product images to view
        model.addAttribute("productImages", productImagesList);
        return "customer/dashboard";
    }

    @GetMapping("/rating-history")
    public String ratingHistory() {
        return "redirect:/customer/reviews";
    }

    @GetMapping("/orderhistory")
    public String orderHistory(@RequestParam(name = "page", required = false, defaultValue = "0") int page,
                               @RequestParam(name = "size", required = false, defaultValue = "3") int size,
                               @RequestParam(name = "search", required = false) String search,
                               @RequestParam(name = "status", required = false) String status,
                               @RequestParam(name = "sort", required = false, defaultValue = "newest") String sort,
                               @RequestParam(name = "dateFilter", required = false, defaultValue = "all") String dateFilter,
                               Model model) {
        try {
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

            // Determine sort direction based on sort parameter
            Sort.Direction sortDirection = "oldest".equalsIgnoreCase(sort)
                    ? Sort.Direction.ASC
                    : Sort.Direction.DESC;

            PageRequest pageable = PageRequest.of(Math.max(page, 0), Math.max(size, 1),
                    Sort.by(sortDirection, "createdAt"));

            // Calculate start date based on dateFilter
            LocalDateTime startDate = null;
            if (dateFilter != null && !dateFilter.equalsIgnoreCase("all")) {
                LocalDateTime now = LocalDateTime.now();
                switch (dateFilter.toLowerCase()) {
                    case "today":
                        startDate = now.toLocalDate().atStartOfDay();
                        break;
                    case "this_week":
                        startDate = now.toLocalDate().atStartOfDay().minusDays(now.getDayOfWeek().getValue() - 1);
                        break;
                    case "this_month":
                        startDate = now.toLocalDate().withDayOfMonth(1).atStartOfDay();
                        break;
                    case "last_3_months":
                        startDate = now.minusMonths(3).toLocalDate().atStartOfDay();
                        break;
                    case "last_6_months":
                        startDate = now.minusMonths(6).toLocalDate().atStartOfDay();
                        break;
                    case "this_year":
                        startDate = now.toLocalDate().withDayOfYear(1).atStartOfDay();
                        break;
                    default:
                        startDate = null;
                }
            }

            Page<Orders> ordersPage;
            List<Orders> orders;

            if (search != null && !search.trim().isEmpty()) {
                if (startDate != null) {
                    if ("oldest".equalsIgnoreCase(sort)) {
                        ordersPage = ordersRepository.findByUserIdAndSearchTermAndCreatedAtAfterAsc(currentUser.getUserId(), search.trim(), startDate, pageable);
                    } else {
                        ordersPage = ordersRepository.findByUserIdAndSearchTermAndCreatedAtAfter(currentUser.getUserId(), search.trim(), startDate, pageable);
                    }
                } else {
                    if ("oldest".equalsIgnoreCase(sort)) {
                        ordersPage = ordersRepository.findByUserIdAndSearchTermAsc(currentUser.getUserId(), search.trim(), pageable);
                    } else {
                        ordersPage = ordersRepository.findByUserIdAndSearchTerm(currentUser.getUserId(), search.trim(), pageable);
                    }
                }
            } else if (status != null && !status.trim().isEmpty() && !status.equalsIgnoreCase("all")) {
                String statusLower = status.trim().toLowerCase();
                if ("active".equals(statusLower) || "expired".equals(statusLower)) {
                    // Filter by license key status
                    if (startDate != null) {
                        if ("oldest".equalsIgnoreCase(sort)) {
                            if ("active".equals(statusLower)) {
                                ordersPage = ordersRepository.findByUserIdAndLicenseActiveAndCreatedAtAfterOrderByCreatedAtAsc(currentUser.getUserId(), startDate, pageable);
                            } else {
                                ordersPage = ordersRepository.findByUserIdAndLicenseExpiredAndCreatedAtAfterOrderByCreatedAtAsc(currentUser.getUserId(), startDate, pageable);
                            }
                        } else {
                            if ("active".equals(statusLower)) {
                                ordersPage = ordersRepository.findByUserIdAndLicenseActiveAndCreatedAtAfterOrderByCreatedAtDesc(currentUser.getUserId(), startDate, pageable);
                            } else {
                                ordersPage = ordersRepository.findByUserIdAndLicenseExpiredAndCreatedAtAfterOrderByCreatedAtDesc(currentUser.getUserId(), startDate, pageable);
                            }
                        }
                    } else {
                        if ("oldest".equalsIgnoreCase(sort)) {
                            if ("active".equals(statusLower)) {
                                ordersPage = ordersRepository.findByUserIdAndLicenseActiveOrderByCreatedAtAsc(currentUser.getUserId(), pageable);
                            } else {
                                ordersPage = ordersRepository.findByUserIdAndLicenseExpiredOrderByCreatedAtAsc(currentUser.getUserId(), pageable);
                            }
                        } else {
                            if ("active".equals(statusLower)) {
                                ordersPage = ordersRepository.findByUserIdAndLicenseActiveOrderByCreatedAtDesc(currentUser.getUserId(), pageable);
                            } else {
                                ordersPage = ordersRepository.findByUserIdAndLicenseExpiredOrderByCreatedAtDesc(currentUser.getUserId(), pageable);
                            }
                        }
                    }
                } else {
                    // Original order status filter (for backward compatibility)
                    if (startDate != null) {
                        if ("oldest".equalsIgnoreCase(sort)) {
                            ordersPage = ordersRepository.findByUserIdAndStatusAndCreatedAtAfterOrderByCreatedAtAsc(currentUser.getUserId(), status.trim(), startDate, pageable);
                        } else {
                            ordersPage = ordersRepository.findByUserIdAndStatusAndCreatedAtAfterOrderByCreatedAtDesc(currentUser.getUserId(), status.trim(), startDate, pageable);
                        }
                    } else {
                        if ("oldest".equalsIgnoreCase(sort)) {
                            ordersPage = ordersRepository.findByUserIdAndStatusOrderByCreatedAtAsc(currentUser.getUserId(), status.trim(), pageable);
                        } else {
                            ordersPage = ordersRepository.findByUserIdAndStatusOrderByCreatedAtDesc(currentUser.getUserId(), status.trim(), pageable);
                        }
                    }
                }
            } else {
                if (startDate != null) {
                    if ("oldest".equalsIgnoreCase(sort)) {
                        ordersPage = ordersRepository.findByUserIdAndCreatedAtAfterOrderByCreatedAtAsc(currentUser.getUserId(), startDate, pageable);
                    } else {
                        ordersPage = ordersRepository.findByUserIdAndCreatedAtAfterOrderByCreatedAtDesc(currentUser.getUserId(), startDate, pageable);
                    }
                } else {
                    if ("oldest".equalsIgnoreCase(sort)) {
                        ordersPage = ordersRepository.findByUserIdOrderByCreatedAtAsc(currentUser.getUserId(), pageable);
                    } else {
                        ordersPage = ordersRepository.findByUserIdOrderByCreatedAtDesc(currentUser.getUserId(), pageable);
                    }
                }
            }

            orders = ordersPage.getContent();

            java.util.Map<Long, List<OrderItems>> orderItemsMap = new java.util.HashMap<>();
            java.util.Map<Long, String> productNamesMap = new java.util.HashMap<>();
            java.util.Map<Long, Products> productsMap = new java.util.HashMap<>();
            java.util.Map<Long, String> orderLicenseStatusMap = new java.util.HashMap<>();

            for (Orders order : orders) {
                List<OrderItems> items = orderItemsRepository.findByOrderId(order.getOrderId());
                orderItemsMap.put(order.getOrderId(), items);

                // Calculate license status for this order
                boolean hasActiveLicense = false;
                try {
                    var licensesPage = productLicensesRepository.findByOrderId(order.getOrderId(),
                            org.springframework.data.domain.PageRequest.of(0, 1000));
                    for (var licenseView : licensesPage.getContent()) {
                        if (licenseView.getIsActive() != null && licenseView.getIsActive()) {
                            hasActiveLicense = true;
                            break;
                        }
                    }
                } catch (Exception ignored) {}
                orderLicenseStatusMap.put(order.getOrderId(), hasActiveLicense ? "Active" : "Expired");

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
            model.addAttribute("orderLicenseStatusMap", orderLicenseStatusMap);
            model.addAttribute("page", ordersPage.getNumber());
            model.addAttribute("totalPages", ordersPage.getTotalPages());
            model.addAttribute("size", ordersPage.getSize());
            model.addAttribute("user", currentUser);
            model.addAttribute("search", search);
            model.addAttribute("status", status);
            model.addAttribute("sort", sort);
            model.addAttribute("dateFilter", dateFilter);

            try {
                model.addAttribute("cartCount", shoppingCartRepository.countByUserId(currentUser.getUserId()));
                model.addAttribute("cartItemCount", shoppingCartRepository.countByUserId(currentUser.getUserId()));
            } catch (Exception ignored) {}

            return "customer/orderhistory";
        } catch (Exception e) {
            e.printStackTrace();
            return "redirect:/customer/dashboard?error=orderhistory_error";
        }
    }

    @GetMapping("/notification")
    public String notification() {
        // Redirect to main notifications page
        return "redirect:/customer/notifications";
    }

    @GetMapping("/customer/seller/{sellerId}")
    public String viewSeller(@PathVariable Long sellerId, Model model) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            Users currentUser = null;
            if (auth != null && auth.isAuthenticated()) {
                String username = auth.getName();
                currentUser = usersRepository.findByUsername(username).orElse(null);
            }
            
            Optional<Users> sellerOptional = usersRepository.findById(sellerId);
            if (sellerOptional.isEmpty()) {
                return "redirect:/customer/dashboard?error=seller_not_found";
            }
            
            Users seller = sellerOptional.get();
            
            List<Products> products = productsRepository.findBySellerId(sellerId);
            
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
            
            Long totalProducts = (long) products.size();
            Long totalSales = productsRepository.totalUnitsSoldBySeller(sellerId);
            
            List<ProductReviews> reviews = new java.util.ArrayList<>();
            for (Products product : products) {
                List<ProductReviews> productReviews = productReviewsRepository.findByProductIdOrderByCreatedAtDesc(product.getProductId());
                for (ProductReviews review : productReviews) {
                    usersRepository.findById(review.getUserId()).ifPresent(user -> {
                        review.setUsername(user.getUsername());
                    });
                }
                reviews.addAll(productReviews);
            }
            Long totalReviews = (long) reviews.size();
            
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
            
            reviews.sort((r1, r2) -> r2.getCreatedAt().compareTo(r1.getCreatedAt()));
            if (reviews.size() > 10) {
                reviews = reviews.subList(0, 10);
            }
            
            model.addAttribute("seller", seller);
            model.addAttribute("products", products);
            model.addAttribute("productImages", productImages);
            model.addAttribute("totalProducts", totalProducts);
            model.addAttribute("totalSales", totalSales);
            model.addAttribute("averageProductRating", averageProductRating);
            model.addAttribute("averageServiceRating", averageServiceRating);
            model.addAttribute("totalReviews", totalReviews);
            model.addAttribute("reviews", reviews);
            
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


