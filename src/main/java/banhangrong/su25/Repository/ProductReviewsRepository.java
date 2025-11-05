package banhangrong.su25.Repository;

import banhangrong.su25.Entity.ProductReviews;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ProductReviewsRepository extends JpaRepository<ProductReviews, Long> {
    List<ProductReviews> findByProductIdOrderByCreatedAtDesc(Long productId);

    // Find all reviews for seller's products (JOIN with products)
    @Query("SELECT pr FROM ProductReviews pr JOIN Products p ON pr.productId = p.productId WHERE p.sellerId = :sellerId ORDER BY pr.createdAt DESC")
    List<ProductReviews> findBySellerId(@Param("sellerId") Long sellerId);

    // PERF-01: Pagination for findBySellerId
    @Query("SELECT pr FROM ProductReviews pr JOIN Products p ON pr.productId = p.productId WHERE p.sellerId = :sellerId")
    Page<ProductReviews> findBySellerId(@Param("sellerId") Long sellerId, Pageable pageable);

    // Find reviews that haven't been responded to
    @Query("SELECT pr FROM ProductReviews pr JOIN Products p ON pr.productId = p.productId WHERE p.sellerId = :sellerId AND pr.sellerResponse IS NULL ORDER BY pr.createdAt DESC")
    List<ProductReviews> findUnansweredReviews(@Param("sellerId") Long sellerId);

    // PERF-01: Pagination for findUnansweredReviews
    @Query("SELECT pr FROM ProductReviews pr JOIN Products p ON pr.productId = p.productId WHERE p.sellerId = :sellerId AND pr.sellerResponse IS NULL")
    Page<ProductReviews> findUnansweredReviews(@Param("sellerId") Long sellerId, Pageable pageable);

    // Count number of unanswered reviews
    @Query("SELECT COUNT(pr) FROM ProductReviews pr JOIN Products p ON pr.productId = p.productId WHERE p.sellerId = :sellerId AND pr.sellerResponse IS NULL")
    Long countUnansweredReviews(@Param("sellerId") Long sellerId);

    // Count total number of reviews for seller
    @Query("SELECT COUNT(pr) FROM ProductReviews pr JOIN Products p ON pr.productId = p.productId WHERE p.sellerId = :sellerId")
    Long countBySellerId(@Param("sellerId") Long sellerId);

    // Filter reviews with multiple conditions (updated with rating range and customer name)
    @Query("SELECT pr FROM ProductReviews pr " +
           "JOIN Products p ON pr.productId = p.productId " +
           "LEFT JOIN Users u ON pr.userId = u.userId " +
           "WHERE p.sellerId = :sellerId " +
           "AND (:status IS NULL OR " +
           "     (:status = 'unanswered' AND pr.sellerResponse IS NULL) OR " +
           "     (:status = 'answered' AND pr.sellerResponse IS NOT NULL)) " +
           "AND (:ratingFrom IS NULL OR pr.rating >= :ratingFrom) " +
           "AND (:ratingTo IS NULL OR pr.rating <= :ratingTo) " +
           "AND (:fromDate IS NULL OR pr.createdAt >= CAST(:fromDate AS timestamp)) " +
           "AND (:toDate IS NULL OR pr.createdAt <= CAST(:toDate AS timestamp)) " +
           "AND (:productId IS NULL OR pr.productId = :productId) " +
           "AND (:customerName IS NULL OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :customerName, '%')) OR LOWER(u.username) LIKE LOWER(CONCAT('%', :customerName, '%')))")
    Page<ProductReviews> findByFilters(@Param("sellerId") Long sellerId,
                                        @Param("status") String status,
                                        @Param("ratingFrom") Integer ratingFrom,
                                        @Param("ratingTo") Integer ratingTo,
                                        @Param("fromDate") String fromDate,
                                        @Param("toDate") String toDate,
                                        @Param("productId") Long productId,
                                        @Param("customerName") String customerName,
                                        Pageable pageable);

    // Find review by productId
    List<ProductReviews> findByProductId(Long productId);

    // Find review by userId
    List<ProductReviews> findByUserId(Long userId);
    
    // Find review by userId with pagination and sorting
    Page<ProductReviews> findByUserId(Long userId, Pageable pageable);
    
    // Custom query for customer reviews with filters including search
    @Query("SELECT pr FROM ProductReviews pr " +
           "LEFT JOIN Products p ON pr.productId = p.productId " +
           "WHERE pr.userId = :userId " +
           "AND (:rating IS NULL OR pr.rating = :rating) " +
           "AND (:fromDate IS NULL OR pr.createdAt >= :fromDate) " +
           "AND (:toDate IS NULL OR pr.createdAt <= :toDate) " +
           "AND (:search IS NULL OR :search = '' OR LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<ProductReviews> findByUserIdWithFilters(@Param("userId") Long userId,
                                                   @Param("rating") Integer rating,
                                                   @Param("fromDate") LocalDateTime fromDate,
                                                   @Param("toDate") LocalDateTime toDate,
                                                   @Param("search") String search,
                                                   Pageable pageable);

    // Find review by userId and sort by creation time descending
    List<ProductReviews> findByUserIdOrderByCreatedAtDesc(Long userId);

    // Check existence of review by order item
    boolean existsByOrderItemId(Long orderItemId);

    // Check existence of review by user and product
    boolean existsByUserIdAndProductId(Long userId, Long productId);

    // FIX SEC-03: Check if review belongs to this seller
    @Query("SELECT CASE WHEN COUNT(pr) > 0 THEN true ELSE false END FROM ProductReviews pr JOIN Products p ON pr.productId = p.productId WHERE pr.reviewId = :reviewId AND p.sellerId = :sellerId")
    boolean existsByReviewIdAndSellerId(@Param("reviewId") Long reviewId, @Param("sellerId") Long sellerId);
}
