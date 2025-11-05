package banhangrong.su25.Controller;

import banhangrong.su25.Entity.ProductReviews;
import banhangrong.su25.Entity.Users;
import banhangrong.su25.Repository.ProductReviewsRepository;
import banhangrong.su25.Repository.UsersRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/reviews")
public class ReviewsApiController {

    private final ProductReviewsRepository productReviewsRepository;
    private final UsersRepository usersRepository;

    public ReviewsApiController(ProductReviewsRepository productReviewsRepository, UsersRepository usersRepository) {
        this.productReviewsRepository = productReviewsRepository;
        this.usersRepository = usersRepository;
    }

    /**
     * DELETE /api/reviews/{reviewId}
     * Delete review of the current user
     */
    @DeleteMapping("/{reviewId}")
    public ResponseEntity<Map<String, Object>> deleteReview(@PathVariable Long reviewId) {
        try {
            // Get current user from authentication
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated() || auth.getName().equals("anonymousUser")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("success", false, "message", "Unauthorized"));
            }

            String username = auth.getName();
            Optional<Users> userOptional = usersRepository.findByUsername(username);
            if (userOptional.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("success", false, "message", "User not found"));
            }

            Users currentUser = userOptional.get();

            // Find review
            Optional<ProductReviews> reviewOptional = productReviewsRepository.findById(reviewId);
            if (reviewOptional.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("success", false, "message", "Review not found"));
            }

            ProductReviews review = reviewOptional.get();

            // Check permission: only allow user to delete their own reviews
            Long reviewUserId = review.getUserId();
            Long currentUserId = currentUser.getUserId();
            if (reviewUserId == null || currentUserId == null || !reviewUserId.equals(currentUserId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("success", false, "message", "You can only delete your own reviews"));
            }

            // Delete review
            productReviewsRepository.delete(review);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Review deleted successfully");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Failed to delete review: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * PUT /api/reviews/{reviewId}
     * Update review of the current user
     */
    @PutMapping("/{reviewId}")
    public ResponseEntity<Map<String, Object>> updateReview(
            @PathVariable Long reviewId,
            @RequestBody Map<String, Object> request) {
        try {
            // Get current user from authentication
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated() || auth.getName().equals("anonymousUser")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("success", false, "message", "Unauthorized"));
            }

            String username = auth.getName();
            Optional<Users> userOptional = usersRepository.findByUsername(username);
            if (userOptional.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("success", false, "message", "User not found"));
            }

            Users currentUser = userOptional.get();

            // Find review
            Optional<ProductReviews> reviewOptional = productReviewsRepository.findById(reviewId);
            if (reviewOptional.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("success", false, "message", "Review not found"));
            }

            ProductReviews review = reviewOptional.get();

            // Check permission: only allow user to update their own reviews
            Long reviewUserId = review.getUserId();
            Long currentUserId = currentUser.getUserId();
            if (reviewUserId == null || currentUserId == null || !reviewUserId.equals(currentUserId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("success", false, "message", "You can only update your own reviews"));
            }

            // Update review information
            if (request.containsKey("rating")) {
                Object ratingObj = request.get("rating");
                if (ratingObj instanceof Number) {
                    review.setRating(((Number) ratingObj).intValue());
                }
            }

            if (request.containsKey("serviceRating")) {
                Object serviceRatingObj = request.get("serviceRating");
                if (serviceRatingObj instanceof Number) {
                    review.setServiceRating(((Number) serviceRatingObj).intValue());
                }
            }

            if (request.containsKey("comment")) {
                review.setComment(request.get("comment").toString());
            }

            // Save updated review
            ProductReviews savedReview = productReviewsRepository.save(review);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Review updated successfully");
            response.put("review", savedReview);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Failed to update review: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
}

