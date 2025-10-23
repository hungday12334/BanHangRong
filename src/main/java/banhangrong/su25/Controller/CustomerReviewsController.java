package banhangrong.su25.Controller;

import banhangrong.su25.Entity.Users;
import banhangrong.su25.Entity.ProductReviews;
import banhangrong.su25.Entity.Products;
import banhangrong.su25.Entity.ProductImages;
import banhangrong.su25.Repository.UsersRepository;
import banhangrong.su25.Repository.ShoppingCartRepository;
import banhangrong.su25.Repository.ProductReviewsRepository;
import banhangrong.su25.Repository.ProductsRepository;
import banhangrong.su25.Repository.ProductImagesRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Optional;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;

@Controller
public class CustomerReviewsController {

    @Autowired
    private UsersRepository usersRepository;

    @Autowired
    private ShoppingCartRepository shoppingCartRepository;
    
    @Autowired
    private ProductReviewsRepository productReviewsRepository;
    
    @Autowired
    private ProductsRepository productsRepository;
    
    @Autowired
    private ProductImagesRepository productImagesRepository;

    @GetMapping("/customer/reviews")
    public String reviews(Model model,
                         @RequestParam(required = false) Integer stars,
                         @RequestParam(required = false) String date,
                         @RequestParam(required = false) String sort,
                         @RequestParam(required = false) String keyword,
                         @RequestParam(defaultValue = "1") int page,
                         @RequestParam(defaultValue = "10") int size) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String username = auth.getName();
            Optional<Users> userOptional = usersRepository.findByUsername(username);
            
            if (userOptional.isEmpty()) {
                return "redirect:/login";
            }
            
            Users user = userOptional.get();
            
            // Lấy tất cả reviews của user hiện tại
            List<ProductReviews> userReviews = productReviewsRepository.findByUserIdOrderByCreatedAtDesc(user.getUserId());
            
            System.out.println("Total reviews: " + userReviews.size());
            System.out.println("Filter - stars: " + stars + ", date: " + date + ", sort: " + sort + ", keyword: " + keyword);
            
            // Filter theo số sao
            if (stars != null) {
                userReviews = userReviews.stream()
                    .filter(review -> review.getRating() != null && review.getRating().equals(stars))
                    .collect(java.util.stream.Collectors.toList());
                System.out.println("After star filter: " + userReviews.size());
            }
            
            // Filter theo ngày
            if (date != null && !date.isEmpty()) {
                try {
                    java.time.LocalDate filterDate = java.time.LocalDate.parse(date);
                    userReviews = userReviews.stream()
                        .filter(review -> review.getCreatedAt() != null && 
                                review.getCreatedAt().toLocalDate().equals(filterDate))
                        .collect(java.util.stream.Collectors.toList());
                } catch (Exception e) {
                    // Bỏ qua lỗi parse date
                }
            }
            
            // Filter theo keyword
            if (keyword != null && !keyword.trim().isEmpty()) {
                String lowerKeyword = keyword.toLowerCase();
                userReviews = userReviews.stream()
                    .filter(review -> {
                        if (review.getComment() != null && 
                            review.getComment().toLowerCase().contains(lowerKeyword)) {
                            return true;
                        }
                        Products product = productsRepository.findById(review.getProductId()).orElse(null);
                        return product != null && product.getName() != null && 
                               product.getName().toLowerCase().contains(lowerKeyword);
                    })
                    .collect(java.util.stream.Collectors.toList());
            }
            
            // Sắp xếp
            if (sort != null) {
                switch (sort) {
                    case "oldest":
                        userReviews.sort((a, b) -> a.getCreatedAt().compareTo(b.getCreatedAt()));
                        break;
                    case "highest":
                        userReviews.sort((a, b) -> b.getRating().compareTo(a.getRating()));
                        break;
                    case "lowest":
                        userReviews.sort((a, b) -> a.getRating().compareTo(b.getRating()));
                        break;
                    // "latest" là mặc định
                }
            }
            
            // Lấy thông tin sản phẩm cho mỗi review
            Map<Long, Products> productsMap = new HashMap<>();
            Map<Long, String> productImagesMap = new HashMap<>();
            
