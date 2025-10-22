package banhangrong.su25.Repository;

import banhangrong.su25.Entity.ProductReviews;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductReviewsRepository extends JpaRepository<ProductReviews, Long> {
    List<ProductReviews> findByProductIdOrderByCreatedAtDesc(Long productId);

    // Tìm tất cả review cho sản phẩm của seller (JOIN với products)
    @Query("SELECT pr FROM ProductReviews pr JOIN Products p ON pr.productId = p.productId WHERE p.sellerId = :sellerId ORDER BY pr.createdAt DESC")
    List<ProductReviews> findBySellerId(@Param("sellerId") Long sellerId);

    // PERF-01: Pagination cho findBySellerId
    @Query("SELECT pr FROM ProductReviews pr JOIN Products p ON pr.productId = p.productId WHERE p.sellerId = :sellerId")
    Page<ProductReviews> findBySellerId(@Param("sellerId") Long sellerId, Pageable pageable);

    // Tìm review chưa được phản hồi
    @Query("SELECT pr FROM ProductReviews pr JOIN Products p ON pr.productId = p.productId WHERE p.sellerId = :sellerId AND pr.sellerResponse IS NULL ORDER BY pr.createdAt DESC")
    List<ProductReviews> findUnansweredReviews(@Param("sellerId") Long sellerId);

    // PERF-01: Pagination cho findUnansweredReviews
    @Query("SELECT pr FROM ProductReviews pr JOIN Products p ON pr.productId = p.productId WHERE p.sellerId = :sellerId AND pr.sellerResponse IS NULL")
    Page<ProductReviews> findUnansweredReviews(@Param("sellerId") Long sellerId, Pageable pageable);

    // Đếm số review chưa phản hồi
    @Query("SELECT COUNT(pr) FROM ProductReviews pr JOIN Products p ON pr.productId = p.productId WHERE p.sellerId = :sellerId AND pr.sellerResponse IS NULL")
    Long countUnansweredReviews(@Param("sellerId") Long sellerId);

    // Đếm tổng số review của seller
    @Query("SELECT COUNT(pr) FROM ProductReviews pr JOIN Products p ON pr.productId = p.productId WHERE p.sellerId = :sellerId")
    Long countBySellerId(@Param("sellerId") Long sellerId);

    // Filter reviews với nhiều điều kiện
    @Query("SELECT pr FROM ProductReviews pr JOIN Products p ON pr.productId = p.productId " +
           "WHERE p.sellerId = :sellerId " +
           "AND (:status IS NULL OR " +
           "     (:status = 'unanswered' AND pr.sellerResponse IS NULL) OR " +
           "     (:status = 'answered' AND pr.sellerResponse IS NOT NULL)) " +
           "AND (:rating IS NULL OR pr.rating = :rating) " +
           "AND (:fromDate IS NULL OR pr.createdAt >= CAST(:fromDate AS timestamp)) " +
           "AND (:toDate IS NULL OR pr.createdAt <= CAST(:toDate AS timestamp)) " +
           "AND (:productId IS NULL OR pr.productId = :productId) " +
           "AND (:userId IS NULL OR pr.userId = :userId)")
    Page<ProductReviews> findByFilters(@Param("sellerId") Long sellerId,
                                        @Param("status") String status,
                                        @Param("rating") Integer rating,
                                        @Param("fromDate") String fromDate,
                                        @Param("toDate") String toDate,
                                        @Param("productId") Long productId,
                                        @Param("userId") Long userId,
                                        Pageable pageable);

    // Tìm review theo productId
    List<ProductReviews> findByProductId(Long productId);

    // Tìm review theo userId
    List<ProductReviews> findByUserId(Long userId);

    // FIX SEC-03: Check xem review có thuộc về seller này không
    @Query("SELECT CASE WHEN COUNT(pr) > 0 THEN true ELSE false END FROM ProductReviews pr JOIN Products p ON pr.productId = p.productId WHERE pr.reviewId = :reviewId AND p.sellerId = :sellerId")
    boolean existsByReviewIdAndSellerId(@Param("reviewId") Long reviewId, @Param("sellerId") Long sellerId);
}


