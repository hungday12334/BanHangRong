package banhangrong.su25.service;

import banhangrong.su25.Entity.ProductReviews;
import banhangrong.su25.Repository.ProductReviewsRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ProductReviewService {

    private final ProductReviewsRepository productReviewsRepository;

    public ProductReviewService(ProductReviewsRepository productReviewsRepository) {
        this.productReviewsRepository = productReviewsRepository;
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