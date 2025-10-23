package banhangrong.su25.service;

import banhangrong.su25.Entity.ProductReviews;
import banhangrong.su25.Entity.Products;
import banhangrong.su25.Repository.ProductReviewsRepository;
import banhangrong.su25.Repository.ProductsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class RatingCalculationService {

    @Autowired
    private ProductReviewsRepository productReviewsRepository;
    
    @Autowired
    private ProductsRepository productsRepository;

    /**
     * Tính toán và cập nhật rating trung bình cho sản phẩm
     * @param productId ID của sản phẩm cần cập nhật rating
     */
    @Transactional
    public void updateProductAverageRating(Long productId) {
        try {
            // Lấy tất cả reviews của sản phẩm
            List<ProductReviews> reviews = productReviewsRepository.findByProductIdOrderByCreatedAtDesc(productId);
            
            if (reviews.isEmpty()) {
                // Nếu không có review, set rating = 0
                updateProductRating(productId, BigDecimal.ZERO);
                return;
            }
            
            // Tính trung bình rating
            double averageRating = reviews.stream()
                    .mapToInt(ProductReviews::getRating)
                    .average()
                    .orElse(0.0);
            
            // Làm tròn đến 1 chữ số thập phân
            BigDecimal roundedRating = BigDecimal.valueOf(averageRating)
                    .setScale(1, RoundingMode.HALF_UP);
            
            // Cập nhật vào database
            updateProductRating(productId, roundedRating);
            
            System.out.println("Updated average rating for product " + productId + ": " + roundedRating);
            
        } catch (Exception e) {
            System.err.println("Error updating average rating for product " + productId + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Cập nhật rating trung bình vào database
     */
    private void updateProductRating(Long productId, BigDecimal averageRating) {
        Products product = productsRepository.findById(productId).orElse(null);
        if (product != null) {
            product.setAverageRating(averageRating);
            productsRepository.save(product);
        }
    }
    
    /**
     * Lấy thống kê rating chi tiết cho sản phẩm
     */
    public RatingStats getProductRatingStats(Long productId) {
        List<ProductReviews> reviews = productReviewsRepository.findByProductIdOrderByCreatedAtDesc(productId);
        
        if (reviews.isEmpty()) {
            return new RatingStats(0.0, 0, new int[]{0, 0, 0, 0, 0});
        }
        
        // Tính trung bình
        double averageRating = reviews.stream()
                .mapToInt(ProductReviews::getRating)
                .average()
                .orElse(0.0);
        
        // Làm tròn đến 1 chữ số thập phân
        double roundedAverage = Math.round(averageRating * 10.0) / 10.0;
        
        // Tính phân phối rating (1-5 sao)
        int[] ratingDistribution = new int[5];
        for (ProductReviews review : reviews) {
            if (review.getRating() >= 1 && review.getRating() <= 5) {
                ratingDistribution[review.getRating() - 1]++;
            }
        }
        
        return new RatingStats(roundedAverage, reviews.size(), ratingDistribution);
    }
    
    /**
     * Class để chứa thống kê rating
     */
    public static class RatingStats {
        private final double averageRating;
        private final int totalReviews;
        private final int[] ratingDistribution;
        
        public RatingStats(double averageRating, int totalReviews, int[] ratingDistribution) {
            this.averageRating = averageRating;
            this.totalReviews = totalReviews;
            this.ratingDistribution = ratingDistribution;
        }
        
        public double getAverageRating() {
            return averageRating;
        }
        
        public int getTotalReviews() {
            return totalReviews;
        }
        
        public int[] getRatingDistribution() {
            return ratingDistribution;
        }
    }
}
