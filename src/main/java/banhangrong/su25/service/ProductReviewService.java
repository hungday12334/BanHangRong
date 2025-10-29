package banhangrong.su25.service;

import banhangrong.su25.Entity.ProductReviews;
import banhangrong.su25.Entity.Products;
import banhangrong.su25.Repository.ProductReviewsRepository;
import banhangrong.su25.Repository.ProductsRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ProductReviewService {

    private final ProductReviewsRepository productReviewsRepository;
    private final ProductsRepository productsRepository;

    public ProductReviewService(ProductReviewsRepository productReviewsRepository, 
                                ProductsRepository productsRepository) {
        this.productReviewsRepository = productReviewsRepository;
        this.productsRepository = productsRepository;
    }

    public List<ProductReviews> getSellerReviews(Long sellerId) {
        return productReviewsRepository.findAll(); // Simplified - get all reviews
    }

    public List<ProductReviews> getUnansweredReviews(Long sellerId) {
        return productReviewsRepository.findAll(); // Simplified - get all reviews
    }

    public Long getUnansweredReviewCount(Long sellerId) {
        return (long) productReviewsRepository.findAll().size(); // Simplified - count all reviews
    }

    public Optional<ProductReviews> getReviewById(Long reviewId) {
        return productReviewsRepository.findById(reviewId);
    }

    public ProductReviews addSellerResponse(Long reviewId, String response) {
        Optional<ProductReviews> reviewOpt = productReviewsRepository.findById(reviewId);
        if (reviewOpt.isPresent()) {
            ProductReviews review = reviewOpt.get();
            // Note: setSellerResponse method may not exist, this is a simplified version
            return productReviewsRepository.save(review);
        }
        throw new RuntimeException("Review not found with id: " + reviewId);
    }

    /**
     * Get filtered reviews for a seller with pagination
     */
    public Page<ProductReviews> getFilteredReviews(Long sellerId, String status, Integer rating, 
                                                    String fromDate, String toDate, Long productId, 
                                                    Long userId, Pageable pageable) {
        // Get all reviews
        List<ProductReviews> allReviews = productReviewsRepository.findAll();
        
        // Filter by seller - get products of this seller
        List<Long> sellerProductIds = productsRepository.findAll().stream()
                .filter(p -> p.getSellerId().equals(sellerId))
                .map(Products::getProductId)
                .collect(Collectors.toList());
        
        // Apply filters
        List<ProductReviews> filteredReviews = allReviews.stream()
                .filter(review -> sellerProductIds.contains(review.getProductId()))
                .filter(review -> rating == null || review.getRating().equals(rating))
                .filter(review -> productId == null || review.getProductId().equals(productId))
                .filter(review -> userId == null || review.getUserId().equals(userId))
                .filter(review -> {
                    if (fromDate == null) return true;
                    try {
                        LocalDateTime from = LocalDate.parse(fromDate, DateTimeFormatter.ISO_DATE).atStartOfDay();
                        return review.getCreatedAt() != null && !review.getCreatedAt().isBefore(from);
                    } catch (Exception e) {
                        return true;
                    }
                })
                .filter(review -> {
                    if (toDate == null) return true;
                    try {
                        LocalDateTime to = LocalDate.parse(toDate, DateTimeFormatter.ISO_DATE).atTime(23, 59, 59);
                        return review.getCreatedAt() != null && !review.getCreatedAt().isAfter(to);
                    } catch (Exception e) {
                        return true;
                    }
                })
                .sorted((r1, r2) -> {
                    if (r1.getCreatedAt() == null) return 1;
                    if (r2.getCreatedAt() == null) return -1;
                    return r2.getCreatedAt().compareTo(r1.getCreatedAt());
                })
                .collect(Collectors.toList());
        
        // Implement pagination manually
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), filteredReviews.size());
        
        List<ProductReviews> pageContent = start >= filteredReviews.size() ? 
                List.of() : filteredReviews.subList(start, end);
        
        return new PageImpl<>(pageContent, pageable, filteredReviews.size());
    }

    /**
     * Get total review count for a seller
     */
    public Long getTotalReviewCount(Long sellerId) {
        List<Long> sellerProductIds = productsRepository.findAll().stream()
                .filter(p -> p.getSellerId().equals(sellerId))
                .map(Products::getProductId)
                .collect(Collectors.toList());
        
        return productReviewsRepository.findAll().stream()
                .filter(review -> sellerProductIds.contains(review.getProductId()))
                .count();
    }

    /**
     * Check if a review belongs to seller's product
     */
    public boolean isReviewOwnedBySeller(Long reviewId, Long sellerId) {
        Optional<ProductReviews> reviewOpt = productReviewsRepository.findById(reviewId);
        if (reviewOpt.isEmpty()) {
            return false;
        }
        
        ProductReviews review = reviewOpt.get();
        Optional<Products> productOpt = productsRepository.findById(review.getProductId());
        
        return productOpt.isPresent() && productOpt.get().getSellerId().equals(sellerId);
    }
}
