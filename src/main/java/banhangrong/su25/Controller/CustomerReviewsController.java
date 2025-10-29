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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
    public String reviews(
            @RequestParam(defaultValue = "newest") String sortBy,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String username = auth.getName();
            Optional<Users> userOptional = usersRepository.findByUsername(username);
            
            if (userOptional.isEmpty()) {
                return "redirect:/login";
            }
            
            Users user = userOptional.get();
            
            // Determine sorting
            Sort sort;
            switch (sortBy) {
                case "oldest":
                    sort = Sort.by("createdAt").ascending();
                    break;
                case "rating_high":
                    sort = Sort.by("rating").descending().and(Sort.by("createdAt").descending());
                    break;
                case "rating_low":
                    sort = Sort.by("rating").ascending().and(Sort.by("createdAt").descending());
                    break;
                case "newest":
                default:
                    sort = Sort.by("createdAt").descending();
                    break;
            }
            
            // Create pageable with sorting
            Pageable pageable = PageRequest.of(page, size, sort);
            
            // Lấy reviews của user hiện tại với pagination
            Page<ProductReviews> reviewsPage = productReviewsRepository.findByUserId(user.getUserId(), pageable);
            List<ProductReviews> userReviews = reviewsPage.getContent();
            
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
            
            // Get cart count
            Long cartCount = shoppingCartRepository.countByUserId(user.getUserId());
            
            model.addAttribute("user", user);
            model.addAttribute("cartCount", cartCount);
            model.addAttribute("reviews", userReviews);
            model.addAttribute("productsMap", productsMap);
            model.addAttribute("productImagesMap", productImagesMap);
            
            // Pagination attributes
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", reviewsPage.getTotalPages());
            model.addAttribute("totalItems", reviewsPage.getTotalElements());
            model.addAttribute("pageSize", size);
            model.addAttribute("sortBy", sortBy);
            
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
}
