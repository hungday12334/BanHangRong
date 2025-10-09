package banhangrong.su25.service;

import banhangrong.su25.Entity.ProductReviews;
import banhangrong.su25.Repository.ProductReviewsRepository;
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

    public List<ProductReviews> getUnansweredReviews(Long sellerId) {
        return productReviewsRepository.findUnansweredReviews(sellerId);
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
        throw new RuntimeException("Review not found with id: " + reviewId);
    }
}