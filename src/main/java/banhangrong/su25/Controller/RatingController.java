package banhangrong.su25.Controller;

import banhangrong.su25.Entity.ProductReviews;
import banhangrong.su25.Entity.Products;
import banhangrong.su25.Entity.Users;
import banhangrong.su25.Entity.OrderItems;
import banhangrong.su25.Repository.ProductReviewsRepository;
import banhangrong.su25.Repository.ProductsRepository;
import banhangrong.su25.Repository.UsersRepository;
import banhangrong.su25.Repository.OrderItemsRepository;
import banhangrong.su25.service.RatingCalculationService;
import banhangrong.su25.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Controller
public class RatingController {

    @Autowired
    private ProductReviewsRepository productReviewsRepository;
    
    @Autowired
    private ProductsRepository productsRepository;
    
    @Autowired
    private UsersRepository usersRepository;
    
    @Autowired
    private OrderItemsRepository orderItemsRepository;
    
    @Autowired
    private RatingCalculationService ratingCalculationService;
    
    @Autowired
    private NotificationService notificationService;

    private static final String UPLOAD_DIR = "src/main/resources/static/uploads/";

    @PostMapping("/api/rating/submit")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> submitRating(
            @RequestParam("orderItemId") Long orderItemId,
            @RequestParam("productRating") Integer productRating,
            @RequestParam("serviceRating") Integer serviceRating,
            @RequestParam("comment") String comment,
            @RequestParam(value = "mediaFiles", required = false) MultipartFile[] mediaFiles) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Lấy user hiện tại
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated()) {
                response.put("success", false);
                response.put("message", "User not authenticated");
                return ResponseEntity.badRequest().body(response);
            }
            
            String username = auth.getName();
            Users user = usersRepository.findByUsername(username).orElse(null);
            if (user == null) {
                response.put("success", false);
                response.put("message", "User not found");
                return ResponseEntity.badRequest().body(response);
            }
            
            // Kiểm tra order item tồn tại và thuộc về user hiện tại
            OrderItems orderItem = orderItemsRepository.findByOrderItemIdAndUserId(orderItemId, user.getUserId()).orElse(null);
            if (orderItem == null) {
                response.put("success", false);
                response.put("message", "Order item not found or not owned by user");
                return ResponseEntity.badRequest().body(response);
            }
            
            // Kiểm tra xem đã có review cho order item này chưa
            if (productReviewsRepository.existsByOrderItemId(orderItemId)) {
                response.put("success", false);
                response.put("message", "Review already exists for this order item");
                return ResponseEntity.badRequest().body(response);
            }
            
            // Kiểm tra xem user đã review sản phẩm này chưa (fallback cho constraint cũ)
            if (productReviewsRepository.existsByUserIdAndProductId(user.getUserId(), orderItem.getProductId())) {
                response.put("success", false);
                response.put("message", "You have already reviewed this product");
                return ResponseEntity.badRequest().body(response);
            }
            
            // Lấy product từ order item
            Products product = productsRepository.findById(orderItem.getProductId()).orElse(null);
            if (product == null) {
                response.put("success", false);
                response.put("message", "Product not found");
                return ResponseEntity.badRequest().body(response);
            }
            
            // Xử lý upload media files
            StringBuilder mediaUrls = new StringBuilder();
            if (mediaFiles != null && mediaFiles.length > 0) {
                for (MultipartFile file : mediaFiles) {
                    if (!file.isEmpty()) {
                        String fileName = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
                        Path filePath = Paths.get(UPLOAD_DIR + fileName);
                        
                        // Tạo thư mục nếu chưa tồn tại
                        Files.createDirectories(filePath.getParent());
                        
                        // Lưu file
                        Files.write(filePath, file.getBytes());
                        
                        if (mediaUrls.length() > 0) {
                            mediaUrls.append(",");
                        }
                        mediaUrls.append("/uploads/").append(fileName);
                    }
                }
            }
            
            // Tạo review
            ProductReviews review = new ProductReviews();
            review.setProductId(orderItem.getProductId());
            review.setUserId(user.getUserId());
            review.setOrderItemId(orderItemId);
            review.setRating(productRating);
            review.setComment(comment);
            review.setMediaUrls(mediaUrls.length() > 0 ? mediaUrls.toString() : null);
            review.setServiceRating(serviceRating);
            review.setCreatedAt(LocalDateTime.now());
            review.setUpdatedAt(LocalDateTime.now());
            
            // Lưu review với xử lý lỗi constraint
            try {
                productReviewsRepository.save(review);
            } catch (org.springframework.dao.DataIntegrityViolationException e) {
                // Xử lý lỗi constraint violation
                if (e.getMessage().contains("ux_reviews_user_product")) {
                    response.put("success", false);
                    response.put("message", "You have already reviewed this product");
                    return ResponseEntity.badRequest().body(response);
                } else if (e.getMessage().contains("ux_reviews_order_item")) {
                    response.put("success", false);
                    response.put("message", "Review already exists for this order item");
                    return ResponseEntity.badRequest().body(response);
                } else {
                    throw e; // Re-throw nếu là lỗi khác
                }
            }
            
            // Cập nhật rating trung bình của product
            ratingCalculationService.updateProductAverageRating(orderItem.getProductId());
            
            // Gửi thông báo đánh giá thành công
            try {
                String productName = product.getName() != null ? product.getName() : "sản phẩm";
                notificationService.createReviewNotification(user.getUserId(), review.getReviewId(), productName);
            } catch (Exception e) {
                System.err.println("[Rating] Failed to send notification: " + e.getMessage());
            }
            
            response.put("success", true);
            response.put("message", "Rating submitted successfully");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "Error submitting rating: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    @GetMapping("/api/rating/available-orders")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getAvailableOrdersForReview() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated()) {
                response.put("success", false);
                response.put("message", "User not authenticated");
                return ResponseEntity.badRequest().body(response);
            }
            
            String username = auth.getName();
            Users user = usersRepository.findByUsername(username).orElse(null);
            if (user == null) {
                response.put("success", false);
                response.put("message", "User not found");
                return ResponseEntity.badRequest().body(response);
            }
            
            // Lấy tất cả order items của user
            List<OrderItems> orderItems = orderItemsRepository.findByUserId(user.getUserId());
            
            // Lọc ra những order items chưa có review
            List<Map<String, Object>> availableOrders = new ArrayList<>();
            for (OrderItems orderItem : orderItems) {
                if (!productReviewsRepository.existsByOrderItemId(orderItem.getOrderItemId())) {
                    Map<String, Object> orderInfo = new HashMap<>();
                    orderInfo.put("orderItemId", orderItem.getOrderItemId());
                    orderInfo.put("productId", orderItem.getProductId());
                    orderInfo.put("quantity", orderItem.getQuantity());
                    orderInfo.put("priceAtTime", orderItem.getPriceAtTime());
                    orderInfo.put("createdAt", orderItem.getCreatedAt());
                    
                    // Lấy thông tin sản phẩm
                    Products product = productsRepository.findById(orderItem.getProductId()).orElse(null);
                    if (product != null) {
                        orderInfo.put("productName", product.getName());
                        orderInfo.put("productDescription", product.getDescription());
                    }
                    
                    availableOrders.add(orderInfo);
                }
            }
            
            response.put("success", true);
            response.put("availableOrders", availableOrders);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "Error fetching available orders: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    @GetMapping("/api/rating/history")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getRatingHistory() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated()) {
                response.put("success", false);
                response.put("message", "User not authenticated");
                return ResponseEntity.badRequest().body(response);
            }
            
            String username = auth.getName();
            Users user = usersRepository.findByUsername(username).orElse(null);
            if (user == null) {
                response.put("success", false);
                response.put("message", "User not found");
                return ResponseEntity.badRequest().body(response);
            }
            
            // Lấy lịch sử đánh giá của user
            List<ProductReviews> reviews = productReviewsRepository.findByUserIdOrderByCreatedAtDesc(user.getUserId());
            
            response.put("success", true);
            response.put("reviews", reviews);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "Error fetching rating history: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    @GetMapping("/api/rating/stats/{productId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getRatingStats(@PathVariable Long productId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            RatingCalculationService.RatingStats stats = ratingCalculationService.getProductRatingStats(productId);
            
            response.put("success", true);
            response.put("averageRating", stats.getAverageRating());
            response.put("totalReviews", stats.getTotalReviews());
            response.put("ratingDistribution", stats.getRatingDistribution());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "Error fetching rating stats: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    @PostMapping("/api/rating/recalculate-all")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> recalculateAllRatings() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated()) {
                response.put("success", false);
                response.put("message", "User not authenticated");
                return ResponseEntity.badRequest().body(response);
            }
            
            String username = auth.getName();
            Users user = usersRepository.findByUsername(username).orElse(null);
            if (user == null || !"admin".equals(user.getUserType())) {
                response.put("success", false);
                response.put("message", "Admin access required");
                return ResponseEntity.badRequest().body(response);
            }
            
            // Lấy tất cả sản phẩm có review
            List<Products> products = productsRepository.findAll();
            int updatedCount = 0;
            
            for (Products product : products) {
                List<ProductReviews> reviews = productReviewsRepository.findByProductIdOrderByCreatedAtDesc(product.getProductId());
                if (!reviews.isEmpty()) {
                    ratingCalculationService.updateProductAverageRating(product.getProductId());
                    updatedCount++;
                }
            }
            
            response.put("success", true);
            response.put("message", "Recalculated ratings for " + updatedCount + " products");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "Error recalculating ratings: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
}
