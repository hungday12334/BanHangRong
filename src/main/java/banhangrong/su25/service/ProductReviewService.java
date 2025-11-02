package banhangrong.su25.service;

import banhangrong.su25.Entity.ProductReviews;
import banhangrong.su25.Entity.Users;
import banhangrong.su25.Repository.ProductReviewsRepository;
import banhangrong.su25.Repository.UsersRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ProductReviewService {

    private final ProductReviewsRepository productReviewsRepository;
    private final UsersRepository usersRepository;

    public ProductReviewService(ProductReviewsRepository productReviewsRepository, UsersRepository usersRepository) {
        this.productReviewsRepository = productReviewsRepository;
        this.usersRepository = usersRepository;
    }

    public List<ProductReviews> getSellerReviews(Long sellerId) {
        return productReviewsRepository.findBySellerId(sellerId);
    }

    // PERF-01: Pagination support cho all reviews
    public Page<ProductReviews> getSellerReviews(Long sellerId, Pageable pageable) {
        return productReviewsRepository.findBySellerId(sellerId, pageable);
    }

    public List<ProductReviews> getUnansweredReviews(Long sellerId) {
        return productReviewsRepository.findUnansweredReviews(sellerId);
    }

    // PERF-01: Pagination support cho unanswered reviews
    public Page<ProductReviews> getUnansweredReviews(Long sellerId, Pageable pageable) {
        return productReviewsRepository.findUnansweredReviews(sellerId, pageable);
    }

    public Long getUnansweredReviewCount(Long sellerId) {
        return productReviewsRepository.countUnansweredReviews(sellerId);
    }

    public Long getTotalReviewCount(Long sellerId) {
        return productReviewsRepository.countBySellerId(sellerId);
    }

    /**
     * Filter reviews with various criteria and populate user full names
     */
    public Page<ProductReviews> getFilteredReviews(Long sellerId, String status, Integer ratingFrom, Integer ratingTo,
                                                     String fromDate, String toDate, Long productId,
                                                     String customerName, Pageable pageable) {
        Page<ProductReviews> reviewsPage = productReviewsRepository.findByFilters(
            sellerId, status, ratingFrom, ratingTo, fromDate, toDate, productId, customerName, pageable
        );

        // Populate user full names for each review
        reviewsPage.forEach(this::populateUserFullName);

        return reviewsPage;
    }

    /**
     * Populate user full name for a review
     */
    private void populateUserFullName(ProductReviews review) {
        if (review.getUserId() != null) {
            Optional<Users> userOpt = usersRepository.findById(review.getUserId());
            userOpt.ifPresent(user -> {
                review.setUserFullName(user.getFullName() != null ? user.getFullName() : user.getUsername());
                review.setUsername(user.getUsername());
            });
        }
    }

    public Optional<ProductReviews> getReviewById(Long reviewId) {
        return productReviewsRepository.findById(reviewId);
    }

    public ProductReviews addSellerResponse(Long reviewId, String response) {
        Optional<ProductReviews> reviewOpt = productReviewsRepository.findById(reviewId);
        if (reviewOpt.isPresent()) {
            ProductReviews review = reviewOpt.get();
            review.setSellerResponse(response);
            return productReviewsRepository.save(review);
        }
        throw new IllegalArgumentException("Review not found with id: " + reviewId);
    }

    /**
     * FIX SEC-03: Validate xem review có thuộc về seller này không
     * @param reviewId ID của review
     * @param sellerId ID của seller
     * @return true nếu review thuộc về seller này
     */
    public boolean isReviewOwnedBySeller(Long reviewId, Long sellerId) {
        return productReviewsRepository.existsByReviewIdAndSellerId(reviewId, sellerId);
    }
}