            for (ProductReviews review : userReviews) {
                if (review.getProductId() != null) {
                    productsRepository.findById(review.getProductId()).ifPresent(product -> {
                        productsMap.put(review.getProductId(), product);
                    });
                    
                    // Lấy hình ảnh chính của sản phẩm
                    List<ProductImages> primaryImages = productImagesRepository.findTop1ByProductIdAndIsPrimaryTrueOrderByImageIdAsc(review.getProductId());
                    if (primaryImages.isEmpty()) {
                        // Nếu không có hình chính, lấy hình đầu tiên
                        List<ProductImages> firstImages = productImagesRepository.findTop1ByProductIdOrderByImageIdAsc(review.getProductId());
                        if (!firstImages.isEmpty()) {
                            productImagesMap.put(review.getProductId(), firstImages.get(0).getImageUrl());
                        }
                    } else {
                        productImagesMap.put(review.getProductId(), primaryImages.get(0).getImageUrl());
                    }
                }
            }
            
            // Pagination
            int totalReviews = userReviews.size();
            int totalPages = totalReviews == 0 ? 1 : (int) Math.ceil((double) totalReviews / size);
            
            // Validate page number
            if (page < 1) page = 1;
            if (page > totalPages) page = totalPages;
            
            // Calculate pagination bounds
            int startIndex = (page - 1) * size;
            int endIndex = Math.min(startIndex + size, totalReviews);
            
            // Get paginated reviews (empty list if no reviews)
            List<ProductReviews> paginatedReviews = totalReviews == 0 ? 
                new ArrayList<>() : userReviews.subList(startIndex, endIndex);
            
            // Get cart count
            Long cartCount = shoppingCartRepository.countByUserId(user.getUserId());
            
            model.addAttribute("user", user);
            model.addAttribute("cartCount", cartCount);
            model.addAttribute("reviews", paginatedReviews);
            model.addAttribute("productsMap", productsMap);
            model.addAttribute("productImagesMap", productImagesMap);
            
            // Add filter params to model
            model.addAttribute("filterStars", stars);
            model.addAttribute("filterDate", date);
            model.addAttribute("filterSort", sort);
            model.addAttribute("filterKeyword", keyword);
            
            // Add pagination info
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", totalPages);
            model.addAttribute("pageSize", size);
            model.addAttribute("totalReviews", totalReviews);
            
            return "customer/reviews";
        } catch (Exception e) {
            e.printStackTrace();
            return "redirect:/customer/dashboard";
        }
    }

    @PostMapping("/customer/reviews/submit")
    public String submitReview(@RequestParam Long productId,
                              @RequestParam Integer rating,
                              @RequestParam String comment) {
        // TODO: Implement review submission
        return "redirect:/customer/reviews?submitted=true";
    }

    @PostMapping("/customer/reviews/edit")
    public String editReview(@RequestParam Long reviewId,
                            @RequestParam Integer rating,
                            @RequestParam String comment) {
        // TODO: Implement review editing
        return "redirect:/customer/reviews?edited=true";
    }
    
    @org.springframework.web.bind.annotation.DeleteMapping("/customer/reviews/delete/{reviewId}")
    @org.springframework.web.bind.annotation.ResponseBody
    public org.springframework.http.ResponseEntity<Map<String, Object>> deleteReview(
            @org.springframework.web.bind.annotation.PathVariable Long reviewId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String username = auth.getName();
            Optional<Users> userOptional = usersRepository.findByUsername(username);
            
            if (userOptional.isEmpty()) {
                response.put("success", false);
                response.put("message", "User not found");
                return org.springframework.http.ResponseEntity.status(401).body(response);
            }
            
            Users user = userOptional.get();
            
            // Kiểm tra xem review có tồn tại và thuộc về user hiện tại không
            Optional<ProductReviews> reviewOptional = productReviewsRepository.findById(reviewId);
            
            if (reviewOptional.isEmpty()) {
                response.put("success", false);
                response.put("message", "Review not found");
                return org.springframework.http.ResponseEntity.status(404).body(response);
            }
            
            ProductReviews review = reviewOptional.get();
            
            if (!review.getUserId().equals(user.getUserId())) {
                response.put("success", false);
                response.put("message", "Unauthorized to delete this review");
                return org.springframework.http.ResponseEntity.status(403).body(response);
            }
            
            // Xóa review
            productReviewsRepository.delete(review);
            
            response.put("success", true);
            response.put("message", "Review deleted successfully");
            return org.springframework.http.ResponseEntity.ok(response);
            
        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "Error deleting review: " + e.getMessage());
            return org.springframework.http.ResponseEntity.status(500).body(response);
        }
    }
}
